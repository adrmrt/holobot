package dev.zawarudo.holo.utils;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The DateTimeUtils class provides utility methods for working with date and time operations. This class contains
 * methods to format date and time strings and various conversion methods to specific formats.
 */
public final class DateTimeUtils {

    /**
     * One example per accepted input shape for {@link #parseDateTime(String)}, shown to users who ask
     * to see the supported formats. Keep in sync with the formatter lists in {@link #parseDateTime(String, ZoneId)}
     * and {@link #tryParseWithoutYear(String, ZoneId)} whenever a shape is added or removed.
     */
    public static final List<String> FORMAT_EXAMPLES = List.of(
        "13.02.2024 21:57",
        "13/02/2024 21:57",
        "2024-02-13 21:57",
        "February 13, 2024 21:57",
        "13.02.2024",
        "2026/09/12 08:00 AM",
        "2026/09/12 20:00",
        "14/08 19:30",
        "14.08 07:30 PM",
        "1708988340000"
    );

    private DateTimeUtils() {
        throw new UnsupportedOperationException();
    }

    /**
     * Formats the given timestamp in milliseconds into a localized date and time string.
     *
     * @param millis The timestamp in milliseconds to be formatted.
     * @return A string representation of the formatted date and time.
     */
    public static String formatDateTime(long millis) {
        DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMANY);
        return dateFormat.format(new Date(millis));
    }

    /**
     * This method generates a string representation of the current local date and time,
     * formatted as "yyyy-MM-dd_HH.mm.ss". This format includes the year, month, day,
     * hours, minutes, and seconds, separated by underscores and periods for readability.
     * <p>
     * Example output: "2023-12-10_15.30.45" for December 10, 2023, at 3:30:45 PM.
     *
     * @return A string representing the current date and time in the format "yyyy-MM-dd_HH.mm.ss".
     */
    public static String getCurrentDateTimeString() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss");
        return now.format(formatter);
    }

    public static long parseDateTime(@NotNull String input) {
        return parseDateTime(input, ZoneId.systemDefault());
    }

    /**
     * Parses the given date and/or time string into an epoch millisecond timestamp, using
     * {@code referenceZone} as the zone for inputs that don't specify their own offset.
     *
     * @param input         The date and/or time string to parse.
     * @param referenceZone The zone to assume when the input doesn't carry an explicit offset.
     */
    public static long parseDateTime(@NotNull String input, @NotNull ZoneId referenceZone) {
        input = input.trim();

        Pattern pattern = Pattern.compile("\\(UTC([+-]\\d+)\\)");
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            String sign = matcher.group(1).startsWith("+") ? "+" : "-";
            String hours = matcher.group(1).substring(1);
            hours = hours.length() == 1 ? "0" + hours : hours;
            String replacement = sign + hours + "00";
            input = matcher.replaceAll(replacement);
        }

        DateTimeFormatter[] dateTimeFormatters = new DateTimeFormatter[]{
            // European date time formats
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"),
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm"),
            DateTimeFormatter.ofPattern("dd. MMMM yyyy HH:mm", Locale.ENGLISH),

            // ISO 8601
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),

            // American
            DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm", Locale.ENGLISH),

            // Slash-separated, e.g. game update announcements: "2026/09/12 08:00 AM" / "2026/09/12 08:00"
            DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),

            // With timezone
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm Z"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm a Z", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm Z"),
        };

        DateTimeFormatter[] dateFormatters = new DateTimeFormatter[]{
            // TODO: Add support for entering just a year or month

            // European date time formats
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yy"),

            // ISO 8601
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),

            // American
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH),
        };

        for (DateTimeFormatter formatter : dateTimeFormatters) {
            try {
                if (formatter.toString().contains("Offset")) {
                    ZonedDateTime zonedDateTime = ZonedDateTime.parse(input, formatter);
                    return zonedDateTime.toInstant().toEpochMilli();
                } else {
                    LocalDateTime localDateTime = LocalDateTime.parse(input, formatter);
                    ZonedDateTime zonedDateTime = localDateTime.atZone(referenceZone);
                    return zonedDateTime.toInstant().toEpochMilli();
                }
            } catch (DateTimeParseException _) {
            }
        }

        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                LocalDate localDate = LocalDate.parse(input, formatter);
                ZonedDateTime zonedDateTime = localDate.atStartOfDay(referenceZone);
                return zonedDateTime.toInstant().toEpochMilli();
            } catch (DateTimeParseException _) {
            }
        }

        Long withoutYear = tryParseWithoutYear(input, referenceZone);
        if (withoutYear != null) {
            return withoutYear;
        }

        try {
            return Long.parseLong(input);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("Unsupported date time format: " + input);
        }
    }

    /**
     * Attempts to parse a date/time string that omits the year, e.g. game update announcements
     * such as "14/08 19:30 (UTC+8)". Day comes before month. The current year in {@code referenceZone} is assumed.
     *
     * @return The parsed epoch millisecond timestamp, or {@code null} if no year-less format matched.
     */
    private static Long tryParseWithoutYear(String input, ZoneId referenceZone) {
        int currentYear = LocalDate.now(referenceZone).getYear();

        for (String pattern : List.of("dd/MM hh:mm a", "dd/MM HH:mm", "dd.MM hh:mm a", "dd.MM HH:mm")) {
            DateTimeFormatter withZone = new DateTimeFormatterBuilder()
                .appendPattern(pattern + " Z")
                .parseDefaulting(ChronoField.YEAR, currentYear)
                .toFormatter(Locale.ENGLISH);
            try {
                return ZonedDateTime.parse(input, withZone).toInstant().toEpochMilli();
            } catch (DateTimeParseException _) {
            }

            DateTimeFormatter withoutZone = new DateTimeFormatterBuilder()
                .appendPattern(pattern)
                .parseDefaulting(ChronoField.YEAR, currentYear)
                .toFormatter(Locale.ENGLISH);
            try {
                return LocalDateTime.parse(input, withoutZone).atZone(referenceZone).toInstant().toEpochMilli();
            } catch (DateTimeParseException _) {
            }
        }

        return null;
    }

    /**
     * Retrieves the week day from a given date String in ISO-8601 format.
     */
    public static String getWeekDayFromDate(String dateString) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("EEEE"));
    }

    /**
     * Retrieves the week day for an instant given as epoch milliseconds, viewed in the given zone.
     */
    public static String getWeekDayFromDate(long millis, ZoneId zone) {
        ZonedDateTime zonedDateTime = Instant.ofEpochMilli(millis).atZone(zone);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH));
    }

    /**
     * Converts a given date String in ISO-8601 format from one given time zone to another.
     */
    public static String convertDate(String dateString, String targetTimeZone) {
        checkTimeZone(targetTimeZone);

        ZonedDateTime source = ZonedDateTime.parse(dateString);
        ZonedDateTime target = source.withZoneSameInstant(ZoneId.of(targetTimeZone));
        return target.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    /**
     * Checks whether a time zone is valid, otherwise throws a {@link IllegalArgumentException}
     *
     * @param timeZoneId The time zone ID as string.
     */
    public static void checkTimeZone(String timeZoneId) {
        if (!isValidTimeZone(timeZoneId)) {
            throw new IllegalArgumentException("Not a valid time zone ID: " + timeZoneId);
        }
    }

    /**
     * Checks whether a time zone is valid.
     *
     * @return True if it is a valid time zone, false otherwise.
     */
    public static boolean isValidTimeZone(String timeZoneId) {
        return ZoneId.getAvailableZoneIds().contains(timeZoneId);
    }
}
