package dev.zawarudo.holo.core;

import java.util.concurrent.*;

public class BotExecutors implements AutoCloseable {

    private final ExecutorService cpu;
    private final ExecutorService io;
    private final ScheduledExecutorService scheduler;

    public BotExecutors() {
        int cores = Runtime.getRuntime().availableProcessors();

        // cpu = cores
        this.cpu = new ThreadPoolExecutor(
            cores, cores,
            0L, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(500),
            new NamedThreadFactory("holo-cpu"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // io = cores * 4
        this.io = new ThreadPoolExecutor(
            Math.max(4, cores * 4), Math.max(4, cores * 4),
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new NamedThreadFactory("holo-io"),
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        // scheduler = 1
        this.scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("holo-scheduler"));
    }

    public ExecutorService cpu() {
        return cpu;
    }

    public ExecutorService io() {
        return io;
    }

    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        io.shutdownNow();
        cpu.shutdownNow();
    }
}
