package dev.zawarudo.holo.commands.image;

import dev.zawarudo.holo.commands.CapturingReply;
import dev.zawarudo.holo.commands.FakeInvocation;
import dev.zawarudo.holo.core.command.CommandContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpCmdTest {

    private HttpCmd cmd;
    private CapturingReply reply;

    @BeforeEach
    void setUp() {
        cmd = new HttpCmd();
        reply = new CapturingReply();
    }

    private void execute(String... args) {
        CommandContext ctx = new CommandContext(
            "http", "http", List.of(args),
            new FakeInvocation(), reply,
            false, false, "!", null
        );
        cmd.execute(ctx);
    }

    @Test
    void knownCode_returnsCorrectUrl() {
        execute("200");
        assertEquals("https://http.cat/200", reply.lastText);
    }

    @Test
    void anotherKnownCode_returnsCorrectUrl() {
        execute("500");
        assertEquals("https://http.cat/500", reply.lastText);
    }

    @Test
    void code404_isValidAndReturnsItself() {
        execute("404");
        assertEquals("https://http.cat/404", reply.lastText);
    }

    @Test
    void firstCodeInList_isValid() {
        execute("100");
        assertEquals("https://http.cat/100", reply.lastText);
    }

    @Test
    void lastCodeInList_isValid() {
        execute("599");
        assertEquals("https://http.cat/599", reply.lastText);
    }

    @Test
    void unknownNumericCode_fallsBackTo404() {
        execute("999");
        assertEquals("https://http.cat/404", reply.lastText);
    }

    @Test
    void nonNumericInput_fallsBackTo404() {
        execute("teapot");
        assertEquals("https://http.cat/404", reply.lastText);
    }

    @Test
    void noArgs_fallsBackTo404() {
        execute();
        assertEquals("https://http.cat/404", reply.lastText);
    }
}
