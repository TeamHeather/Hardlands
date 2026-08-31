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
                @MinuteOptionDef(name = "pvpMinute"),
                @MinuteOptionDef(name = "borderShrinkMinute"),
                @MinuteOptionDef(name = "meetupMinute"),
                @MinuteOptionDef(name = "finalShrinkMinute"),
                @MinuteOptionDef(name = "deathmatchMinute")
        }
)
public final class GameManager extends GameManagerConfiguration {

    private final Hardlands plugin;
    private final GameTimeline timeline;
    private final GameLoopTask loopTask;

    private Phase phase = Phase.OFF_GAME;
    private boolean running;

    public GameManager(Hardlands plugin) {
        this.plugin = plugin;
        this.timeline = new GameTimeline(this);
        this.loopTask = new GameLoopTask(plugin, this.timeline);
    }

    public void start() {
        if (this.running) {
            throw new IllegalStateException("Game manager is already running");
        }

        this.timeline.syncPhase();
        this.loopTask.start();

        this.running = true;
    }

    public void stop() {
        if (!this.running) {
            return;
        }

        this.loopTask.close();
        this.running = false;
    }

    public void transitionTo(Phase nextPhase) {
        if (nextPhase == null) {
            throw new IllegalArgumentException("Phase cannot be null");
        }

        if (this.phase == nextPhase) {
            return;
        }

        this.phase.onStop(this.plugin);
        this.phase = nextPhase;

        this.timeline.syncPhase();
        this.phase.onStart(this.plugin);
    }

    public void completePhase() {
        this.timeline.completeCurrentPhase();
    }

    public void refreshTimelineStartTime() {
        this.timeline.refreshStartTime();
    }

    public void setElapsedSeconds(int seconds) {
        this.timeline.setCounter(seconds);
    }

    public void resetElapsedTime() {
        this.timeline.resetCounter();
    }

    public void setPreparationProgress(float progress) {
        this.timeline.updatePercentageProgress(
                Phase.PREPARATION,
                progress
        );
    }

    public void setScatterProgress(float progress) {
        this.timeline.updatePercentageProgress(
                Phase.SCATTER,
                progress
        );
    }

    public void addTimelineViewer(Player player) {
        this.timeline.addViewer(player);
    }

    public void removeTimelineViewer(Player player) {
        this.timeline.removeViewer(player);
    }

    public Phase getPhase() {
        return this.phase;
    }

    public GameTimeline getTimeline() {
        return this.timeline;
    }

    public boolean isRunning() {
        return this.running;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) {
            return false;
        }

        int pvpMinute = this.getPvpMinuteOption().getValue();
        int borderShrinkMinute = this.getBorderShrinkMinuteOption().getValue();
        int meetupMinute = this.getMeetupMinuteOption().getValue();
        int finalShrinkMinute = this.getFinalShrinkMinuteOption().getValue();
        int deathmatchMinute = this.getDeathmatchMinuteOption().getValue();

        return pvpMinute > 0
                && pvpMinute < borderShrinkMinute
                && borderShrinkMinute < meetupMinute
                && meetupMinute < finalShrinkMinute
                && finalShrinkMinute < deathmatchMinute;
    }
}