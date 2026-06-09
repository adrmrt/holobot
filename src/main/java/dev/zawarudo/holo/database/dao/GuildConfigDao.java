package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.core.GuildConfig;
import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class GuildConfigDao {

    private final SQLManager sql;

    public GuildConfigDao(SQLManager sql) {
        this.sql = sql;
    }

    public void insert(GuildConfig config) throws SQLException {
        if (config == null) throw new IllegalArgumentException("config must not be null");

        String stmt = sql.getStatement("config/insert-guild-config");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, config.getGuildId());
            ps.setString(2, config.getPrefix());
            ps.setBoolean(3, config.isNSFWEnabled());
            ps.setString(4, config.getDisabledModulesCsv());
            ps.executeUpdate();
        }
    }

    public Map<Long, GuildConfig> findAll() throws SQLException {
        String stmt = sql.getStatement("config/select-all-guild-configs");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            Map<Long, GuildConfig> map = new HashMap<>();
            while (rs.next()) {
                long guildId = rs.getLong("guild_id");

                GuildConfig config = new GuildConfig(guildId);
                config.setPrefix(rs.getString("prefix"));
                config.setAllowNSFW(readBooleanLenient(rs, "nsfw"));
                config.setDisabledModulesCsv(rs.getString("disabled_modules"));

                map.put(guildId, config);
            }
            return map;
        }
    }

    public void update(GuildConfig config) throws SQLException {
        if (config == null) throw new IllegalArgumentException("config must not be null");

        String stmt = sql.getStatement("config/update-guild-config");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setString(1, config.getPrefix());
            ps.setBoolean(2, config.isNSFWEnabled());
            ps.setString(3, config.getDisabledModulesCsv());
            ps.setLong(4, config.getGuildId());

            ps.executeUpdate();
        }
    }

    public void deleteByGuildId(long guildId) throws SQLException {
        String stmt = sql.getStatement("config/delete-guild-config");
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setLong(1, guildId);
            ps.executeUpdate();
        }
    }

    private static boolean readBooleanLenient(ResultSet rs, String col) throws SQLException {
        String s = rs.getString(col);
        if (s == null) return false;
        s = s.trim();
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        try {
            return Integer.parseInt(s) != 0;
        } catch (NumberFormatException ignored) {
        }
        return rs.getBoolean(col);
    }
}
