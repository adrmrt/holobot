package dev.zawarudo.holo.modules.anime.anilist;

import com.google.gson.JsonArray;
import dev.zawarudo.holo.modules.anime.model.AnimeResult;
import dev.zawarudo.holo.modules.anime.model.MangaResult;

import java.util.List;

public final class AniListMappers {

    private AniListMappers() {
        throw new UnsupportedOperationException();
    }

    public static List<AnimeResult> toAnimeResults(JsonArray media) {
        System.out.println(media);

        throw new UnsupportedOperationException("Not supported yet.");
    }

    public static List<MangaResult> toMangaResults(JsonArray media) {
        System.out.println(media);

        throw new UnsupportedOperationException("Not supported yet.");
    }
}