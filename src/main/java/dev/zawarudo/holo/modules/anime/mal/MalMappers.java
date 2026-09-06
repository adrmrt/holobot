package dev.zawarudo.holo.modules.anime.mal;

import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;

/**
 * Maps typed MAL DTOs ({@link MalDtos}) to the domain {@link AnimeResult}/{@link MangaResult}.
 * Thin compared to {@code AniListMappers} - field access is already typed and Gson-checked, so
 * there's little defensive JSON-poking left to do here.
 */
final class MalMappers {

    private MalMappers() {
        throw new UnsupportedOperationException();
    }

    static @NotNull AnimeResult toAnimeResult(@NotNull MalDtos.Anime anime) {
        return new AnimeResult(
            MediaPlatform.MAL,
            anime.id(),

            anime.title(),
            formatAnimeMediaType(anime.mediaType()),
            "https://myanimelist.net/anime/" + anime.id(),

            selectImage(anime.mainPicture()),
            anime.synopsis(),

            altTitle(anime.alternativeTitles(), MalDtos.AlternativeTitles::en),
            altTitle(anime.alternativeTitles(), MalDtos.AlternativeTitles::ja),

            formatScore(anime.mean()),
            orZero(anime.rank()),
            anime.numEpisodes(),

            formatAnimeStatus(anime.status()),
            formatSeason(anime.startSeason()),

            names(anime.studios(), MalDtos.Studio::name),
            names(anime.genres(), MalDtos.Genre::name),
            List.of(), // MAL API has no separate themes field
            List.of()  // MAL API has no separate demographics field
        );
    }

    static @NotNull MangaResult toMangaResult(@NotNull MalDtos.Manga manga) {
        return new MangaResult(
            MediaPlatform.MAL,
            manga.id(),

            manga.title(),
            formatMangaMediaType(manga.mediaType()),
            "https://myanimelist.net/manga/" + manga.id(),

            selectImage(manga.mainPicture()),
            manga.synopsis(),

            altTitle(manga.alternativeTitles(), MalDtos.AlternativeTitles::en),
            altTitle(manga.alternativeTitles(), MalDtos.AlternativeTitles::ja),

            formatScore(manga.mean()),
            orZero(manga.rank()),
            manga.numChapters(),
            manga.numVolumes(),

            formatMangaStatus(manga.status()),

            formatAuthors(manga.authors()),
            names(manga.genres(), MalDtos.Genre::name),
            List.of(), // MAL API has no separate themes field
            List.of()  // MAL API has no separate demographics field
        );
    }

    private static @Nullable String selectImage(@Nullable MalDtos.Picture picture) {
        if (picture == null) return null;
        return picture.large() != null ? picture.large() : picture.medium();
    }

    private static @Nullable String altTitle(
        @Nullable MalDtos.AlternativeTitles titles,
        @NotNull Function<MalDtos.AlternativeTitles, String> extractor
    ) {
        if (titles == null) return null;
        String value = extractor.apply(titles);
        return (value == null || value.isBlank()) ? null : value;
    }

    private static @NotNull String formatScore(@Nullable Double mean) {
        return mean == null ? "N/A" : String.valueOf(mean);
    }

    private static int orZero(@Nullable Integer value) {
        return value == null ? 0 : value;
    }

    private static @Nullable String formatAnimeStatus(@Nullable String rawStatus) {
        return labelOrHumanize(MalDtos.AnimeStatus.class, rawStatus);
    }

    private static @Nullable String formatMangaStatus(@Nullable String rawStatus) {
        return labelOrHumanize(MalDtos.MangaStatus.class, rawStatus);
    }

    private static @NotNull String formatAnimeMediaType(@Nullable String rawMediaType) {
        String label = labelOrHumanize(MalDtos.AnimeMediaType.class, rawMediaType);
        return label == null ? "?" : label;
    }

    private static @NotNull String formatMangaMediaType(@Nullable String rawMediaType) {
        String label = labelOrHumanize(MalDtos.MangaMediaType.class, rawMediaType);
        return label == null ? "?" : label;
    }

    private static <T extends Enum<T> & MalDtos.ApiLabeled> @Nullable String labelOrHumanize(
        @NotNull Class<T> type,
        @Nullable String rawValue
    ) {
        if (rawValue == null) return null;
        T match = MalDtos.lookup(type, rawValue);
        return match != null ? match.label() : humanizeOrNull(rawValue);
    }

    private static @Nullable String formatSeason(@Nullable MalDtos.Season season) {
        if (season == null || season.season() == null || season.season().isBlank()) return null;
        return capitalize(season.season()) + " " + season.year();
    }

    private static @NotNull List<String> formatAuthors(@Nullable List<MalDtos.AuthorEntry> authors) {
        if (authors == null || authors.isEmpty()) return List.of();
        return authors.stream()
            .map(MalDtos.AuthorEntry::node)
            .filter(Objects::nonNull)
            .map(node -> joinNonBlank(node.firstName(), node.lastName()))
            .filter(name -> !name.isBlank())
            .toList();
    }

    private static @NotNull String joinNonBlank(@Nullable String first, @Nullable String last) {
        boolean hasFirst = first != null && !first.isBlank();
        boolean hasLast = last != null && !last.isBlank();
        if (hasFirst && hasLast) return first + " " + last;
        if (hasFirst) return first;
        if (hasLast) return last;
        return "";
    }

    private static <T> @NotNull List<String> names(
        @Nullable List<T> items,
        @NotNull Function<T, String> nameExtractor
    ) {
        if (items == null || items.isEmpty()) return List.of();
        return items.stream().map(nameExtractor).toList();
    }

    /**
     * Fallback for a MAL snake_case enum-like value that isn't one of the known {@link MalDtos.ApiLabeled}
     * constants (e.g. a new value MAL added after this enum was written).
     */
    private static @Nullable String humanizeOrNull(@Nullable String snakeCase) {
        if (snakeCase == null || snakeCase.isBlank()) return null;
        String[] words = snakeCase.split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(capitalize(word));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private static @NotNull String capitalize(@NotNull String word) {
        return word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1);
    }
}
