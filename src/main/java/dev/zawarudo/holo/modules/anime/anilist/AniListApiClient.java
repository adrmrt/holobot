package dev.zawarudo.holo.modules.anime.anilist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.HoloRateLimiter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Client for the public AniList GraphQL API (<a href="https://docs.anilist.co">docs.anilist.co</a>).
 * No authentication needed for search queries.
 */
public final class AniListApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AniListApiClient.class);

    private static final String BASE_URL = "https://graphql.anilist.co";

    // AniList's documented limit is 90 req/min, dropped to 30 req/min during periods of high
    // load - 1/sec stays under the normal limit without needing to track which mode is active.
    private final HoloRateLimiter rateLimiter = new HoloRateLimiter(1);

    private static final String Q_SEARCH_ANIME = """
        query ($search: String, $perPage: Int) {
          Page(perPage: $perPage) {
            media(search: $search, type: ANIME) {
              id
              siteUrl
              title { romaji english native }
              description
              format
              status
              episodes
              averageScore
              coverImage { extraLarge large medium }
              genres
              season
              seasonYear
              studios(isMain: true) { nodes { name } }
              rankings {
                rank
                type
                allTime
              }
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
              description
              format
              status
              chapters
              volumes
              averageScore
              coverImage { extraLarge large medium}
              genres
              rankings {
                rank
                type
                allTime
              }
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
        rateLimiter.acquire();

        JsonObject body = new JsonObject();
        body.addProperty("query", gqlQuery);
        body.add("variables", variables);

        try {
            JsonObject res = HoloHttp.postJsonObject(BASE_URL, body, Map.of());
            LOGGER.debug("AniList response: {}", res);

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
            LOGGER.debug("AniList error response ({}): {}", e.getStatusCode(), e.getBodySnippet());
            if (e.getStatusCode() == 429) {
                throw new APIException("429 Too Many Requests (AniList rate limit).", e);
            }
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
