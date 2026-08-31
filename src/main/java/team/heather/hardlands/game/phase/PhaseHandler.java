package team.heather.hardlands.game.phase;

import team.heather.hardlands.Hardlands;

public interface PhaseHandler {

    default void onStart(Hardlands plugin, Phase phase) {}

    default void onStop(Hardlands plugin, Phase phase) {}
}