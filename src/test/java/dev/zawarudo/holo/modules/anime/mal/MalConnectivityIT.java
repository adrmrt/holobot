package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.HoloHttp;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bare-bones smoke test for reaching the official MAL API with a client ID.
 */
class MalConnectivityIT {

    private static final String BASE_URL = "https://api.myanimelist.net/v2";

    private static String clientId;

    @BeforeAll
    static void setup() {
        Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

        clientId = dotenv.get("MAL_CLIENT_ID");

        Assumptions.assumeTrue(
            clientId != null && !clientId.isBlank(),
            "MAL_CLIENT_ID not set - skipping MAL connectivity test"
        );
    }

    @Test
    void searchAnime_withClientIdHeader_returnsResults() throws Exception {
        String url = BASE_URL + "/anime?q=one+piece&limit=1&fields=id,title,synopsis,mean";
        Map<String, String> headers = Map.of("X-MAL-CLIENT-ID", clientId);

        JsonObject json = HoloHttp.getJsonObject(url, headers);
        JsonArray data = json.getAsJsonArray("data");

        assertNotNull(data);
        assertFalse(data.isEmpty(), "Expected at least one search result");

        JsonObject node = data.get(0).getAsJsonObject().getAsJsonObject("node");
        assertTrue(node.get("id").getAsInt() > 0);
        assertFalse(node.get("title").getAsString().isBlank());
    }
}
