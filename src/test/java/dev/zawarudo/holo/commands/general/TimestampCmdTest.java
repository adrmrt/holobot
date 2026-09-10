package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CapturingReply;
import dev.zawarudo.holo.commands.FakeInvocation;
import dev.zawarudo.holo.core.command.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimestampCmdTest {

    private TimestampCmd cmd;
    private CapturingReply reply;

    @BeforeEach
    void setUp() {
        cmd = new TimestampCmd();
        reply = new CapturingReply();
    }

    private void execute(String... args) {
        CommandContext ctx = new CommandContext(
            "timestamp", "timestamp", List.of(args),
            new FakeInvocation(), reply,
            false, false, "!", null
        );
        cmd.execute(ctx);
    }

    @Test
    void noArgs_showsError() {
        execute();
        assertNotNull(reply.lastError);
        assertNull(reply.lastEmbed);
    }

    @Test
    void invalidDate_showsError() {
        execute("not", "a", "date");
        assertNotNull(reply.lastError);
    }

    @Test
    void validDate_showsAllEightStyles() {
        execute("26.02.2024", "18:00");
        assertNotNull(reply.lastEmbed);
        assertEquals(8, reply.lastEmbed.getFields().size());
        assertTrue(reply.lastEmbed.getFields().getFirst().getValue().contains("<t:"));
    }
}
