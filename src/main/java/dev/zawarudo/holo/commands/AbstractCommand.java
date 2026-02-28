package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.core.Bootstrap;
import dev.zawarudo.holo.core.command.ExecutableCommand;
import dev.zawarudo.holo.utils.annotations.CommandInfo;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import org.apache.commons.validator.routines.UrlValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
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
     * @param event The {@link MessageReceivedEvent} to trigger the command with.
     */
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
                                error -> {
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
     * Sends an embed to the owner of the bot.
     */
    public void sendToOwner(EmbedBuilder builder) {
        User owner = Bootstrap.holo.getJDA().getUserById(Bootstrap.holo.getConfig().getOwnerId());
        if (owner == null) {
            if (logger.isErrorEnabled()) {
                logger.error("Owner is null which wasn't supposed to happen! Please check your config!");
            }
            return;
        }
        owner.openPrivateChannel().queue(channel -> channel.sendMessageEmbeds(builder.build()).queue());
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
     * Checks whether a String is an integer.
     *
     * @param s The String to check.
     * @return True if the String is an integer, false otherwise.
     */
    protected boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Parses a positive integer from the given string.
     *
     * @param raw The string to parse
     * @return The parsed integer if it is {@code >= 1}, or {@code -1} if parsing fails or the value is less than 1
     */
    protected int parseInt(String raw) {
        try {
            int n = Integer.parseInt(raw);
            return (n >= 1) ? n : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    /**
     * Checks whether a String is a boolean.
     *
     * @param s The String to check.
     * @return True if the String is a boolean, false otherwise.
     */
    protected boolean isBoolean(String s) {
        return "true".equals(s.toLowerCase(Locale.UK)) || "false".equals(s.toLowerCase(Locale.UK));
    }

    /**
     * Checks whether a given URL is valid.
     *
     * @param url The URL to check.
     * @return True if the URL is valid, false otherwise.
     */
    protected boolean isValidUrl(String url) {
        return new UrlValidator().isValid(url);
    }

    /**
     * Tries to get the given user, either from a mention or from the given id. If there is none to be
     * found, simply return the author of the message.
     */
    protected Optional<User> fetchMentionedUser(MessageReceivedEvent event) {
        if (args.length == 0) {
            return Optional.of(event.getAuthor());
        }

        String userId = args[0].replaceAll("\\D", ""); // Remove all non-digits
        if (!userId.isEmpty()) {
            try {
                long id = Long.parseLong(userId);
                User user = event.getJDA().getUserById(id);
                return Optional.ofNullable(user);
            } catch (NumberFormatException ignored) {
            }
        }
        return Optional.of(event.getAuthor());
    }

    /**
     * Tries to get the given user as a member of the guild. If the user is not a member of the given
     * guild, then the Optional will be empty.
     */
    protected Optional<Member> getAsGuildMember(@NotNull User user, @NotNull Guild guild) {
        try {
            return Optional.of(guild.retrieveMember(user).complete());
        } catch (ErrorResponseException ex) {
            return Optional.empty();
        }
    }

    /**
     * Method to get an embed with multiple images (up to four images). This method works because embeds with the same
     * settings will be merged so that their images are displayed in one embed.
     *
     * @param builder The {@link EmbedBuilder} with the settings the final embed should have.
     * @param images  An array of image links that should be added to the embed. Note that the array can only contain up
     *                to four images. At least one image is required.
     * @return A list of {@link MessageEmbed} that will be merged to one final embed with multiple images. The list
     * should be passed to the {@link MessageChannelUnion#sendMessageEmbeds(Collection)} method that will then
     * automatically merge the embeds.
     */
    protected List<MessageEmbed> getEmbedWithMultipleImages(@NotNull EmbedBuilder builder, @NotNull String... images) {
        if (images.length > 4) {
            throw new IllegalArgumentException("A single embed can only display four images at most! Given: " + images.length);
        }
        if (images.length == 0) {
            throw new IllegalArgumentException("This method requires at least one image!");
        }
        List<MessageEmbed> embeds = new ArrayList<>();
        for (String image : images) {
            builder.setImage(image);
            embeds.add(builder.build());
        }
        return embeds;
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