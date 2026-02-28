package dev.zawarudo.holo.modules.anime.jikan;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.modules.anime.jikan.model.*;
import dev.zawarudo.holo.modules.anime.jikan.model.Character;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.HoloRateLimiter;
import dev.zawarudo.holo.utils.TypeTokenUtils;
import dev.zawarudo.holo.utils.exceptions.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

// TODO: Make JikanApiClient an instance for JikanProvider
public final class JikanApiClient {

    private static final String BASE_URL = "https://api.jikan.moe/v4";
    private static int limit = 10;

    private static final HoloRateLimiter RATE_LIMITER = new HoloRateLimiter(1);

    private JikanApiClient() {
    }

    /**
     * Sets the limit of the results. By default, the limit is 10.
     *
     * @param limit The limit of the results.
     */
    public static void setLimit(int limit) {
        // Jikan has a configured max value of 25
        JikanApiClient.limit = Math.min(25, limit);
    }

    /**
     * Search for an anime using its name.
     *
     * @param name The name of the anime to search for.
     * @return A {@link List} of {@link Anime} objects.
     */
    public static List<Anime> searchAnime(String name) throws APIException, InvalidRequestException {
        String url = BASE_URL + "/anime?q=" + Formatter.encodeUrl(name) + "&limit=" + limit;

        RATE_LIMITER.acquire();

        JsonObject json = fetchJsonOrThrow(url);
        JsonArray array = json.getAsJsonArray("data");
        return new Gson().fromJson(array, TypeTokenUtils.getListTypeToken(Anime.class));
    }

    /**
     * Search for a manga using its name.
     *
     * @param name The name of the manga to search for.
     * @return A {@link List} of {@link Manga} objects.
     */
    public static List<Manga> searchManga(String name) throws InvalidRequestException, APIException {
        String url = BASE_URL + "/manga?q=" + Formatter.encodeUrl(name) + "&limit=" + limit;

        RATE_LIMITER.acquire();

        JsonObject json = fetchJsonOrThrow(url);
        JsonArray array = json.getAsJsonArray("data");
        return new Gson().fromJson(array, TypeTokenUtils.getListTypeToken(Manga.class));
    }

    /**
     * Fetches an {@link Anime} object using the id from MyAnimeList.
     *
     * @param id MyAnimeList id of the anime.
     * @return An {@link Anime} object.
     */
    public static Anime getAnime(int id) throws InvalidIdException, APIException {
        if (id <= 0) throw new InvalidIdException("Id must be at least 1.");

        String url = BASE_URL + "/anime/" + id;

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            return new Gson().fromJson(json.getAsJsonObject("data"), Anime.class);
        } catch (InvalidRequestException e) {
            throw new InvalidIdException("Invalid anime id: " + id, e);
        }
    }

    /**
     * Fetches a {@link Manga} object using the id from MyAnimeList.
     *
     * @param id MyAnimeList id of the manga.
     * @return A {@link Manga} object.
     */
    public static Manga getManga(int id) throws APIException, InvalidIdException {
        if (id <= 0) throw new InvalidIdException("Id must be at least 1.");

        String url = BASE_URL + "/manga/" + id;

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            return new Gson().fromJson(json.getAsJsonObject("data"), Manga.class);
        } catch (InvalidRequestException e) {
            throw new InvalidIdException("Invalid manga id: " + id, e);
        }
    }

    /**
     * Fetches a list of the best anime according to MyAnimeList.
     *
     * @param limit The number of anime to retrieve.
     */
    public static List<Anime> getTopAnime(int limit) throws APIException {
        String url = BASE_URL + "/top/anime?page=";

        List<Anime> topAnime = new ArrayList<>();
        int count = 0;
        int page = 1;

        Type type = TypeTokenUtils.getListTypeToken(Anime.class);

        while (count < limit) {
            RATE_LIMITER.acquire();

            JsonObject json;
            try {
                json = fetchJsonOrThrow(url + page);
            } catch (InvalidRequestException e) {
                // This should never happen
                throw new APIException("Unexpected Jikan error while fetching top anime page " + page, e);
            }

            List<Anime> temp = new Gson().fromJson(json.getAsJsonArray("data"), type);

            for (Anime a : temp) {
                if (count >= limit) break;
                topAnime.add(a);
                count++;
            }

            page++;
        }
        return topAnime;
    }

    /**
     * Fetches the relations of a given media.
     *
     * @param id   The id of the media.
     * @param type The type of the media.
     * @return A {@link List} of {@link Related} objects.
     */
    public static List<Related> getRelated(int id, MediaType type) throws APIException, InvalidRequestException {
        String url = BASE_URL + "/" + type.getType() + "/" + id + "/relations";

        RATE_LIMITER.acquire();

        JsonObject json = fetchJsonOrThrow(url);
        JsonArray array = json.getAsJsonArray("data");
        return new Gson().fromJson(array, TypeTokenUtils.getListTypeToken(Related.class));
    }

    /**
     * Fetches a list of {@link Anime} of the current anime season.
     *
     * @return A {@link List} of {@link Anime} objects.
     */
    public static List<Anime> getSeason() throws APIException {
        String url = BASE_URL + "/seasons/now";
        return collectAnime(url);
    }

    /**
     * Fetches a list of {@link Anime} of the given anime season.
     *
     * @param season The season to get the anime from.
     * @param year   The year to get the anime from.
     */
    public static List<Anime> getSeason(Season season, int year) throws APIException {
        String url = BASE_URL + "/seasons/" + year + "/" + season;
        return collectAnime(url);
    }

    private static List<Anime> collectAnime(String url) throws APIException {
        List<Anime> list = new ArrayList<>();
        Type type = TypeTokenUtils.getListTypeToken(Anime.class);

        int page = 1;
        boolean hasNext;

        do {
            RATE_LIMITER.acquire();

            JsonObject obj;
            try {
                obj = fetchJsonOrThrow(url + "?page=" + page++);
            } catch (InvalidRequestException e) {
                throw new APIException("Unexpected Jikan error while paging seasons endpoint.", e);
            }

            list.addAll(new Gson().fromJson(obj.getAsJsonArray("data"), type));
            hasNext = obj.getAsJsonObject("pagination").get("has_next_page").getAsBoolean();

        } while (hasNext);

        return list;
    }

    /**
     * Searches for a {@link Character} using its name.
     *
     * @param name The name of the character to search for.
     * @return A {@link List} of {@link Character} objects.
     */
    public static List<Character> searchCharacter(String name) throws APIException, InvalidRequestException {
        String url = BASE_URL + "/characters?q=" + Formatter.encodeUrl(name) + "&order_by=favorites&sort=desc";

        RATE_LIMITER.acquire();

        JsonObject json = fetchJsonOrThrow(url);
        JsonArray array = json.getAsJsonArray("data");
        return new Gson().fromJson(array, TypeTokenUtils.getListTypeToken(Character.class));
    }

    /**
     * Returns a character from a given id.
     *
     * @param id MyAnimeList id of the character. See also <a href="https://myanimelist.net/character.php">MyAnimeList</a>.
     * @return A {@link Character} object.
     */
    public static Character getCharacter(long id) throws InvalidIdException, APIException {
        if (id <= 0) throw new InvalidIdException("Id must be at least 1.");

        String url = BASE_URL + "/characters/" + id;

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            return new Gson().fromJson(json.getAsJsonObject("data"), Character.class);
        } catch (InvalidRequestException e) {
            throw new InvalidIdException("Invalid character id: " + id, e);
        }
    }

    /**
     * Fetches the anime or manga in which a specified character had an appearance.
     *
     * @param id   MyAnimeList id of the character
     * @param type Anime or manga. See also {@link MediaType}
     * @return A {@link List} of {@link Appearance} objects.
     */
    public static List<Appearance> getCharacter(long id, MediaType type) throws APIException, InvalidIdException {
        if (id <= 0) throw new InvalidIdException("Id must be at least 1.");

        String url = BASE_URL + "/characters/" + id + "/" + type.getType();

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            JsonArray array = json.getAsJsonArray("data");
            return new Gson().fromJson(array, TypeTokenUtils.getListTypeToken(Appearance.class));
        } catch (InvalidRequestException e) {
            throw new InvalidIdException("Invalid character id: " + id, e);
        }
    }

    /**
     * Fetches a random {@link Anime} from MyAnimeList.
     *
     * @return A {@link Anime} object.
     */
    public static Anime getRandomAnime() throws APIException {
        String url = BASE_URL + "/random/anime";

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            return new Gson().fromJson(json.getAsJsonObject("data"), Anime.class);
        } catch (InvalidRequestException e) {
            throw new APIException("Unexpected Jikan error while fetching random anime.", e);
        }
    }

    /**
     * Fetches a random {@link Manga} from MyAnimeList.
     *
     * @return A {@link Manga} object.
     */
    public static Manga getRandomManga() throws APIException {
        String url = BASE_URL + "/random/manga";

        RATE_LIMITER.acquire();

        try {
            JsonObject json = fetchJsonOrThrow(url);
            return new Gson().fromJson(json.getAsJsonObject("data"), Manga.class);
        } catch (InvalidRequestException e) {
            throw new APIException("Unexpected Jikan error while fetching random manga.", e);
        }
    }

    private static JsonObject fetchJsonOrThrow(String url) throws InvalidRequestException, APIException {
        try {
            return HoloHttp.getJsonObject(url);
        } catch (HttpStatusException ex) {
            int code = ex.getStatusCode();

            if (code == 429) {
                throw new APIException("429 Too Many Requests (Jikan rate limit).", ex);
            }

            // Bad request
            if (code >= 400 && code < 500) {
                throw new InvalidRequestException(code + " Client error from Jikan.", ex);
            }

            // Server-side errors
            throw new APIException(code + " Server error from Jikan.", ex);
        } catch (HttpTransportException ex) {
            // Network/timeout/interrupted
            throw new APIException("Transport error while contacting Jikan.", ex);
        }
    }
}