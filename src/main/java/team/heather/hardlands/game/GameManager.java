package team.heather.hardlands.game;

import java.time.LocalTime;

import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.MinuteOptionDef;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.game.timeline.GameTimeline;

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
    private final GameTimeline timer;
    private final GameLoopTask loopTask;

    private Phase phase = Phase.OFF_GAME;
    private boolean initialized;

    public GameManager(Hardlands plugin) {
        this.plugin = plugin;
        this.timer = new GameTimeline(this);
        this.loopTask = new GameLoopTask(plugin, this.timer);
    }

    // Lifecycle

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

    // Phase

    public void changePhase(Phase phase) {
        if (phase == null) {
            throw new IllegalArgumentException("Phase cannot be null");
        }

        this.phase.onStop(this.plugin);
        this.phase = phase;

        this.timer.updateState();
        this.phase.onStart(this.plugin);
    }

    public void completeCurrentPhase() {
        this.timer.completeCurrentPhase();
    }

    // Timer

    public void refreshStartTime() {
        this.timer.refreshStartTime();
    }

    public void setChronometer(int seconds) {
        this.timer.setCounter(seconds);
    }

    public void resetChronometer() {
        this.timer.resetChronometer();
    }

    public void updatePreparationProgress(float progress) {
        this.timer.updatePreparationProgress(progress);
    }

    public void updateScatterProgress(float progress) {
        this.timer.updateScatterProgress(progress);
    }

    // Boss bar

    public void addViewer(Player player) {
        this.timer.addViewer(player);
    }

    public void removeViewer(Player player) {
        this.timer.removeViewer(player);
    }

    // Properties

    public Phase getPhase() {
        return this.phase;
    }

    public GameTimeline getTimer() {
        return this.timer;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) {
            return false;
        }

        int gracePeriod = this.getGracePeriodMinuteOption().getValue();
        int pvp = this.getPvpMinuteOption().getValue();
        int borderShrink = this.getBorderShrinkMinuteOption().getValue();
        int meetup = this.getMeetupMinuteOption().getValue();
        int finalShrink = this.getFinalShrinkMinuteOption().getValue();
        int deathmatch = this.getDeathmatchMinuteOption().getValue();

        return gracePeriod < pvp
                && pvp < borderShrink
                && borderShrink < meetup
                && meetup < finalShrink
                && finalShrink < deathmatch;
    }
}