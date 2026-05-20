package dev.zawarudo.holo.modules.anime.anilist;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.utils.Formatter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class AniListMappers {

    private AniListMappers() {
        throw new UnsupportedOperationException();
    }

    public static @NotNull List<AnimeResult> toAnimeResults(@Nullable JsonArray media) {
        if (media == null || media.isEmpty()) return List.of();

        List<AnimeResult> out = new ArrayList<>(media.size());
        for (JsonElement el : media) {
            if (el == null || !el.isJsonObject()) continue;
            out.add(mapAnime(el.getAsJsonObject()));
        }
        return out;
    }

    public static @NotNull List<MangaResult> toMangaResults(@Nullable JsonArray media) {
        if (media == null || media.isEmpty()) return List.of();

        List<MangaResult> out = new ArrayList<>(media.size());
        for (JsonElement el : media) {
            if (el == null || !el.isJsonObject()) continue;
            out.add(mapManga(el.getAsJsonObject()));
        }
        return out;
    }

    private static @NotNull AnimeResult mapAnime(@NotNull JsonObject object) {
        int id = object.get("id").getAsInt();

        JsonObject titleObj = object.getAsJsonObject("title");
        String titleRomaji = getSafeString(titleObj, "romaji");
        String titleEnglish = getSafeString(titleObj, "english");
        String titleNative = getSafeString(titleObj, "native");
        String displayTitle = pickFirstNonBlank(titleRomaji, titleEnglish, titleNative, "Unknown");

        String imageUrl = selectBestImage(object.getAsJsonObject("coverImage"));

        String description = Formatter.htmlToDiscord(getSafeString(object, "description"));

        String averageScoreStr = formatAverageScore(object.get("averageScore"));
        int rank = getAllTimeRank(object.getAsJsonArray("rankings"), 0);

        String season = formatSeason(object);

        return new AnimeResult(
                MediaPlatform.ANILIST,
                id,

                displayTitle,
                safeText(getSafeString(object, "format"), "?"),
                safeText(getSafeString(object, "siteUrl"), ""),

                imageUrl,
                description,

                emptyToNull(titleEnglish),
                emptyToNull(titleNative),

                averageScoreStr,
                rank,
                intValNullable(object.get("episodes"), 0),

                emptyToNull(getSafeString(object, "status")),
                season,

                List.of(), // studios not requested
                extractGenres(object.getAsJsonArray("genres")),
                List.of(), // themes not requested
                List.of()  // demographics not requested
        );
    }


    private static @NotNull MangaResult mapManga(@NotNull JsonObject object) {
        int id = object.get("id").getAsInt();

        JsonObject titleObj = object.getAsJsonObject("title");
        String titleRomaji = getSafeString(titleObj, "romaji");
        String titleEnglish = getSafeString(titleObj, "english");
        String titleNative = getSafeString(titleObj, "native");
        String displayTitle = pickFirstNonBlank(titleRomaji, titleEnglish, titleNative, "Unknown");

        String imageUrl = selectBestImage(object.getAsJsonObject("coverImage"));

        String description = Formatter.htmlToDiscord(getSafeString(object, "description"));

        String averageScoreStr = formatAverageScore(object.get("averageScore"));
        int rank = getAllTimeRank(object.getAsJsonArray("rankings"), 0);

        return new MangaResult(
                MediaPlatform.ANILIST,
                id,

                displayTitle,
                safeText(getSafeString(object, "format"), "?"),
                safeText(getSafeString(object, "siteUrl"), ""),

                imageUrl,
                description,

                emptyToNull(titleEnglish),
                emptyToNull(titleNative),

                averageScoreStr,
                rank,
                intValNullable(object.get("chapters"), 0),
                intValNullable(object.get("volumes"), 0),

                emptyToNull(getSafeString(object, "status")),
                List.of(), // authors not requested
                extractGenres(object.getAsJsonArray("genres")),
                List.of(), // themes not requested
                List.of()  // demographics not requested
        );
    }

    private static @Nullable String getSafeString(@Nullable JsonObject o, @NotNull String key) {
        if (o == null) return null;
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) return null;
        try {
            String s = el.getAsString();
            return (s == null || s.isBlank()) ? null : s;
        } catch (UnsupportedOperationException ignored) {
            return null;
        }
    }

    private static int intValNullable(@Nullable JsonElement el, int fallback) {
        if (el == null || el.isJsonNull()) return fallback;
        try {
            return el.getAsInt();
        } catch (UnsupportedOperationException | NumberFormatException ignored) {
            return fallback;
        }
    }

    private static @NotNull String safeText(@Nullable String s, @NotNull String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static @Nullable String emptyToNull(@Nullable String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static @NotNull String pickFirstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private static @NotNull String formatAverageScore(@Nullable JsonElement scoreEl) {
        if (scoreEl == null || scoreEl.isJsonNull()) {
            return "N/A";
        }

        return scoreEl.getAsInt() + "%";
    }

    private static @Nullable String selectBestImage(@Nullable JsonObject coverImage) {
        if (coverImage == null || coverImage.isJsonNull()) return null;

        String extraLarge = coverImage.get("extraLarge").getAsString();
        if (extraLarge != null && !extraLarge.isBlank()) return extraLarge;

        String large = coverImage.get("large").getAsString();
        if (large != null && !large.isBlank()) return large;

        String medium = coverImage.get("medium").getAsString();
        if (medium != null && !medium.isBlank()) return medium;

        return null;
    }

    private static @Nullable String formatSeason(@NotNull JsonObject object) {
        JsonElement seasonEl = object.get("season");
        JsonElement yearEl = object.get("seasonYear");

        if (seasonEl == null || seasonEl.isJsonNull()) {
            return null;
        }

        if (yearEl == null || yearEl.isJsonNull()) {
            return null;
        }

        try {
            String season = seasonEl.getAsString();
            int year = yearEl.getAsInt();

            if (season.isBlank()) {
                return null;
            }

            return season + " " + year;
        } catch (UnsupportedOperationException | NumberFormatException e) {
            return null;
        }
    }

    private static int getAllTimeRank(@Nullable JsonArray rankings, int fallback) {
        if (rankings == null || rankings.isEmpty()) return fallback;

        Integer best = null;

        for (JsonElement el : rankings) {
            if (el == null || !el.isJsonObject()) continue;
            JsonObject r = el.getAsJsonObject();

            // type == RATED
            String type = getSafeString(r, "type");
            if (!"RATED".equals(type)) continue;

            // allTime == true
            JsonElement allTimeEl = r.get("allTime");
            if (allTimeEl == null || allTimeEl.isJsonNull() || !allTimeEl.getAsBoolean()) continue;

            int rank = intValNullable(r.get("rank"), Integer.MAX_VALUE);
            if (rank == Integer.MAX_VALUE) continue;

            if (best == null || rank < best) best = rank;
        }

        return best != null ? best : fallback;
    }

    private static @NotNull List<String> extractGenres(@Nullable JsonArray genresArray) {
        if (genresArray == null || genresArray.isEmpty()) {
            return List.of();
        }

        List<String> genres = new ArrayList<>(genresArray.size());

        for (JsonElement el : genresArray) {
            if (el == null || el.isJsonNull()) continue;

            try {
                String genre = el.getAsString();
                if (!genre.isBlank()) {
                    genres.add(genre);
                }
            } catch (UnsupportedOperationException ignored) {
                // skip invalid entries
            }
        }

        return genres.isEmpty() ? List.of() : List.copyOf(genres);
    }
}
