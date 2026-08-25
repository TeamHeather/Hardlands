package org.heather.hardlands.util;

import java.time.Duration;
import java.util.concurrent.*;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class ThreadScheduler {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Plugin plugin;

    public ThreadScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public void terminate() {
        this.executor.shutdownNow();
    }

    public void loop(Runnable task, Duration period) {
        this.executor.scheduleAtFixedRate(
                task,
                0L,
                period.toNanos(),
                TimeUnit.NANOSECONDS);
    }

    public void loopOnMainThread(Runnable task, Duration period) {
        this.loop(() -> Bukkit.getScheduler().runTask(this.plugin, task), period);
    }

    public void schedule(Runnable task, Duration delay) {
        this.executor.schedule(
                task,
                delay.toNanos(),
                TimeUnit.NANOSECONDS);
    }

    public void scheduleOnMainThread(Runnable task, Duration delay) {
        this.schedule(() -> Bukkit.getScheduler().runTask(this.plugin, task), delay);
    }
}