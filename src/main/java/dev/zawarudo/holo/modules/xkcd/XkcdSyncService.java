package dev.zawarudo.holo.modules.xkcd;

import dev.zawarudo.holo.database.BatchResult;
import dev.zawarudo.holo.database.dao.XkcdDao;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class XkcdSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XkcdSyncService.class);

    private static final int SYNC_BATCH_SIZE = 100;
    private static final int SYNC_SLEEP_MS = 250;

    private final ExecutorService executor;
    private final AtomicReference<Future<?>> running = new AtomicReference<>(null);

    private volatile boolean stopRequested = false;

    private final AtomicInteger lastInserted = new AtomicInteger(0);
    private final AtomicInteger lastChecked = new AtomicInteger(0);
    private final AtomicInteger targetIssue = new AtomicInteger(0);
    private final AtomicInteger attemptedThisRun = new AtomicInteger(0);
    private final AtomicInteger affectedThisRun = new AtomicInteger(0);

    private volatile long startedAtMs = 0L;
    private volatile long lastUpdateAtMs = 0L;
    private volatile String lastError = null;

    private final XkcdDao dao;

    public XkcdSyncService(XkcdDao dao, ExecutorService executor) {
        this.dao = Objects.requireNonNull(dao, "dao must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
    }

    public boolean isRunning() {
        Future<?> f = running.get();
        return f != null && !f.isDone();
    }

    public boolean start(int fromIssueInclusive, int toIssueInclusive) {
        if (fromIssueInclusive < 1 || toIssueInclusive < fromIssueInclusive) {
            throw new IllegalArgumentException("Invalid sync range: " + fromIssueInclusive + " -> " + toIssueInclusive);
        }

        stopRequested = false;
        startedAtMs = System.currentTimeMillis();
        lastUpdateAtMs = startedAtMs;
        lastError = null;

        attemptedThisRun.set(0);
        affectedThisRun.set(0);

        lastChecked.set(Math.max(0, fromIssueInclusive - 1));
        lastInserted.set(0);

        targetIssue.set(toIssueInclusive);

        Runnable task = () -> runLoop(fromIssueInclusive, toIssueInclusive);
        Future<?> f = executor.submit(task);

        if (!running.compareAndSet(null, f)) {
            f.cancel(true);
            return false;
        }

        return true;
    }

    public void stop() {
        stopRequested = true;
        lastUpdateAtMs = System.currentTimeMillis();

        Future<?> f = running.getAndSet(null);
        if (f != null) {
            f.cancel(true); // interrupts sleep/wait where possible
        }
    }

    public SyncStatus status(int maxStoredIssue, int dbCount) {
        boolean runningLocal = isRunning();
        int target = targetIssue.get();
        int lastCheckedIssue = lastChecked.get();
        int lastInsertedIssue = lastInserted.get();

        int left = 0;
        if (target > 0) {
            left = Math.max(0, target - lastCheckedIssue);

            // Adjust 404 gap
            if (lastCheckedIssue < 404 && target >= 404) left -= 1;
            if (left < 0) left = 0;
        }

        return new SyncStatus(
            runningLocal,
            left,
            lastCheckedIssue,
            lastInsertedIssue,
            target,
            maxStoredIssue,
            dbCount,
            attemptedThisRun.get(),
            affectedThisRun.get(),
            startedAtMs > 0 ? new Date(startedAtMs) : null,
            lastUpdateAtMs > 0 ? new Date(lastUpdateAtMs) : null,
            lastError
        );
    }

    private void runLoop(int from, int to) {
        List<XkcdComic> batch = new ArrayList<>(SYNC_BATCH_SIZE);

        try {
            for (int i = from; i <= to; i++) {
                if (stopRequested) break;
                if (i == 404) continue;

                lastChecked.set(i);
                lastUpdateAtMs = System.currentTimeMillis();

                try {
                    // Skip already stored issues
                    if (dao.exists(i)) {
                        continue;
                    }

                    XkcdComic comic = XkcdAPI.getComic(i);

                    batch.add(comic);
                    lastInserted.set(i);

                    if (batch.size() >= SYNC_BATCH_SIZE) {
                        BatchResult result = dao.insertAllIgnore(batch);
                        attemptedThisRun.addAndGet(result.attempted());
                        mergeAffectedCounter(affectedThisRun, result.affected());
                        batch.clear();
                    }

                    // Avoid spamming xkcd website
                    Thread.sleep(SYNC_SLEEP_MS);

                } catch (InvalidRequestException ex) {
                    lastError = "Invalid request at issue " + i + ": " + ex.getMessage();
                    LOGGER.error("XKCD sync failed at issue {} (invalid request).", i, ex);
                    break;
                } catch (APIException ex) {
                    lastError = "API error at issue " + i + ": " + ex.getMessage();
                    LOGGER.error("XKCD sync failed at issue {} (API).", i, ex);
                    break;
                } catch (SQLException ex) {
                    lastError = "DB error at issue " + i + ": " + ex.getMessage();
                    LOGGER.error("XKCD sync failed at issue {} (DB).", i, ex);
                    break;
                } catch (InterruptedException _) {
                    // stopRequested or external interrupt
                    Thread.currentThread().interrupt();
                    if (!stopRequested) {
                        lastError = "Interrupted.";
                        LOGGER.warn("XKCD sync interrupted.");
                    }
                    break;
                } catch (Exception ex) {
                    lastError = "Unexpected error at issue " + i + ": " + ex.getMessage();
                    LOGGER.error("XKCD sync failed at issue {} (unexpected).", i, ex);
                    break;
                }
            }

            if (!batch.isEmpty() && !stopRequested) {
                try {
                    BatchResult result = dao.insertAllIgnore(batch);
                    attemptedThisRun.addAndGet(result.attempted());
                    mergeAffectedCounter(affectedThisRun, result.affected());
                    batch.clear();
                } catch (SQLException ex) {
                    lastError = "DB error while flushing final batch: " + ex.getMessage();
                    LOGGER.error("XKCD sync failed while flushing final batch.", ex);
                }
            }
        } finally {
            lastUpdateAtMs = System.currentTimeMillis();
            running.set(null);
        }
    }

    private static void mergeAffectedCounter(AtomicInteger counter, int deltaOrUnknown) {
        if (deltaOrUnknown < 0) {
            counter.set(-1);
            return;
        }
        counter.updateAndGet(cur -> cur < 0 ? -1 : cur + deltaOrUnknown);
    }

    public record SyncStatus(
        boolean running,
        int leftToSync,
        int lastCheckedIssue,
        int lastInsertedIssue,
        int targetIssue,
        int maxStoredIssue,
        int dbCount,
        int attemptedThisRun,
        int affectedThisRun,
        Date startedAt,
        Date lastUpdateAt,
        String lastError
    ) {
    }
}
