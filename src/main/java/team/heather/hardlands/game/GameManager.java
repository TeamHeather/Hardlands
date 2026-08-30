package team.heather.hardlands.game;

import java.time.Duration;

import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.MinuteOptionDef;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.phase.Phase;

@ConfigBuilder(
        identifier = "game",
        minuteOptions = {
                @MinuteOptionDef(name = "gracePeriodMinute"),
                @MinuteOptionDef(name = "pvpMinute"),
                @MinuteOptionDef(name = "borderShrinkMinute"),
                @MinuteOptionDef(name = "meetupMinute"),
                @MinuteOptionDef(name = "finalShrinkMinute"),
                @MinuteOptionDef(name = "deathmatchMinute")
        }
)
public final class GameManager extends GameManagerConfiguration {

    private final GameTimer gameTimer = new GameTimer(this);
    private final Hardlands plugin;

    private Phase phase = Phase.IDLE;
    private boolean initialized;

    public GameManager(Hardlands plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (this.initialized) {
            throw new IllegalStateException("Game is already initialized");
        }

        this.gameTimer.updateState();

        this.plugin.getSingleThreadScheduler().loop(
                _ -> this.gameTimer.updateProgress(this.plugin),
                Duration.ofSeconds(1)
        );

        this.initialized = true;
    }

    public synchronized void changePhase(Phase newPhase) {
        Phase previousPhase = this.phase;

        previousPhase.onStop();

        this.phase = newPhase;
        this.gameTimer.updateState();

        newPhase.onStart();
    }

    public Phase getPhase() {
        return this.phase;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) {
            return false;
        }

        int gracePeriod = super.gracePeriodMinute.getValue();
        int pvp = super.pvpMinute.getValue();
        int borderShrink = super.borderShrinkMinute.getValue();
        int meetup = super.meetupMinute.getValue();
        int finalShrink = super.finalShrinkMinute.getValue();
        int deathmatch = super.deathmatchMinute.getValue();

        return gracePeriod < pvp
                && pvp < borderShrink
                && borderShrink < meetup
                && meetup < finalShrink
                && finalShrink < deathmatch;
    }

    private void schedulePhase(Phase phase) {
        Option<Integer> minuteOption = phase.getMinuteOption(this);
        if (minuteOption == null) return;

        this.plugin.getSingleThreadScheduler().schedule(
                () -> this.changePhase(phase),
                Duration.ofMinutes(minuteOption.getValue())
        );
    }
}