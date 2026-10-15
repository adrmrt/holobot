package dev.zawarudo.holo.commands.games.pokemon;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PokemonUtilsTest {

    private static boolean matches(String guess, String... names) {
        return PokemonUtils.nameKeys(names).contains(PokemonUtils.normalizeName(guess));
    }

    @Test
    void guessIgnoresCaseAccentsAndPunctuation() {
        assertTrue(matches("pikachu", "Pikachu"));
        assertTrue(matches("flabebe", "Flabébé"));
        assertTrue(matches("mr mime", "Mr. Mime"));
        assertTrue(matches("farfetchd", "Farfetch’d"));
        assertTrue(matches("type null", "Type: Null"));
        assertTrue(matches("porygon z", "Porygon-Z"));
    }

    /** English and German names exactly as PokeAPI returns them, with inputs a player would type. */
    @Test
    void specialPokeApiNamesMatchTypedGuesses() {
        assertTrue(matches("Farfetch'd", "Farfetch’d", "Porenta"));
        assertTrue(matches("sirfetchd", "Sirfetch’d", "Lauchzelot"));
        assertTrue(matches("Type: Null", "Type: Null", "Typ:Null"));
        assertTrue(matches("typ null", "Type: Null", "Typ:Null"));
        assertTrue(matches("Mime Jr", "Mime Jr.", "Pantimimi"));
        assertTrue(matches("Mr.Rime", "Mr. Rime", "Pantifrost"));
        assertTrue(matches("ho oh", "Ho-Oh", "Ho-Oh"));
        assertTrue(matches("Porygon2", "Porygon2", "Porygon2"));
        assertTrue(matches("kommo o", "Kommo-o", "Grandiras"));
        assertTrue(matches("Kapu-Riki", "Tapu Koko", "Kapu-Riki"));
        assertTrue(matches("chi yu", "Chi-Yu", "Yuyu"));
        assertTrue(matches("ironTreads", "Iron Treads", "Eisenrad"));
        assertTrue(matches("Nidoran♀️", "Nidoran♀", "Nidoran♀"));
    }

    @Test
    void genderSignsMatchFAndM() {
        assertTrue(matches("nidoran f", "Nidoran♀"));
        assertTrue(matches("Nidoran♂", "Nidoran♂"));
        assertFalse(matches("nidoran", "Nidoran♀"));
        assertFalse(matches("nidoran m", "Nidoran♀"));
    }

    @Test
    void germanUmlautsMatchPlainAndSpelledOut() {
        Set<String> keys = PokemonUtils.nameKeys("Glurak", "Flügelfaust");
        assertTrue(keys.contains(PokemonUtils.normalizeName("glurak")));
        assertTrue(keys.contains(PokemonUtils.normalizeName("Flügelfaust")));
        assertTrue(keys.contains(PokemonUtils.normalizeName("flugelfaust")));
        assertTrue(keys.contains(PokemonUtils.normalizeName("fluegelfaust")));
    }

    @Test
    void wrongGuessDoesNotMatch() {
        assertFalse(matches("raichu", "Pikachu", "Pikachu"));
        assertFalse(matches("", "Pikachu"));
    }

    @Test
    void catchPoints() {
        assertEquals(1, PokemonUtils.catchPoints(false, false));
        assertEquals(10, PokemonUtils.catchPoints(true, false));
        assertEquals(5, PokemonUtils.catchPoints(false, true));
        assertEquals(50, PokemonUtils.catchPoints(true, true));
    }
}
