package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;

/**
 * A {@link Nameable} object without url and that is dependent on the language.
 */
public class LangNameable {
    @SerializedName(value = "name", alternate = {"genus", "description"})
    String name;
    @SerializedName("language")
    Nameable language;

    /**
     * Returns the name of this object.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the language of this object.
     */
    public Nameable getLanguage() {
        return language;
    }

    @Override
    public String toString() {
        return name;
    }
}
