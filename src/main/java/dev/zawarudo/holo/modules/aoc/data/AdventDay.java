package dev.zawarudo.holo.modules.aoc.data;

/**
 * Represents a single day of the Advent of Code.
 *
 * @param day         The day of the Advent of Code.
 * @param goldCount   The number of people who have completed both tasks of the day.
 * @param silverCount The number of people who have completed the first task of the day.
 * @param grayCount   The number of people who have not solved any tasks.
 */
public record AdventDay(int day, int goldCount, int silverCount, int grayCount) implements Comparable<AdventDay> {

    @Override
    public String toString() {
        return String.format("AdventDay{day=%d, goldCount=%d, silverCount=%d, grayCount=%d}",
            day, goldCount, silverCount, grayCount);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof AdventDay other && day == other.day;
    }

    @Override
    public int compareTo(AdventDay o) {
        return Integer.compare(day, o.day);
    }
}
