package dev.zawarudo.holo.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeTest {

    private static TimeZone originalTz;
    private static final ZoneId ZH = ZoneId.of("Europe/Zurich");

    @BeforeAll
    static void pinTimezone() {
        originalTz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(ZH));
    }

    @AfterAll
    static void restoreTimezone() {
        TimeZone.setDefault(originalTz);
    }

    @Test
    void testAmericanFormatWithTimezone() {
        String input = "February 26, 2024 23:59 (UTC+8)";
        long expected = ZonedDateTime.of(2024, 2, 26, 23, 59, 0, 0, ZoneId.of("UTC+8"))
            .toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @Test
    void testAmericanFormat() {
        String input = "February 26, 2024 23:59";
        long expected = LocalDateTime.of(2024, 2, 26, 23, 59)
            .atZone(ZH).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @Test
    void testAmericanFormatWithoutTime() {
        String input = "February 26, 2024";
        long expected = LocalDateTime.of(2024, 2, 26, 0, 0)
            .atZone(ZH).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @Test
    void testEuropeanFormat() {
        String input = "26. February 2024 23:59";
        long expected = LocalDateTime.of(2024, 2, 26, 23, 59)
            .atZone(ZH).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @Test
    void testMillis() {
        String input = "1708988340000";
        long expected = LocalDateTime.of(2024, 2, 26, 23, 59)
            .atZone(ZH).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @Test
    void testEuropean() {
        assertDoesNotThrow(() -> DateTimeUtils.parseDateTime("14.02.24 18:00"));
        assertDoesNotThrow(() -> DateTimeUtils.parseDateTime("14.02.24"));
    }
}
