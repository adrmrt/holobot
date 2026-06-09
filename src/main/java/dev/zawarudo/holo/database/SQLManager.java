package dev.zawarudo.holo.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class SQLManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLManager.class);
    private static final String SQL_DIRECTORY_PATH = "database";
    private final Map<String, String> sqlStatements;

    /**
     * Initializes the SQLManager by loading SQL statements from the defined directory.
     *
     * @throws IOException If there is an issue accessing or reading the SQL files.
     */
    public SQLManager() throws IOException {
        sqlStatements = loadSQLStatements();
        LOGGER.info("Loaded {} SQL statements!", sqlStatements.size());
    }

    /**
     * Retrieves a SQL statement by its name.
     *
     * @param name The name of the SQL file, with or without the ".sql" extension.
     * @return The SQL statement as a string.
     * @throws IllegalArgumentException If there is no SQL statement with the given name.
     */
    public String getStatement(String name) {
        String key = keyFromRelativePath(name);
        String stmt = sqlStatements.get(key);
        if (stmt == null) {
            throw new IllegalArgumentException("No SQL file found with name: " + key);
        }
        return stmt;
    }

    /**
     * Loads SQL statements from the defined directory, handling both IDE and Jar scenarios.
     *
     * @return A map of file names to their SQL content.
     */
    private Map<String, String> loadSQLStatements() throws IOException {
        URL resourceURL = getClass().getClassLoader().getResource(SQL_DIRECTORY_PATH);
        if (resourceURL == null) {
            throw new IOException("SQL directory not found: " + SQL_DIRECTORY_PATH);
        }

        if ("file".equals(resourceURL.getProtocol())) {
            LOGGER.info("Loading SQL files from file system...");
            return loadFromDirectory(new File(resourceURL.getPath()));
        } else if ("jar".equals(resourceURL.getProtocol())) {
            LOGGER.info("Loading SQL files from JAR...");
            return loadFromJar(resourceURL);
        } else {
            throw new IOException("Unsupported protocol for resource URL: " + resourceURL);
        }
    }

    /**
     * Loads SQL files from a directory in the file system.
     *
     * @param directory The directory to scan.
     * @return A map of file names to their SQL content.
     */
    private Map<String, String> loadFromDirectory(File directory) throws IOException {
        Map<String, String> statements = new HashMap<>();

        if (!directory.isDirectory()) {
            throw new IOException("Expected a directory but found: " + directory.getPath());
        }

        // Recursively walk the database/ folder
        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            paths.filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().endsWith(".sql"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p, StandardCharsets.UTF_8);

                        // Build key relative to database/
                        Path rel = directory.toPath().relativize(p);
                        String key = keyFromRelativePath(rel.toString());

                        statements.put(key, content);
                        LOGGER.debug("Loaded SQL file: {} -> key={}", rel, key);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        return statements;
    }

    private Map<String, String> loadFromJar(URL resourceURL) throws IOException {
        Map<String, String> statements = new HashMap<>();

        JarURLConnection jarConnection = (JarURLConnection) resourceURL.openConnection();
        JarFile jarFile = jarConnection.getJarFile();

        String prefix = SQL_DIRECTORY_PATH + "/";

        jarFile.stream()
            .filter(entry -> !entry.isDirectory())
            .filter(entry -> entry.getName().startsWith(prefix))
            .filter(entry -> entry.getName().endsWith(".sql"))
            .forEach(entry -> {
                try (InputStream is = getClass().getClassLoader().getResourceAsStream(entry.getName())) {
                    if (is == null) return;

                    String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

                    // Key relative to "database/"
                    String rel = entry.getName().substring(prefix.length());
                    String key = keyFromRelativePath(rel);

                    statements.put(key, content);
                    LOGGER.debug("Loaded SQL file: {} -> key={}", rel, key);
                } catch (IOException e) {
                    LOGGER.error("Failed to load SQL file: {}", entry.getName(), e);
                }
            });

        return statements;
    }

    private String keyFromRelativePath(String relativePath) {
        String normalized = relativePath.replace('\\', '/');
        normalized = normalized.replace(" ", "-");
        normalized = normalized.replaceAll("\\.sql$", "");
        return normalized;
    }
}
