package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.CommandContext;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.concurrent.TimeUnit;

/**
 * Abstract class representing a bot command.
 */
public abstract class AbstractCommand {

    protected String[] args;

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Abstract method that defines the function of the command.
     *
     * @deprecated Use {@link ExecutableCommand#execute(CommandContext)} instead.
     * @param event The {@link MessageReceivedEvent} to trigger the command with.
     */
    @Deprecated(forRemoval = true)
    public void onCommand(@NotNull MessageReceivedEvent event) {
        if (this instanceof ExecutableCommand) {
            throw new IllegalStateException(
                "ContextCommand was invoked via deprecated onCommand(MessageReceivedEvent) without a context bridge. Call via CommandListener context path."
            );
        }
        throw new UnsupportedOperationException(getClass().getSimpleName() + " must override onCommand(MessageReceivedEvent).");
    }

    /**
     * Returns the prefix of the bot in the guild. If the guild didn't
     * set a custom prefix, the default prefix will be returned.
     *
     * @return The prefix of the bot needed to invoke commands.
     */
    protected String getPrefix(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            return Bootstrap.holo.getGuildConfigManager().getOrCreate(event.getGuild()).getPrefix();
        }

        return Bootstrap.holo.getConfig().getDefaultPrefix();
    }

    /**
     * Checks if a given {@link User} is the owner of this bot.
     *
     * @param user The {@link User} to check.
     * @return True if the user is the owner of the guild, false otherwise.
     */
    public boolean isBotOwner(User user) {
        return user.getIdLong() == Bootstrap.holo.getConfig().getOwnerId();
    }

    /**
     * Checks if the user is an administrator of the guild.
     *
     * @param event The {@link MessageReceivedEvent} to check with.
     * @return True if the user is an administrator of the guild, false otherwise.
     */
    public boolean isGuildAdmin(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild() && event.getMember() != null) {
            return event.getMember().equals(event.getGuild().getOwner());
        }
        return false;
    }

    /**
     * Deletes the message that triggered the command if and only if the message is from a guild. Messages
     * from direct channels can't be deleted by the bot.
     *
     * @param event The {@link MessageReceivedEvent} to delete the message from.
     */
    protected void deleteInvoke(@NotNull MessageReceivedEvent event) {
        if (event.isFromGuild()) {
            event.getMessage().delete().queue();
        }
    }

    /**
     * Sends a typing notification in the channel the command was invoked in. In other words, the user will
     * see <code>Holo is typing...</code> at the bottom.
     *
     * @param event The {@link MessageReceivedEvent} to send the typing notification in.
     */
    protected void sendTyping(@NotNull MessageReceivedEvent event) {
        event.getChannel().sendTyping().queue();
    }

    /**
     * Sends an embed to the channel the command was invoked in.
     */
    protected void sendEmbed(MessageReceivedEvent event, EmbedBuilder embedBuilder, boolean footer) {
        sendEmbed(event, embedBuilder, footer, null);
    }

    /**
     * Sends an embed to the channel the command was invoked in.
     */
    protected void sendEmbed(MessageReceivedEvent event, EmbedBuilder embedBuilder, boolean footer, Color embedColor) {
        if (footer) {
            addFooter(event, embedBuilder);
        }
        embedBuilder.setColor(embedColor);
        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }

    protected void sendReplyEmbed(Message replyTo, EmbedBuilder embedBuilder, Color embedColor) {
        embedBuilder.setColor(embedColor);
        replyTo.replyEmbeds(embedBuilder.build()).queue();
    }

    /**
     * Sends an embed to the channel the command was invoked in and deletes it after a given amount of time.
     */
    protected void sendEmbed(MessageReceivedEvent event, EmbedBuilder embedBuilder, boolean footer, long delay, TimeUnit unit) {
        sendEmbed(event, embedBuilder, footer, delay, unit, null);
    }

    /**
     * Sends an embed to the channel the command was invoked in and deletes it after a given amount of time.
     */
    protected void sendEmbed(MessageReceivedEvent event, EmbedBuilder embedBuilder, boolean footer, long delay, TimeUnit unit, Color embedColor) {
        if (footer) {
            addFooter(event, embedBuilder);
        }
        embedBuilder.setColor(embedColor);
        event.getChannel()
            .sendMessageEmbeds(embedBuilder.build())
            .queue(msg -> msg.delete()
                .queueAfter(delay, unit,
                    null,
                    _ -> {
                        // Ignore if message is already deleted
                    }
                )
            );
    }

    /**
     * Sends an embed stating that an error occurred with some information.
     */
    protected void sendErrorEmbed(MessageReceivedEvent event, String message) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle("Error");
        builder.setDescription(message);
        sendEmbed(event, builder, false, 30, TimeUnit.SECONDS, Color.RED);
    }

    /**
     * Adds a footer to the embed if and only if the event is from a guild.
     */
    private void addFooter(@NotNull MessageReceivedEvent event, @NotNull EmbedBuilder builder) {
        if (event.getMember() == null) {
            return;
        }
        String footerText = String.format("Invoked by %s", event.getMember().getEffectiveName());
        builder.setFooter(footerText, event.getAuthor().getAvatarUrl());
    }

    /**
     * Returns the name of the command.
     */
    @NotNull
    public String getName() {
        return getClass().getAnnotation(CommandInfo.class).name();
    }

    /**
     * Returns the description of the command.
     */
    @NotNull
    public String getDescription() {
        return getClass().getAnnotation(CommandInfo.class).description();
    }

    /**
     * Returns the usage of the command.
     */
    @Nullable
    public String getUsage() {
        String usage = getClass().getAnnotation(CommandInfo.class).usage();
        if (usage.isEmpty()) {
            return null;
        }
        return usage;
    }

    /**
     * Returns whether the command has a specific usage.
     */
    public boolean hasUsage() {
        return getUsage() != null;
    }

    /**
     * Returns an example of the command.
     */
    @Nullable
    public String getExample() {
        String example = getClass().getAnnotation(CommandInfo.class).example();
        if (example.isEmpty()) {
            return null;
        }
        return example;
    }

    /**
     * Returns whether the command has an example.
     */
    public boolean hasExample() {
        return getExample() != null;
    }

    /**
     * Returns the aliases of the command.
     */
    @NotNull
    public String[] getAlias() {
        return getClass().getAnnotation(CommandInfo.class).alias();
    }

    /**
     * Returns whether the command has aliases.
     */
    public boolean hasAlias() {
        return getAlias().length > 0;
    }

    /**
     * Returns the thumbnail of the command.
     */
    @Nullable
    public String getThumbnail() {
        String thumbnail = getClass().getAnnotation(CommandInfo.class).thumbnail();
        if (thumbnail.isEmpty()) {
            return null;
        }
        return thumbnail;
    }

    /**
     * Returns whether the command has a thumbnail.
     */
    public boolean hasThumbnail() {
        return getThumbnail() != null;
    }

    /**
     * Returns the embed color of the command.
     */
    @Nullable
    public Color getEmbedColor() {
        return getClass().getAnnotation(CommandInfo.class).embedColor().getColor();
    }

    /**
     * Returns the command category.
     */
    @NotNull
    public CommandCategory getCategory() {
        return getClass().getAnnotation(CommandInfo.class).category();
    }

    /**
     * Checks whether this command can only be used in a guild.
     */
    public boolean isGuildOnly() {
        return getClass().getAnnotation(CommandInfo.class).guildOnly();
    }

    /**
     * Checks whether this command can only be used by guild administrators.
     */
    public boolean isAdminOnly() {
        return getClass().getAnnotation(CommandInfo.class).adminOnly();
    }

    /**
     * Checks whether this command can only be used by the bot owner.
     */
    public boolean isOwnerOnly() {
        return getClass().getAnnotation(CommandInfo.class).ownerOnly();
    }

    /**
     * Checks whether this command is NSFW (not safe for work).
     */
    public boolean isNSFW() {
        return getClass().getAnnotation(CommandInfo.class).isNSFW();
    }
}
