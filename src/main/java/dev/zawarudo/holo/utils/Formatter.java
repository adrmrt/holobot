package dev.zawarudo.holo.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public final class Formatter {

    private Formatter() {
    }

    public static String formatTrackTime(long timeInMillis) {
        long hours = timeInMillis / TimeUnit.HOURS.toMillis(1);
        long minutes = timeInMillis % TimeUnit.HOURS.toMillis(1) / TimeUnit.MINUTES.toMillis(1);
        long seconds = timeInMillis % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String formatTime(long timeInMillis) {
        long days = timeInMillis / TimeUnit.DAYS.toMillis(1);
        long hours = timeInMillis % TimeUnit.DAYS.toMillis(1) / TimeUnit.HOURS.toMillis(1);
        long minutes = timeInMillis % TimeUnit.HOURS.toMillis(1) / TimeUnit.MINUTES.toMillis(1);
        long seconds = timeInMillis % TimeUnit.MINUTES.toMillis(1) / TimeUnit.SECONDS.toMillis(1);
        StringBuilder formatted = new StringBuilder(100);
        if (days > 0) {
            formatted.append(days).append(" days, ");
        }
        if (hours > 0) {
            formatted.append(hours).append(" hours, ");
        }
        if (minutes > 0) {
            formatted.append(minutes).append(" minutes and ");
        }
        return formatted.append(seconds).append(" seconds").toString();
    }

    /**
     * Returns the same String but with the first letter in uppercase.
     */
    public static String capitalize(String string) {
        return string.substring(0, 1).toUpperCase(Locale.UK) + string.substring(1);
    }

    /**
     * Encodes a given URL string to avoid unsafe characters.
     *
     * @param link The URL string to encode.
     * @return The encoded URL string.
     */
    public static String encodeUrl(String link) {
        return URLEncoder.encode(link, StandardCharsets.US_ASCII).toLowerCase(Locale.UK);
    }

    /**
     * Formats a Japanese name properly by reversing the order.
     */
    public static String reverseJapaneseName(@NotNull String name) {
        String[] nameParts = name.split(",");
        if (nameParts.length == 1) {
            return name;
        }
        return nameParts[1].trim() + " " + nameParts[0].trim();
    }

    /**
     * Truncates a given string to a specified maximum length and appends three dots ("...")
     * at the end if truncation occurs. This method is useful for ensuring that strings
     * do not exceed a certain length while providing a visual indication that they have
     * been shortened.
     *
     * @param input     The string to be truncated. If this is {@code null}, the method
     *                  will return {@code null}.
     * @param maxLength The maximum allowed length of the string. It must be greater than
     *                  or equal to 4, as 3 characters are reserved for the ellipsis ("...").
     *                  If {@code maxLength} is less than 4, the method returns the input string
     *                  as-is, without truncation.
     * @return A truncated string with an appended ellipsis if the original string's length
     * exceeded {@code maxLength}. If the original string is shorter than or equal
     * to {@code maxLength}, or if {@code maxLength} is less than 4, the original
     * string is returned as-is. Returns {@code null} if the input string is {@code null}.
     */
    public static String truncate(String input, int maxLength) {
        if (input == null || maxLength < 4) {
            return input;
        }

        if (input.length() > maxLength) {
            return input.substring(0, maxLength - 3) + "...";
        }

        return input;
    }

    /**
     * Converts a Color object into its hexadecimal color code representation.
     * <p>
     * This method takes a Color object and converts it to a string representing its
     * hexadecimal color code. The format of the hexadecimal code is "#RRGGBB", where
     * RR, GG, and BB are two-digit hexadecimal values for the red, green, and blue
     * components of the color, respectively.
     * <p>
     * Example: If the Color object represents the color red (255,0,0), the output will be "#ff0000".
     *
     * @param color The Color object to be converted into a hexadecimal color code.
     * @return A string representing the hexadecimal color code of the provided Color object.
     */
    public static String getColorHexString(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static String checkWindowsCompatibleName(@NotNull String fileName) {
        String invalidCharsRegex = "[/\\\\:*?\"<>|]";
        String invalidEndRegex = "[ .]+$";

        String windowsCompatibleName = fileName.replaceAll(invalidCharsRegex, "_");
        windowsCompatibleName = windowsCompatibleName.replaceAll(invalidEndRegex, "_");


        if (windowsCompatibleName.matches("^(?i)(con|prn|aux|nul|com[1-9]|lpt[1-9])\\.png$")) {
            windowsCompatibleName = "_" + windowsCompatibleName;
        }

        // Shouldn't exceed 255 characters (Windows file name limit)
        int maxFileNameLength = 255 - ".png".length();
        if (windowsCompatibleName.length() > maxFileNameLength) {
            windowsCompatibleName = windowsCompatibleName.substring(0, maxFileNameLength);
        }

        return windowsCompatibleName + ".png";
    }

    public static String removeStartingChar(String string, String character) {
        return string.substring(0, string.indexOf(character));
    }

    /**
     * Formats a given String the following way: lightning-rod -> Lightning Rod
     */
    public static String formatPokemonName(String name) {
        return Arrays.stream(name.replace("--", "-").split("-"))
                .map(Formatter::capitalize)
                .collect(Collectors.joining(" "))
                .replace("Mr", "Mr.")
                .replace("Jr", "Jr.");
    }

    public static String getRelativeTime(long millis) {
        long diff = Math.abs(millis - System.currentTimeMillis());

        long[] timeUnits = {
                TimeUnit.MILLISECONDS.toDays(diff),
                TimeUnit.MILLISECONDS.toHours(diff) % 24,
                TimeUnit.MILLISECONDS.toMinutes(diff) % 60,
                TimeUnit.MILLISECONDS.toSeconds(diff) % 60
        };
        String[] unitNames = {" day", " hour", " minute", " second"};

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < timeUnits.length; i++) {
            if (timeUnits[i] > 0) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(timeUnits[i]).append(unitNames[i]).append(timeUnits[i] > 1 ? "s" : "");
            }
        }

        if (sb.isEmpty()) {
            sb.append("less than a second");
        }

        return Formatter.capitalize(sb.toString());
    }

    public static String asCodeBlock(String text) {
        return String.format("```%n%s%n```", text);
    }

    /**
     * Converts Merriam-Webster formatting tokens (e.g. <code>{it}</code>, <code>{b}</code>, <code>{bc}</code>)
     * into Discord-compatible Markdown and punctuation.
     * <p>
     * See <a href="https://dictionaryapi.com/products/json#sec-2.tokens">official documentation</a>.
     */
    public static String mwToDiscord(@NotNull String s) {
        if (s.isBlank()) return "";

        // Punctuation tokens
        s = s.replace("{bc}", "**: ** ");
        s = s.replace("{ldquo}", "“"); // “
        s = s.replace("{rdquo}", "”"); // ”
        s = s.replace("{p_br}", "\n\n");

        // Formatting tokens (basic)
        s = s.replace("{it}", "*").replace("{/it}", "*");
        s = s.replace("{b}", "**").replace("{/b}", "**");

        // Small caps: Discord doesn't support it; bold is a decent approximation
        s = s.replace("{sc}", "**").replace("{/sc}", "**");

        // Sup/sub: no native Markdown in Discord; keep readable
        s = s.replace("{sup}", "^(").replace("{/sup}", ")");
        s = s.replace("{inf}", "_(").replace("{/inf}", ")");

        // Link-ish tokens: keep only the display text
        // e.g. {a_link|ammonia} -> ammonia
        s = s.replaceAll("\\{[a-z_]+\\|([^}|]+)(?:\\|[^}]*)?}", "$1");

        // Any remaining "{...}" tokens you don't handle yet -> strip token markers but keep content conservative
        // (prevents raw tokens leaking into Discord)
        s = s.replaceAll("\\{/?[a-z_]+}", "");

        return s.trim();
    }

    /**
     * Converts basic HTML formatting into Discord-compatible Markdown.
     */
    public static @Nullable String htmlToDiscord(@Nullable String input) {
        if (input == null || input.isBlank()) return null;

        String s = input;

        // Line breaks
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");

        // Italic
        s = s.replaceAll("(?i)<i>(.*?)</i>", "*$1*");
        s = s.replaceAll("(?i)<em>(.*?)</em>", "*$1*");

        // Bold
        s = s.replaceAll("(?i)<b>(.*?)</b>", "**$1**");
        s = s.replaceAll("(?i)<strong>(.*?)</strong>", "**$1**");

        // Underline
        s = s.replaceAll("(?i)<u>(.*?)</u>", "__$1__");

        // Strikethrough
        s = s.replaceAll("(?i)<s>(.*?)</s>", "~~$1~~");
        s = s.replaceAll("(?i)<strike>(.*?)</strike>", "~~$1~~");

        // Remove remaining HTML tags
        s = s.replaceAll("<[^>]+>", "");

        // Decode common HTML entities
        s = s.replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&#039;", "'")
                .replace("&nbsp;", " ");

        // Normalize line endings
        s = s.replace("\r", "");

        // Trim whitespace around newlines
        s = s.replaceAll("[ \\t]+\\n", "\n");
        s = s.replaceAll("\\n[ \\t]+", "\n");

        // Collapse excessive blank lines
        s = s.replaceAll("\\n{3,}", "\n\n");

        // Remove empty line before list items
        s = s.replaceAll("\\n\\n(- )", "\n$1");

        return s.trim();
    }
}