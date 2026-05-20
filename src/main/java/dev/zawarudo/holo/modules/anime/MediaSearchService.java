package dev.zawarudo.holo.modules.anime;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MediaSearchService {

    private final Map<MediaPlatform, MediaSearchProvider> providers = new EnumMap<>(MediaPlatform.class);

    private final List<MediaPlatform> defaultOrder;

    private final boolean fallbackOnErrors;

    public MediaSearchService(List<MediaSearchProvider> providers, List<MediaPlatform> defaultOrder, boolean fallbackOnErrors) {
        Objects.requireNonNull(providers, "providers");
        Objects.requireNonNull(defaultOrder, "defaultOrder");

        for (MediaSearchProvider p : providers) {
            this.providers.put(p.platform(), p);
        }

        for (MediaPlatform platform : defaultOrder) {
            if (!this.providers.containsKey(platform)) {
                throw new IllegalArgumentException("No provider registered for platform: " + platform);
            }
        }

        this.defaultOrder = List.copyOf(defaultOrder);
        this.fallbackOnErrors = fallbackOnErrors;
    }

    public List<AnimeResult> searchAnime(String query, int limit) throws APIException, InvalidRequestException {
        return searchAnime(query, limit, defaultOrder);
    }

    public List<MangaResult> searchManga(String query, int limit) throws APIException, InvalidRequestException {
        return searchManga(query, limit, defaultOrder);
    }

    public List<AnimeResult> searchAnime(String query, int limit, List<MediaPlatform> order)
            throws APIException, InvalidRequestException {

        Objects.requireNonNull(query, "query");
        if (order == null || order.isEmpty()) return List.of();

        APIException lastApi = null;
        InvalidRequestException lastReq = null;

        for (MediaPlatform platform : order) {
            MediaSearchProvider provider = providers.get(platform);
            if (provider == null) continue;

            try {
                List<AnimeResult> res = provider.searchAnime(query, limit);
                if (res != null && !res.isEmpty()) return res;
            } catch (APIException e) {
                lastApi = e;
                if (!fallbackOnErrors) throw e;
            } catch (InvalidRequestException e) {
                lastReq = e;
                if (!fallbackOnErrors) throw e;
            }
        }

        // if all failed with errors, rethrow last
        if (lastReq != null) throw lastReq;
        if (lastApi != null) throw lastApi;

        return List.of();
    }

    public List<MangaResult> searchManga(String query, int limit, List<MediaPlatform> order)
            throws APIException, InvalidRequestException {

        Objects.requireNonNull(query, "query");
        if (order == null || order.isEmpty()) return List.of();

        APIException lastApi = null;
        InvalidRequestException lastReq = null;

        for (MediaPlatform platform : order) {
            MediaSearchProvider provider = providers.get(platform);
            if (provider == null) continue;

            try {
                List<MangaResult> res = provider.searchManga(query, limit);
                if (res != null && !res.isEmpty()) return res;
            } catch (APIException e) {
                lastApi = e;
                if (!fallbackOnErrors) throw e;
            } catch (InvalidRequestException e) {
                lastReq = e;
                if (!fallbackOnErrors) throw e;
            }
        }

        if (lastReq != null) throw lastReq;
        if (lastApi != null) throw lastApi;

        return List.of();
    }
}
