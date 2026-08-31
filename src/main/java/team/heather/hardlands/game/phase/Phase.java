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
            Stage.OFF_GAME,
            Progression.EMPTY,
            PhaseHandlers.OFF_GAME
    ),

    PRE_GENERATION(
            "Pregeneración",
            Stage.OFF_GAME,
            Progression.FILL,
            PhaseHandlers.PRE_GENERATION
    ),

    WAITING(
            "Espera",
            Stage.OFF_GAME,
            Progression.DRAIN,
            PhaseHandlers.WAITING,
            GameManagerConfiguration::getWaitingMinuteOption
    ),

    SCATTER(
            "Dispersión",
            Stage.OFF_GAME,
            Progression.FILL,
            PhaseHandlers.SCATTER
    ),

    SURVIVAL(
            "Supervivencia",
            Stage.SURVIVAL,
            Progression.DRAIN,
            PhaseHandlers.SURVIVAL,
            GameManagerConfiguration::getGracePeriodMinuteOption
    ),

    BORDER_SHRINK(
            "Reducción del borde",
            Stage.MEETUP,
            Progression.FILL,
            PhaseHandlers.BORDER_SHRINK,
            GameManagerConfiguration::getBorderShrinkMinuteOption
    ),

    MEETUP(
            "Encuentro",
            Stage.MEETUP,
            Progression.DRAIN,
            PhaseHandlers.MEETUP,
            GameManagerConfiguration::getMeetupMinuteOption
    ),

    FINAL_SHRINK(
            "Reducción final",
            Stage.MEETUP,
            Progression.FILL,
            PhaseHandlers.FINAL_SHRINK,
            GameManagerConfiguration::getFinalShrinkMinuteOption
    ),

    DEATHMATCH(
            "Combate a muerte",
            Stage.DEATHMATCH,
            Progression.FULL,
            PhaseHandlers.DEATHMATCH,
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
        if (this.minuteOption == null) return null;
        return this.minuteOption.apply(configuration).getValue();
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

        int durationMinutes = endMinute - startMinute;

        return durationMinutes > 0
                ? Duration.ofMinutes(durationMinutes)
                : Duration.ZERO;
    }

    // Behavior

    public boolean advancesChronometer() {
        return switch (this) {
            case OFF_GAME,
                 PRE_GENERATION,
                 SCATTER -> false;
            default -> true;
        };
    }

    public boolean advancesAutomatically() {
        return switch (this) {
            case OFF_GAME,
                 SCATTER,
                 DEATHMATCH -> false;
            default -> true;
        };
    }

    public boolean allowsForcedAdvance() {
        return this != SCATTER;
    }

    public boolean showsTargetMinute() {
        return this.progression == Progression.DRAIN
                || this == BORDER_SHRINK
                || this == FINAL_SHRINK;
    }

    public boolean isScatterQueueOpen() {
        return switch (this) {
            case OFF_GAME,
                 PRE_GENERATION,
                 WAITING,
                 SCATTER -> true;
            default -> false;
        };
    }

    public boolean isRunning() {
        return switch (this) {
            case SURVIVAL,
                 BORDER_SHRINK,
                 MEETUP,
                 FINAL_SHRINK,
                 DEATHMATCH -> true;
            default -> false;
        };
    }

    // Getters

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

        OFF_GAME(BossBar.Color.WHITE),
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