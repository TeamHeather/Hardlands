package org.heather.hardlands.task;

import java.time.Duration;

import org.heather.hardlands.Hardlands;

public final class LoopProcessor {

    private static final Duration MAIN_INTERVAL = Duration.ofSeconds(1);

    private LoopProcessor() {}

    public static void initialize(SingleThreadScheduler<Hardlands> scheduler) {
        scheduler.loop(LoopProcessor::mainLoop, MAIN_INTERVAL);
    }

    private static void mainLoop(Hardlands plugin) {
        plugin.getGameFlow().getGameBossBar().progress();
    }
}