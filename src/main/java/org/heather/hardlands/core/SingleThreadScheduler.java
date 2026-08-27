package org.heather.hardlands.core;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.bukkit.plugin.Plugin;

public final class SingleThreadScheduler<P extends Plugin> implements AutoCloseable {

    private final P plugin;
    private final ScheduledThreadPoolExecutor executor;

    public SingleThreadScheduler(P plugin) {
        this.plugin = plugin;
        this.executor = new ScheduledThreadPoolExecutor(
                1,
                Thread.ofPlatform()
                        .name(plugin.getName() + "-Scheduler")
                        .factory()
        );

        this.executor.setRemoveOnCancelPolicy(true);
        this.executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        this.executor.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    public ScheduledFuture<?> loop(Consumer<P> task, Duration interval) {
        return this.executor.scheduleAtFixedRate(
                safe(() -> task.accept(this.plugin)),
                0L,
                positiveNanos(interval),
                TimeUnit.NANOSECONDS
        );
    }

    public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return this.executor.schedule(
                safe(task),
                nonNegativeNanos(delay),
                TimeUnit.NANOSECONDS
        );
    }

    private Runnable safe(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        return () -> {
            try {
                task.run();
            } catch (Exception exception) {
                this.plugin.getLogger().log(Level.SEVERE, "Scheduled task failed", exception);
            }
        };
    }

    private static long positiveNanos(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        return duration.toNanos();
    }

    private static long nonNegativeNanos(Duration duration) {
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("Duration cannot be negative");
        }

        return duration.toNanos();
    }
}