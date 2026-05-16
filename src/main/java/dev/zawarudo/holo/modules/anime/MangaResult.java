package dev.zawarudo.holo.modules.anime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record MangaResult(
        @NotNull MediaPlatform platform,
        int id,

        @NotNull String title,
        @NotNull String type,
        @NotNull String url,

        @Nullable String imageUrl,
        @Nullable String synopsis,
        @Nullable String titleEnglish,
        @Nullable String titleJapanese,

        @NotNull String score,
        int rank,
        int chapters,
        int volumes,

        @Nullable String status,

        @NotNull List<String> authors,
        @NotNull List<String> genres,
        @NotNull List<String> themes,
        @NotNull List<String> demographics
) {
}
