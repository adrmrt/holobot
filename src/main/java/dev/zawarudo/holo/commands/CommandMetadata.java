package dev.zawarudo.holo.commands;

import dev.zawarudo.holo.utils.annotations.CommandInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Provides metadata about a bot command, derived from its {@link CommandInfo} annotation.
 */
public interface CommandMetadata {

    /**
     * Returns the name of the command.
     */
    @NotNull
    default String getName() {
        return getClass().getAnnotation(CommandInfo.class).name();
    }

    /**
     * Returns the description of the command.
     */
    @NotNull
    default String getDescription() {
        return getClass().getAnnotation(CommandInfo.class).description();
    }

    /**
     * Returns the usage of the command.
     */
    @Nullable
    default String getUsage() {
        String usage = getClass().getAnnotation(CommandInfo.class).usage();
        if (usage.isEmpty()) {
            return null;
        }
        return usage;
    }

    /**
     * Returns whether the command has a specific usage.
     */
    default boolean hasUsage() {
        return getUsage() != null;
    }

    /**
     * Returns an example of the command.
     */
    @Nullable
    default String getExample() {
        String example = getClass().getAnnotation(CommandInfo.class).example();
        if (example.isEmpty()) {
            return null;
        }
        return example;
    }

    /**
     * Returns whether the command has an example.
     */
    default boolean hasExample() {
        return getExample() != null;
    }

    /**
     * Returns the aliases of the command.
     */
    @NotNull
    default String[] getAlias() {
        return getClass().getAnnotation(CommandInfo.class).alias();
    }

    /**
     * Returns whether the command has aliases.
     */
    default boolean hasAlias() {
        return getAlias().length > 0;
    }

    /**
     * Returns the thumbnail of the command.
     */
    @Nullable
    default String getThumbnail() {
        String thumbnail = getClass().getAnnotation(CommandInfo.class).thumbnail();
        if (thumbnail.isEmpty()) {
            return null;
        }
        return thumbnail;
    }

    /**
     * Returns whether the command has a thumbnail.
     */
    default boolean hasThumbnail() {
        return getThumbnail() != null;
    }

    /**
     * Returns the embed color of the command.
     */
    @Nullable
    default Color getEmbedColor() {
        return getClass().getAnnotation(CommandInfo.class).embedColor().getColor();
    }

    /**
     * Returns the command category.
     */
    @NotNull
    default CommandCategory getCategory() {
        return getClass().getAnnotation(CommandInfo.class).category();
    }

    /**
     * Checks whether this command can only be used in a guild.
     */
    default boolean isGuildOnly() {
        return getClass().getAnnotation(CommandInfo.class).guildOnly();
    }

    /**
     * Checks whether this command can only be used by guild administrators.
     */
    default boolean isAdminOnly() {
        return getClass().getAnnotation(CommandInfo.class).adminOnly();
    }

    /**
     * Checks whether this command can only be used by the bot owner.
     */
    default boolean isOwnerOnly() {
        return getClass().getAnnotation(CommandInfo.class).ownerOnly();
    }

    /**
     * Checks whether this command is NSFW (not safe for work).
     */
    default boolean isNSFW() {
        return getClass().getAnnotation(CommandInfo.class).isNSFW();
    }
}