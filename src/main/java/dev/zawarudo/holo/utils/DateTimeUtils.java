package dev.zawarudo.holo.utils;

import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The DateTimeUtils class provides utility methods for working with date and time operations. This class contains
 * methods to format date and time strings and various conversion methods to specific formats.
 */
public final class DateTimeUtils {

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

            // With timezone
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd/MM/yy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm Z"),
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm Z"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm Z"),
            DateTimeFormatter.ofPattern("MMMM d, yyyy HH:mm Z", Locale.ENGLISH),
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
                    ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                    return zonedDateTime.toInstant().toEpochMilli();
                }
            } catch (DateTimeParseException _) {
            }
        }

        for (DateTimeFormatter formatter : dateFormatters) {
            try {
                LocalDate localDate = LocalDate.parse(input, formatter);
                ZonedDateTime zonedDateTime = localDate.atStartOfDay(ZoneId.systemDefault());
                return zonedDateTime.toInstant().toEpochMilli();
            } catch (DateTimeParseException _) {
            }
        }

        try {
            return Long.parseLong(input);
        } catch (NumberFormatException _) {
            throw new IllegalArgumentException("Unsupported date time format: " + input);
        }
    }

    /**
     * Retrieves the week day from a given date String in ISO-8601 format.
     */
    public static String getWeekDayFromDate(String dateString) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        return zonedDateTime.format(DateTimeFormatter.ofPattern("EEEE"));
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
