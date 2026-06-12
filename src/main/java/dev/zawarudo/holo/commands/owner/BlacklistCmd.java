package dev.zawarudo.holo.commands.owner;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.commands.CommandCategory;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.core.security.BlacklistService;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;

/**
 * Command to blacklist a user from using the bot.
 */
@CommandInfo(name = "blacklist",
    description = "Blacklists an user from using the bot.",
    usage = "<user id|@mention> [reason...] | remove <user id|@mention>",
    ownerOnly = true,
    category = CommandCategory.OWNER)
public class BlacklistCmd implements CommandMetadata, ExecutableCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlacklistCmd.class);

    private final BlacklistService blacklistService;

    public BlacklistCmd(BlacklistService blacklistService) {
        this.blacklistService = blacklistService;
    }

    @Override
    public void execute(@NotNull CommandContext ctx) {
        ctx.invocation().deleteInvokeIfPossible();

        if (!ctx.hasArgs()) {
            sendUsage(ctx, "Missing arguments.");
            return;
        }

        if (isRemoveMode(ctx.args().getFirst())) {
            handleRemove(ctx);
            return;
        }

        handleAdd(ctx);
    }

    private void handleAdd(CommandContext ctx) {
        Long userId = parseUserId(ctx.args().getFirst());
        if (userId == null) {
            sendUsage(ctx, "Invalid user id / mention: `" + ctx.args().getFirst() + "`");
            return;
        }

        String reason = parseReasonFromIndex(ctx, 1);

        try {
            blacklistService.blacklist(
                userId,
                reason,
                ctx.message().map(m -> m.getTimeCreated().toString()).orElse("")
            );
        } catch (SQLException ex) {
            LOGGER.error("Failed to blacklist userId={}", userId, ex);
            sendErrorToOwner(ctx, "Database error while blacklisting user.");
            return;
        }

        User cached = ctx.jda().getUserById(userId);
        String who = formatUser(cached, userId);

        ctx.notifyOwner(embed()
            .setTitle("User successfully blacklisted")
            .setDescription("**User:** " + who + "\n**Reason:** " + reason));
    }

    private void handleRemove(CommandContext ctx) {
        if (ctx.argCount() < 2) {
            sendUsage(ctx, "Missing user id / mention for remove.");
            return;
        }

        Long userId = parseUserId(ctx.args().get(1));
        if (userId == null) {
            sendUsage(ctx, "Invalid user id / mention: `" + ctx.args().get(1) + "`");
            return;
        }

        try {
            blacklistService.unblacklist(userId);
        } catch (SQLException ex) {
            LOGGER.error("Failed to unblacklist userId={}", userId, ex);
            sendErrorToOwner(ctx, "Database error while removing user from blacklist.");
            return;
        }

        User cached = ctx.jda().getUserById(userId);
        String who = formatUser(cached, userId);

        ctx.notifyOwner(embed()
            .setTitle("User removed from blacklist")
            .setDescription("**User:** " + who));
    }

    private boolean isRemoveMode(String firstArg) {
        String s = firstArg.toLowerCase();
        return s.equals("remove") || s.equals("unblacklist") || s.equals("delete");
    }

    private Long parseUserId(String raw) {
        String s = raw.trim()
            .replace("<@!", "")
            .replace("<@", "")
            .replace(">", "");

        try {
            return Long.parseLong(s);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private String parseReasonFromIndex(CommandContext ctx, int index) {
        if (ctx.argCount() <= index) return "None given";
        String r = String.join(" ", ctx.args().subList(index, ctx.args().size())).trim();
        return r.isBlank() ? "None given" : r;
    }

    private String formatUser(User cached, long userId) {
        if (cached == null) return "`" + userId + "`";
        return cached.getAsMention() + " (`" + cached.getName() + "`, `" + cached.getId() + "`)";
    }

    private void sendUsage(CommandContext ctx, String message) {
        String p = ctx.prefix().orElse("");
        ctx.notifyOwner(embed()
            .setTitle("Incorrect Usage")
            .setDescription(message + "\n\n" +
                "Add: `" + p + "blacklist <userId|@mention> [reason...]`\n" +
                "Remove: `" + p + "blacklist remove <userId|@mention>`"));
    }

    private void sendErrorToOwner(CommandContext ctx, String message) {
        ctx.notifyOwner(embed()
            .setTitle("Error")
            .setDescription(message));
    }

    private EmbedBuilder embed() {
        return new EmbedBuilder().setTimestamp(Instant.now());
    }
}
