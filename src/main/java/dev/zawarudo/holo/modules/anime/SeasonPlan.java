package dev.zawarudo.holo.modules.anime;

import dev.zawarudo.holo.modules.anime.jikan.JikanApiClient;
import dev.zawarudo.holo.modules.anime.jikan.model.Anime;
import dev.zawarudo.holo.modules.anime.jikan.model.Season;
import dev.zawarudo.holo.utils.exceptions.APIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
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
    public static BufferedImage createWeekPlan(Season season, int year) throws APIException {
        List<Anime> seasonalAnime = JikanApiClient.getSeason(season, year);

        for (Anime anime : seasonalAnime) {
            anime.changeBroadcastTimeZone("Europe/Zurich");

            LOGGER.info(anime.getTitle());

            LOGGER.info("{} {}", anime.getBroadcast().getDay(), anime.getBroadcast().getTime());
        }

        throw new UnsupportedOperationException("Not yet implemented!");
    }

    /**
     * Creates an image displaying the start dates of the seasonal anime.
     */
    public static BufferedImage createStartPlan(Season season, int year) throws APIException {
        throw new UnsupportedOperationException("Not yet implemented!");
    }

    public static void main(String[] args) throws APIException {
        createWeekPlan(Season.FALL, 2024);
    }
}
