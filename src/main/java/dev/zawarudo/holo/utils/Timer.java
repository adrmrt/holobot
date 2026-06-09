package dev.zawarudo.holo.utils;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for high-precision timing and measurement of code sections.
 */
public class Timer {
    private long startTime = 0;
    private long endTime = 0;
    private boolean running = false;

    /**
     * Starts the timer.
     */
    public void start() {
        this.startTime = System.nanoTime();
        this.running = true;
    }

    /**
     * Stops the timer.
     */
    public void stop() {
        this.endTime = System.nanoTime();
        this.running = false;
    }

    /**
     * Returns the elapsed time in the specified time unit. If the timer hasn't been stopped,
     * then it used the elapsed time so far.
     *
     * @param unit The time unit to get the elapsed time in.
     * @return The elapsed time in the given time unit.
     */
    public long getElapsedTime(TimeUnit unit) {
        long elapsedNanos = running ? System.nanoTime() - startTime : endTime - startTime;
        return unit.convert(elapsedNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Resets the timer.
     */
    public void reset() {
        startTime = 0;
        endTime = 0;
        running = false;
    }
}
