package dev.zawarudo.holo.modules.anime;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * A seasonal anime entry from a season-browse query (as opposed to {@link AnimeResult}, which
 * backs the title-search commands). Used by {@code SeasonPlan}/{@code AnimeSeason}.
 */
public record SeasonalAnime(
    int id,
    @NotNull String title,
    @NotNull String url,
    int popularity,
    @Nullable LocalDate startDate,
    @Nullable ZonedDateTime nextAiringAt
) {
}
