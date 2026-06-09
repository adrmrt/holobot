package dev.zawarudo.holo.modules.anime.jikan.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Represents a manga
 */
public class Manga extends AbstractMedium<Manga> {
    @SerializedName("chapters")
    private int chapters;
    @SerializedName("volumes")
    private int volumes;
    @SerializedName("publishing")
    private boolean publishing;
    @SerializedName("authors")
    private List<Nameable> authors;
    @SerializedName("serializations")
    private List<Nameable> serializations;

    /**
     * Returns the number of chapters this manga has. Note that this method returns 0 if
     * the manga has either no chapters or the chapter count is unknown.
     *
     * @return The number of chapters.
     */
    public int getChapters() {
        return chapters;
    }

    /**
     * Returns the number of volumes this manga has. Note that this method returns 0 if
     * the manga has either no volumes or the volume count is unknown.
     *
     * @return The number of volumes.
     */
    public int getVolumes() {
        return volumes;
    }

    /**
     * Returns whether the manga is currently being published.
     *
     * @return True if the manga is being published, false if the manga is finished or
     * hasn't been released yet.
     */
    public boolean isPublishing() {
        return publishing;
    }

    /**
     * Returns a list of people that are involved in this manga.
     *
     * @return A list of people.
     */
    public List<Nameable> getAuthors() {
        return authors;
    }

    /**
     * Returns a list of serializations.
     */
    public List<Nameable> getSerializations() {
        return serializations;
    }
}
