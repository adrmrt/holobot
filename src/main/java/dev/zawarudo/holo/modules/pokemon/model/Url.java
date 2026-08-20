package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;

/**
 * A simple URL object
 */
public class Url {
    @SerializedName("url")
    String urlString;

    public String getUrl() {
        return urlString;
    }
}
