package dev.zawarudo.holo.utils;

import org.apache.commons.validator.routines.UrlValidator;

/**
 * Utility methods for parsing and validating user-supplied strings.
 */
public final class ParsingUtils {

    private ParsingUtils() {
    }

    /**
     * Checks whether a string can be parsed as an integer.
     *
     * @param s The string to check.
     * @return True if the string is an integer, false otherwise.
     */
    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException _) {
            return false;
        }
    }

    /**
     * Parses a positive integer from the given string.
     *
     * @param raw The string to parse.
     * @return The parsed integer if it is {@code >= 1}, or {@code -1} if parsing fails or the value is less than 1.
     */
    public static int parsePositiveInt(String raw) {
        try {
            int n = Integer.parseInt(raw);
            return (n >= 1) ? n : -1;
        } catch (NumberFormatException _) {
            return -1;
        }
    }

    /**
     * Checks whether a string is {@code "true"} or {@code "false"}, ignoring case.
     *
     * @param s The string to check.
     * @return True if the string is a boolean, false otherwise.
     */
    public static boolean isBoolean(String s) {
        return "true".equalsIgnoreCase(s) || "false".equalsIgnoreCase(s);
    }

    /**
     * Checks whether a given URL is valid.
     *
     * @param url The URL to check.
     * @return True if the URL is valid, false otherwise.
     */
    public static boolean isValidUrl(String url) {
        return new UrlValidator().isValid(url);
    }
}
