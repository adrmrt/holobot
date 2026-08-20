package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;
import dev.zawarudo.holo.modules.pokemon.PokeApiClient;
import dev.zawarudo.holo.utils.Formatter;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.NotFoundException;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Pokémon are the creatures that inhabit the world of Pokémon. They can be caught using
 * Pokéballs and trained by battling with other Pokémon. Each Pokémon belongs to a specific
 * species but may take on a variant which makes it differ from other Pokémon of the same
 * species, such as base stats, available abilities and types. See
 * <a href="http://bulbapedia.bulbagarden.net/wiki/Pok%C3%A9mon_(species)">Bulbapedia</a>
 * for greater detail.
 */
public class Pokemon implements Comparable<Pokemon> {
    @SerializedName("name")
    String name;
    @SerializedName("id")
    int pokedexId;
    @SerializedName("species")
    Nameable species;
    @SerializedName("types")
    List<PokemonType> types;
    @SerializedName("weight")
    int weight;
    @SerializedName("height")
    int height;
    @SerializedName("sprites")
    Sprites sprites;
    @SerializedName("forms")
    List<Nameable> forms;
    @SerializedName("is_default")
    boolean isDefault;
    @SerializedName("base_experience")
    int baseExperience;
    @SerializedName("abilities")
    List<Ability> abilities;
    @SerializedName("moves")
    List<Move> moves;
    @SerializedName("held_items")
    List<HeldItem> heldItems;
    @SerializedName("stats")
    List<Stat> stats;

    /**
     * Types are properties for Pokémon and their moves. Each type has three properties: which
     * types of Pokémon it is super effective against, which types of Pokémon it is not very
     * effective against, and which types of Pokémon it is completely ineffective against.
     */
    public static class PokemonType {
        @SerializedName("type")
        private Nameable type;
        @SerializedName("slot")
        private int slot;

        /**
         * The type the referenced Pokémon has.
         */
        public Nameable getType() {
            return type;
        }

        /**
         * The order this type appears among the types of this Pokémon.
         */
        public int getSlot() {
            return slot;
        }
    }

    public static class Ability {
        @SerializedName("ability")
        private Nameable ability;
        @SerializedName("is_hidden")
        private boolean isHidden;
        @SerializedName("slot")
        private int slot;

        public String getName() {
            return ability.getName();
        }

        public String getUrl() {
            return ability.getUrl();
        }

        public boolean isHidden() {
            return isHidden;
        }

        public int getSlot() {
            return slot;
        }
    }

    public static class Move {
        @SerializedName("move")
        private Nameable move;
        @SerializedName("version_group_details")
        private List<VersionDetails> details;

        public static class VersionDetails {
            @SerializedName("level_learned_at")
            private int level;
            @SerializedName("move_learn_method")
            private Nameable learnMethod;
            @SerializedName("version")
            private Nameable gameVersion;

            public int getLevel() {
                return level;
            }

            public Nameable getLearnMethod() {
                return learnMethod;
            }

            public Nameable getGameVersion() {
                return gameVersion;
            }
        }

        public String getName() {
            return move.getName();
        }

        public String getUrl() {
            return move.getUrl();
        }

        public List<VersionDetails> getDetails() {
            return details;
        }
    }

    public static class HeldItem {
        @SerializedName("item")
        private Nameable item;
        @SerializedName("version_details")
        private List<VersionDetails> details;

        public static class VersionDetails {
            @SerializedName("rarity")
            private int rarity;
            @SerializedName("version")
            private Nameable version;

            /**
             * The chance of this Pokémon holding this item.
             */
            public int getRarity() {
                return rarity;
            }

            /**
             * The version of the game this item is held by the referenced Pokémon.
             */
            public Nameable getVersion() {
                return version;
            }
        }

        /**
         * The item the referenced Pokémon holds.
         */
        public Nameable getItem() {
            return item;
        }

        /**
         * The details of the different game versions in which the Pokémon holds the item.
         */
        public List<VersionDetails> getDetails() {
            return details;
        }
    }

    public static class Stat {
        @SerializedName("stat")
        private Nameable stat;
        @SerializedName("base_stat")
        private int baseStat;
        @SerializedName("effort")
        private int effort;

        public Nameable getStat() {
            return stat;
        }

        public int getBaseStat() {
            return baseStat;
        }

        public int getEffort() {
            return effort;
        }
    }

    public static class Sprites {
        @SerializedName("front_default")
        private String frontDefault;
        @SerializedName("front_female")
        private String frontFemale;
        @SerializedName("front_shiny")
        private String frontShiny;
        @SerializedName("front_shiny_female")
        private String frontShinyFemale;
        @SerializedName("back_default")
        private String backDefault;
        @SerializedName("back_female")
        private String backFemale;
        @SerializedName("back_shiny")
        private String backShiny;
        @SerializedName("back_shiny_female")
        private String backShinyFemale;

        @SerializedName("other")
        private Other other;

        public static class Other {
            @SerializedName("dream_world")
            private DreamWorld dreamWorld;
            @SerializedName("home")
            private Home home;
            @SerializedName("official-artwork")
            private Artwork artwork;

            public DreamWorld getDreamWorld() {
                return dreamWorld;
            }

            public Home getHome() {
                return home;
            }

            public Artwork getArtwork() {
                return artwork;
            }
        }

        public static class DreamWorld {
            @SerializedName("front_default")
            private String frontDefault;
            @SerializedName("front_female")
            private String frontFemale;

            public String getFrontDefault() {
                return frontDefault;
            }

            public String getFrontFemale() {
                return frontFemale;
            }
        }

        public static class Home {
            @SerializedName("front_default")
            private String frontDefault;
            @SerializedName("front_female")
            private String frontFemale;
            @SerializedName("front_shiny")
            private String frontShiny;
            @SerializedName("front_shiny_female")
            private String frontShinyFemale;

            public String getFrontDefault() {
                return frontDefault;
            }

            public String getFrontFemale() {
                return frontFemale;
            }

            public String getFrontShiny() {
                return frontShiny;
            }

            public String getFrontShinyFemale() {
                return frontShinyFemale;
            }
        }

        public static class Artwork {
            @SerializedName("front_default")
            private String frontDefault;

            public String getFrontDefault() {
                return frontDefault;
            }
        }

        public String getFrontDefault() {
            return frontDefault;
        }

        public String getFrontFemale() {
            return frontFemale;
        }

        public String getFrontShiny() {
            return frontShiny;
        }

        public String getFrontShinyFemale() {
            return frontShinyFemale;
        }

        public String getBackDefault() {
            return backDefault;
        }

        public String getBackFemale() {
            return backFemale;
        }

        public String getBackShiny() {
            return backShiny;
        }

        public String getBackShinyFemale() {
            return backShinyFemale;
        }

        public Other getOther() {
            return other;
        }
    }

    public String getName() {
        return Formatter.formatPokemonName(name);
    }

    public int getPokedexId() {
        return pokedexId;
    }

    public Nameable getSpecies() {
        return species;
    }

    /**
     * Returns a list of types of this Pokémon. Note that a Pokémon can either have
     * one or two types.
     */
    public List<String> getTypes() {
        List<String> types = new ArrayList<>();
        for (PokemonType type : this.types) {
            String name = Formatter.formatPokemonName(type.type.getName());
            if (type.slot == 1) {
                types.add(0, name);
            } else {
                types.add(name);
            }
        }
        return types;
    }

    /**
     * The weight of this Pokémon in grams.
     */
    public int getWeight() {
        return weight * 100;
    }

    /**
     * The height of this Pokémon in centimetres.
     */
    public int getHeight() {
        return height * 10;
    }

    public Sprites getSprites() {
        return sprites;
    }

    /**
     * Returns a list of forms this Pokémon can take on.
     */
    public List<Nameable> getForms() {
        return forms;
    }

    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Returns the base experience gained for defeating this Pokémon.
     */
    public int getBaseExperience() {
        return baseExperience;
    }

    public List<Ability> getAbilities() {
        return abilities;
    }

    /**
     * Returns a list of abilities this Pokémon can have. Note that a Pokémon can
     * have at most three abilities: two normal and one hidden ability. If the
     * Pokémon has a hidden ability, it will always be in the last slot.
     */
    public List<String> getAbilitiesAsString() {
        return abilities.stream().map(Ability::getName).map(Formatter::formatPokemonName).toList();
    }

    /**
     * Returns a list of moves with learn methods and level details pertaining to specific version groups.
     */
    public List<Move> getMoves() {
        return moves;
    }

    /**
     * Returns a list of items this Pokémon may be holding when encountered.
     */
    public List<HeldItem> getHeldItems() {
        return heldItems;
    }

    /**
     * Returns a list of base stat values for this Pokémon.
     */
    public List<Stat> getStats() {
        return stats;
    }

    /**
     * Returns an {@link URL} link to the shiny sprite of the Pokémon.
     */
    public String getShiny() {
        return sprites.frontShiny;
    }

    /**
     * Returns an {@link URL} link to the given form of the Pokémon.
     */
    public String getPokemonForm(String formName) {
        for (Nameable form : forms) {
            if (form.getName().equals(formName)) {
                return form.getUrl();
            }
        }
        return null;
    }

    /**
     * Returns the {@link PokemonSpecies} of this Pokémon.
     */
    public PokemonSpecies getPokemonSpecies() throws NotFoundException, APIException {
        return PokeApiClient.getPokemonSpecies(species.getName());
    }

    @Override
    public int compareTo(@NotNull Pokemon o) {
        return Integer.compare(pokedexId, o.pokedexId);
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pokemon pokemon && pokemon.pokedexId == pokedexId;
    }
}
