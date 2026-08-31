package team.heather.hardlands.game;

import java.time.LocalTime;
import java.util.function.BooleanSupplier;

import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.MinuteOptionDef;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.game.phase.Phase;

@ConfigBuilder(
        identifier = "game",
        options = {
                @OptionDef(name = "startTime", type = LocalTime.class)
        },
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

    private final Hardlands plugin;
    private final GameTimer timer;
    private final GameLoopTask loopTask;

    private Phase phase = Phase.OFF_GAME;
    private boolean initialized;

    public GameManager(Hardlands plugin) {
        this.plugin = plugin;
        this.timer = new GameTimer(this);
        this.loopTask = new GameLoopTask(plugin, this.timer);
    }

    public void initialize() {
        if (this.initialized) {
            throw new IllegalStateException("Game is already initialized");
        }

        this.timer.updateState();
        this.loopTask.start();

        this.initialized = true;
    }

    public void shutdown() {
        this.loopTask.close();
        this.initialized = false;
    }

    public void changePhase(Phase newPhase) {
        if (newPhase == null) {
            throw new IllegalArgumentException("Phase cannot be null");
        }

        Phase previousPhase = this.phase;

        previousPhase.onStop(this.plugin);

        this.phase = newPhase;
        this.timer.updateState();

        newPhase.onStart(this.plugin);
    }

    public void completeCurrentPhase() {
        this.timer.completeCurrentPhase();
    }

    public void refreshStartTime() {
        this.timer.refreshStartTime();
    }

    public void setChronometer(int seconds) {
        this.timer.setChronometer(seconds);
    }

    public void resetChronometer() {
        this.timer.resetChronometer();
    }

    public void setTimerProgressCondition(BooleanSupplier condition) {
        this.timer.setProgressCondition(condition);
    }

    public void resetTimerProgressCondition() {
        this.timer.resetProgressCondition();
    }

    public void updatePregenerationProgress(float progress) {
        this.timer.updatePregenerationProgress(progress);
    }

    public void updateScatterProgress(float progress) {
        this.timer.updateScatterProgress(progress);
    }

    public void addViewer(Player player) {
        this.timer.addViewer(player);
    }

    public void removeViewer(Player player) {
        this.timer.removeViewer(player);
    }

    public Phase getPhase() {
        return this.phase;
    }

    public GameTimer getTimerManager() {
        return this.timer;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) return false;

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
}