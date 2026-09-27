package dev.zawarudo.holo.modules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.*;

class RandomOrgClientIT {

    // Calls fetchInt because nextInt silently falls back to a local generator on failure
    @Test
    @Timeout(10)
    void fetchInt_returnsNumberInRange() throws Exception {
        int n = RandomOrgClient.fetchInt(1, 6);

        assertTrue(n >= 1 && n <= 6, "Out of range: " + n);
    }
}
