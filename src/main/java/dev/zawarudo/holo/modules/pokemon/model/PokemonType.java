package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;
import dev.zawarudo.holo.utils.Formatter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokemonType {

    @SerializedName("id")
    private int id;
    @SerializedName("name")
    private String name;
    @SerializedName("names")
    private List<LangNameable> names;
    @SerializedName("moves")
    private List<LangNameable> moves;
    @SerializedName("pokemon")
    private List<Pokemon> pokemon;
    @SerializedName("damage_relations")
    private DamageRelations damageRelations;

    private static class DamageRelations {
        @SerializedName("no_damage_to")
        private List<Nameable> noDamageTo;
        @SerializedName("half_damage_to")
        private List<Nameable> halfDamageTo;
        @SerializedName("double_damage_to")
        private List<Nameable> doubleDamageTo;
        @SerializedName("no_damage_from")
        private List<Nameable> noDamageFrom;
        @SerializedName("half_damage_from")
        private List<Nameable> halfDamageFrom;
        @SerializedName("double_damage_from")
        private List<Nameable> doubleDamageFrom;
    }

    private static class Pokemon {
        @SerializedName("slot")
        private int slot;
        @SerializedName("pokemon")
        private Nameable pokemon;
    }

    /**
     * Returns the id of this type.
     *
     * @return The id of this type as an integer.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the name of this type.
     *
     * @return The name of this type as a String.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the formatted name of this type.
     *
     * @return The formatted name of this type as a String.
     */
    public String getNameFormatted() {
        return Formatter.capitalize(name);
    }

    /**
     * Returns the name of this type in the given language.
     *
     * @param language The language to get the name in.
     * @return The name of this type in the given language.
     */
    public String getName(@NotNull String language) {
        if (names.stream().noneMatch(n -> n.getLanguage().getName().equals(language))) {
            throw new IllegalArgumentException("The given language is invalid: " + language);
        }
        return names.stream()
            .filter(n -> n.getLanguage().getName().equals(language))
            .toList()
            .get(0).getName();
    }

    public List<String> getMoves() {
        List<String> moves = new ArrayList<>(this.moves.stream().map(LangNameable::getName).toList());
        String specialMove = moves.stream().filter(m -> m.contains("--")).findFirst().orElse(null);
        if (specialMove != null) {
            specialMove = Formatter.removeStartingChar(specialMove, "--");
            moves.remove(specialMove + "--special");
            moves.remove(specialMove + "--physical");
            moves.add(specialMove);
        }
        Collections.sort(moves);
        return moves;
    }

    public List<String> getMovesFormatted() {
        return getMoves().stream().map(Formatter::formatPokemonName).toList();
    }

    public List<String> getNoDamageTo() {
        return damageRelations.noDamageTo.stream()
            .map(Nameable::getName)
            .toList();
    }

    public List<String> getHalfDamageTo() {
        return damageRelations.halfDamageTo.stream()
            .map(Nameable::getName)
            .toList();
    }

    public List<String> getDoubleDamageTo() {
        return damageRelations.doubleDamageTo.stream()
            .map(Nameable::getName)
            .toList();
    }

    public List<String> getNoDamageFrom() {
        return damageRelations.noDamageFrom.stream()
            .map(Nameable::getName)
            .toList();
    }

    public List<String> getHalfDamageFrom() {
        return damageRelations.halfDamageFrom.stream()
            .map(Nameable::getName)
            .toList();
    }

    public List<String> getDoubleDamageFrom() {
        return damageRelations.doubleDamageFrom.stream()
            .map(Nameable::getName)
            .toList();
    }
}
