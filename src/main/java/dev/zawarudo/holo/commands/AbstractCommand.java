package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.utils.annotations.CommandInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * Abstract class representing a bot command.
 */
public abstract class AbstractCommand {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

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
