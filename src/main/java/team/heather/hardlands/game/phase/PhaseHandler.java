package team.heather.hardlands.game.phase;

public interface PhaseHandler {

    void onStart(Phase phase);

    void onStop(Phase phase);
}