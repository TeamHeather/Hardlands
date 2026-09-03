package team.heather.hardlands.game;

import java.time.LocalTime;

import org.jetbrains.annotations.NotNull;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.MinuteOptionDef;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.game.team.TeamManager;
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
    private final GameTask task;
    private final HardlandsGame data;
    private final TeamManager teamManager;

    private Phase phase = Phase.OFF_GAME;

    public GameManager(final Hardlands plugin) {
        this.plugin = plugin;
        this.timeline = new GameTimeline(this);
        this.task = new GameTask(plugin, this.timeline);
        this.data = new HardlandsGame();
        this.teamManager = new TeamManager();
    }

    public TeamManager getTeamManager() {
        return this.teamManager;
    }

    public void transitionTo(@NotNull Phase nextPhase) {
        if (this.phase == nextPhase) {
            return;
        }

        this.phase.onStop(this.plugin);
        this.phase = nextPhase;

        this.timeline.syncPhase();
        this.phase.onStart(this.plugin);
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

    public Phase getPhase() {
        return this.phase;
    }

    public HardlandsGame getData() {
        return this.data;
    }

    public GameTask getTask() {
        return this.task;
    }

    public GameTimeline getTimeline() {
        return this.timeline;
    }

    @Override
    public boolean onConfigValidation() {
        int pvpMinute = super.getPvpMinuteOption().getValue();
        int borderShrinkMinute = super.getBorderShrinkMinuteOption().getValue();
        int meetupMinute = super.getMeetupMinuteOption().getValue();
        int finalShrinkMinute = super.getFinalShrinkMinuteOption().getValue();
        int deathmatchMinute = super.getDeathmatchMinuteOption().getValue();

        return pvpMinute > 0
                && pvpMinute < borderShrinkMinute
                && borderShrinkMinute < meetupMinute
                && meetupMinute < finalShrinkMinute
                && finalShrinkMinute < deathmatchMinute;
    }
}