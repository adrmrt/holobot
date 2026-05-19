package dev.zawarudo.holo.utils;

import dev.zawarudo.holo.core.Bootstrap;

/**
 * Utility class for retrieving version information at runtime.
 */
public final class VersionInfo {
    private VersionInfo() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns the bot version from the JAR manifest, or {@code dev} if running outside a packaged JAR (e.g. from an IDE).
     */
    public static String getBotVersion() {
        Package p = Bootstrap.class.getPackage();
        String v = (p != null) ? p.getImplementationVersion() : null;
        return (v == null || v.isBlank()) ? "dev" : v;
    }

    /**
     * Returns the Java runtime version.
     */
    public static String getJavaVersion() {
        String v = System.getProperty("java.version");
        return (v == null || v.isBlank()) ? "unknown" : v;
    }
}