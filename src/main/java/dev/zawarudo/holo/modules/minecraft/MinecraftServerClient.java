package dev.zawarudo.holo.modules.minecraft;

import net.lenni0451.mcping.MCPing;
import net.lenni0451.mcping.responses.MCPingResponse;

/**
 * Thin wrapper around MCPing to fetch a Java Edition server's status via Server List Ping.
 */
public class MinecraftServerClient {

    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 8_000;
    private static final int PROTOCOL_VERSION = 763; // 1.20.1, servers reply with status regardless of match

    /**
     * Pings the given server and returns its status.
     *
     * @throws RuntimeException if the server is unreachable or doesn't respond in time
     */
    public MCPingResponse ping(String host, int port) {
        return MCPing.pingModern(PROTOCOL_VERSION)
            .address(host, port)
            .timeout(CONNECT_TIMEOUT_MS, READ_TIMEOUT_MS)
            .getSync();
    }
}
