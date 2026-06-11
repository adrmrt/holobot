package dev.zawarudo.holo.commands.fun;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.XkcdDao;
import dev.zawarudo.holo.modules.xkcd.XkcdAPI;
import dev.zawarudo.holo.modules.xkcd.XkcdComic;
import dev.zawarudo.holo.modules.xkcd.XkcdSyncService;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.ParsingUtils;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.InvalidRequestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
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
public class XkcdCmd extends AbstractCommand implements ExecutableCommand {

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
    public void execute(@NotNull CommandContext ctx) {
        if (!ctx.hasArgs()) {
            sendRandomComic(ctx);
            return;
        }

        // Fetch and display newest xkcd comic
        if (ctx.argCount() == 1 && "new".equalsIgnoreCase(ctx.args().getFirst())) {
            sendNewestComic(ctx);
            return;
        }

        // Full-text search using FTS5
        if (ctx.argCount() >= 2 && "search".equalsIgnoreCase(ctx.args().getFirst())) {
            sendSearch(ctx);
            return;
        }

        // Sync xkcd comics
        if (ctx.argCount() >= 2 && "sync".equalsIgnoreCase(ctx.args().getFirst())) {
            handleSync(ctx);
            return;
        }

        // Fetch comic by issue number
        if (ParsingUtils.isInteger(ctx.args().getFirst())) {
            sendComicByIssueNumber(ctx);
            return;
        }

        // Fetch comic by title
        sendComicByTitle(ctx);
    }

    private void sendRandomComic(CommandContext ctx) {
        try {
            int upper = getLatestIssue();
            if (upper <= 0) {
                ctx.reply().errorEmbed(ERROR_RETRIEVING);
                return;
            }

            int issue = RANDOM.nextInt(upper) + 1;

            // 404 intentionally missing
            if (issue == 404) issue = 403;

            XkcdComic comic = getComicDbFirst(issue).orElseThrow();
            sendXkcd(ctx, comic);
        } catch (APIException | InvalidRequestException | SQLException ex) {
            logger.error("Failed to fetch/store random XKCD comic.", ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
        }
    }

    private void sendNewestComic(CommandContext ctx) {
        try {
            XkcdComic latest = XkcdAPI.getLatest();
            latestIssue.updateAndGet(cur -> Math.max(cur, latest.getIssueNr()));
            sendXkcd(ctx, latest);
            xkcdDao.insertIgnore(latest);
        } catch (APIException ex) {
            logger.error("Failed to fetch newest XKCD comic.", ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
        } catch (SQLException ex) {
            logger.warn("Failed to store latest XKCD comic.", ex);
        }
    }

    private void sendSearch(CommandContext ctx) {
        String raw = String.join(" ", ctx.args().subList(1, ctx.args().size())).trim();
        if (raw.isBlank()) {
            ctx.reply().errorEmbed("Usage: `" + ctx.prefix().orElse("") + "xkcd search <query>`");
            return;
        }

        String broadQuery = toBroadQuery(raw);
        if (broadQuery.isBlank()) {
            ctx.reply().errorEmbed("Search query is empty after filtering.");
            return;
        }

        String phraseQuery = toPhraseQuery(raw);

        try {
            List<XkcdComic> results = xkcdDao.searchPrioritized(broadQuery, phraseQuery, SEARCH_LIMIT, 0);

            if (results.isEmpty()) {
                ctx.reply().embed(
                    new EmbedBuilder()
                        .setTitle("xkcd Search Results")
                        .setDescription("No results found for:\n`" + raw + "`")
                        .setColor(getEmbedColor())
                );
                return;
            }

            StringBuilder body = new StringBuilder();
            for (XkcdComic comic : results) {
                body.append(comic.getIssueNr()).append('\n')
                    .append(comic.getTitle()).append('\n')
                    .append(comic.getAlt()).append("\n\n");
            }

            ctx.reply().embed(
                new EmbedBuilder()
                    .setTitle("xkcd Search Results")
                    .setDescription(Formatter.asCodeBlock(body.toString()))
                    .setFooter("Showing top " + results.size()
                        + " results • Open with " + ctx.prefix().orElse("") + "xkcd <issue nr>")
                    .setColor(getEmbedColor())
            );

        } catch (SQLException ex) {
            logger.error("XKCD search failed. broadQuery='{}' phraseQuery='{}'", broadQuery, phraseQuery, ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
        }
    }

    private void handleSync(CommandContext ctx) {
        if (!ctx.isBotOwner()) {
            // Command is owner-only
            return;
        }

        String sub = ctx.args().get(1).toLowerCase(Locale.ROOT);

        switch (sub) {
            case "start" -> syncStart(ctx);
            case "status" -> syncStatus(ctx);
            case "stop" -> syncStop(ctx);
            default -> ctx.reply().errorEmbed(
                "Usage: `" + ctx.prefix().orElse("") + "xkcd sync <start|status|stop>`");
        }
    }

    private void sendComicByIssueNumber(CommandContext ctx) {
        int num = Integer.parseInt(ctx.args().getFirst());
        if (num < 1) {
            ctx.reply().errorEmbed(String.format(ERROR_DOES_NOT_EXIST, ctx.prefix().orElse("")));
            return;
        }

        try {
            int latest = getLatestIssue();
            if (latest > 0 && num > latest) {
                ctx.reply().errorEmbed(String.format(ERROR_DOES_NOT_EXIST, ctx.prefix().orElse("")));
                return;
            }

            Optional<XkcdComic> comic = getComicDbFirst(num);
            if (comic.isEmpty()) {
                ctx.reply().errorEmbed(String.format(ERROR_DOES_NOT_EXIST, ctx.prefix().orElse("")));
                return;
            }

            sendXkcd(ctx, comic.get());
        } catch (APIException | InvalidRequestException | SQLException ex) {
            logger.error("Failed to fetch/store XKCD comic #{}.", num, ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
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

    private void sendComicByTitle(CommandContext ctx) {
        String title = ctx.argString();
        if (title.isBlank()) {
            ctx.reply().errorEmbed("Usage: `" + ctx.prefix().orElse("") + "xkcd <title>`");
            return;
        }

        try {
            Optional<XkcdComic> comic = xkcdDao.findByExactTitle(title);
            if (comic.isEmpty()) {
                ctx.reply().errorEmbed(String.format(ERROR_DOES_NOT_EXIST, ctx.prefix().orElse("")));
                return;
            }
            sendXkcd(ctx, comic.get());
        } catch (SQLException ex) {
            logger.error("DB lookup by title failed: '{}'", title, ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
        }
    }

    private void sendXkcd(CommandContext ctx, XkcdComic comic) {
        String alt = comic.getAlt();
        alt = alt.length() > MessageEmbed.TEXT_MAX_LENGTH
            ? alt.substring(0, MessageEmbed.TEXT_MAX_LENGTH - 3) + "..."
            : alt;

        ctx.reply().embed(
            new EmbedBuilder()
                .setTitle("xkcd " + comic.getIssueNr() + ": " + comic.getTitle())
                .setDescription("[Explanation](" + comic.getExplainedUrl() + ")")
                .setImage(comic.getImg())
                .setFooter(alt)
                .setColor(getEmbedColor())
        );
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

    private void syncStart(@NotNull CommandContext ctx) {
        int latest;

        if (latestIssue.get() <= 0) {
            getLatestIssue();
        }
        latest = latestIssue.get();
        if (latest <= 0) {
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
            return;
        }

        int dbCount;
        try {
            dbCount = xkcdDao.countComics();
        } catch (SQLException ex) {
            logger.error("Failed to count XKCD comics.", ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
            return;
        }

        // xkcd #404 is intentionally missing
        int expectedCount = latest - ((latest >= 404) ? 1 : 0);

        if (dbCount >= expectedCount) {
            ctx.reply().embed(
                new EmbedBuilder()
                    .setTitle("xkcd sync")
                    .setDescription("Already up to date.\n"
                        + "Comics in DB: **" + dbCount + "** / **" + expectedCount + "**\n"
                        + "Latest: **#" + latest + "**")
                    .setColor(getEmbedColor())
            );
            return;
        }

        int from = 1;
        int to = latest;

        boolean started;
        try {
            started = xkcdSyncService.start(from, to);
        } catch (IllegalArgumentException ex) {
            logger.error("Failed to start XKCD sync due to invalid range: {} -> {}", from, to, ex);
            ctx.reply().errorEmbed("Failed to start sync (invalid range).");
            return;
        } catch (Exception ex) {
            logger.error("Failed to start XKCD sync.", ex);
            ctx.reply().errorEmbed(ERROR_RETRIEVING);
            return;
        }

        if (!started) {
            ctx.reply().errorEmbed("Sync is already running. Use `" + ctx.prefix().orElse("") + "xkcd sync status`.");
            return;
        }

        ctx.reply().embed(
            new EmbedBuilder()
                .setTitle("xkcd sync started")
                .setDescription("Syncing comics (safe re-sync) from **#" + from + "** to **#" + to + "**.\n"
                    + "Progress: **" + dbCount + "** / **" + expectedCount + "** stored.\n"
                    + "Check progress with `" + ctx.prefix().orElse("") + "xkcd sync status`.")
                .setColor(getEmbedColor())
        );
    }

    private void syncStatus(@NotNull CommandContext ctx) {
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

        ctx.reply().embed(
            new EmbedBuilder()
                .setTitle("xkcd sync status")
                .setDescription(desc.toString())
                .setColor(getEmbedColor())
        );
    }

    private void syncStop(@NotNull CommandContext ctx) {
        if (!xkcdSyncService.isRunning()) {
            ctx.reply().errorEmbed("No sync is currently running.");
            return;
        }

        xkcdSyncService.stop();

        ctx.reply().embed(
            new EmbedBuilder()
                .setTitle("xkcd sync stopping")
                .setDescription("Stopping sync... (it should stop shortly)")
                .setColor(getEmbedColor())
        );
    }
}
