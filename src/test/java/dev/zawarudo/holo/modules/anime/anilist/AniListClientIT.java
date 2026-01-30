package dev.zawarudo.holo.modules.anime.anilist;

import dev.zawarudo.holo.modules.anime.model.AnimeResult;
import dev.zawarudo.holo.modules.anime.model.MangaResult;
import dev.zawarudo.holo.modules.anime.provider.AniListProvider;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.HoloRateLimiter;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AniListClientIT {

    private static AniListProvider provider;

    @BeforeAll
    static void init() {
        HoloHttp.setHostRateLimit("graphql.anilist.co", new HoloRateLimiter(1));

        provider = new AniListProvider(new AniListApiClient());
    }

    @Test
    @Timeout(10)
    void searchAnime_realAniList_works() throws Exception {
        List<AnimeResult> results = provider.searchAnime("Cowboy Bebop", 3);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Expected AniList to return at least one result");

        AnimeResult first = results.getFirst();
        assertNotNull(first);
    }

    @Test
    @Timeout(10)
    void searchManga_realAniList_works() throws Exception {
        List<MangaResult> results = provider.searchManga("Berserk", 3);

        assertNotNull(results);
        assertFalse(results.isEmpty(), "Expected AniList to return at least one result");

        MangaResult first = results.getFirst();
        assertNotNull(first);
    }

    @Test
    @Timeout(10)
    void searchAnime_realAniList_handlesBadRequest() {
        assertThrows(Exception.class, () -> provider.searchAnime("Cowboy Bebop", 0));
    }
}
