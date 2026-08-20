package dev.zawarudo.holo.core;

import dev.zawarudo.holo.commands.CommandMetadata;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.security.BlacklistService;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import org.jetbrains.annotations.NotNull;

/**
 * Handles the permissions and checks if a user is allowed to use a command.
 */
public class PermissionManager {

    private final BlacklistService blacklist;

    public PermissionManager(BlacklistService blacklist) {
        this.blacklist = blacklist;
    }

    public Decision check(@NotNull CommandContext ctx, @NotNull CommandMetadata command) {
        long userId = ctx.user().getIdLong();

        // Check if user has been blacklisted
        if (blacklist.isBlacklisted(userId)) {
            return Decision.deny(Decision.DenyReason.BLACKLISTED, null);
        }

        Decision user = checkUserPermission(ctx, command);
        if (!user.allowed()) return user;

        return checkChannelPermission(ctx, command);
    }

    private Decision checkUserPermission(CommandContext ctx, CommandMetadata command) {
        if (command.isOwnerOnly()) {
            return ctx.isBotOwner()
                ? Decision.allow()
                : Decision.deny(Decision.DenyReason.OWNER_ONLY, null);
        }

        if (command.isAdminOnly()) {
            // Bot owner can bypass admin requirement
            return ctx.isGuildAdmin() || ctx.isBotOwner()
                ? Decision.allow()
                : Decision.deny(Decision.DenyReason.ADMIN_ONLY, null);
        }

        return Decision.allow();
    }

    private Decision checkChannelPermission(CommandContext ctx, CommandMetadata command) {
        if (command.isGuildOnly() && !ctx.inGuild()) {
            return Decision.deny(Decision.DenyReason.GUILD_ONLY, "This command can only be used in a server.");
        }

        if (!command.isNSFW() || !ctx.inGuild()) {
            return Decision.allow();
        }

        GuildConfig config = ctx.guildConfig().orElseThrow();

        if (!config.isNSFWEnabled()) {
            return Decision.deny(
                Decision.DenyReason.NSFW_DISABLED,
                "NSFW commands are disabled in this server."
            );
        }

        if (!isChannelNSFW(ctx.channel())) {
            return Decision.deny(
                Decision.DenyReason.NSFW_CHANNEL_REQUIRED,
                "You can't use NSFW commands outside NSFW channels.\nPlease move to a NSFW channel to use this command."
            );
        }

        return Decision.allow();
    }

    private boolean isChannelNSFW(@NotNull MessageChannelUnion channel) {
        // Thread channel inherits settings from parent channel
        if (channel.getType().isThread()) {
            ThreadChannel thread = channel.asThreadChannel();
            return thread.getParentChannel().asTextChannel().isNSFW();
        }
        return channel instanceof IAgeRestrictedChannel c && c.isNSFW();
    }

    public void respondDenied(@NotNull CommandContext ctx, @NotNull Decision decision) {
        if (decision.message() == null || decision.message().isBlank()) {
            return; // silent denies (blacklist/admin/owner)
        }

        ctx.invocation().deleteInvokeIfPossible();
        ctx.reply().errorEmbed(decision.message());
    }

    public record Decision(boolean allowed, DenyReason reason, String message) {
        public static Decision allow() {
            return new Decision(true, DenyReason.NONE, null);
        }

        public static Decision deny(DenyReason reason, String message) {
            return new Decision(false, reason, message);
        }

        public enum DenyReason {
            NONE,
            BLACKLISTED,
            OWNER_ONLY,
            ADMIN_ONLY,
            GUILD_ONLY,
            NSFW_DISABLED,
            NSFW_CHANNEL_REQUIRED
        }
    }
}
