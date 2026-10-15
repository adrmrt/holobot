package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class PokemonDao {

    /**
     * A spawn channel and the wild Pokemon waiting in it, if any ({@code pokemonId} and
     * {@code messageId} are {@code 0} when the channel is empty).
     */
    public record SpawnRow(long channelId, long guildId, int pokemonId, boolean shiny, long messageId, long spawnedAt) {
        public boolean hasPokemon() {
            return pokemonId > 0 && messageId > 0;
        }
    }

    /** All catches of one species by one user. */
    public record CollectionEntry(int speciesId, String name, int total, int shinies) {
    }

    /** A user's point total in a guild. */
    public record LeaderboardEntry(long userId, long points) {
    }

    private final SQLManager sql;

    public PokemonDao(SQLManager sql) {
        this.sql = sql;
    }

    public List<SpawnRow> findAllSpawns() throws SQLException {
        String stmt = sql.getStatement("pokemon/select-spawns");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            List<SpawnRow> rows = new ArrayList<>();
            while (rs.next()) {
                rows.add(new SpawnRow(
                    rs.getLong("channel_id"),
                    rs.getLong("guild_id"),
                    rs.getInt("pokemon_id"),
                    rs.getBoolean("shiny"),
                    rs.getLong("message_id"),
                    rs.getLong("spawned_at")
                ));
            }
            return rows;
        }
    }

    /** Registers a spawn channel. Does nothing if it is already registered. */
    public void insertSpawn(long channelId, long guildId) throws SQLException {
        String stmt = sql.getStatement("pokemon/insert-spawn");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, channelId);
            ps.setLong(2, guildId);
            ps.executeUpdate();
        }
    }

    public void updateSpawn(long channelId, int pokemonId, boolean shiny, long messageId, long spawnedAt) throws SQLException {
        String stmt = sql.getStatement("pokemon/update-spawn");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setInt(1, pokemonId);
            ps.setBoolean(2, shiny);
            ps.setLong(3, messageId);
            ps.setLong(4, spawnedAt);
            ps.setLong(5, channelId);
            ps.executeUpdate();
        }
    }

    /** Marks the spawn channel as empty, keeping it registered. */
    public void clearSpawn(long channelId) throws SQLException {
        executeForChannel("pokemon/clear-spawn", channelId);
    }

    public void deleteSpawn(long channelId) throws SQLException {
        executeForChannel("pokemon/delete-spawn", channelId);
    }

    public void insertCatch(long userId, long guildId, int speciesId, String name, boolean shiny, int points, long caughtAt) throws SQLException {
        String stmt = sql.getStatement("pokemon/insert-catch");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, userId);
            ps.setLong(2, guildId);
            ps.setInt(3, speciesId);
            ps.setString(4, name);
            ps.setBoolean(5, shiny);
            ps.setInt(6, points);
            ps.setLong(7, caughtAt);
            ps.executeUpdate();
        }
    }

    /** Returns the user's catches across all guilds, one entry per species, ordered by Pokedex number. */
    public List<CollectionEntry> findCollection(long userId) throws SQLException {
        String stmt = sql.getStatement("pokemon/select-collection");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                List<CollectionEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    entries.add(new CollectionEntry(
                        rs.getInt("species_id"),
                        rs.getString("name"),
                        rs.getInt("total"),
                        rs.getInt("shinies")
                    ));
                }
                return entries;
            }
        }
    }

    public long findPoints(long guildId, long userId) throws SQLException {
        String stmt = sql.getStatement("pokemon/select-points");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("points") : 0;
            }
        }
    }

    /** Returns every user's point total in the guild, highest first. */
    public List<LeaderboardEntry> findLeaderboard(long guildId) throws SQLException {
        String stmt = sql.getStatement("pokemon/select-leaderboard");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                List<LeaderboardEntry> entries = new ArrayList<>();
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(rs.getLong("user_id"), rs.getLong("points")));
                }
                return entries;
            }
        }
    }

    /** Deletes all catches made in the guild. Returns the number of deleted catches. */
    public int deleteCatches(long guildId) throws SQLException {
        String stmt = sql.getStatement("pokemon/delete-catches-by-guild");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, guildId);
            return ps.executeUpdate();
        }
    }

    /** Deletes the user's catches made in the guild. Returns the number of deleted catches. */
    public int deleteCatches(long guildId, long userId) throws SQLException {
        String stmt = sql.getStatement("pokemon/delete-catches-by-user");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, guildId);
            ps.setLong(2, userId);
            return ps.executeUpdate();
        }
    }

    public int countCatches(long userId, int speciesId) throws SQLException {
        String stmt = sql.getStatement("pokemon/select-catch-count");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, userId);
            ps.setInt(2, speciesId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    private void executeForChannel(String statement, long channelId) throws SQLException {
        String stmt = sql.getStatement(statement);
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, channelId);
            ps.executeUpdate();
        }
    }
}
