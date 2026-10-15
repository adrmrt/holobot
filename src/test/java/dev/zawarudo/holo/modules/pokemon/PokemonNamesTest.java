package dev.zawarudo.holo.modules.pokemon;

import dev.zawarudo.holo.utils.Formatter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Converting between PokeAPI species slugs and English names, for the names with special characters.
 */
class PokemonNamesTest {

    @Test
    void slugsFormatToEnglishNames() {
        assertEquals("Nidoran♀", Formatter.formatPokemonName("nidoran-f"));
        assertEquals("Nidoran♂", Formatter.formatPokemonName("nidoran-m"));
        assertEquals("Farfetch’d", Formatter.formatPokemonName("farfetchd"));
        assertEquals("Type: Null", Formatter.formatPokemonName("type-null"));
        assertEquals("Flabébé", Formatter.formatPokemonName("flabebe"));
        assertEquals("Ho-Oh", Formatter.formatPokemonName("ho-oh"));
        assertEquals("Jangmo-o", Formatter.formatPokemonName("jangmo-o"));
        assertEquals("Chi-Yu", Formatter.formatPokemonName("chi-yu"));
        assertEquals("Mr. Mime", Formatter.formatPokemonName("mr-mime"));
        assertEquals("Mime Jr.", Formatter.formatPokemonName("mime-jr"));
        assertEquals("Tapu Koko", Formatter.formatPokemonName("tapu-koko"));
        assertEquals("Iron Treads", Formatter.formatPokemonName("iron-treads"));
        assertEquals("Porygon2", Formatter.formatPokemonName("porygon2"));
        assertEquals("Lightning Rod", Formatter.formatPokemonName("lightning-rod"));
    }

    @Test
    void typedNamesEscapeToSlugs() {
        assertEquals("type-null", PokeApiClient.escape("Type: Null"));
        assertEquals("flabebe", PokeApiClient.escape("Flabébé"));
        assertEquals("farfetchd", PokeApiClient.escape("Farfetch’d"));
        assertEquals("farfetchd", PokeApiClient.escape("farfetch'd"));
        assertEquals("nidoran-f", PokeApiClient.escape("Nidoran♀"));
        assertEquals("nidoran-f", PokeApiClient.escape("Nidoran ♀️"));
        assertEquals("nidoran-m", PokeApiClient.escape("nidoran m"));
        assertEquals("mr-mime", PokeApiClient.escape("Mr. Mime"));
        assertEquals("mime-jr", PokeApiClient.escape("Mime Jr."));
        assertEquals("ho-oh", PokeApiClient.escape("Ho-Oh"));
        assertEquals("tapu-koko", PokeApiClient.escape("tapu koko"));
        assertEquals("porygon2", PokeApiClient.escape("Porygon2"));
    }
}
