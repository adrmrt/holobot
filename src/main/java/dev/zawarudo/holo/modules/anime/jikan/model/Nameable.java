package dev.zawarudo.holo.modules.anime.jikan.model;

import com.google.gson.annotations.SerializedName;

/**
 * A MyAnimeList object, for example an author or a studio.
 */
public class Nameable implements Comparable<Nameable> {
    @SerializedName("mal_id")
    private int id;
    @SerializedName("type")
    private String type;
    @SerializedName("name")
    private String name;
    @SerializedName("url")
    private String url;

    /**
     * Returns the id of this object as per MyAnimeList
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the type of this object
     */
    public String getType() {
        return type;
    }

    /**
     * Returns the name of this object
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the URL of this object on MyAnimeList
     */
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Nameable && ((Nameable) obj).getId() == id;
    }

    @Override
    public int compareTo(Nameable o) {
        return Integer.compare(id, o.id);
    }
}
