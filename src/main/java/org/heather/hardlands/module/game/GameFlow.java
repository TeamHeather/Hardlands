package org.heather.hardlands.module.game;

import java.time.Duration;

import org.bukkit.Bukkit;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.configuration.MinuteOptionDef;
import org.heather.hardlands.core.configuration.Option;

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
public final class GameFlow extends GameFlowConfiguration {

    private final GameBossBar gameBossBar = new GameBossBar(this);
    private final Hardlands plugin;

    private GameState gameState = GameState.IDLE;

    public GameFlow(Hardlands plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean isConfigurationValid() {
        if (!super.isConfigurationValid()) return false;

        int gracePeriod = this.gracePeriodMinute.getValue();
        int pvp = this.pvpMinute.getValue();
        int borderShrink = this.borderShrinkMinute.getValue();
        int meetup = this.meetupMinute.getValue();
        int deathmatch = this.deathmatchMinute.getValue();

        return gracePeriod < pvp
                && pvp < borderShrink
                && borderShrink < meetup
                && meetup < deathmatch;
    }

    //* Public API

    public void startPhase(GameState gameState) {
        this.gameState = gameState;
        this.gameBossBar.update();
    }

    public GameBossBar getGameBossBar() {
        return this.gameBossBar;
    }

    public GameState getPhase() {
        return this.gameState;
    }

    //* Internal Class Utility

    private void schedulePhase(GameState gameState) {
        Option<Integer> minuteOption = gameState.getMinuteOption(this);
        if (minuteOption == null) return;

        this.plugin.getSingleThreadScheduler().schedule(
                () -> Bukkit.getScheduler().runTask(this.plugin, () -> startPhase(gameState)),
                Duration.ofMinutes(minuteOption.getValue())
        );
    }
}