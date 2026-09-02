package dev.zawarudo.holo.modules.minecraft;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Minimal client for the Minecraft/Source RCON protocol, used to run remote console
 * commands (e.g. spark's {@code tps}/{@code health} reports) against a server.
 *
 * @see <a href="https://wiki.vg/RCON">RCON protocol reference</a>
 */
public class RconClient {

    private static final int TYPE_RESPONSE = 0;
    private static final int TYPE_EXEC_COMMAND = 2;
    private static final int TYPE_AUTH = 3;
    private static final int SOCKET_TIMEOUT_MS = 8_000;

    /**
     * Authenticates and runs a single command against the RCON server, then closes the connection.
     *
     * @throws IOException              if the connection fails or authentication is rejected
     * @throws IllegalArgumentException if the password is blank
     */
    public String sendCommand(String host, int port, String password, String command) throws IOException {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("RCON password must be provided");
        }

        try (Socket socket = new Socket(host, port)) {
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            OutputStream out = socket.getOutputStream();
            InputStream in = socket.getInputStream();

            writePacket(out, 1, TYPE_AUTH, password);
            Packet authResponse = readPacket(in);
            if (authResponse.requestId() == -1) {
                throw new IOException("RCON authentication failed - check the configured password");
            }

            writePacket(out, 2, TYPE_EXEC_COMMAND, command);
            Packet commandResponse = readPacket(in);
            return commandResponse.body();
        }
    }

    private void writePacket(OutputStream out, int requestId, int type, String payload) throws IOException {
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        int length = 4 + 4 + payloadBytes.length + 2;

        ByteBuffer buffer = ByteBuffer.allocate(4 + length).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(length);
        buffer.putInt(requestId);
        buffer.putInt(type);
        buffer.put(payloadBytes);
        buffer.put((byte) 0);
        buffer.put((byte) 0);

        out.write(buffer.array());
        out.flush();
    }

    private Packet readPacket(InputStream in) throws IOException {
        byte[] lengthBytes = in.readNBytes(4);
        if (lengthBytes.length < 4) {
            throw new IOException("RCON connection closed unexpectedly");
        }
        int length = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN).getInt();

        byte[] body = in.readNBytes(length);
        if (body.length < length) {
            throw new IOException("RCON connection closed unexpectedly");
        }
        ByteBuffer buffer = ByteBuffer.wrap(body).order(ByteOrder.LITTLE_ENDIAN);
        int requestId = buffer.getInt();
        buffer.getInt(); // type, unused
        byte[] payload = new byte[length - 4 - 4 - 2];
        buffer.get(payload);
        return new Packet(requestId, new String(payload, StandardCharsets.UTF_8));
    }

    private record Packet(int requestId, String body) {
    }
}
