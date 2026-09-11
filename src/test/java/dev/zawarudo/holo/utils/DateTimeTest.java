package dev.zawarudo.holo.utils;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void testParseDateTimeWithReferenceZone() {
        String input = "26.02.2024 18:00";
        ZoneId tokyo = ZoneId.of("Asia/Tokyo");
        long expected = LocalDateTime.of(2024, 2, 26, 18, 0).atZone(tokyo).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input, tokyo));
    }

    @Test
    void testGetWeekDayFromMillis() {
        // 26 February 2024 is a Monday
        long millis = LocalDateTime.of(2024, 2, 26, 12, 0).atZone(ZH).toInstant().toEpochMilli();
        assertEquals("Monday", DateTimeUtils.getWeekDayFromDate(millis, ZH));
    }

    @Test
    void testHoyoverseAnnouncementFormat() {
        // Format used by Genshin Impact/HoYoverse special program announcements
        String input = "2026/09/12 08:00 AM (UTC-4)";
        long expected = ZonedDateTime.of(2026, 9, 12, 8, 0, 0, 0, ZoneId.of("UTC-4"))
            .toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime(input));
    }

    @ParameterizedTest
    @CsvSource({
        "2026/09/12 08:00 AM (UTC-4), 14, +02:00", // 12 September is within CEST
        "2026/01/12 08:00 AM (UTC-4), 13, +01:00"  // 12 January is within CET, not CEST
    })
    void testHoyoverseAnnouncementFormat_convertsWithCorrectDstOffset(String input, int expectedHour, String expectedOffset) {
        long millis = DateTimeUtils.parseDateTime(input);
        ZonedDateTime converted = ZonedDateTime.ofInstant(Instant.ofEpochMilli(millis), ZH);
        assertEquals(expectedHour, converted.getHour());
        assertEquals(expectedOffset, converted.getOffset().getId());
    }

    @ParameterizedTest
    @CsvSource({
        "2026/09/12 08:00 AM (UTC-4), 2026/09/12 08:00 (UTC-4)", // AM notation matches its 24h equivalent
        "2026/09/12 08:00 PM (UTC-4), 2026/09/12 20:00 (UTC-4)", // PM notation matches its 24h equivalent
        "2026/09/12 08:00 AM,         2026/09/12 08:00"          // same, without an explicit timezone
    })
    void testSlashFormat_amPmNotationMatches24Hour(String amPmInput, String h24Input) {
        assertEquals(DateTimeUtils.parseDateTime(amPmInput), DateTimeUtils.parseDateTime(h24Input));
    }

    @Test
    void testYearlessFormat_assumesCurrentYear() {
        // Format used by e.g. Honkai: Star Rail livestream announcements: "08/14 19:30 (UTC+8)"
        int currentYear = ZonedDateTime.now(ZH).getYear();
        long expected = ZonedDateTime.of(currentYear, 8, 14, 19, 30, 0, 0, ZoneId.of("UTC+8"))
            .toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime("08/14 19:30 (UTC+8)"));
    }

    @Test
    void testYearlessFormat_withoutTimezoneUsesReferenceZone() {
        int currentYear = ZonedDateTime.now(ZH).getYear();
        long expected = LocalDateTime.of(currentYear, 8, 14, 19, 30).atZone(ZH).toInstant().toEpochMilli();
        assertEquals(expected, DateTimeUtils.parseDateTime("08/14 19:30"));
    }

    @Test
    void testYearlessFormat_invalidDateStillThrows() {
        assertThrows(IllegalArgumentException.class, () -> DateTimeUtils.parseDateTime("13/40 19:30 (UTC+8)"));
    }

    @Test
    void formatExamples_allActuallyParse() {
        // Guards DateTimeUtils.FORMAT_EXAMPLES against silently drifting out of sync with the
        // formatters in parseDateTime/tryParseWithoutYear: every advertised example must parse.
        for (String example : DateTimeUtils.FORMAT_EXAMPLES) {
            assertDoesNotThrow(() -> DateTimeUtils.parseDateTime(example),
                "Advertised format example failed to parse: " + example);
        }
    }
}
