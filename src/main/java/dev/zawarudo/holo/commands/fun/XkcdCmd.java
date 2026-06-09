package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.XkcdDao;
import dev.zawarudo.holo.modules.xkcd.XkcdAPI;
import dev.zawarudo.holo.modules.xkcd.XkcdComic;
import dev.zawarudo.holo.modules.xkcd.XkcdSyncService;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@CommandInfo(name = "xkcd",
    description = "Use this command to access the comics of xkcd.",
    usage = "[new | search <query> | <issue nr> | <title>]",
    thumbnail = "https://xkcd.com/s/0b7742.png",
    embedColor = EmbedColor.WHITE,
    category = CommandCategory.MISC)
public class XkcdCmd extends AbstractCommand {

    private static final Random RANDOM = new Random();

    private static final String ERROR_RETRIEVING = "Something went wrong while retrieving the comic. Please try again later.";
    private static final String ERROR_DOES_NOT_EXIST = "This comic does not exist! If you think it should exist, consider using `%sxkcd new` to refresh my database.";

    private static final int SEARCH_LIMIT = 8;

    private final XkcdDao xkcdDao;
    private final XkcdSyncService xkcdSyncService;

    private final AtomicInteger latestIssue = new AtomicInteger();

    public XkcdCmd(XkcdDao xkcdDao, XkcdSyncService xkcdSyncService) {
        this.xkcdDao = xkcdDao;
        this.xkcdSyncService = xkcdSyncService;

        // Try to fetch latest once
        getLatestIssue();
    }

    @Override
    public void onCommand(@NotNull MessageReceivedEvent event) {
        if (args.length == 0) {
            sendRandomComic(event);
            return;
        }

        // Fetch and display newest xkcd comic
        if (args.length == 1 && args[0].equalsIgnoreCase("new")) {
            sendNewestComic(event);
            return;
        }

        // Full-text search using FTS5
        if (args.length >= 2 && args[0].equalsIgnoreCase("search")) {
            sendSearch(event);
            return;
        }

        // Sync xkcd comics
        if (args.length >= 2 && args[0].equalsIgnoreCase("sync")) {
            handleSync(event);
            return;
        }

        // Fetch comic by issue number
        if (isInteger(args[0])) {
            sendComicByIssueNumber(event);
            return;
        }

        // Fetch comic by title
        sendComicByTitle(event);
    }

    private void sendRandomComic(MessageReceivedEvent event) {
        try {
            int upper = getLatestIssue();
            if (upper <= 0) {
                sendErrorEmbed(event, ERROR_RETRIEVING);
                return;
            }

            int issue = RANDOM.nextInt(upper) + 1;

            // 404 intentionally missing
            if (issue == 404) issue = 403;

            XkcdComic comic = getComicDbFirst(issue).orElseThrow();
            sendXkcd(event, comic);
        } catch (APIException | InvalidRequestException | SQLException ex) {
            logger.error("Failed to fetch/store random XKCD comic.", ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
        }
    }

    private void sendNewestComic(MessageReceivedEvent event) {
        try {
            XkcdComic latest = XkcdAPI.getLatest();
            latestIssue.updateAndGet(cur -> Math.max(cur, latest.getIssueNr()));
            sendXkcd(event, latest);
            xkcdDao.insertIgnore(latest);
        } catch (APIException ex) {
            logger.error("Failed to fetch newest XKCD comic.", ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
        } catch (SQLException ex) {
            logger.warn("Failed to store latest XKCD comic.", ex);
        }
    }

    private void sendSearch(MessageReceivedEvent event) {
        String raw = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();
        if (raw.isBlank()) {
            sendErrorEmbed(event, "Usage: `" + getPrefix(event) + "xkcd search <query>`");
            return;
        }

        String broadQuery = toBroadQuery(raw);
        if (broadQuery.isBlank()) {
            sendErrorEmbed(event, "Search query is empty after filtering.");
            return;
        }

        String phraseQuery = toPhraseQuery(raw);

        try {
            List<XkcdComic> results = xkcdDao.searchPrioritized(broadQuery, phraseQuery, SEARCH_LIMIT, 0);

            if (results.isEmpty()) {
                event.getMessage().replyEmbeds(
                    new EmbedBuilder()
                        .setTitle("xkcd Search Results")
                        .setDescription("No results found for:\n`" + raw + "`")
                        .setColor(getEmbedColor())
                        .build()
                ).queue();
                return;
            }

            StringBuilder body = new StringBuilder();
            for (XkcdComic comic : results) {
                body.append(comic.getIssueNr()).append('\n')
                    .append(comic.getTitle()).append('\n')
                    .append(comic.getAlt()).append("\n\n");
            }

            event.getMessage().replyEmbeds(
                new EmbedBuilder()
                    .setTitle("xkcd Search Results")
                    .setDescription(Formatter.asCodeBlock(body.toString()))
                    .setFooter("Showing top " + results.size()
                        + " results • Open with " + getPrefix(event) + "xkcd <issue nr>")
                    .setColor(getEmbedColor())
                    .build()
            ).queue();

        } catch (SQLException ex) {
            logger.error("XKCD search failed. broadQuery='{}' phraseQuery='{}'", broadQuery, phraseQuery, ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
        }
    }

    private void handleSync(MessageReceivedEvent event) {
        if (!isBotOwner(event.getAuthor())) {
            // Command is owner-only
            return;
        }

        String sub = args[1].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "start" -> syncStart(event);
            case "status" -> syncStatus(event);
            case "stop" -> syncStop(event);
            default -> sendErrorEmbed(event,
                "Usage: `" + getPrefix(event) + "xkcd sync <start|status|stop>`");
        }
    }

    private void sendComicByIssueNumber(MessageReceivedEvent event) {
        int num = Integer.parseInt(args[0]);
        if (num < 1) {
            sendErrorEmbed(event, String.format(ERROR_DOES_NOT_EXIST, getPrefix(event)));
            return;
        }

        try {
            int latest = getLatestIssue();
            if (latest > 0 && num > latest) {
                sendErrorEmbed(event, String.format(ERROR_DOES_NOT_EXIST, getPrefix(event)));
                return;
            }

            Optional<XkcdComic> comic = getComicDbFirst(num);
            if (comic.isEmpty()) {
                sendErrorEmbed(event, String.format(ERROR_DOES_NOT_EXIST, getPrefix(event)));
                return;
            }

            sendXkcd(event, comic.get());
        } catch (APIException | InvalidRequestException | SQLException ex) {
            logger.error("Failed to fetch/store XKCD comic #{}.", num, ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
        }
    }

    private int getLatestIssue() {
        int cached = latestIssue.get();
        if (cached > 0) return cached;

        try {
            XkcdComic latest = XkcdAPI.getLatest();
            latestIssue.updateAndGet(cur -> Math.max(cur, latest.getIssueNr()));

            xkcdDao.insertIgnore(latest);

            return latest.getIssueNr();
        } catch (APIException | SQLException ex) {
            logger.warn("Could not fetch/store latest XKCD issue.", ex);
            return latestIssue.get();
        }
    }

    private Optional<XkcdComic> getComicDbFirst(int issue) throws SQLException, APIException, InvalidRequestException {
        if (issue < 1 || issue == 404) return Optional.empty();

        Optional<XkcdComic> fromDb = xkcdDao.findById(issue);
        if (fromDb.isPresent()) return fromDb;

        XkcdComic fetched = XkcdAPI.getComic(issue);
        xkcdDao.insertIgnore(fetched);
        latestIssue.updateAndGet(cur -> Math.max(cur, fetched.getIssueNr()));
        return Optional.of(fetched);
    }

    private void sendComicByTitle(MessageReceivedEvent event) {
        String title = String.join(" ", args);
        if (title.isBlank()) {
            sendErrorEmbed(event, "Usage: `" + getPrefix(event) + "xkcd <title>`");
            return;
        }

        try {
            Optional<XkcdComic> comic = xkcdDao.findByExactTitle(title);
            if (comic.isEmpty()) {
                sendErrorEmbed(event, String.format(ERROR_DOES_NOT_EXIST, getPrefix(event)));
                return;
            }
            sendXkcd(event, comic.get());
        } catch (SQLException ex) {
            logger.error("DB lookup by title failed: '{}'", title, ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
        }
    }

    private void sendXkcd(MessageReceivedEvent event, XkcdComic comic) {
        String alt = comic.getAlt();
        alt = alt.length() > MessageEmbed.TEXT_MAX_LENGTH
            ? alt.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 3) + "..."
            : alt;

        event.getMessage().replyEmbeds(
            new EmbedBuilder()
                .setTitle("xkcd " + comic.getIssueNr() + ": " + comic.getTitle())
                .setDescription("[Explanation](" + comic.getExplainedUrl() + ")")
                .setImage(comic.getImg())
                .setFooter(alt)
                .setColor(getEmbedColor())
                .build()
        ).queue();
    }

    private static String toBroadQuery(String raw) {
        String[] parts = raw.toLowerCase().trim().split("\\s+");
        List<String> tokens = new ArrayList<>();

        for (String p : parts) {
            String t = p.replaceAll("[^\\p{L}\\p{N}]+", "");
            if (!t.isBlank()) tokens.add(t);
        }

        if (tokens.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append('"').append(tokens.get(i)).append('"');
            if (i == tokens.size() - 1) sb.append('*');
        }
        return sb.toString();
    }

    private static String toPhraseQuery(String raw) {
        String normalized = raw.toLowerCase()
            .trim()
            .replaceAll("\\s+", " ") // Replace extra spaces
            .replace("\"", "");

        if (normalized.isBlank()) return "";

        return "\"" + normalized + "\"";
    }

    private void syncStart(@NotNull MessageReceivedEvent event) {
        int latest;

        if (latestIssue.get() <= 0) {
            getLatestIssue();
        }
        latest = latestIssue.get();
        if (latest <= 0) {
            sendErrorEmbed(event, ERROR_RETRIEVING);
            return;
        }

        int dbCount;
        try {
            dbCount = xkcdDao.countComics();
        } catch (SQLException ex) {
            logger.error("Failed to count XKCD comics.", ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
            return;
        }

        // xkcd #404 is intentionally missing
        int expectedCount = latest - ((latest >= 404) ? 1 : 0);

        if (dbCount >= expectedCount) {
            event.getMessage().replyEmbeds(
                new EmbedBuilder()
                    .setTitle("xkcd sync")
                    .setDescription("Already up to date.\n"
                        + "Comics in DB: **" + dbCount + "** / **" + expectedCount + "**\n"
                        + "Latest: **#" + latest + "**")
                    .setColor(getEmbedColor())
                    .build()
            ).queue();
            return;
        }

        int from = 1;
        int to = latest;

        boolean started;
        try {
            started = xkcdSyncService.start(from, to);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to start XKCD sync due to invalid range: {} -> {}", from, to, ex);
            sendErrorEmbed(event, "Failed to start sync (invalid range).");
            return;
        } catch (Exception ex) {
            logger.error("Failed to start XKCD sync.", ex);
            sendErrorEmbed(event, ERROR_RETRIEVING);
            return;
        }

        if (!started) {
            sendErrorEmbed(event, "Sync is already running. Use `" + getPrefix(event) + "xkcd sync status`.");
            return;
        }

        event.getMessage().replyEmbeds(
            new EmbedBuilder()
                .setTitle("xkcd sync started")
                .setDescription("Syncing comics (safe re-sync) from **#" + from + "** to **#" + to + "**.\n"
                    + "Progress: **" + dbCount + "** / **" + expectedCount + "** stored.\n"
                    + "Check progress with `" + getPrefix(event) + "xkcd sync status`.")
                .setColor(getEmbedColor())
                .build()
        ).queue();
    }

    private void syncStatus(@NotNull MessageReceivedEvent event) {
        int dbCount = -1;

        try {
            dbCount = xkcdDao.countComics();
        } catch (SQLException ex) {
            logger.error("countComics failed", ex);
        }

        XkcdSyncService.SyncStatus s = xkcdSyncService.status(0, dbCount);

        StringBuilder desc = new StringBuilder();
        desc.append("**Running:** ").append(s.running() ? "Yes" : "No").append('\n');

        if (s.dbCount() >= 0) {
            desc.append("**Comics in DB:** ").append(s.dbCount()).append('\n');
        } else {
            desc.append("**Comics in DB:** unknown\n");
        }

        if (s.targetIssue() > 0) {
            desc.append("**Target (latest):** #").append(s.targetIssue()).append('\n');
        } else {
            desc.append("**Target (latest):** unknown\n");
        }

        desc.append("**Last checked:** #").append(s.lastCheckedIssue()).append('\n');
        desc.append("**Last inserted:** #").append(s.lastInsertedIssue()).append('\n');
        desc.append("**Left to sync:** ").append(s.leftToSync()).append('\n');
        desc.append("**Inserted this run:** ").append(s.affectedThisRun()).append('\n');

        if (s.startedAt() != null) {
            desc.append("**Started:** ").append(s.startedAt()).append('\n');
        }
        if (s.lastUpdateAt() != null) {
            desc.append("**Last update:** ").append(s.lastUpdateAt()).append('\n');
        }
        if (s.lastError() != null) {
            desc.append("\n**Last error:** ").append(s.lastError());
        }

        event.getMessage().replyEmbeds(
            new EmbedBuilder()
                .setTitle("xkcd sync status")
                .setDescription(desc.toString())
                .setColor(getEmbedColor())
                .build()
        ).queue();
    }

    private void syncStop(@NotNull MessageReceivedEvent event) {
        if (!xkcdSyncService.isRunning()) {
            sendErrorEmbed(event, "No sync is currently running.");
            return;
        }

        xkcdSyncService.stop();

        event.getMessage().replyEmbeds(
            new EmbedBuilder()
                .setTitle("xkcd sync stopping")
                .setDescription("Stopping sync... (it should stop shortly)")
                .setColor(getEmbedColor())
                .build()
        ).queue();
    }
}
