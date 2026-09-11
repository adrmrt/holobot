package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.commands.CapturingReply;
import dev.zawarudo.holo.commands.FakeInvocation;
import dev.zawarudo.holo.core.command.CommandContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateCheckCmdTest {

    private static TimeZone originalTz;
    private static final ZoneId ZH = ZoneId.of("Europe/Zurich");

    private DateCheckCmd cmd;
    private CapturingReply reply;

    @BeforeAll
    static void pinTimezone() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZH));
    }

    @AfterAll
    static void restoreTimezone() {
        TimeZone.setDefault(originalTz);
    }

    @BeforeEach
    void setUp() {
        cmd = new DateCheckCmd();
        reply = new CapturingReply();
    }

    private void execute(String... args) {
        CommandContext ctx = new CommandContext(
            "datecheck", "datecheck", List.of(args),
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
    void validDate_showsMonday() {
        // 26 February 2024 is a Monday
        execute("26.02.2024");
        assertNotNull(reply.lastEmbed);
        assertTrue(reply.lastEmbed.getFields().stream()
            .anyMatch(f -> "Weekday".equals(f.getName()) && "Monday".equals(f.getValue())));
    }

    @Test
    void withTimezone_addsConvertedField() {
        execute("26.02.2024", "18:00", "America/New_York");
        assertNotNull(reply.lastEmbed);
        assertTrue(reply.lastEmbed.getFields().stream()
            .anyMatch(f -> "In America/New_York".equals(f.getName())));
    }
}
