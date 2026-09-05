package dev.zawarudo.holo.modules.anime.anilist;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import dev.zawarudo.holo.modules.anime.AnimeResult;
import dev.zawarudo.holo.modules.anime.MangaResult;
import dev.zawarudo.holo.modules.anime.MediaPlatform;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AniListMappersTest {

    @Test
    void toAnimeResults_mapsAllFields() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 1,
              "siteUrl": "https://anilist.co/anime/1",
              "title": {"romaji": "Test Anime", "english": "Test Anime EN", "native": "テスト"},
              "description": "A description.",
              "format": "TV",
              "status": "FINISHED",
              "episodes": 12,
              "averageScore": 85,
              "coverImage": {"extraLarge": "https://example.com/xl.jpg", "large": "https://example.com/l.jpg", "medium": "https://example.com/m.jpg"},
              "genres": ["Action", "Comedy"],
              "season": "SPRING",
              "seasonYear": 2020,
              "studios": {"nodes": [{"name": "Test Studio"}]},
              "rankings": [{"rank": 42, "type": "RATED", "allTime": true}]
            }]
            """).getAsJsonArray();

        List<AnimeResult> results = AniListMappers.toAnimeResults(media);

        assertEquals(1, results.size());
        AnimeResult result = results.getFirst();

        assertEquals(MediaPlatform.ANILIST, result.platform());
        assertEquals(1, result.id());
        assertEquals("Test Anime", result.title());
        assertEquals("TV", result.type());
        assertEquals("https://anilist.co/anime/1", result.url());
        assertEquals("https://example.com/xl.jpg", result.imageUrl());
        assertEquals("A description.", result.synopsis());
        assertEquals("Test Anime EN", result.titleEnglish());
        assertEquals("テスト", result.titleJapanese());
        assertEquals("85%", result.score());
        assertEquals(42, result.rank());
        assertEquals(12, result.episodes());
        assertEquals("FINISHED", result.status());
        assertEquals("SPRING 2020", result.season());
        assertEquals(List.of("Test Studio"), result.studios());
        assertEquals(List.of("Action", "Comedy"), result.genres());
        assertTrue(result.themes().isEmpty());
        assertTrue(result.demographics().isEmpty());
    }

    @Test
    void toAnimeResults_nullCoverImageFields_fallsBackWithoutThrowing() {
        // Regression guard: coverImage sizes AniList doesn't have for a given entry come back
        // as explicit JSON null, not a missing key - selectBestImage must not NPE on that.
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 2,
              "siteUrl": "https://anilist.co/anime/2",
              "title": {"romaji": "No Cover Anime"},
              "coverImage": {"extraLarge": null, "large": null, "medium": "https://example.com/m.jpg"},
              "rankings": []
            }]
            """).getAsJsonArray();

        List<AnimeResult> results = AniListMappers.toAnimeResults(media);

        assertEquals("https://example.com/m.jpg", results.getFirst().imageUrl());
    }

    @Test
    void toAnimeResults_missingCoverImage_imageUrlIsNull() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 3,
              "siteUrl": "https://anilist.co/anime/3",
              "title": {"romaji": "Coverless Anime"},
              "coverImage": null,
              "rankings": []
            }]
            """).getAsJsonArray();

        List<AnimeResult> results = AniListMappers.toAnimeResults(media);

        assertNull(results.getFirst().imageUrl());
    }

    @Test
    void toAnimeResults_missingOptionalFields_fallsBackGracefully() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 4,
              "title": {},
              "rankings": []
            }]
            """).getAsJsonArray();

        AnimeResult result = AniListMappers.toAnimeResults(media).getFirst();

        assertEquals("Unknown", result.title());
        assertEquals("?", result.type());
        assertEquals("", result.url());
        assertNull(result.imageUrl());
        assertEquals("N/A", result.score());
        assertEquals(0, result.rank());
        assertEquals(0, result.episodes());
        assertNull(result.status());
        assertNull(result.season());
        assertTrue(result.studios().isEmpty());
        assertTrue(result.genres().isEmpty());
    }

    @Test
    void toAnimeResults_multipleMainStudios_extractsAllNames() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 5,
              "title": {"romaji": "Co-produced Anime"},
              "studios": {"nodes": [{"name": "Studio A"}, {"name": "Studio B"}]},
              "rankings": []
            }]
            """).getAsJsonArray();

        AnimeResult result = AniListMappers.toAnimeResults(media).getFirst();

        assertEquals(List.of("Studio A", "Studio B"), result.studios());
    }

    @Test
    void toAnimeResults_rankIgnoresNonRatedAndNonAllTimeEntries() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 6,
              "title": {"romaji": "Ranked Anime"},
              "rankings": [
                {"rank": 5, "type": "POPULARITY", "allTime": true},
                {"rank": 99, "type": "RATED", "allTime": false},
                {"rank": 17, "type": "RATED", "allTime": true}
              ]
            }]
            """).getAsJsonArray();

        AnimeResult result = AniListMappers.toAnimeResults(media).getFirst();

        assertEquals(17, result.rank());
    }

    @Test
    void toMangaResults_mapsAllFields() {
        JsonArray media = JsonParser.parseString("""
            [{
              "id": 10,
              "siteUrl": "https://anilist.co/manga/10",
              "title": {"romaji": "Test Manga", "english": "Test Manga EN"},
              "description": "A manga description.",
              "format": "MANGA",
              "status": "RELEASING",
              "chapters": 100,
              "volumes": 10,
              "averageScore": 90,
              "coverImage": {"extraLarge": "https://example.com/xl.jpg"},
              "genres": ["Drama"],
              "rankings": [{"rank": 3, "type": "RATED", "allTime": true}]
            }]
            """).getAsJsonArray();

        List<MangaResult> results = AniListMappers.toMangaResults(media);

        assertEquals(1, results.size());
        MangaResult result = results.getFirst();

        assertEquals(MediaPlatform.ANILIST, result.platform());
        assertEquals(10, result.id());
        assertEquals("Test Manga", result.title());
        assertEquals("MANGA", result.type());
        assertEquals("90%", result.score());
        assertEquals(3, result.rank());
        assertEquals(100, result.chapters());
        assertEquals(10, result.volumes());
        assertEquals("RELEASING", result.status());
        assertEquals(List.of("Drama"), result.genres());
        assertTrue(result.authors().isEmpty());
    }

    @Test
    void toAnimeResults_nullOrEmptyMedia_returnsEmptyList() {
        assertTrue(AniListMappers.toAnimeResults(null).isEmpty());
        assertTrue(AniListMappers.toAnimeResults(new JsonArray()).isEmpty());
    }
}
