package dev.zawarudo.holo.modules.anime;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

public class AnimeWeek {

    public static void sortAnimeByRelease(List<SeasonalAnime> anime) {
        anime.sort(
            Comparator.comparing(SeasonalAnime::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(a -> a.nextAiringAt() != null ? a.nextAiringAt().toLocalTime() : LocalTime.MIDNIGHT)
        );
    }
}
