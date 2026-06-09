package dev.zawarudo.holo.modules.aoc.data;

import com.google.gson.JsonObject;
import dev.zawarudo.holo.utils.HoloHttp;
import dev.zawarudo.holo.utils.exceptions.APIException;
import dev.zawarudo.holo.utils.exceptions.HttpStatusException;
import dev.zawarudo.holo.utils.exceptions.HttpTransportException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public final class AdventOfCodeClient {

    private static final String BASE_URL = "https://adventofcode.com";

    private static int year;

    private AdventOfCodeClient() {
    }

    public static List<AdventDay> getAdventDays(int year, int leaderboardId, String sessionKey) throws APIException {
        AdventOfCodeClient.year = year;

        JsonObject members = fetchJson(year, leaderboardId, sessionKey).get("members").getAsJsonObject();
        List<JsonObject> completionRates = new ArrayList<>();

        for (String key : members.keySet()) {
            JsonObject completion = members.get(key).getAsJsonObject().get("completion_day_level").getAsJsonObject();
            completionRates.add(completion);
        }
        return parseData(completionRates);
    }

    private static List<AdventDay> parseData(List<JsonObject> dataList) {
        List<AdventDay> days = new LinkedList<>();
        int maxDay = getMaxDay(year);

        for (int dayIndex = 1; dayIndex <= maxDay; dayIndex++) {
            String day = String.valueOf(dayIndex);
            int goldCount = 0;
            int silverCount = 0;
            int grayCount = 0;

            for (JsonObject obj : dataList) {
                if (!obj.has(day)) {
                    grayCount++;
                } else if (obj.get(day).getAsJsonObject().size() == 1) {
                    silverCount++;
                } else {
                    goldCount++;
                }
            }
            days.add(new AdventDay(dayIndex, goldCount, silverCount, grayCount));
        }
        return days;
    }

    private static int getMaxDay(int year) {
        ZonedDateTime currentDateTime = ZonedDateTime.now(ZoneId.of("Europe/Zurich"));
        if (currentDateTime.getYear() == year && currentDateTime.getMonthValue() == 12) {
            return Math.min(25, currentDateTime.getDayOfMonth());
        }
        return 25;
    }

    private static JsonObject fetchJson(int year, int leaderboardId, String sessionKey) throws APIException {
        String url = String.format("%s/%d/leaderboard/private/view/%d.json", BASE_URL, year, leaderboardId);

        Map<String, String> headers = Map.of(
            "Cookie", "session=" + sessionKey,
            "Accept", "application/json"
        );

        try {
            return HoloHttp.getJsonObject(url, headers);
        } catch (HttpStatusException ex) {
            throw new APIException("AoC request failed (HTTP " + ex.getStatusCode() + "): " + ex.getMessage(), ex);
        } catch (HttpTransportException ex) {
            throw new APIException("I/O error while contacting the AOC website.", ex);
        }
    }
}
