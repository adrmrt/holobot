package dev.zawarudo.holo.modules.anime.jikan.model;

/**
 * Represents a season where various new {@link Anime}s are released.
 */
public enum Season {
    SPRING("spring"),
    SUMMER("summer"),
    FALL("fall"),
    WINTER("winter");

    private final String seasonString;

    Season(String seasonString) {
        this.seasonString = seasonString;
    }

    public String getSeason() {
        return seasonString;
    }

    @Override
    public String toString() {
        return seasonString;
    }
}
