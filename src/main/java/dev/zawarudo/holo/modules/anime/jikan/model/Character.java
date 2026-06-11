package dev.zawarudo.holo.modules.anime.jikan.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents a character from an anime or manga.
 */
public class Character implements Comparable<Character> {
    @SerializedName("mal_id")
    private int id;
    @SerializedName("url")
    private String url;
    @SerializedName("images")
    private Images images;
    @SerializedName("name")
    private String name;
    @SerializedName("name_kanji")
    private String nameKanji;
    @SerializedName("nicknames")
    private String[] nicknames;
    @SerializedName("favorites")
    private int favorites;
    @SerializedName("about")
    private String about;

    /**
     * Returns the MyAnimeList ID of the character.
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the URL of the character's page on MyAnimeList.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns images of the character.
     */
    public Images getImages() {
        return images;
    }

    /**
     * Returns the name of the character.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the kanji name of the character.
     */
    public String getNameKanji() {
        return nameKanji;
    }

    /**
     * Returns the nicknames of the character.
     */
    public String[] getNicknames() {
        return nicknames;
    }

    /**
     * Returns the number of users who have favorited the character.
     */
    public int getFavorites() {
        return favorites;
    }

    /**
     * Returns the biography of the character.
     */
    public String getAbout() {
        return about;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Character character && character.id == id;
    }

    @Override
    public int compareTo(Character o) {
        return Integer.compare(id, o.id);
    }
}
