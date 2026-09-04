package team.heather.hardlands.game;

import java.time.LocalTime;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
                @MinuteOptionDef(name = "borderShrinkMinute"),
                @MinuteOptionDef(name = "deathmatchMinute"),
                @MinuteOptionDef(name = "finalShrinkMinute"),
                @MinuteOptionDef(name = "meetupMinute"),
                @MinuteOptionDef(name = "pvpMinute")
        }
)
public final class GameManager extends GameManagerConfiguration implements AutoCloseable {

    private final TeamManager teamManager;
    private final GameTimeline timeline;
    private final GameData data;
    private final Hardlands plugin;

    @NotNull private Phase phase = Phase.OFF_GAME;
    @Nullable private BukkitTask task;

    public GameManager(final Hardlands plugin) {
        this.teamManager = new TeamManager();
        this.timeline = new GameTimeline(this);
        this.data = new GameData(plugin.getPlayerManager());
        this.plugin = plugin;
    }

    @Override
    public void close() {
        if (this.task == null) {
            return;
        }

        this.task.cancel();
        this.task = null;
    }

    public GameData getData() {
        return this.data;
    }

    public Phase getPhase() {
        return this.phase;
    }

    public TeamManager getTeamManager() {
        return this.teamManager;
    }

    public GameTimeline getTimeline() {
        return this.timeline;
    }

    public boolean isRunning() {
        return this.task != null;
    }

    @Override
    public boolean onConfigValidation() {
        int borderShrinkMinute = super.getBorderShrinkMinuteOption().getValue();
        int deathmatchMinute = super.getDeathmatchMinuteOption().getValue();
        int finalShrinkMinute = super.getFinalShrinkMinuteOption().getValue();
        int meetupMinute = super.getMeetupMinuteOption().getValue();
        int pvpMinute = super.getPvpMinuteOption().getValue();

        return pvpMinute > 0
                && pvpMinute < borderShrinkMinute
                && borderShrinkMinute < meetupMinute
                && meetupMinute < finalShrinkMinute
                && finalShrinkMinute < deathmatchMinute;
    }

    public void resetElapsedTime() {
        this.timeline.resetCounter();
    }

    public void setElapsedSeconds(int seconds) {
        this.timeline.setCounter(seconds);
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

    public void run() {
        if (this.task != null) {
            throw new IllegalStateException("Game task is already running");
        }

        this.task = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                this.timeline::tick,
                20L,
                20L
        );
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
}