package dev.zawarudo.holo.modules.anime.jikan;

import dev.zawarudo.holo.modules.anime.jikan.model.*;
import dev.zawarudo.holo.modules.anime.jikan.model.Character;
import dev.zawarudo.holo.modules.anime.jikan.model.MediaType;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidIdException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

public class JikanClientIT {

    private static final int CLIENT_LIMIT = 25;

    @BeforeAll
    static void setup() {
        JikanApiClient.setLimit(CLIENT_LIMIT);
    }

    private static void assertBasicAnime(Anime a) {
        assertNotNull(a, "Anime must not be null");
        assertTrue(a.getId() > 0, "Anime id must be > 0");
        assertNotNull(a.getTitle(), "Anime title must not be null");
        assertFalse(a.getTitle().isBlank(), "Anime title must not be blank");
    }

    private static void assertBasicManga(Manga m) {
        assertNotNull(m, "Manga must not be null");
        assertTrue(m.getId() > 0, "Manga id must be > 0");
        assertNotNull(m.getTitle(), "Manga title must not be null");
        assertFalse(m.getTitle().isBlank(), "Manga title must not be blank");
    }

    private static void assertBasicCharacter(Character c) {
        assertNotNull(c, "Character must not be null");
        assertTrue(c.getId() > 0, "Character id must be > 0");
        assertNotNull(c.getName(), "Character name must not be null");
        assertFalse(c.getName().isBlank(), "Character name must not be blank");
    }

    @Nested
    class AnimeEndpoints {

        @Test
        @Timeout(10)
        void getAnime_onePiece() throws APIException, InvalidIdException {
            Anime a = JikanApiClient.getAnime(21);
            assertBasicAnime(a);

            assertEquals(21, a.getId());
            assertEquals("one piece", a.getTitle().toLowerCase(Locale.UK));
        }

        @Test
        @Timeout(10)
        void getAnime_blackClover() throws APIException, InvalidIdException {
            Anime a = JikanApiClient.getAnime(34572);
            assertBasicAnime(a);
            assertEquals(34572, a.getId());
            assertEquals("black clover", a.getTitle().toLowerCase(Locale.UK));
        }

        @Test
        @Timeout(10)
        void getAnime_invalidId() {
            assertThrows(InvalidIdException.class, () -> JikanApiClient.getAnime(0));
            assertThrows(InvalidIdException.class, () -> JikanApiClient.getAnime(-1));
        }

        @Test
        @Timeout(10)
        void searchAnime_onePiece() throws APIException, InvalidRequestException {
            List<Anime> results = JikanApiClient.searchAnime("one piece");
            assertNotNull(results);
            assertFalse(results.isEmpty(), "Expected non-empty search results");

            assertTrue(results.stream().anyMatch(a -> a.getId() == 21),
                    "Expected search results to include anime id=21 (One Piece)");
        }

        @Test
        @Timeout(10)
        void randomAnime() throws APIException {
            Anime a = JikanApiClient.getRandomAnime();
            assertBasicAnime(a);
        }

        @Test
        @Timeout(10)
        void topAnime_smoke() throws APIException {
            int n = 10;
            List<Anime> top = JikanApiClient.getTopAnime(n);

            assertNotNull(top);
            assertFalse(top.isEmpty(), "Expected at least 1 top anime");

            assertTrue(top.size() <= n, "Expected <= " + n + " entries, got " + top.size());

            top.stream().limit(3).forEach(JikanClientIT::assertBasicAnime);
        }
    }

    @Nested
    class MangaEndpoints {

        @Test
        @Timeout(10)
        void getManga_onePiece() throws APIException, InvalidIdException {
            Manga m = JikanApiClient.getManga(13);
            assertBasicManga(m);
            assertEquals(13, m.getId());

            assertEquals("One Piece", m.getTitle());
        }

        @Test
        @Timeout(10)
        void getManga_naruto() throws APIException, InvalidIdException {
            Manga m = JikanApiClient.getManga(11);
            assertBasicManga(m);
            assertEquals(11, m.getId());
            assertEquals("Naruto", m.getTitle());
        }

        @Test
        @Timeout(10)
        void searchManga_onePiece() throws APIException, InvalidRequestException {
            List<Manga> results = JikanApiClient.searchManga("one piece");
            assertNotNull(results);
            assertFalse(results.isEmpty());

            assertTrue(results.stream().anyMatch(m -> m.getId() == 13),
                    "Expected search results to include manga id=13 (One Piece)");
        }

        @Test
        @Timeout(10)
        void randomManga() throws APIException {
            Manga m = JikanApiClient.getRandomManga();
            assertBasicManga(m);
        }
    }

    @Nested
    class CharacterEndpoints {

        @Test
        @Timeout(10)
        void getCharacter_luffy() throws APIException, InvalidIdException {
            Character c = JikanApiClient.getCharacter(40);
            assertBasicCharacter(c);

            assertEquals(40, c.getId());
            assertTrue(c.getName().toLowerCase(Locale.UK).contains("luffy"),
                    "Expected character name to contain 'luffy'");
        }

        @Test
        @Timeout(10)
        void characterAppearances_anime() throws APIException, InvalidIdException {
            List<Appearance> apps = JikanApiClient.getCharacter(40, MediaType.ANIME);
            assertNotNull(apps);
            assertFalse(apps.isEmpty(), "Expected non-empty appearance list");
        }

        @Test
        @Timeout(10)
        void searchCharacter_luffy() throws APIException, InvalidRequestException {
            List<Character> results = JikanApiClient.searchCharacter("Luffy");
            assertNotNull(results);
            assertFalse(results.isEmpty());

            assertTrue(results.stream().anyMatch(c -> c.getId() == 40),
                    "Expected search results to include character id=40 (Luffy)");
        }
    }

    @Nested
    class OtherEndpoints {

        @Test
        @Timeout(10)
        void seasonNow_smoke() throws APIException {
            List<Anime> now = JikanApiClient.getSeason();
            assertNotNull(now);
            assertFalse(now.isEmpty(), "Expected current season list to be non-empty");
        }

        @Test
        @Timeout(20)
        void seasonSpring2022_smoke() throws APIException {
            List<Anime> spring2022 = JikanApiClient.getSeason(Season.SPRING, 2022);

            assertNotNull(spring2022);
            assertFalse(spring2022.isEmpty(), "Expected Spring 2022 list to be non-empty");
        }

        @Test
        @Timeout(10)
        void related_smoke() throws APIException, InvalidIdException, InvalidRequestException {
            Anime onePiece = JikanApiClient.getAnime(21);
            assertBasicAnime(onePiece);

            List<Related> related = onePiece.getRelated();
            assertNotNull(related);

            assertFalse(related.isEmpty(), "Expected related list to be non-empty for One Piece");
        }
    }
}