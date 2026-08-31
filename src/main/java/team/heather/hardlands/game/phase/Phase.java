package team.heather.hardlands.game.phase;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import net.kyori.adventure.bossbar.BossBar;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.GameManagerConfiguration;

public enum Phase {

    OFF_GAME(
            "Fuera de partida",
            Stage.LOBBY,
            Progression.EMPTY,
            PhaseBehavior.OFF_GAME
    ),

    PREPARATION(
            "Preparación",
            Stage.LOBBY,
            Progression.FILL,
            PhaseBehavior.PREPARATION
    ),

    WAITING(
            "Esperando",
            Stage.LOBBY,
            Progression.DRAIN,
            PhaseBehavior.WAITING
    ),

    SCATTER(
            "Dispersión",
            Stage.LOBBY,
            Progression.FILL,
            PhaseBehavior.SCATTER
    ),

    SURVIVAL(
            "Supervivencia",
            Stage.SURVIVAL,
            Progression.DRAIN,
            PhaseBehavior.SURVIVAL,
            GameManagerConfiguration::getGracePeriodMinuteOption
    ),

    BORDER_SHRINK(
            "Reducción del borde",
            Stage.MEETUP,
            Progression.FILL,
            PhaseBehavior.BORDER_SHRINK,
            GameManagerConfiguration::getBorderShrinkMinuteOption
    ),

    MEETUP(
            "Encuentro",
            Stage.MEETUP,
            Progression.DRAIN,
            PhaseBehavior.MEETUP,
            GameManagerConfiguration::getMeetupMinuteOption
    ),

    FINAL_SHRINK(
            "Reducción final",
            Stage.DEATHMATCH,
            Progression.FILL,
            PhaseBehavior.FINAL_SHRINK,
            GameManagerConfiguration::getFinalShrinkMinuteOption
    ),

    DEATHMATCH(
            "Combate a muerte",
            Stage.DEATHMATCH,
            Progression.FULL,
            PhaseBehavior.DEATHMATCH,
            GameManagerConfiguration::getDeathmatchMinuteOption
    );

    private static final Phase[] VALUES = values();

    private final String label;
    private final Stage stage;
    private final Progression progression;
    private final PhaseHandler handler;
    private final Function<GameManagerConfiguration, Option<Integer>> minuteOption;

    Phase(
            String label,
            Stage stage,
            Progression progression,
            PhaseHandler handler
    ) {
        this(label, stage, progression, handler, null);
    }

    Phase(
            String label,
            Stage stage,
            Progression progression,
            PhaseHandler handler,
            Function<GameManagerConfiguration, Option<Integer>> minuteOption
    ) {
        this.label = label;
        this.stage = stage;
        this.progression = progression;
        this.handler = handler;
        this.minuteOption = minuteOption;
    }

    // Lifecycle

    public void onStart(Hardlands plugin) {
        this.handler.onStart(plugin, this);
    }

    public void onStop(Hardlands plugin) {
        this.handler.onStop(plugin, this);
    }

    // Navigation

    public Optional<Phase> previous() {
        int index = this.ordinal() - 1;

        return index >= 0
                ? Optional.of(VALUES[index])
                : Optional.empty();
    }

    public Optional<Phase> next() {
        int index = this.ordinal() + 1;

        return index < VALUES.length
                ? Optional.of(VALUES[index])
                : Optional.empty();
    }

    // Timing

    public Integer getMinute(GameManagerConfiguration configuration) {
        return this.minuteOption != null
                ? this.minuteOption.apply(configuration).getValue()
                : null;
    }

    public Integer getNextMinute(GameManagerConfiguration configuration) {
        for (int index = this.ordinal() + 1; index < VALUES.length; index++) {
            Integer minute = VALUES[index].getMinute(configuration);

            if (minute != null) return minute;
        }

        return null;
    }

    public Duration getDuration(GameManagerConfiguration configuration) {
        Integer startMinute = this.getMinute(configuration);
        Integer endMinute = this.getNextMinute(configuration);

        if (startMinute == null || endMinute == null) {
            return Duration.ZERO;
        }

        return Duration.ofMinutes(Math.max(0, endMinute - startMinute));
    }

    // Behavior

    public boolean isCounterAdvanceEnabled() {
        return this.stage != Stage.LOBBY;
    }

    public boolean isCounterResetEnabled() {
        return this == OFF_GAME;
    }

    public boolean isClockProgressionEnabled() {
        return this == WAITING;
    }

    public boolean isPercentageProgressionEnabled() {
        return this == PREPARATION || this == SCATTER;
    }

    public boolean isPreparationProgressEnabled() {
        return this == PREPARATION;
    }

    public boolean isScatterProgressEnabled() {
        return this == SCATTER;
    }

    public boolean isPvpTransitionEnabled() {
        return this == SURVIVAL;
    }

    public boolean isAutomaticAdvanceEnabled() {
        return switch (this) {
            case OFF_GAME,
                 SCATTER,
                 DEATHMATCH -> false;
            default -> true;
        };
    }

    public boolean isForcedAdvanceEnabled() {
        return this != SCATTER;
    }

    public boolean isTargetMinuteDisplayEnabled() {
        return this.stage != Stage.LOBBY
                && this.progression != Progression.FULL;
    }

    public boolean isScatterQueueEnabled() {
        return this.stage == Stage.LOBBY;
    }

    // Properties

    public String getLabel() {
        return this.label;
    }

    public Stage getStage() {
        return this.stage;
    }

    public Progression getProgression() {
        return this.progression;
    }

    public enum Stage {

        LOBBY(BossBar.Color.WHITE),
        SURVIVAL(BossBar.Color.RED),
        MEETUP(BossBar.Color.PINK),
        DEATHMATCH(BossBar.Color.PURPLE);

        private final BossBar.Color bossBarColor;

        Stage(BossBar.Color bossBarColor) {
            this.bossBarColor = bossBarColor;
        }

        public BossBar.Color getBossBarColor() {
            return this.bossBarColor;
        }
    }

    public enum Progression {
        EMPTY,
        FILL,
        DRAIN,
        FULL
    }
}