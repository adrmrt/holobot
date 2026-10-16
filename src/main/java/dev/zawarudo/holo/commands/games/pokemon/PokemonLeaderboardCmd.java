package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.misc.EmbedColor;
import dev.zawarudo.holo.database.dao.PokemonDao;
import dev.zawarudo.holo.database.dao.PokemonDao.LeaderboardEntry;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@CommandInfo(name = "pokelb",
    description = "Shows the Pokémon catching leaderboard of this server. The bot owner can reset it, " +
        "which deletes the catches from the players' collections as well.",
    usage = "[reset [<user>] [confirm]]",
    embedColor = EmbedColor.POKEMON,
    category = CommandCategory.GAMES)
public class PokemonLeaderboardCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(PokemonLeaderboardCmd.class);
    private static final int TOP = 10;

    private final PokemonDao dao;

    public PokemonLeaderboardCmd(PokemonDao dao) {
        this.dao = dao;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        try {
            if (ctx.arg(0).filter("reset"::equalsIgnoreCase).isPresent()) {
                reset(ctx);
            } else {
                showLeaderboard(ctx);
            }
        } catch (SQLException e) {
            LOGGER.error("Pokémon leaderboard query failed in guild id={}", ctx.guildIdOrZero(), e);
            ctx.reply().errorEmbed("Something went wrong with the leaderboard. Please try again later.");
        }
    }

    private void showLeaderboard(CommandContext ctx) throws SQLException {
        List<LeaderboardEntry> board = dao.findLeaderboard(ctx.guildIdOrZero());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Pokémon Leaderboard");
        builder.setColor(getEmbedColor());

        if (board.isEmpty()) {
            builder.setDescription("Nobody has caught a Pokémon on this server yet.");
            ctx.reply().embed(builder);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(TOP, board.size()); i++) {
            sb.append(line(i, board.get(i))).append('\n');
        }
        // Show the caller's own rank when they're outside the top
        for (int i = TOP; i < board.size(); i++) {
            if (board.get(i).userId() == ctx.user().getIdLong()) {
                sb.append("...\n").append(line(i, board.get(i)));
                break;
            }
        }
        // Mentions in embeds show the name without pinging anyone
        builder.setDescription(sb.toString());
        ctx.reply().embed(builder);
    }

    /** Parsed {@code reset [<user>] [confirm]} arguments; {@code userId} is null for the whole server. */
    record ResetArgs(@Nullable Long userId, boolean confirm) {
    }

    /**
     * Parses the arguments of {@code reset [<user>] [confirm]}, starting with "reset".
     * Returns empty if they are malformed.
     */
    static Optional<ResetArgs> parseResetArgs(List<String> args) {
        List<String> rest = args.subList(1, args.size());
        boolean confirm = !rest.isEmpty() && "confirm".equalsIgnoreCase(rest.getLast());
        List<String> target = confirm ? rest.subList(0, rest.size() - 1) : rest;

        if (target.isEmpty()) {
            return Optional.of(new ResetArgs(null, confirm));
        }
        // A mention (<@123>) or a plain user id
        String digits = target.getFirst().replaceAll("\\D", "");
        if (target.size() > 1 || digits.isEmpty() || digits.length() > 19) {
            return Optional.empty();
        }
        return Optional.of(new ResetArgs(Long.parseLong(digits), confirm));
    }

    /**
     * {@code reset [<user>]} shows what would be deleted; adding {@code confirm} deletes it.
     */
    private void reset(CommandContext ctx) throws SQLException {
        if (!ctx.isBotOwner()) {
            ctx.reply().errorEmbed("Only the bot owner can reset points.");
            return;
        }

        Optional<ResetArgs> parsed = parseResetArgs(ctx.args());
        if (parsed.isEmpty()) {
            ctx.reply().errorEmbed("Usage: `" + prefix(ctx) + "pokelb reset [<user>] [confirm]`");
            return;
        }
        ResetArgs resetArgs = parsed.get();
        if (resetArgs.userId() == null) {
            resetServer(ctx, resetArgs.confirm());
        } else {
            resetUser(ctx, resetArgs.userId(), resetArgs.confirm());
        }
    }

    private void resetServer(CommandContext ctx, boolean confirm) throws SQLException {
        long guildId = ctx.guildIdOrZero();
        if (confirm) {
            reply(ctx, "Deleted " + catches(dao.deleteCatches(guildId)) + " in this server.");
            return;
        }

        List<LeaderboardEntry> board = dao.findLeaderboard(guildId);
        if (board.isEmpty()) {
            reply(ctx, "There are no points to reset in this server.");
            return;
        }
        long points = board.stream().mapToLong(LeaderboardEntry::points).sum();
        reply(ctx, String.format("This deletes all catches in this server: **%d** points of **%d** players. " +
                "The Pokémon also leave their collections.\nRun `%spokelb reset confirm` to proceed.",
            points, board.size(), prefix(ctx)));
    }

    private void resetUser(CommandContext ctx, long userId, boolean confirm) throws SQLException {
        long guildId = ctx.guildIdOrZero();
        if (confirm) {
            reply(ctx, "Deleted " + catches(dao.deleteCatches(guildId, userId)) + " of <@" + userId + "> in this server.");
            return;
        }

        long points = dao.findPoints(guildId, userId);
        if (points == 0) {
            reply(ctx, "<@" + userId + "> has no points in this server.");
            return;
        }
        reply(ctx, String.format("This deletes the catches of <@%d> in this server: **%d** points. " +
                "The Pokémon also leave their collection.\nRun `%spokelb reset %d confirm` to proceed.",
            userId, points, prefix(ctx), userId));
    }

    private void reply(CommandContext ctx, String text) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setDescription(text);
        builder.setColor(getEmbedColor());
        ctx.reply().embed(builder);
    }

    private static String prefix(CommandContext ctx) {
        return ctx.prefix().orElse("");
    }

    private static String catches(int count) {
        return count + (count == 1 ? " catch" : " catches");
    }

    private static String line(int index, LeaderboardEntry entry) {
        return String.format("**%d.** <@%d> • %d %s", index + 1, entry.userId(), entry.points(), entry.points() == 1 ? "point" : "points");
    }
}
