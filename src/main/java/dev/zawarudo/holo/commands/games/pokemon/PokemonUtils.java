package dev.zawarudo.holo.commands.games.pokemon;

import dev.zawarudo.holo.modules.pokemon.model.Pokemon;
import dev.zawarudo.holo.utils.ImageOperations;
import org.jetbrains.annotations.NotNull;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Locale;

/**
 * Utility class for Pokémon-related functionality.
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
     * Draws a black silhouette of the given Pokémon on the background.
     *
     * @param pokemon The Pokémon whose silhouette should be drawn.
     * @return A {@link BufferedImage} of the Pokémon silhouette on a background.
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
        File bgFile = new File("src/main/resources/image/pokemon/background.png");
        return ImageIO.read(bgFile);
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