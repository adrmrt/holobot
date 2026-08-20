package dev.zawarudo.holo.commands;

/**
 * Enum representing categories of {@link CommandMetadata}s.
 */
public enum CommandCategory {
    /**
     * Commands that show information about the bot, guilds and users.
     */
    GENERAL("General Commands"),
    /**
     * Commands related to anime or manga.
     */
    ANIME("Anime Commands"),
    /**
     * Commands related to music and the music player.
     */
    MUSIC("Music Commands"),
    /**
     * Commands related to mini-games.
     */
    GAMES("Game Commands"),
    /**
     * Commands related to fetching pictures and various image manipulations.
     */
    IMAGE("Image Commands"),
    /**
     * Commands that send a random reaction or action gif
     */
    ACTION("Action Commands"),
    /**
     * Miscellaneous commands that don't fit into any other category.
     */
    MISC("Miscellaneous Commands"),
    /**
     * Experimental commands that are not yet ready for public use.
     */
    EXPERIMENTAL("Experimental Commands"),
    /**
     * Commands that are only available to the admins of a guild.
     */
    ADMIN("Admin Commands"),
    /**
     * Commands that are only available to the owner of the bot.
     */
    OWNER("Owner Commands"),
    /**
     * Default category
     */
    BLANK("Uncategorized Commands");

    private final String name;

    CommandCategory(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the category.
     *
     * @return The category name as a String.
     */
    public String getName() {
        return this.name;
    }
}
