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
    }

    @Test
    void searchManga_returnsResults() throws APIException, InvalidRequestException {
        List<MalDtos.Manga> results = client.searchManga("one piece", 3);

        assertFalse(results.isEmpty(), "Expected at least one manga result");

        MalDtos.Manga first = results.getFirst();
        assertTrue(first.id() > 0);
        assertNotNull(first.title());
        assertFalse(first.title().isBlank());
    }

    @Test
    void searchAnime_noMatches_returnsEmptyList() throws APIException, InvalidRequestException {
        List<MalDtos.Anime> results = client.searchAnime("asdkjhqweiuhqwoeiuasdlkjzxc", 3);
        assertNotNull(results);
    }
}
