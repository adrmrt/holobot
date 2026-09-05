package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.HoloRateLimiter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Client for the official MyAnimeList API v2. Public search/detail endpoints only need the
 * {@code X-MAL-CLIENT-ID} header, no OAuth2 token.
 */
final class MalApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(MalApiClient.class);

    private static final String BASE_URL = "https://api.myanimelist.net/v2";

    // Must list every field MalDtos.Anime declares (its wire key, i.e. after @SerializedName).
    // MalDtosFieldsConsistencyTest asserts these two stay in sync with the DTO shape.
    static final String ANIME_FIELDS = "id,title,main_picture,alternative_titles,synopsis,mean,rank,"
        + "media_type,status,num_episodes,start_season,genres,studios,source";

    // Must list every field MalDtos.Manga declares. authors needs the nested selector because
    // MAL only returns author names when explicitly asked via authors{first_name,last_name}.
    static final String MANGA_FIELDS = "id,title,main_picture,alternative_titles,synopsis,mean,rank,"
        + "media_type,status,num_chapters,num_volumes,genres,authors{first_name,last_name}";

    private static final Gson GSON = new Gson();

    private final String clientId;
    private final HoloRateLimiter rateLimiter;

    MalApiClient(String clientId) {
        this.clientId = Objects.requireNonNull(clientId, "clientId");
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        this.rateLimiter = new HoloRateLimiter(2);
    }

    List<MalDtos.Anime> searchAnime(String query, int limit) throws APIException, InvalidRequestException {
        String url = BASE_URL + "/anime?q=" + Formatter.encodeUrl(query)
            + "&limit=" + limit + "&fields=" + encodeFields(ANIME_FIELDS);

        Type type = TypeToken.getParameterized(MalDtos.SearchResult.class, MalDtos.Anime.class).getType();
        return toNodeList(fetch(url, type));
    }

    List<MalDtos.Manga> searchManga(String query, int limit) throws APIException, InvalidRequestException {
        String url = BASE_URL + "/manga?q=" + Formatter.encodeUrl(query)
            + "&limit=" + limit + "&fields=" + encodeFields(MANGA_FIELDS);

        Type type = TypeToken.getParameterized(MalDtos.SearchResult.class, MalDtos.Manga.class).getType();
        return toNodeList(fetch(url, type));
    }

    private <T> List<T> toNodeList(MalDtos.SearchResult<T> result) {
        if (result == null || result.data() == null) return List.of();
        return result.data().stream().map(MalDtos.Node::node).toList();
    }

    private <T> T fetch(String url, Type type) throws APIException, InvalidRequestException {
        rateLimiter.acquire();

        String body;
        try {
            body = HoloHttp.getString(url, Map.of("X-MAL-CLIENT-ID", clientId));
            LOGGER.debug("MAL response: {}", body);
        } catch (HttpStatusException e) {
            int code = e.getStatusCode();
            LOGGER.debug("MAL error response ({}): {}", code, e.getBodySnippet());

            if (code == 429) {
                throw new APIException("429 Too Many Requests (MAL rate limit).", e);
            }
            if (code >= 400 && code < 500) {
                throw new InvalidRequestException(code + " Client error from MAL.", e);
            }
            throw new APIException(code + " Server error from MAL.", e);
        } catch (HttpTransportException e) {
            throw new APIException("Transport error while contacting MAL.", e);
        }

        return GSON.fromJson(body, type);
    }

    private static String encodeFields(String fields) {
        return URLEncoder.encode(fields, StandardCharsets.UTF_8);
    }
}
