package dev.zawarudo.holo.database.dao;

import dev.zawarudo.holo.database.BatchResult;
import dev.zawarudo.holo.database.Database;
import dev.zawarudo.holo.database.SQLManager;
import dev.zawarudo.holo.modules.xkcd.XkcdComic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class XkcdDao {

    private static final int BATCH_SIZE = 100;

    private final SQLManager sql;

    public XkcdDao(SQLManager sql) {
        this.sql = Objects.requireNonNull(sql, "sql must not be null");
    }

    public List<XkcdComic> findAll() throws SQLException {
        final String stmt = sql.getStatement("xkcd/select-all-xkcd-comics");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            List<XkcdComic> result = new ArrayList<>();
            while (rs.next()) {
                result.add(mapRow(rs));
            }
            return result;
        }
    }

    public Optional<XkcdComic> findById(int issueNr) throws SQLException {
        if (issueNr < 1) throw new IllegalArgumentException("issue must be >= 1");

        final String stmt = sql.getStatement("xkcd/select-xkcd-by-id");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setInt(1, issueNr);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Searches for a xkcd comic that matches the exact specified title (case-insensitive).
     */
    public Optional<XkcdComic> findByExactTitle(String title) throws SQLException {
        if (title == null || title.isBlank()) return Optional.empty();

        final String stmt = sql.getStatement("xkcd/select-xkcd-by-title-ci");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setString(1, title.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Executes an INSERT OR IGNORE.
     *
     * @return 1 if inserted, 0 if ignored.
     */
    public int insertIgnore(XkcdComic comic) throws SQLException {
        if (comic == null) throw new IllegalArgumentException("comic must not be null");

        final String stmt = sql.getStatement("xkcd/insert-xkcd-comic");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            bindInsert(ps, comic);
            return ps.executeUpdate();
        }
    }

    /**
     * Executes INSERT OR IGNORE in batches inside a single transaction.
     */
    public BatchResult insertAllIgnore(List<XkcdComic> comics) throws SQLException {
        if (comics == null) throw new IllegalArgumentException("comics must not be null");
        if (comics.isEmpty()) return new BatchResult(0, 0);

        final String stmt = sql.getStatement("xkcd/insert-xkcd-comic");

        int attempted = 0;
        int affected = 0;

        try (Connection conn = Database.getConnection()) {
            boolean origAuto = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement(stmt)) {
                int batchCount = 0;

                for (XkcdComic comic : comics) {
                    if (comic == null) continue;

                    attempted++;
                    bindInsert(ps, comic);
                    ps.addBatch();
                    batchCount++;

                    if (batchCount >= BATCH_SIZE) {
                        affected = mergeBatchAffected(affected, ps.executeBatch());
                        ps.clearBatch();
                        batchCount = 0;
                    }
                }

                if (batchCount > 0) {
                    affected = mergeBatchAffected(affected, ps.executeBatch());
                    ps.clearBatch();
                }

                conn.commit();
                return new BatchResult(attempted, affected);

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(origAuto);
            }
        }
    }

    public int countComics() throws SQLException {
        final String stmt = sql.getStatement("xkcd/count-xkcd-comics");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getInt("cnt") : 0;
        }
    }

    public boolean exists(int issue) throws SQLException {
        if (issue < 1) throw new IllegalArgumentException("issue must be >= 1");

        final String stmt = sql.getStatement("xkcd/exists-xkcd-comic");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setInt(1, issue);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<XkcdComic> search(String ftsQuery, int limit, int offset) throws SQLException {
        if (ftsQuery == null || ftsQuery.isBlank()) return List.of();
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");

        final String stmt = sql.getStatement("xkcd/search-xkcd");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setString(1, ftsQuery);
            ps.setInt(2, limit);
            ps.setInt(3, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<XkcdComic> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        }
    }

    public List<XkcdComic> searchPrioritized(String broadQuery, String phraseQuery, int limit, int offset) throws SQLException {
        if (broadQuery == null || broadQuery.isBlank()) return List.of();
        if (limit <= 0) throw new IllegalArgumentException("limit must be > 0");
        if (offset < 0) throw new IllegalArgumentException("offset must be >= 0");

        if (phraseQuery == null || phraseQuery.isBlank()) {
            return search(broadQuery, limit, offset);
        }

        final String stmt = sql.getStatement("xkcd/search-xkcd-prioritized");

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(stmt)) {

            ps.setString(1, phraseQuery);
            ps.setString(2, broadQuery);
            ps.setInt(3, limit);
            ps.setInt(4, offset);

            try (ResultSet rs = ps.executeQuery()) {
                List<XkcdComic> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(mapRow(rs));
                }
                return result;
            }
        }
    }

    private static int mergeBatchAffected(int currentAffected, int[] batchResults) {
        if (currentAffected < 0) return -1;

        int batchAffected = sumBatchResultsOrUnknown(batchResults);
        if (batchAffected < 0) return -1;

        return currentAffected + batchAffected;
    }

    private static int sumBatchResultsOrUnknown(int[] results) {
        int sum = 0;
        for (int r : results) {
            if (r == java.sql.Statement.SUCCESS_NO_INFO) {
                return -1; // unknown
            }
            if (r > 0) sum += r;
        }
        return sum;
    }

    private static void bindInsert(PreparedStatement ps, XkcdComic comic) throws SQLException {
        ps.setInt(1, comic.getIssueNr());
        ps.setString(2, comic.getTitle());
        ps.setString(3, comic.getAlt());
        ps.setString(4, comic.getImg());
        ps.setInt(5, comic.getDay());
        ps.setInt(6, comic.getMonth());
        ps.setInt(7, comic.getYear());
    }

    private static XkcdComic mapRow(ResultSet rs) throws SQLException {
        XkcdComic comic = new XkcdComic();
        comic.setIssueNr(rs.getInt("id"));
        comic.setTitle(rs.getString("title"));
        comic.setAlt(rs.getString("alt"));
        comic.setImg(rs.getString("img"));
        comic.setDate(
            rs.getInt("day"),
            rs.getInt("month"),
            rs.getInt("year")
        );
        return comic;
    }
}
