package dev.zawarudo.holo.modules.anime.jikan.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Class that contains all the different types of relations of a referenced media and their entries.
 */
public final class Related {

    @SerializedName("relation")
    private String relation;
    @SerializedName("entry")
    private List<Nameable> entries;

    /**
     * Returns the type of relation between this object and the entries
     */
    public String getRelation() {
        return relation;
    }

    /**
     * Returns all the entries of this type of relation
     */
    public List<Nameable> getEntries() {
        return entries;
    }
}
