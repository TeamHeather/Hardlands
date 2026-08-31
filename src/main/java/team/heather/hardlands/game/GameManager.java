package team.heather.hardlands.game;

import java.util.function.BooleanSupplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.MinuteOptionDef;
import team.heather.hardlands.game.phase.Phase;

@ConfigBuilder(
        identifier = "game",
        minuteOptions = {
                @MinuteOptionDef(name = "waitingMinute"),
                @MinuteOptionDef(name = "gracePeriodMinute"),
                @MinuteOptionDef(name = "pvpMinute"),
                @MinuteOptionDef(name = "borderShrinkMinute"),
                @MinuteOptionDef(name = "meetupMinute"),
                @MinuteOptionDef(name = "finalShrinkMinute"),
                @MinuteOptionDef(name = "deathmatchMinute")
        }
)
public final class GameManager extends GameManagerConfiguration {

    private final GameTimer timerManager;
    private final Hardlands plugin;

    private Phase phase = Phase.OFF_GAME;
    private boolean initialized;

    public GameManager(Hardlands plugin) {
        this.plugin = plugin;
        this.timerManager = new GameTimer(this);
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) {
            return false;
        }

        int waiting = super.waitingMinute.getValue();
        int gracePeriod = super.gracePeriodMinute.getValue();
        int pvp = super.pvpMinute.getValue();
        int borderShrink = super.borderShrinkMinute.getValue();
        int meetup = super.meetupMinute.getValue();
        int finalShrink = super.finalShrinkMinute.getValue();
        int deathmatch = super.deathmatchMinute.getValue();

        return waiting < gracePeriod
                && gracePeriod < pvp
                && pvp < borderShrink
                && borderShrink < meetup
                && meetup < finalShrink
                && finalShrink < deathmatch;
    }

    public void initialize() {
        if (this.initialized) {
            throw new IllegalStateException("Game is already initialized");
        }

        this.timerManager.updateState();

        new GameLoopTask(this.plugin, this).start();

        Bukkit.getPluginManager().registerEvents(new GameListener(this), this.plugin);

        this.initialized = true;
    }

    public synchronized void changePhase(Phase newPhase) {
        Phase previousPhase = this.phase;

        previousPhase.onStop(this.plugin);

        this.phase = newPhase;
        this.timerManager.updateState();

        newPhase.onStart(this.plugin);
    }

    public Phase getPhase() {
        return this.phase;
    }

    public GameTimer getTimerManager() {
        return this.timerManager;
    }
}