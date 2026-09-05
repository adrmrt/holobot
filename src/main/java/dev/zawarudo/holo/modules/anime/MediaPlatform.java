package dev.zawarudo.holo.modules.anime;

public enum MediaPlatform {
    MAL_JIKAN("MyAnimeList", "https://myanimelist.net/", "https://upload.wikimedia.org/wikipedia/commons/7/7a/MyAnimeList_Logo.png"),
    MAL("MyAnimeList", "https://myanimelist.net/", "https://upload.wikimedia.org/wikipedia/commons/7/7a/MyAnimeList_Logo.png"),
    ANILIST("AniList", "https://anilist.co/", "https://anilist.co/img/icons/android-chrome-512x512.png");

    private final String name;
    private final String url;
    private final String iconUrl;

    MediaPlatform(String name, String url, String iconUrl) {
        this.name = name;
        this.url = url;
        this.iconUrl = iconUrl;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public String getIconUrl() {
        return iconUrl;
    }
}