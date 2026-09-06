package dev.zawarudo.holo.modules.anime.mal;

import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MalMappersTest {

    @Test
    void toAnimeResult_mapsAllFields() {
        MalDtos.Anime dto = new MalDtos.Anime(
            1,
            "Test Anime",
            new MalDtos.Picture("https://example.com/m.jpg", "https://example.com/l.jpg"),
            new MalDtos.AlternativeTitles(List.of("Synonym"), "Test Anime EN", "テスト"),
            "A synopsis.",
            7.5,
            123,
            "tv_special",
            "finished_airing",
            12,
            new MalDtos.Season(2020, "spring"),
            List.of(new MalDtos.Genre(1, "Action")),
            List.of(new MalDtos.Studio(10, "Test Studio")),
            "original"
        );

        AnimeResult result = MalMappers.toAnimeResult(dto);

        assertEquals(MediaPlatform.MAL, result.platform());
        assertEquals(1, result.id());
        assertEquals("Test Anime", result.title());
        assertEquals("Tv Special", result.type());
        assertEquals("https://myanimelist.net/anime/1", result.url());
        assertEquals("https://example.com/l.jpg", result.imageUrl());
        assertEquals("A synopsis.", result.synopsis());
        assertEquals("Test Anime EN", result.titleEnglish());
        assertEquals("テスト", result.titleJapanese());
        assertEquals("7.5", result.score());
        assertEquals(123, result.rank());
        assertEquals(12, result.episodes());
        assertEquals("Finished Airing", result.status());
        assertEquals("Spring 2020", result.season());
        assertEquals(List.of("Test Studio"), result.studios());
        assertEquals(List.of("Action"), result.genres());
        assertTrue(result.themes().isEmpty());
        assertTrue(result.demographics().isEmpty());
    }

    @Test
    void toAnimeResult_missingOptionalFields_fallsBackGracefully() {
        MalDtos.Anime dto = new MalDtos.Anime(
            2, "Minimal Anime", null, null, null, null, null,
            "movie", null, 0, null, List.of(), List.of(), null
        );

        AnimeResult result = MalMappers.toAnimeResult(dto);

        assertNull(result.imageUrl());
        assertNull(result.titleEnglish());
        assertNull(result.titleJapanese());
        assertEquals("N/A", result.score());
        assertEquals(0, result.rank());
        assertNull(result.status());
        assertNull(result.season());
        assertTrue(result.studios().isEmpty());
        assertTrue(result.genres().isEmpty());
    }

    @Test
    void toMangaResult_mapsAllFields() {
        MalDtos.Manga dto = new MalDtos.Manga(
            2,
            "Test Manga",
            new MalDtos.Picture("https://example.com/m.jpg", null),
            new MalDtos.AlternativeTitles(List.of(), "Test Manga EN", null),
            "A manga synopsis.",
            8.3,
            45,
            "one_shot",
            "currently_publishing",
            100,
            10,
            List.of(new MalDtos.Genre(2, "Comedy")),
            List.of(new MalDtos.AuthorEntry(new MalDtos.AuthorNode(99, "Test", "Author"), "Story & Art"))
        );

        MangaResult result = MalMappers.toMangaResult(dto);

        assertEquals(MediaPlatform.MAL, result.platform());
        assertEquals(2, result.id());
        assertEquals("Test Manga", result.title());
        assertEquals("One-shot", result.type());
        assertEquals("https://myanimelist.net/manga/2", result.url());
        assertEquals("https://example.com/m.jpg", result.imageUrl());
        assertEquals("8.3", result.score());
        assertEquals(45, result.rank());
        assertEquals(100, result.chapters());
        assertEquals(10, result.volumes());
        assertEquals("Currently Publishing", result.status());
        assertEquals(List.of("Test Author"), result.authors());
        assertEquals(List.of("Comedy"), result.genres());
    }

    @Test
    void toAnimeResult_unrecognizedStatus_fallsBackToHumanizedRawValue() {
        // Regression guard: if MAL adds a new status value AnimeStatus doesn't know about yet,
        // mapping must degrade gracefully (humanized raw string) instead of losing the field.
        MalDtos.Anime dto = new MalDtos.Anime(
            3, "Future Anime", null, null, null, null, null,
            "tv", "some_new_status", 0, null, List.of(), List.of(), null
        );

        AnimeResult result = MalMappers.toAnimeResult(dto);

        assertEquals("Some New Status", result.status());
    }

    @Test
    void toAnimeResult_recognizedMediaType_usesEnumLabel() {
        MalDtos.Anime dto = new MalDtos.Anime(
            4, "TV Anime", null, null, null, null, null,
            "tv", "currently_airing", 0, null, List.of(), List.of(), null
        );

        AnimeResult result = MalMappers.toAnimeResult(dto);

        assertEquals("TV", result.type());
    }

    @Test
    void toAnimeResult_unrecognizedMediaType_fallsBackToHumanizedRawValue() {
        MalDtos.Anime dto = new MalDtos.Anime(
            5, "Weird Anime", null, null, null, null, null,
            "some_new_format", "finished_airing", 0, null, List.of(), List.of(), null
        );

        AnimeResult result = MalMappers.toAnimeResult(dto);

        assertEquals("Some New Format", result.type());
    }
}
