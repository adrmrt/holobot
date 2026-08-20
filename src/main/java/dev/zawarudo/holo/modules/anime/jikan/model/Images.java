package dev.zawarudo.holo.modules.anime.jikan.model;

import com.google.gson.annotations.SerializedName;

/**
 * Represents images related to an entry.
 */
public class Images {
    @SerializedName("jpg")
    private Jpg jpg;
    @SerializedName("webp")
    private Webp webp;

    public static class Jpg {
        @SerializedName("image_url")
        private String imageUrl;
        @SerializedName("small_image_url")
        private String smallImageUrl;
        @SerializedName("large_image_url")
        private String largeImageUrl;

        public String getImage() {
            return imageUrl;
        }

        public String getSmallImage() {
            return smallImageUrl;
        }

        public String getLargeImage() {
            return largeImageUrl;
        }
    }

    public static class Webp {
        @SerializedName("image_url")
        private String imageUrl;
        @SerializedName("small_image_url")
        private String smallImageUrl;
        @SerializedName("large_image_url")
        private String largeImageUrl;

        public String getImage() {
            return imageUrl;
        }

        public String getSmallImage() {
            return smallImageUrl;
        }

        public String getLargeImage() {
            return largeImageUrl;
        }
    }

    public Jpg getJpg() {
        return jpg;
    }

    public Webp getWebp() {
        return webp;
    }
}
