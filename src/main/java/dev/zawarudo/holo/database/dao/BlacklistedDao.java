package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class BlacklistedDao {

    private final SQLManager sql;

    public BlacklistedDao(SQLManager sql) {
        this.sql = sql;
    }

    public List<Blacklisted> findAll() throws SQLException {
        final String stmt = sql.getStatement("blacklist/select-all-blacklisted");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            List<Blacklisted> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new Blacklisted(
                    rs.getLong("user_id"),
                    rs.getString("reason"),
                    rs.getString("date")
                ));
            }
            return result;
        }
    }

    public List<Long> findAllUserIds() throws SQLException {
        final String stmt = sql.getStatement("blacklist/select-all-blacklisted-ids");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            List<Long> out = new ArrayList<>();
            while (rs.next()) out.add(rs.getLong(1));
            return out;
        }
    }

    public boolean exists(long userId) throws SQLException {
        final String stmt = sql.getStatement("blacklist/exists-blacklisted");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int insertIgnore(Blacklisted blacklisted) throws SQLException {
        final String stmt = sql.getStatement("blacklist/insert-blacklisted-ignore");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, blacklisted.userId());
            ps.setString(2, blacklisted.reason());
            ps.setString(3, blacklisted.date());
            return ps.executeUpdate(); // 1 if inserted, 0 if ignored
        }
    }

    public int deleteByUserId(long userId) throws SQLException {
        final String stmt = sql.getStatement("blacklist/delete-blacklisted");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, userId);
            return ps.executeUpdate(); // 1 if deleted, 0 if not found
        }
    }

    public record Blacklisted(long userId, String reason, String date) {
    }
}
