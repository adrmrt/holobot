package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.modules.pokemon.utils.EvolutionChainFormatter;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * A Pokémon Species forms the basis for at least one Pokémon. Attributes of a
 * Pokémon species are shared across all varieties of Pokémon.
 */
public class PokemonSpecies implements Comparable<PokemonSpecies> {
    @SerializedName("name")
    String name;
    @SerializedName("id")
    int pokedexId;
    @SerializedName("generation")
    Nameable generation;
    @SerializedName("gender_rate")
    int genderRate;
    @SerializedName("has_gender_differences")
    boolean hasGenderDifferences;

    @SerializedName("evolution_chain")
    Url evolutionChain;
    @SerializedName("evolves_from_species")
    Nameable evolvesFromSpecies;

    @SerializedName("capture_rate")
    int captureRate;
    @SerializedName("base_happiness")
    int baseHappiness;
    @SerializedName("habitat")
    Nameable habitat;
    @SerializedName("growth_rate")
    Nameable growthRate;
    @SerializedName("color")
    Nameable color;
    @SerializedName("egg_groups")
    List<Nameable> eggGroups;
    @SerializedName("shape")
    Nameable shape;

    @SerializedName("is_baby")
    boolean isBaby;
    @SerializedName("is_legendary")
    boolean isLegendary;
    @SerializedName("is_mythical")
    boolean isMythical;

    @SerializedName("names")
    List<LangNameable> names;
    @SerializedName("genera")
    List<LangNameable> genera;
    @SerializedName("flavor_text_entries")
    List<PokedexEntry> pokedexEntries;

    @SerializedName("forms_switchable")
    boolean formsSwitchable;
    @SerializedName("form_descriptions")
    List<LangNameable> formDescriptions;
    @SerializedName("varieties")
    List<Variety> varieties;

    /**
     * A Pokédex entry in a given language and game version
     */
    public static class PokedexEntry {
        @SerializedName("flavor_text")
        String text;
        @SerializedName("language")
        Nameable language;
        @SerializedName("version")
        Nameable version;

        public String getText() {
            return text;
        }

        public String getLanguage() {
            return language.getName();
        }

        public String getVersion() {
            return version.getName();
        }

        public String getCleanText(String name) {
            return text.replace("\n", " ")
                .replace("\r", " ")
                .replace("POKéMON", "Pokémon")
                .replace("", " ")
                .replace("BERRIES", "berries")
                .replace("STONES", "stones")
                .replace("TRAINER", "trainer")
                .replace("POKé BALL", "Poké Ball")
                .replace(name.toUpperCase(Locale.UK), Formatter.formatPokemonName(name));
        }
    }

    public static class Variety {
        @SerializedName("is_default")
        boolean isDefault;
        @SerializedName("pokemon")
        Nameable pokemon;

        /**
         * Returns whether this is the default variety of the Pokémon.
         */
        public boolean isDefault() {
            return isDefault;
        }

        /**
         * Returns the Pokémon this variety is of.
         */
        public Nameable getPokemon() {
            return pokemon;
        }
    }

    /**
     * Returns the English name of this Pokémon species.
     */
    public String getName() {
        return Formatter.formatPokemonName(name);
    }

    /**
     * Returns the Pokédex id of this Pokémon species. Note that the id is from the national Pokédex.
     */
    public int getPokedexId() {
        return pokedexId;
    }

    /**
     * Returns the generation this Pokémon species was introduced in.
     */
    public Nameable getGeneration() {
        return generation;
    }

    /**
     * Returns the gender rate of this Pokémon species, in eights; or -1 for genderless.
     */
    public int getGenderRate() {
        return genderRate;
    }

    /**
     * Returns whether there are differences between male and female members of this Pokémon species.
     */
    public boolean hasGenderDifferences() {
        return hasGenderDifferences;
    }

    /**
     * Returns the url to the evolution chain of this Pokémon species.
     */
    public String getEvolutionChainUrl() {
        return evolutionChain.getUrl();
    }

    /**
     * Returns the Pokémon species that evolves into this one.
     */
    public Nameable getEvolvesFromSpecies() {
        return evolvesFromSpecies;
    }

    /**
     * Returns the base capture rate of this Pokémon species; up to 255. The higher the number, the easier it is to catch.
     */
    public int getCaptureRate() {
        return captureRate;
    }

    /**
     * Returns the happiness when caught by a normal Pokéball; up to 255. The higher the number, the happier the Pokémon.
     */
    public int getBaseHappiness() {
        return baseHappiness;
    }

    /**
     * Returns the habitat of this Pokémon species.
     */
    public Nameable getHabitat() {
        return habitat;
    }

    /**
     * Returns the rate at which this Pokémon species gains levels.
     */
    public Nameable getGrowthRate() {
        return growthRate;
    }

    /**
     * Returns the color of this Pokémon for the Pokédex search function.
     */
    public String getColor() {
        return color.getName();
    }

    /**
     * Returns a list of egg groups this Pokémon species is a member of.
     */
    public List<Nameable> getEggGroups() {
        return eggGroups;
    }

    /**
     * Returns the shape of this Pokémon for the Pokédex search function.
     */
    public Nameable getShape() {
        return shape;
    }

    /**
     * Checks whether this Pokémon is a baby. Baby Pokémon are at the lowest
     * stage of Pokémon evolution and cannot breed.
     */
    public boolean isBaby() {
        return isBaby;
    }

    /**
     * Checks whether this Pokémon is a legendary Pokémon. Legendary Pokémon
     * are a group of incredibly rare and often very powerful Pokémon, generally
     * featured prominently in the legends and myths of the Pokémon world.
     */
    public boolean isLegendary() {
        return isLegendary;
    }

    /**
     * Checks whether this Pokémon is a mythical Pokémon. Mythical Pokémon
     * are a group of Pokémon seen so rarely that some question their very
     * existence.
     */
    public boolean isMythical() {
        return isMythical;
    }

    /**
     * Checks whether this Pokémon species is an Ultra Beast. The Ultra Beasts are
     * a group of Pokémon originating from Ultra Space.
     */
    public boolean isUltraBeast() {
        // Ids of Ultra Beast Pokémon
        List<Integer> ids = new ArrayList<>(List.of(793, 794, 795, 796, 797, 798, 799, 803, 804, 805, 806));
        return ids.contains(pokedexId);
    }

    /**
     * Returns the name of the Pokémon in a given language. Note that the language
     * is given in its abbreviated form, i.e. English -> en.
     */
    public String getName(String language) {
        for (LangNameable name : names) {
            if (name.getLanguage().getName().equals(language)) {
                return name.getName();
            }
        }
        return getName();
    }

    /**
     * Returns the genus of the Pokémon in a given language. Note that the language
     * is its abbreviated form, i.e. English -> en.
     */
    @Nullable
    public String getGenus(String language) {
        for (LangNameable genus : genera) {
            if (genus.getLanguage().getName().equals(language)) {
                return genus.getName();
            }
        }
        return null;
    }

    /**
     * Returns a random Pokédex entry of the Pokémon in a given language.
     *
     * @param language = The language of the entry
     * @return The Pokédex entry of the Pokémon in a given language.
     */
    @Nullable
    public String getPokedexEntry(String language) {
        List<String> list = new ArrayList<>();
        for (PokedexEntry entry : pokedexEntries) {
            if (entry.language.getName().equals(language)) {
                list.add(entry.getCleanText(name));
            }
        }
        if (list.isEmpty()) {
            return null;
        }
        return list.get(new Random().nextInt(list.size()));
    }

    /**
     * Returns the Pokédex entry of the Pokémon in a given language ang game
     * version.
     *
     * @param language = The language of the entry
     * @param version  = The game version of the entry
     * @return The Pokédex entry of the Pokémon in a given language and game
     * version.
     */
    @Nullable
    public String getPokedexEntry(String language, String version) {
        for (PokedexEntry entry : pokedexEntries) {
            if (entry.language.getName().equals(language) && entry.version.getName().equals(version)) {
                return entry.getCleanText(name);
            }
        }
        return null;
    }

    /**
     * Returns a list of Pokédex entries for this Pokémon in a given language.
     *
     * @param language = The language the entries should be in
     * @return List of Pokédex entries
     */
    public List<String> getPokedexEntries(String language) {
        List<String> list = new ArrayList<>();
        for (PokedexEntry entry : pokedexEntries) {
            if (entry.language.getName().equals(language)) {
                list.add(entry.getCleanText(name));
            }
        }
        return list;
    }

    /**
     * Returns whether this Pokémon has multiple forms and can switch between them.
     */
    public boolean isFormSwitchable() {
        return formsSwitchable;
    }

    /**
     * Returns the description of the form of a Pokémon in a given language. Note that the language
     * is given in its abbreviated form, i.e. English -> en.
     */
    @Nullable
    public String getFormDescription(String language) {
        for (LangNameable description : formDescriptions) {
            if (description.getLanguage().getName().equals(language)) {
                return description.getName();
            }
        }
        return null;
    }

    /**
     * Returns a list of Pokémon that exist within this Pokémon species.
     */
    public List<Variety> getVarieties() {
        return varieties;
    }

    /**
     * Returns the evolution tree as a formatted String.
     */
    @Nullable
    public String getEvolutionChainString() throws APIException {
        EvolutionChain chain = PokeApiClient.getEvolutionChainByUrl(getEvolutionChainUrl());
        return EvolutionChainFormatter.format(chain, name);
    }

    /**
     * Returns an individual Pokémon. In this case, it's the default variant.
     */
    public Pokemon getPokemon() throws APIException, NotFoundException {
        String pokemonName = getDefaultVarietyName();
        if (pokemonName == null) {
            // This is “should never happen” unless the API changed / data is weird
            throw new APIException("Species " + pokedexId + " has no default variety");
        }

        // Prefer name-based lookup; it’s stable and matches what species gives you
        return PokeApiClient.getPokemon(pokemonName);
    }

    private @Nullable String getDefaultVarietyName() {
        if (varieties == null || varieties.isEmpty()) return null;

        for (Variety v : varieties) {
            if (v != null && v.isDefault() && v.getPokemon() != null) {
                return v.getPokemon().getName();
            }
        }

        // Fallback: first variety
        Variety first = varieties.getFirst();
        return first != null && first.getPokemon() != null ? first.getPokemon().getName() : null;
    }

    @Override
    public int compareTo(@NotNull PokemonSpecies o) {
        return Integer.compare(pokedexId, o.pokedexId);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PokemonSpecies && ((PokemonSpecies) obj).pokedexId == pokedexId;
    }
}
