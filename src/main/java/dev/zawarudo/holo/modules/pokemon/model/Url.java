package dev.zawarudo.holo.modules.pokemon.model;

import com.google.gson.annotations.SerializedName;

/**
 * A simple URL object
 */
public class Url {
    @SerializedName("url")
    String url;

    public String getUrl() {
        return url;
    }
}
