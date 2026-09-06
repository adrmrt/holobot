package dev.zawarudo.holo.modules.anime;

import com.google.gson.JsonArray;
import dev.zawarudo.holo.modules.anime.anilist.AniListApiClient;
import dev.zawarudo.holo.modules.anime.anilist.AniListMappers;
import dev.zawarudo.holo.modules.anime.anilist.AniListSeason;
import dev.zawarudo.holo.utils.exceptions.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 */
public final class SeasonPlan {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeasonPlan.class);

    private SeasonPlan() {
        throw new UnsupportedOperationException("Not implemented!");
    }

    /**
     * Creates an image displaying the anime that air on the respective week day.
     */
    public static BufferedImage createWeekPlan(AniListSeason season, int year) throws APIException {
        List<SeasonalAnime> seasonalAnime = fetchSeason(season, year);

        for (SeasonalAnime anime : seasonalAnime) {
            if (anime.nextAiringAt() == null) continue;

            ZonedDateTime local = anime.nextAiringAt().withZoneSameInstant(ZoneId.of("Europe/Zurich"));
            LOGGER.info(anime.title());
            LOGGER.info("{} {}", local.getDayOfWeek(), local.toLocalTime());
        }

        throw new UnsupportedOperationException("Not yet implemented!");
    }

    /**
     * Creates an image displaying the start dates of the seasonal anime.
     */
    public static BufferedImage createStartPlan(AniListSeason season, int year) throws APIException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    private static List<SeasonalAnime> fetchSeason(AniListSeason season, int year) throws APIException {
        JsonArray media = new AniListApiClient().searchSeasonRaw(season, year, 50)
            .getAsJsonObject("Page").getAsJsonArray("media");
        return AniListMappers.toSeasonalAnime(media);
    }

    public static void main(String[] args) throws APIException {
        createWeekPlan(AniListSeason.FALL, 2024);
    }
}
