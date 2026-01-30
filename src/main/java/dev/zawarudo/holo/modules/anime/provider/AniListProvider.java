package dev.zawarudo.holo.modules.anime.provider;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.anilist.AniListApiClient;
import dev.zawarudo.holo.modules.anime.anilist.AniListMappers;
import dev.zawarudo.holo.modules.anime.model.AnimeResult;
import dev.zawarudo.holo.modules.anime.model.MangaResult;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;

import java.util.List;

public final class AniListProvider implements MediaSearchProvider {

    private final AniListApiClient client;

    public AniListProvider(AniListApiClient client) {
        this.client = client;
    }

    @Override
    public MediaPlatform platform() {
        return MediaPlatform.ANILIST;
    }

    @Override
    public List<AnimeResult> searchAnime(String query, int limit) throws APIException, InvalidRequestException {
        validate(query, limit);

        JsonObject data = client.searchAnimeRaw(query, limit);
        JsonArray media = data.getAsJsonObject("Page").getAsJsonArray("media");

        return AniListMappers.toAnimeResults(media);
    }

    @Override
    public List<MangaResult> searchManga(String query, int limit) throws APIException, InvalidRequestException {
        validate(query, limit);

        JsonObject data = client.searchMangaRaw(query, limit);
        JsonArray media = data.getAsJsonObject("Page").getAsJsonArray("media");

        return AniListMappers.toMangaResults(media);
    }

    private void validate(String query, int limit) throws InvalidRequestException {
        if (query == null || query.isBlank()) throw new InvalidRequestException("Query must not be empty.");
        if (limit < 1 || limit > 25) throw new InvalidRequestException("Limit must be between 1 and 25.");
    }
}
