package dev.zawarudo.holo.database;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class required for database connection. To use operations on the database, see
 * {@link DBOperations}.
 */
public final class Database {

    // Default if nothing is provided
    private static volatile String dbPath = "./data/Holo.db";

    private Database() {
        throw new UnsupportedOperationException();
    }

    public static void setDbPath(String path) {
        if (path == null || path.isBlank()) {
            throw new IllegalArgumentException("dbPath must be non-blank");
        }
        dbPath = path.trim();
    }

    public static String getDbPath() {
        return dbPath;
    }

    /**
     * Creates a connection to the database.
     *
     * @return The {@link Connection} object to the database.
     * @throws SQLException If an error occurred while connecting.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            // This error should never occur.
            throw new NoClassDefFoundError("Could not find SQLite JDBC driver. " + e.getMessage());
        }

        Path p = Paths.get(dbPath);
        Path parent = p.getParent();

        try {
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception ioe) {
            throw new SQLException("Failed to create DB parent directory for " + p.toAbsolutePath(), ioe);
        }

        String url = "jdbc:sqlite:" + dbPath;
        return DriverManager.getConnection(url);
    }

    public static boolean tableExists(String tableName) throws SQLException {
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }

    public static List<String> getColumnNames(String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (Connection conn = getConnection();
             ResultSet rs = conn.getMetaData().getColumns(null, null, tableName, null)) {
            while (rs.next()) {
                columns.add(rs.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }
}
