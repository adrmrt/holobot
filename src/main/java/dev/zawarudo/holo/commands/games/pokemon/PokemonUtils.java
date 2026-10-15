package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.ImageOperations;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Utility class for Pokemon-related functionality.
 */
public final class PokemonUtils {

    private PokemonUtils() {
    }

    /**
     * Returns the {@link PokemonType} from the given name.
     *
     * @param type The name of the type.
     * @throws IllegalArgumentException If the given name is not a valid type name.
     */
    public static PokemonType getType(@NotNull String type) {
        return switch (type.toLowerCase(Locale.UK)) {
            case "normal" -> PokemonType.NORMAL;
            case "fire" -> PokemonType.FIRE;
            case "fighting" -> PokemonType.FIGHTING;
            case "flying" -> PokemonType.FLYING;
            case "water" -> PokemonType.WATER;
            case "grass" -> PokemonType.GRASS;
            case "electric" -> PokemonType.ELECTRIC;
            case "poison" -> PokemonType.POISON;
            case "dark" -> PokemonType.DARK;
            case "fairy" -> PokemonType.FAIRY;
            case "psychic" -> PokemonType.PSYCHIC;
            case "steel" -> PokemonType.STEEL;
            case "rock" -> PokemonType.ROCK;
            case "ground" -> PokemonType.GROUND;
            case "bug" -> PokemonType.BUG;
            case "dragon" -> PokemonType.DRAGON;
            case "ghost" -> PokemonType.GHOST;
            case "ice" -> PokemonType.ICE;
            default -> throw new IllegalArgumentException("Unknown type: " + type);
        };
    }

    /**
     * Draws a black silhouette of the given Pokemon on the background.
     *
     * @param pokemon The Pokemon whose silhouette should be drawn.
     * @return A {@link BufferedImage} of the Pokemon silhouette on a background.
     */
    public static BufferedImage drawHiddenPokemon(Pokemon pokemon) throws IOException {
        BufferedImage blackPokemon = getBlackPokemon(pokemon);
        BufferedImage bg = getBackgroundImage();
        return createPokemonScene(blackPokemon, bg);
    }

    private static BufferedImage getBlackPokemon(Pokemon pokemon) throws IOException {
        String spriteUrl = pokemon.getSprites().getOther().getArtwork().getFrontDefault();
        BufferedImage spriteImage = ImageIO.read(URI.create(spriteUrl).toURL());
        return ImageOperations.turnBlack(spriteImage);
    }

    private static BufferedImage getBackgroundImage() throws IOException {
        try (InputStream in = PokemonUtils.class.getResourceAsStream("/image/pokemon/background.png")) {
            if (in == null) {
                throw new IOException("Missing resource image/pokemon/background.png");
            }
            return ImageIO.read(in);
        }
    }

    /**
     * Returns the points a catch is worth: 1, or 10 for legendaries and mythicals, times 5 for shinies.
     */
    public static int catchPoints(boolean legendaryOrMythical, boolean shiny) {
        int points = legendaryOrMythical ? 10 : 1;
        return shiny ? points * 5 : points;
    }

    /**
     * Returns the normalized forms of the given names that a guess is compared against.
     * German umlauts are accepted both plain (u) and spelled out (ue).
     */
    public static Set<String> nameKeys(String... names) {
        Set<String> keys = new HashSet<>();
        for (String name : names) {
            keys.add(normalizeName(name));
            keys.add(normalizeName(name.toLowerCase(Locale.ROOT)
                .replace("ä", "ae")
                .replace("ö", "oe")
                .replace("ü", "ue")));
        }
        return keys;
    }

    /**
     * Lowercases the name and drops accents, punctuation and spaces. The female and male signs
     * become f and m, so "Nidoran f" matches Nidoran's female form.
     */
    public static String normalizeName(String name) {
        String s = name.toLowerCase(Locale.ROOT)
            .replace("♀", "f")
            .replace("♂", "m")
            .replace("ß", "ss");
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("[^a-z0-9]", "");
    }

    private static BufferedImage createPokemonScene(BufferedImage silhouette, BufferedImage background) {
        Graphics2D graphics = background.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.drawImage(silhouette, 200, 50, 400, 400, null);
        graphics.dispose();
        return background;
    }
}