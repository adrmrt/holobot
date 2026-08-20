package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;

/**
 * An object with a name and an url
 */
public class Nameable {
    @SerializedName("name")
    String name;
    @SerializedName("url")
    String url;

    /**
     * Returns the name of this object.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the url of this object.
     */
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }
}
