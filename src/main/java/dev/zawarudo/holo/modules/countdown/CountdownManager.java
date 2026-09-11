package dev.zawarudo.holo.modules.countdown;

import dev.zawarudo.holo.database.dao.CountdownDao;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Business-logic layer for countdowns: persistence via {@link CountdownDao} plus a periodic
 * sweep that notifies the origin channel once a countdown's time has arrived.
 */
public final class CountdownManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(CountdownManager.class);
    private static final long SWEEP_INTERVAL_SECONDS = 30;

    private final CountdownDao countdownDao;
    private final JDA jda;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService io;

    public CountdownManager(CountdownDao countdownDao, JDA jda, ScheduledExecutorService scheduler, ExecutorService io) {
        this.countdownDao = countdownDao;
        this.jda = jda;
        this.scheduler = scheduler;
        this.io = io;
    }

    /**
     * Starts the periodic sweep that notifies channels of arrived countdowns. Call once at startup.
     */
    public void start() {
        scheduler.scheduleWithFixedDelay(this::safeSweep, SWEEP_INTERVAL_SECONDS, SWEEP_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    public long createCountdown(Countdown countdown) throws SQLException {
        return countdownDao.insert(countdown);
    }

    public void removeCountdown(long countdownId) throws SQLException {
        countdownDao.delete(countdownId);
    }

    public List<Countdown> listFor(long userId) throws SQLException {
        return countdownDao.findAllById(userId);
    }

    public List<Countdown> listGlobal(long guildId) throws SQLException {
        return countdownDao.findGlobalForGuild(guildId);
    }

    public Optional<Countdown> findById(long id) throws SQLException {
        return countdownDao.findById(id);
    }

    private void safeSweep() {
        try {
            sweepDueCountdowns();
        } catch (Exception e) {
            LOGGER.error("Countdown sweep failed", e);
        }
    }

    private void sweepDueCountdowns() throws SQLException {
        List<Countdown> due = countdownDao.findDueForNotification(System.currentTimeMillis());
        for (Countdown countdown : due) {
            io.execute(() -> notify(countdown));
        }
    }

    private void notify(Countdown countdown) {
        try {
            MessageChannel channel = jda.getChannelById(MessageChannel.class, countdown.channelId());
            if (channel == null) {
                LOGGER.warn("Countdown {} arrived but its channel {} is no longer accessible; marking notified anyway.",
                    countdown.id(), countdown.channelId());
                countdownDao.markNotified(countdown.id());
                return;
            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle("Countdown Arrived!");
            builder.setDescription(String.format("**%s** has arrived!", countdown.name()));
            channel.sendMessageEmbeds(builder.build()).queue();
            countdownDao.markNotified(countdown.id());
        } catch (Exception e) {
            LOGGER.error("Failed to notify countdown {}", countdown.id(), e);
        }
    }
}
