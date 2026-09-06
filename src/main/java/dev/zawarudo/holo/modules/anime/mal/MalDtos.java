package dev.zawarudo.holo.modules.anime.mal;

import com.google.gson.annotations.SerializedName;

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

    /** Implemented by enums mirroring a documented MAL string-enum field (status, media_type). */
    interface ApiLabeled {
        String apiValue();

        String label();
    }

    /**
     * Looks up the enum constant matching a MAL wire value, or null if unrecognized. Kept
     * lenient (never throws) since these enums back a display-formatting concern, not
     * deserialization - a value MAL adds later shouldn't break the whole search.
     */
    static <T extends Enum<T> & ApiLabeled> T lookup(Class<T> type, String apiValue) {
        if (apiValue == null) return null;
        for (T constant : type.getEnumConstants()) {
            if (constant.apiValue().equals(apiValue)) return constant;
        }
        return null;
    }

    /** Documented values for anime {@code status} (MAL API v2 reference). */
    enum AnimeStatus implements ApiLabeled {
        FINISHED_AIRING("finished_airing", "Finished Airing"),
        CURRENTLY_AIRING("currently_airing", "Currently Airing"),
        NOT_YET_AIRED("not_yet_aired", "Not Yet Aired");

        private final String apiValue;
        private final String label;

        AnimeStatus(String apiValue, String label) {
            this.apiValue = apiValue;
            this.label = label;
        }

        @Override
        public String apiValue() {
            return apiValue;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** Documented values for manga {@code status} (MAL API v2 reference). */
    enum MangaStatus implements ApiLabeled {
        FINISHED("finished", "Finished"),
        CURRENTLY_PUBLISHING("currently_publishing", "Currently Publishing"),
        NOT_YET_PUBLISHED("not_yet_published", "Not Yet Published");

        private final String apiValue;
        private final String label;

        MangaStatus(String apiValue, String label) {
            this.apiValue = apiValue;
            this.label = label;
        }

        @Override
        public String apiValue() {
            return apiValue;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** Documented values for anime {@code media_type} (MAL API v2 reference). */
    enum AnimeMediaType implements ApiLabeled {
        UNKNOWN("unknown", "Unknown"),
        TV("tv", "TV"),
        OVA("ova", "OVA"),
        MOVIE("movie", "Movie"),
        SPECIAL("special", "Special"),
        ONA("ona", "ONA"),
        MUSIC("music", "Music");

        private final String apiValue;
        private final String label;

        AnimeMediaType(String apiValue, String label) {
            this.apiValue = apiValue;
            this.label = label;
        }

        @Override
        public String apiValue() {
            return apiValue;
        }

        @Override
        public String label() {
            return label;
        }
    }

    /** Documented values for manga {@code media_type} (MAL API v2 reference). */
    enum MangaMediaType implements ApiLabeled {
        UNKNOWN("unknown", "Unknown"),
        MANGA("manga", "Manga"),
        NOVEL("novel", "Novel"),
        ONE_SHOT("one_shot", "One-shot"),
        DOUJINSHI("doujinshi", "Doujinshi"),
        MANHWA("manhwa", "Manhwa"),
        MANHUA("manhua", "Manhua"),
        OEL("oel", "OEL");

        private final String apiValue;
        private final String label;

        MangaMediaType(String apiValue, String label) {
            this.apiValue = apiValue;
            this.label = label;
        }

        @Override
        public String apiValue() {
            return apiValue;
        }

        @Override
        public String label() {
            return label;
        }
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
        List<AuthorEntry> authors
    ) {
    }
}
