package dev.zawarudo.holo.core.misc;

import java.awt.*;

/**
 * Enum to store the colors used in the embeds of Holo.
 */
public enum EmbedColor {

    /**
     * The Discord default embed color.
     */
    DEFAULT(null),
    /**
     * The embed color for the error embed.
     */
    ERROR(Color.RED),
    /**
     * The color of Akinator.
     */
    AKINATOR(new Color(112, 28, 84)),
    DICTIONARY(Color.WHITE),
    /**
     * The color of Gelbooru.
     */
    GELBOORU(new Color(0, 100, 225)),
    /**
     * The color of Inspirobot.me.
     */
    INSPIRO(new Color(35, 96, 19)),
    /**
     * The color of Pokémon-related commands.
     */
    POKEMON(new Color(255, 0, 0)),
    /**
     * The color of MyAnimeList.
     */
    MAL(new Color(46, 81, 162)),
    WHITE(Color.WHITE),
    LIGHT_GRAY(Color.LIGHT_GRAY),
    URBAN(new Color(27, 41, 54));

    private final Color color;

    EmbedColor(Color color) {
        this.color = color;
    }

    /**
     * Returns the color of the embed.
     */
    public Color getColor() {
        return color;
    }
}
