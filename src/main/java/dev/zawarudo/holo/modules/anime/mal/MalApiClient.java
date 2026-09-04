package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.HoloRateLimiter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;

import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Client for the official MyAnimeList API v2. Public search/detail endpoints only need the
 * {@code X-MAL-CLIENT-ID} header, no OAuth2 token.
 */
final class MalApiClient {

    private static final String BASE_URL = "https://api.myanimelist.net/v2";

    // Derived from the DTO shape itself (see fieldsOf) so the request always matches what
    // MalDtos.Anime/Manga actually declare - no separate field list to keep in sync by hand.
    private static final String ANIME_FIELDS = fieldsOf(MalDtos.Anime.class);
    private static final String MANGA_FIELDS = fieldsOf(MalDtos.Manga.class);

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
        } catch (HttpStatusException e) {
            int code = e.getStatusCode();

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

    /**
     * Builds a MAL {@code fields=} value from a DTO record's own components, so the request
     * always matches what the record declares. Each component contributes its wire key (its
     * {@link SerializedName} if annotated, otherwise its own name) plus any
     * {@link MalDtos.NestedFields} selector.
     */
    private static String fieldsOf(Class<? extends Record> dto) {
        return Arrays.stream(dto.getRecordComponents())
            .map(MalApiClient::fieldSpec)
            .collect(Collectors.joining(","));
    }

    private static String fieldSpec(RecordComponent component) {
        SerializedName serializedName = component.getAnnotation(SerializedName.class);
        String key = serializedName != null ? serializedName.value() : component.getName();

        MalDtos.NestedFields nested = component.getAnnotation(MalDtos.NestedFields.class);
        return nested != null ? key + nested.value() : key;
    }
}
