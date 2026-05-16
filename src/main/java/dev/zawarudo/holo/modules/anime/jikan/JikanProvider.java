package dev.zawarudo.holo.modules.anime.jikan;

import dev.zawarudo.holo.modules.anime.MediaPlatform;
import dev.zawarudo.holo.modules.anime.MediaSearchProvider;
import dev.zawarudo.holo.modules.anime.jikan.model.Anime;
import dev.zawarudo.holo.modules.anime.jikan.model.Images;
import dev.zawarudo.holo.modules.anime.jikan.model.Manga;
import dev.zawarudo.holo.modules.anime.jikan.model.Nameable;
import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class JikanProvider implements MediaSearchProvider {

    @Override
    public MediaPlatform platform() {
        return MediaPlatform.MAL_JIKAN;
    }

    @Override
    public List<AnimeResult> searchAnime(@NotNull String query, int limit) throws APIException, InvalidRequestException {
        JikanApiClient.setLimit(limit);

        List<Anime> results = JikanApiClient.searchAnime(query);
        return results.stream().map(JikanProvider::mapAnime).toList();
    }

    @Override
    public List<MangaResult> searchManga(@NotNull String query, int limit) throws APIException, InvalidRequestException {
        JikanApiClient.setLimit(limit);

        List<Manga> res = JikanApiClient.searchManga(query);
        return res.stream().map(JikanProvider::mapManga).toList();
    }

    private static AnimeResult mapAnime(Anime anime) {
        String score = anime.getScore() == 0.0 ? "N/A" : String.valueOf(anime.getScore());

        return new AnimeResult(
                MediaPlatform.MAL_JIKAN,
                anime.getId(),
                safeText(anime.getTitle(), "Unknown"),
                safeText(anime.getType(), "?"),
                safeText(anime.getUrl(), ""),

                pickImageUrl(anime.getImages()),
                anime.getSynopsis().orElse(null),

                anime.getTitleEnglish().orElse(null),
                anime.getTitleJapanese().orElse(null),

                score,
                anime.getRank(),
                anime.getEpisodes(),

                emptyToNull(anime.getStatus()),
                anime.getSeason(),

                namesOrEmpty(anime.getStudios()),
                namesOrEmpty(anime.getGenres()),
                namesOrEmpty(anime.getThemes()),
                namesOrEmpty(anime.getDemographics())
        );
    }

    private static MangaResult mapManga(Manga manga) {
        String score = manga.getScore() == 0.0 ? "N/A" : String.valueOf(manga.getScore());

        return new MangaResult(
                MediaPlatform.MAL_JIKAN,
                manga.getId(),

                safeText(manga.getTitle(), "Unknown"),
                safeText(manga.getType(), "?"),
                safeText(manga.getUrl(), ""),

                pickImageUrl(manga.getImages()),
                manga.getSynopsis().orElse(null),

                manga.getTitleEnglish().orElse(null),
                manga.getTitleJapanese().orElse(null),

                score,
                manga.getRank(),
                manga.getChapters(),
                manga.getVolumes(),

                emptyToNull(manga.getStatus()),
                formatAuthors(manga.getAuthors()),

                namesOrEmpty(manga.getGenres()),
                namesOrEmpty(manga.getThemes()),
                namesOrEmpty(manga.getDemographics())
        );
    }

    private static @Nullable String pickImageUrl(@Nullable Images images) {
        if (images == null || images.getJpg() == null) return null;
        String large = images.getJpg().getLargeImage();
        return (large != null) ? large : images.getJpg().getImage();
    }

    private static @NotNull List<String> namesOrEmpty(@Nullable List<? extends Nameable> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream().map(Nameable::getName).toList();
    }

    // Reverse Japanese name order (family -> given) for display
    private static @NotNull List<String> formatAuthors(@Nullable List<Nameable> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream()
                .map(Nameable::getName)
                .map(Formatter::reverseJapaneseName)
                .toList();
    }

    private static @NotNull String safeText(@Nullable String s, @NotNull String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }

    private static @Nullable String emptyToNull(@Nullable String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}