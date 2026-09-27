package dev.zawarudo.holo.modules;

import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.*;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A wrapper class for the <a href="https://www.random.org/">random.org</a> integer generator.
 */
public final class RandomOrgClient {

    private RandomOrgClient() {
    }

    /**
     * Returns a random integer between min and max (both inclusive) from random.org,
     * falling back to a local generator if random.org is unavailable.
     */
    public static int nextInt(int min, int max) {
        try {
            return fetchInt(min, max);
        } catch (Exception _) {
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
    }

    /**
     * Fetches a random integer between min and max (both inclusive) from random.org without a fallback.
     */
    public static int fetchInt(int min, int max) throws APIException {
        String url = "https://www.random.org/integers/?num=1&min=" + min + "&max=" + max + "&col=1&base=10&format=plain";
        try {
            String line = HoloHttp.readLine(url).trim();
            return Integer.parseInt(line);
        } catch (HttpStatusException e) {
            throw new APIException("random.org returned HTTP " + e.getStatusCode(), e);
        } catch (HttpTransportException e) {
            throw new APIException("I/O error contacting random.org", e);
        } catch (NumberFormatException e) {
            throw new APIException("random.org returned an invalid number", e);
        }
    }
}
