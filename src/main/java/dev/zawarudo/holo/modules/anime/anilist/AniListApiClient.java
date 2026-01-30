package dev.zawarudo.holo.modules.anime.anilist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;

import java.util.Map;

/**
 * TODO:
 *  - https://netflix.github.io/dgs/advanced/java-client/
 *  - https://spec.graphql.org/draft/
 *  - https://graphql.org/learn/response/
 */
public final class AniListApiClient {

    private static final String BASE_URL = "https://graphql.anilist.co";

    private static final String Q_SEARCH_ANIME = """
            query ($search: String, $perPage: Int) {
              Page(perPage: $perPage) {
                media(search: $search, type: ANIME) {
                  id
                  siteUrl
                  title { romaji english native }
                  format
                  status
                  episodes
                  averageScore
                  coverImage { large }
                }
              }
            }
            """;

    private static final String Q_SEARCH_MANGA = """
            query ($search: String, $perPage: Int) {
              Page(perPage: $perPage) {
                media(search: $search, type: MANGA) {
                  id
                  siteUrl
                  title { romaji english native }
                  format
                  status
                  chapters
                  volumes
                  averageScore
                  coverImage { large }
                }
              }
            }
            """;

    public JsonObject searchAnimeRaw(String query, int limit) throws APIException {
        return request(Q_SEARCH_ANIME, variablesSearch(query, limit));
    }

    public JsonObject searchMangaRaw(String query, int limit) throws APIException {
        return request(Q_SEARCH_MANGA, variablesSearch(query, limit));
    }

    private JsonObject variablesSearch(String query, int limit) {
        JsonObject vars = new JsonObject();
        vars.addProperty("search", query);
        vars.addProperty("perPage", limit);
        return vars;
    }

    private JsonObject request(String gqlQuery, JsonObject variables) throws APIException {
        JsonObject body = new JsonObject();
        body.addProperty("query", gqlQuery);
        body.add("variables", variables);

        try {
            JsonObject res = HoloHttp.postJsonObject(BASE_URL, body, Map.of());

            // GraphQL errors can come back with HTTP 200
            JsonArray errors = res.has("errors") ? res.getAsJsonArray("errors") : null;
            if (errors != null && !errors.isEmpty()) {
                throw new APIException("AniList GraphQL error: " + firstErrorMessage(errors));
            }

            if (!res.has("data") || res.get("data").isJsonNull()) {
                throw new APIException("AniList response did not contain data");
            }

            return res.getAsJsonObject("data");
        } catch (HttpStatusException e) {
            throw new APIException("AniList HTTP error: " + e.getStatusCode() + " (" + e.getUrl() + ")", e);
        } catch (HttpTransportException e) {
            throw new APIException("AniList transport error", e);
        }
    }

    private String firstErrorMessage(JsonArray errors) {
        JsonElement e0 = errors.get(0);
        if (e0 != null && e0.isJsonObject()) {
            JsonObject o = e0.getAsJsonObject();
            if (o.has("message")) return o.get("message").getAsString();
        }
        return "Unknown error";
    }
}
