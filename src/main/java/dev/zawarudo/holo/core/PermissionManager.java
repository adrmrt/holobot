package dev.zawarudo.holo.core;

import dev.zawarudo.holo.commands.AbstractCommand;
import dev.zawarudo.holo.core.security.BlacklistService;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.attribute.IAgeRestrictedChannel;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Handles the permissions and checks if a user is allowed to use a command.
 */
public class PermissionManager {

    private final BlacklistService blacklist;
    private final GuildConfigManager guildConfigManager;

    public PermissionManager(BlacklistService blacklist, GuildConfigManager guildConfigManager) {
        this.blacklist = blacklist;
        this.guildConfigManager = guildConfigManager;
    }

    public Decision check(@NotNull MessageReceivedEvent event, @NotNull AbstractCommand command) {
        long userId = event.getAuthor().getIdLong();

        // Check if user has been blacklisted
        if (blacklist.isBlacklisted(userId)) {
            return Decision.deny(Decision.DenyReason.BLACKLISTED, null);
        }

        Decision user = checkUserPermission(event, command);
        if (!user.allowed()) return user;

        return checkChannelPermission(event, command);
    }

    private Decision checkUserPermission(MessageReceivedEvent event, AbstractCommand command) {
        if (command.isOwnerOnly()) {
            return command.isBotOwner(event.getAuthor())
                ? Decision.allow()
                : Decision.deny(Decision.DenyReason.OWNER_ONLY, null);
        }

        if (command.isAdminOnly()) {
            // Bot owner can bypass admin requirement
            return command.isGuildAdmin(event) || command.isBotOwner(event.getAuthor())
                ? Decision.allow()
                : Decision.deny(Decision.DenyReason.ADMIN_ONLY, null);
        }

        return Decision.allow();
    }

    private Decision checkChannelPermission(MessageReceivedEvent event, AbstractCommand command) {
        if (command.isGuildOnly() && !event.isFromGuild()) {
            return Decision.deny(Decision.DenyReason.GUILD_ONLY, "This command can only be used in a server.");
        }

        if (!command.isNSFW() || !event.isFromGuild()) {
            return Decision.allow();
        }

        Guild guild = event.getGuild();
        GuildConfig config = guildConfigManager.getOrCreate(guild);

        if (!config.isNSFWEnabled()) {
            return Decision.deny(
                Decision.DenyReason.NSFW_DISABLED,
                "NSFW commands are disabled in this server."
            );
        }

        if (!isChannelNSFW(event.getChannel())) {
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

    public void respondDenied(@NotNull MessageReceivedEvent event, @NotNull Decision decision) {
        if (decision.message() == null || decision.message().isBlank()) {
            return; // silent denies (blacklist/admin/owner)
        }

        if (event.isFromGuild()) {
            event.getMessage().delete().queueAfter(30, TimeUnit.SECONDS);
        }

        EmbedBuilder builder = new EmbedBuilder()
            .setTitle("Error")
            .setDescription(decision.message())
            .setColor(Color.RED);

        event.getChannel()
            .sendMessageEmbeds(builder.build())
            .queue(msg -> msg.delete().queueAfter(30, TimeUnit.SECONDS));
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
