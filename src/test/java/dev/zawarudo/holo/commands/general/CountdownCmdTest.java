package dev.zawarudo.holo.commands.general;

import dev.zawarudo.holo.modules.countdown.Countdown;
import dev.zawarudo.holo.utils.DateTimeUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Args below are given pre-tokenized, i.e. as {@code CommandListener.parseArguments}
 * would produce them (quoted phrases already grouped into one entry) - see {@code CommandListenerTest} for
 * coverage of that tokenization step itself.
 */
class CountdownCmdTest {

    @Test
    void quotedMultiWordNameWithSlashDateAndOffset_parsesCorrectly() {
        List<String> args = List.of("add", "public", "Genshin Version 7.1 Special Program",
            "2026/09/12", "08:00", "AM", "(UTC-4)");

        Optional<CountdownCmd.AddArgs> parsed = CountdownCmd.parseAddArgs(args);
        assertTrue(parsed.isPresent());

        CountdownCmd.AddArgs addArgs = parsed.get();
        assertEquals(Countdown.Visibility.PUBLIC, addArgs.visibility());
        assertEquals("Genshin Version 7.1 Special Program", addArgs.name());
        assertEquals("2026/09/12 08:00 AM (UTC-4)", addArgs.dateInput());
        assertDoesNotThrow(() -> DateTimeUtils.parseDateTime(addArgs.dateInput()));
    }

    @Test
    void unquotedSingleWordName_stillWorks() {
        List<String> args = List.of("add", "global", "NewYear", "01/01/2027", "00:00");
        CountdownCmd.AddArgs addArgs = CountdownCmd.parseAddArgs(args).orElseThrow();

        assertEquals(Countdown.Visibility.GLOBAL, addArgs.visibility());
        assertEquals("NewYear", addArgs.name());
        assertEquals("01/01/2027 00:00", addArgs.dateInput());
    }

    @Test
    void noVisibilityKeyword_defaultsToPrivate() {
        List<String> args = List.of("add", "My Birthday", "25/12/2026", "18:00");
        CountdownCmd.AddArgs addArgs = CountdownCmd.parseAddArgs(args).orElseThrow();

        assertEquals(Countdown.Visibility.PRIVATE, addArgs.visibility());
        assertEquals("My Birthday", addArgs.name());
        assertEquals("25/12/2026 18:00", addArgs.dateInput());
    }

    @Test
    void missingDate_returnsEmpty() {
        List<String> args = List.of("add", "public", "Name Only");
        assertEquals(Optional.empty(), CountdownCmd.parseAddArgs(args));
    }

    @Test
    void isVisibleTo_ownerAlwaysSeesTheirOwn() {
        Countdown priv = countdown(1L, 100L, Countdown.Visibility.PRIVATE);
        assertTrue(priv.isVisibleTo(1L, 0L));
        assertTrue(priv.isVisibleTo(1L, 999L));
    }

    @Test
    void isVisibleTo_privateHiddenFromOthers() {
        Countdown priv = countdown(1L, 100L, Countdown.Visibility.PRIVATE);
        assertFalse(priv.isVisibleTo(2L, 100L));
    }

    @Test
    void isVisibleTo_publicVisibleToSameGuildOnly() {
        Countdown pub = countdown(1L, 100L, Countdown.Visibility.PUBLIC);
        assertTrue(pub.isVisibleTo(2L, 100L));
        assertFalse(pub.isVisibleTo(2L, 200L));
        assertFalse(pub.isVisibleTo(2L, 0L));
    }

    @Test
    void isVisibleTo_globalVisibleToSameGuildOnly() {
        Countdown global = countdown(1L, 100L, Countdown.Visibility.GLOBAL);
        assertTrue(global.isVisibleTo(2L, 100L));
        assertFalse(global.isVisibleTo(2L, 200L));
    }

    private static Countdown countdown(long userId, long guildId, Countdown.Visibility visibility) {
        return new Countdown(1L, "Test", 0L, 0L, userId, guildId, visibility, 0L, false);
    }
}
