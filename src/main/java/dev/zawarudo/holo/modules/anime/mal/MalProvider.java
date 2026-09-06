package dev.zawarudo.holo.modules.anime.mal;

import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.MediaSearchProvider;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;

import java.util.List;
import java.util.Optional;

public final class MalProvider implements MediaSearchProvider {

    private final MalApiClient client;

    MalProvider(MalApiClient client) {
        this.client = client;
    }

    /**
     * Builds a provider from a MAL client ID, or empty if none is configured. Keeps the
     * "MAL needs a client ID" knowledge here instead of leaking it into the composition root.
     */
    public static Optional<MalProvider> create(String clientId) {
        if (clientId == null || clientId.isBlank()) return Optional.empty();
        return Optional.of(new MalProvider(new MalApiClient(clientId)));
    }

    @Override
    public MediaPlatform platform() {
        return MediaPlatform.MAL;
    }

    @Override
    public List<AnimeResult> searchAnime(String query, int limit) throws APIException, InvalidRequestException {
        validate(query, limit);

        List<MalDtos.Anime> results = client.searchAnime(query, limit);
        return results.stream().map(MalMappers::toAnimeResult).toList();
    }

    @Override
    public List<MangaResult> searchManga(String query, int limit) throws APIException, InvalidRequestException {
        validate(query, limit);

        List<MalDtos.Manga> results = client.searchManga(query, limit);
        return results.stream().map(MalMappers::toMangaResult).toList();
    }

    private void validate(String query, int limit) throws InvalidRequestException {
        if (query == null || query.isBlank()) throw new InvalidRequestException("Query must not be empty.");
        if (limit < 1 || limit > 25) throw new InvalidRequestException("Limit must be between 1 and 25.");
    }
}
