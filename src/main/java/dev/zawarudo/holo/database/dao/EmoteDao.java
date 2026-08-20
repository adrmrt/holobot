package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class EmoteDao {

    private static final int BATCH_SIZE = 100;

    private static final String COL_ID = "emote_id";
    private static final String COL_NAME = "emote_name";
    private static final String COL_ANIM = "is_animated";

    private final SQLManager sql;

    public EmoteDao(SQLManager sql) {
        this.sql = sql;
    }

    public List<EmoteLite> findAll() throws SQLException {
        String stmt = sql.getStatement("emotes/select-all-emotes");
        List<EmoteLite> result = new ArrayList<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new EmoteLite(
                    rs.getLong(COL_ID),
                    rs.getString(COL_NAME),
                    rs.getBoolean(COL_ANIM)
                ));
            }
        }

        return result;
    }

    public boolean existsById(long emoteId) throws SQLException {
        String stmt = sql.getStatement("emotes/exists-emote-by-id");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, emoteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertAll(@NotNull List<EmoteRow> emotes) throws SQLException {
        if (emotes.isEmpty()) return;

        String stmt = sql.getStatement("emotes/insert-emote");

        try (var conn = Database.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (var ps = conn.prepareStatement(stmt)) {
                int i = 0;

                for (EmoteRow e : emotes) {
                    if (e == null) continue;

                    ps.setLong(1, e.id());
                    ps.setString(2, e.name());
                    ps.setBoolean(3, e.animated());
                    ps.setString(4, e.createdAt());
                    ps.setString(5, e.imageUrl());
                    ps.addBatch();

                    if (++i % BATCH_SIZE == 0) ps.executeBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
    }

    public Map<String, List<EmoteNameRow>> findDuplicateNameGroups() throws SQLException {
        String stmt = sql.getStatement("emotes/find-duplicate-emote-names");

        Map<String, List<EmoteNameRow>> groups = new LinkedHashMap<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String baseName = rs.getString("base_name");
                EmoteNameRow row = new EmoteNameRow(
                    rs.getLong(COL_ID),
                    rs.getString(COL_NAME)
                );

                groups.computeIfAbsent(baseName, k -> new ArrayList<>()).add(row);
            }
        }

        return groups;
    }

    public void updateNames(@NotNull List<EmoteRename> updates) throws SQLException {
        if (updates.isEmpty()) return;

        String stmt = sql.getStatement("emotes/update-emote-name");

        try (Connection conn = Database.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(stmt)) {
                int i = 0;

                for (EmoteRename u : updates) {
                    if (u == null) continue;

                    ps.setString(1, u.uniqueName());
                    ps.setLong(2, u.emoteId());
                    ps.addBatch();

                    if (++i % BATCH_SIZE == 0) {
                        ps.executeBatch();
                    }
                }

                ps.executeBatch();
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
    }

    public List<EmoteLite> searchByNameLike(@NotNull String name) throws SQLException {
        String stmt = sql.getStatement("emotes/search-emotes-like");
        List<EmoteLite> out = new ArrayList<>();

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(stmt)) {

            ps.setString(1, "%" + name + "%");

            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new EmoteLite(
                        rs.getLong(COL_ID),
                        rs.getString(COL_NAME),
                        rs.getBoolean(COL_ANIM)
                    ));
                }
                return out;
            }
        }
    }

    public Optional<EmoteLite> findByExactNameIgnoreCase(@NotNull String name) throws SQLException {
        String stmt = sql.getStatement("emotes/select-emote-by-name-ignore-case");

        try (var conn = Database.getConnection();
             var ps = conn.prepareStatement(stmt)) {

            ps.setString(1, name);

            try (var rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new EmoteLite(
                    rs.getLong(COL_ID),
                    rs.getString(COL_NAME),
                    rs.getBoolean(COL_ANIM)
                ));
            }
        }
    }

    public Set<String> findNamesStartingWithIgnoreCase(@NotNull String prefix) throws SQLException {
        String stmt = sql.getStatement("emotes/select-emote-names-starting-with");

        Set<String> names = new HashSet<>();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setString(1, prefix.toLowerCase(Locale.ROOT) + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String n = rs.getString(COL_NAME);
                    if (n != null) names.add(n);
                }
            }
        }

        return names;
    }

    public void renameOrSwap(@NotNull String emoteName, @NotNull String newName) throws SQLException {
        if (emoteName.isBlank()) throw new IllegalArgumentException("emoteName must be non-blank");
        if (newName.isBlank()) throw new IllegalArgumentException("newName must be non-blank");

        String selectIdAndNameByName = sql.getStatement("emotes/select-emote-id-and-name-by-name-ignore-case");
        String updateNameById = sql.getStatement("emotes/update-emote-name");

        try (Connection conn = Database.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement psSelect = conn.prepareStatement(selectIdAndNameByName);
                 PreparedStatement psUpdate = conn.prepareStatement(updateNameById)) {

                // Find target emote
                psSelect.setString(1, emoteName);
                long targetId;

                try (ResultSet rs = psSelect.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }
                    targetId = rs.getLong(COL_ID);
                }

                // Find conflict by newName
                psSelect.setString(1, newName);
                Long conflictId = null;
                String conflictCurrentName = null;

                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        conflictId = rs.getLong(COL_ID);
                        conflictCurrentName = rs.getString(COL_NAME);
                    }
                }

                if (conflictId != null) {
                    psUpdate.setString(1, conflictCurrentName);
                    psUpdate.setLong(2, targetId);
                    psUpdate.addBatch();

                    psUpdate.setString(1, newName);
                    psUpdate.setLong(2, conflictId);
                    psUpdate.addBatch();

                    psUpdate.executeBatch();
                } else {
                    // Simple rename
                    psUpdate.setString(1, newName);
                    psUpdate.setLong(2, targetId);
                    psUpdate.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
    }

    public record EmoteRow(long id, String name, boolean animated, String createdAt, String imageUrl) {
    }

    public record EmoteLite(long id, String name, boolean animated) {
    }

    public record EmoteNameRow(long emoteId, String emoteName) {
    }

    public record EmoteRename(long emoteId, String uniqueName) {
    }
}
