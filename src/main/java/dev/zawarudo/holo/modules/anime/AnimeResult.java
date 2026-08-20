package dev.zawarudo.holo.modules.anime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AnimeResult(
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
    int episodes,

    @Nullable String status,
    @Nullable String season,

    @NotNull List<String> studios,
    @NotNull List<String> genres,
    @NotNull List<String> themes,
    @NotNull List<String> demographics
) {
}
