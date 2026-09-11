package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;
import dev.zawarudo.holo.modules.countdown.Countdown;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CountdownDao {

    private final SQLManager sql;

    public CountdownDao(SQLManager sql) {
        this.sql = sql;
    }

    public List<Countdown> findAll() throws SQLException {
        String stmt = sql.getStatement("countdown/select-countdown");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {
            return readAll(rs);
        }
    }

    public List<Countdown> findAllById(long userId) throws SQLException {
        String stmt = sql.getStatement("countdown/select-countdown-by-user");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return readAll(rs);
            }
        }
    }

    public List<Countdown> findGlobalForGuild(long guildId) throws SQLException {
        String stmt = sql.getStatement("countdown/select-countdown-by-guild-global");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, guildId);
            try (ResultSet rs = ps.executeQuery()) {
                return readAll(rs);
            }
        }
    }

    public Optional<Countdown> findById(long id) throws SQLException {
        String stmt = sql.getStatement("countdown/select-countdown-by-id");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Countdown> findDueForNotification(long now) throws SQLException {
        String stmt = sql.getStatement("countdown/select-countdown-due");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, now);
            try (ResultSet rs = ps.executeQuery()) {
                return readAll(rs);
            }
        }
    }

    /**
     * Inserts the given countdown and returns its generated id.
     */
    public long insert(Countdown countdown) throws SQLException {
        String stmt = sql.getStatement("countdown/insert-countdown");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, countdown.name());
            ps.setLong(2, countdown.timeCreated());
            ps.setLong(3, countdown.dateTime());
            ps.setLong(4, countdown.userId());
            ps.setLong(5, countdown.guildId());
            ps.setString(6, countdown.visibility().name());
            if (countdown.channelId() != 0) {
                ps.setLong(7, countdown.channelId());
            } else {
                ps.setNull(7, java.sql.Types.INTEGER);
            }
            ps.setBoolean(8, countdown.notified());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        }
    }

    public void delete(long countdownId) throws SQLException {
        String stmt = sql.getStatement("countdown/delete-countdown");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, countdownId);
            ps.executeUpdate();
        }
    }

    public void markNotified(long countdownId) throws SQLException {
        String stmt = sql.getStatement("countdown/update-countdown-notified");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {
            ps.setLong(1, countdownId);
            ps.executeUpdate();
        }
    }

    private List<Countdown> readAll(ResultSet rs) throws SQLException {
        List<Countdown> countdowns = new ArrayList<>();
        while (rs.next()) {
            countdowns.add(map(rs));
        }
        return countdowns;
    }

    private Countdown map(ResultSet rs) throws SQLException {
        long channelId = rs.getLong("channel_id");
        if (rs.wasNull()) {
            channelId = 0;
        }
        return new Countdown(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getLong("time_created"),
            rs.getLong("date_time"),
            rs.getLong("user_id"),
            rs.getLong("guild_id"),
            Countdown.Visibility.valueOf(rs.getString("visibility")),
            channelId,
            rs.getBoolean("notified")
        );
    }
}
