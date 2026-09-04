package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.annotations.SerializedName;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Wire-format DTOs for the official MAL API v2, scoped to the anime/manga search fields
 * actually used by {@code MalMappers}. Gson-deserialized directly, no manual JSON navigation.
 * Package-private: only {@code MalApiClient} and {@code MalMappers} need to see the wire shape.
 */
final class MalDtos {

    private MalDtos() {
    }

    /** Envelope MAL wraps every list-search response in, regardless of media type. */
    record SearchResult<T>(List<Node<T>> data) {
    }

    record Node<T>(T node) {
    }

    record Picture(String medium, String large) {
    }

    record AlternativeTitles(List<String> synonyms, String en, String ja) {
    }

    record Season(int year, String season) {
    }

    record Genre(int id, String name) {
    }

    record Studio(int id, String name) {
    }

    record AuthorNode(
        int id,
        @SerializedName("first_name") String firstName,
        @SerializedName("last_name") String lastName
    ) {
    }

    record AuthorEntry(AuthorNode node, String role) {
    }

    /**
     * Marks a record component whose value MAL only returns when explicitly requested with a
     * nested field selector, e.g. {@code authors{first_name,last_name}}. Read by
     * {@link MalApiClient} to build the {@code fields=} query parameter from the DTO shape
     * itself, so the request always matches what the record actually declares.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.RECORD_COMPONENT)
    @interface NestedFields {
        String value();
    }

    record Anime(
        int id,
        String title,
        @SerializedName("main_picture") Picture mainPicture,
        @SerializedName("alternative_titles") AlternativeTitles alternativeTitles,
        String synopsis,
        Double mean,
        Integer rank,
        @SerializedName("media_type") String mediaType,
        String status,
        @SerializedName("num_episodes") int numEpisodes,
        @SerializedName("start_season") Season startSeason,
        List<Genre> genres,
        List<Studio> studios,
        String source
    ) {
    }

    record Manga(
        int id,
        String title,
        @SerializedName("main_picture") Picture mainPicture,
        @SerializedName("alternative_titles") AlternativeTitles alternativeTitles,
        String synopsis,
        Double mean,
        Integer rank,
        @SerializedName("media_type") String mediaType,
        String status,
        @SerializedName("num_chapters") int numChapters,
        @SerializedName("num_volumes") int numVolumes,
        List<Genre> genres,
        @NestedFields("{first_name,last_name}") List<AuthorEntry> authors
    ) {
    }
}
