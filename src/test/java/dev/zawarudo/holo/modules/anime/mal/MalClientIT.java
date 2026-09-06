package dev.zawarudo.holo.modules.anime.mal;

import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MalClientIT {

    private static MalApiClient client;

    @BeforeAll
    static void setup() {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

        String clientId = dotenv.get("MAL_CLIENT_ID");

        Assumptions.assumeTrue(
            clientId != null && !clientId.isBlank(),
            "MAL_CLIENT_ID not set - skipping MAL client tests"
        );

        client = new MalApiClient(clientId);
    }

    @Test
    void searchAnime_returnsResults() throws APIException, InvalidRequestException {
        List<MalDtos.Anime> results = client.searchAnime("one piece", 3);

        assertFalse(results.isEmpty(), "Expected at least one anime result");

        MalDtos.Anime first = results.getFirst();
        assertTrue(first.id() > 0);
        assertNotNull(first.title());
        assertFalse(first.title().isBlank());

        // Fields requiring @SerializedName (snake_case wire keys) actually round-trip -
        // regression guard for a bug where the reflective fields= builder silently sent
        // camelCase names MAL doesn't recognize, leaving these null.
        assertNotNull(first.mediaType(), "mediaType should be populated (e.g. \"tv\", \"movie\")");
        assertFalse(first.mediaType().isBlank());
        assertNotNull(first.mainPicture(), "mainPicture should be populated");
        assertNotNull(first.startSeason(), "startSeason should be populated for an aired anime");
    }

    @Test
    void searchManga_returnsResults() throws APIException, InvalidRequestException {
        List<MalDtos.Manga> results = client.searchManga("one piece", 3);

        assertFalse(results.isEmpty(), "Expected at least one manga result");

        MalDtos.Manga first = results.getFirst();
        assertTrue(first.id() > 0);
        assertNotNull(first.title());
        assertFalse(first.title().isBlank());

        assertNotNull(first.mediaType(), "mediaType should be populated (e.g. \"manga\")");
        assertFalse(first.mediaType().isBlank());
        assertNotNull(first.authors(), "authors should be populated via the NestedFields selector");
        assertFalse(first.authors().isEmpty());
        assertNotNull(first.authors().getFirst().node().firstName());
    }

    @Test
    void searchAnime_noMatches_returnsEmptyList() throws APIException, InvalidRequestException {
        List<MalDtos.Anime> results = client.searchAnime("asdkjhqweiuhqwoeiuasdlkjzxc", 3);
        assertNotNull(results);
        assertTrue(results.isEmpty(), "Expected no results for a nonsense search term");
    }
}
