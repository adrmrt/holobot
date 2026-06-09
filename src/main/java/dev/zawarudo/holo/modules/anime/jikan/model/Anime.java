package dev.zawarudo.holo.modules.anime.jikan.model;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Represents an anime.
 */
public final class Anime extends AbstractMedium<Anime> {

    @SerializedName("source")
    private String source;
    @SerializedName("episodes")
    private int episodes;
    @SerializedName("airing")
    private boolean airing;
    @SerializedName("duration")
    private String duration;
    @SerializedName("rating")
    private String rating;
    @SerializedName("season")
    private String season;
    @SerializedName("year")
    private int year;
    @SerializedName("broadcast")
    private Broadcast broadcast;
    @SerializedName("producers")
    private List<Nameable> producers;
    @SerializedName("licensors")
    private List<Nameable> licensors;
    @SerializedName("studios")
    private List<Nameable> studios;

    public String getSource() {
        return source;
    }

    public int getEpisodes() {
        return episodes;
    }

    /**
     * Checks whether this anime is currently airing.
     */
    public boolean isAiring() {
        return airing;
    }

    public String getRating() {
        return rating;
    }

    public String getDuration() {
        return duration;
    }

    public String getSeason() {
        if (season != null) {
            return season + " " + year;
        }
        return null;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    /**
     * Changes the time zone of the broadcast information to the specified one.
     *
     * @param timeZone The time zone to show broadcast information in.
     */
    public void changeBroadcastTimeZone(String timeZone) {
        broadcast = broadcast.convertToTimeZone(timeZone);
    }

    public List<Nameable> getProducers() {
        return producers;
    }

    public List<Nameable> getLicensors() {
        return licensors;
    }

    public List<Nameable> getStudios() {
        return studios;
    }

    /**
     * Returns the start date of the anime in ISO-8601 format. The timezone is JST (Asia/Tokyo).
     * <p>
     * Make sure to call this method before Broadcast#changeBroadcastTimeZone!
     */
    public String getStartDate() {
        String time = broadcast.getTime().isPresent() ? broadcast.getTime().get() : "00:00";
        String timeZone = "Asia/Tokyo";

        ZonedDateTime zonedDateTime = ZonedDateTime.parse(released.from);
        LocalDateTime newLocalDateTime = LocalDateTime.of(
            zonedDateTime.toLocalDate(), // Keep the original date
            LocalDateTime.parse("1970-01-01T" + time + ":00").toLocalTime()
        );
        ZonedDateTime newZonedDateTime = newLocalDateTime.atZone(ZoneId.of(timeZone));
        return newZonedDateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
