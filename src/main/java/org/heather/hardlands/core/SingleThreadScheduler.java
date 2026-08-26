package org.heather.hardlands.core;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;

/**
 * Executes delayed and repeating tasks on a dedicated single thread.
 */
public final class SingleThreadScheduler implements AutoCloseable {

    private final Logger logger;
    private final ScheduledThreadPoolExecutor executor;

    public SingleThreadScheduler(Plugin plugin) {
        this.logger = plugin.getLogger();
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

    /**
     * Stops the scheduler and cancels pending tasks.
     */
    @Override
    public void close() {
        this.executor.shutdownNow();
    }

    // Public API

    /**
     * Repeats a task at the given interval.
     */
    public ScheduledFuture<?> loop(Runnable task, Duration interval) {
        return this.executor.scheduleAtFixedRate(
                safe(task),
                0L,
                positiveNanos(interval),
                TimeUnit.NANOSECONDS
        );
    }

    /**
     * Executes a task after the given delay.
     */
    public ScheduledFuture<?> schedule(Runnable task, Duration delay) {
        return this.executor.schedule(
                safe(task),
                nonNegativeNanos(delay),
                TimeUnit.NANOSECONDS
        );
    }

    // Internal Utilities

    private Runnable safe(Runnable task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null");
        }

        return () -> {
            try {
                task.run();
            } catch (Exception exception) {
                this.logger.log(
                        Level.SEVERE,
                        "Scheduled task failed",
                        exception
                );
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