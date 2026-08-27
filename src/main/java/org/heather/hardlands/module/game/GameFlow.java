package org.heather.hardlands.module.game;

import java.time.Duration;

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.configuration.MinuteOptionDef;
import org.heather.hardlands.configuration.OptionDef;
import org.heather.hardlands.core.SingleThreadScheduler;
import org.heather.hardlands.core.configuration.Option;
import org.heather.hardlands.core.configuration.Validator;

@ConfigBuilder(
        identifier = "game",
        minuteOptions = {
                @MinuteOptionDef(name = "gracePeriodMinute"),
                @MinuteOptionDef(name = "pvpMinute"),
                @MinuteOptionDef(name = "borderShrinkMinute"),
                @MinuteOptionDef(name = "meetupMinute"),
                @MinuteOptionDef(name = "deathmatchMinute")
        }
)
public final class GameFlow extends GameFlowConfiguration {

    private final SingleThreadScheduler scheduler;
    private final Plugin plugin;

    private Phase phase = Phase.IDLE;

    public GameFlow(Hardlands plugin) {
        this.scheduler = plugin.getSingleThreadScheduler();
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

    public void startPhase(Phase phase) {
        this.phase = phase;
    }

    public Phase getPhase() {
        return this.phase;
    }

    //* Subclasses

    public enum Phase {

        IDLE("En espera", Category.NEUTRAL, ProgressMode.EMPTY),
        PREPARATION("Preparación del mundo", Category.NEUTRAL, ProgressMode.FILL),
        STARTING("Inicio de la partida", Category.NEUTRAL, ProgressMode.DRAIN),
        SCATTER("Dispersión de jugadores", Category.EARLY_GAME, ProgressMode.FILL),
        GRACE_PERIOD("Supervivencia - Periodo de gracia", Category.EARLY_GAME, ProgressMode.DRAIN),
        OPEN_COMBAT("Supervivencia - Combate abierto", Category.COMBAT, ProgressMode.DRAIN),
        BORDER_SHRINK("Reducción del borde", Category.COMBAT, ProgressMode.FILL),
        MEETUP("Encuentro", Category.COMBAT, ProgressMode.DRAIN),
        FINAL_BORDER_SHRINK("Reducción final del borde", Category.COMBAT, ProgressMode.FILL),
        DEATHMATCH("Combate final", Category.DEATHMATCH, ProgressMode.FULL),
        POST_GAME("Fin de la partida", Category.NEUTRAL, ProgressMode.EMPTY);

        private final String displayName;
        private final Category category;
        private final ProgressMode progressMode;

        Phase(String displayName, Category category, ProgressMode progressMode) {
            this.displayName = displayName;
            this.category = category;
            this.progressMode = progressMode;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        public Category getCategory() {
            return this.category;
        }

        public ProgressMode getProgressMode() {
            return this.progressMode;
        }

        public boolean isRunning() {
            return switch (this) {
                case SCATTER,
                     GRACE_PERIOD,
                     OPEN_COMBAT,
                     BORDER_SHRINK,
                     MEETUP,
                     FINAL_BORDER_SHRINK,
                     DEATHMATCH -> true;
                default -> false;
            };
        }
    }

    public enum Category {

        NEUTRAL(BossBar.Color.WHITE),
        EARLY_GAME(BossBar.Color.PINK),
        COMBAT(BossBar.Color.RED),
        DEATHMATCH(BossBar.Color.PURPLE);

        private final BossBar.Color bossBarColor;

        Category(BossBar.Color bossBarColor) {
            this.bossBarColor = bossBarColor;
        }

        public BossBar.Color getBossBarColor() {
            return this.bossBarColor;
        }
    }

    public enum ProgressMode {
        EMPTY,
        FILL,
        DRAIN,
        FULL
    }

    //* Internal Class Utilities

    private void scheduleForOption(Runnable task, Option<Integer> minute) {
        this.scheduler.schedule(
                () -> Bukkit.getScheduler().runTask(this.plugin, task),
                Duration.ofMinutes(minute.getValue())
        );
    }
}