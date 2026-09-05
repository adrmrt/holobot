package dev.zawarudo.holo.modules.anime;

import com.google.gson.JsonArray;
import dev.zawarudo.holo.modules.anime.anilist.AniListApiClient;
import dev.zawarudo.holo.modules.anime.anilist.AniListMappers;
import dev.zawarudo.holo.modules.anime.anilist.AniListSeason;
import dev.zawarudo.holo.utils.exceptions.APIException;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AnimeSeason {

    public static void main(String[] args) throws APIException {
        JsonArray media = new AniListApiClient().searchSeasonRaw(AniListSeason.FALL, 2024, 50)
            .getAsJsonObject("Page").getAsJsonArray("media");
        List<SeasonalAnime> seasonalAnime = new ArrayList<>(AniListMappers.toSeasonalAnime(media));

        // Sort by popularity
        seasonalAnime.sort(Comparator.comparingInt(SeasonalAnime::popularity));

        Map<String, List<SeasonalAnime>> map = seasonalAnime.stream()
            .filter(anime -> anime.startDate() != null)
            .collect(Collectors.groupingBy(
                anime -> getFormattedDateString(anime.startDate()),
                TreeMap::new,
                Collectors.toList()
            ));

        System.out.println("Anime Releases Fall 2024");
        System.out.println(map.size() + " days");

        for (String key : map.keySet()) {
            System.out.println(key);
            map.get(key).forEach(System.out::println);
            System.out.println();
        }
    }

    // TODO: Pick cover of most popular anime for each day

    private static String getFormattedDateString(java.time.LocalDate date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
        return date.format(formatter);
    }
}
