package dev.zawarudo.holo.modules.anime;

import dev.zawarudo.holo.modules.anime.jikan.JikanApiClient;
import dev.zawarudo.holo.modules.anime.jikan.model.AbstractMedium;
import dev.zawarudo.holo.modules.anime.jikan.model.Anime;
import dev.zawarudo.holo.modules.anime.jikan.model.Broadcast;
import dev.zawarudo.holo.modules.anime.jikan.model.Season;
import dev.zawarudo.holo.utils.DateTimeUtils;
import dev.zawarudo.holo.utils.exceptions.APIException;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class AnimeSeason {

    private static final String TARGET_TIME_ZONE = "Europe/Zurich";

    public static void main(String[] args) throws APIException {
        List<Anime> seasonalAnime = JikanApiClient.getSeason(Season.FALL, 2024);

        // Sort by popularity
        seasonalAnime.sort(Comparator.comparingInt(AbstractMedium::getPopularity));

        Map<String, List<Anime>> map = seasonalAnime.stream()
            .filter(anime -> {
                Broadcast b = anime.getBroadcast();
                return b.getString().isPresent() &&
                    b.getDay().isPresent() &&
                    b.getTime().isPresent() &&
                    b.getTimeZone().isPresent();
            })
            .collect(Collectors.groupingBy(
                anime -> getFormattedDateString(
                    DateTimeUtils.convertDate(anime.getStartDate(), TARGET_TIME_ZONE)
                ),
                TreeMap::new,
                Collectors.toList()
            ));

        System.out.println("Anime Releases Fall 2024");
        System.out.println(map.size() + " days");

        for (String key : map.keySet()) {
            System.out.println(key);
            map.get(key).stream()
                .peek(anime -> anime.changeBroadcastTimeZone(TARGET_TIME_ZONE))
                .forEach(System.out::println);
            System.out.println();
        }
    }

    // TODO: Pick cover of most popular anime for each day

    private static String getFormattedDateString(String date) {
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(date);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
        return zonedDateTime.format(formatter);
    }
}
