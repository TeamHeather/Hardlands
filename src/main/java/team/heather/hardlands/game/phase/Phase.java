package team.heather.hardlands.game.phase;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import net.kyori.adventure.bossbar.BossBar;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.GameManagerConfiguration;

public enum Phase {

    OFF_GAME(
            "Off-Game",
            Stage.OFF_GAME,
            Progression.EMPTY,
            PhaseHandler.IDLE
    ),

    PRE_GENERATION(
            "Pre-Generation",
            Stage.OFF_GAME,
            Progression.FILL,
            PhaseHandler.PRE_GENERATION
    ),

    WAITING(
            "Waiting",
            Stage.OFF_GAME,
            Progression.DRAIN,
            PhaseHandler.WAITING,
            GameManagerConfiguration::getWaitingMinuteOption
    ),

    SCATTER(
            "Scatter",
            Stage.OFF_GAME,
            Progression.FILL,
            PhaseHandler.SCATTER
    ),

    SURVIVAL(
            "Survival",
            Stage.SURVIVAL,
            Progression.DRAIN,
            PhaseHandler.SURVIVAL,
            Phase::calculateSurvivalDuration
    ),

    BORDER_SHRINK(
            "Border Shrink",
            Stage.MEETUP,
            Progression.FILL,
            PhaseHandler.BORDER_SHRINK,
            GameManagerConfiguration::getBorderShrinkMinuteOption
    ),

    MEETUP(
            "Meetup",
            Stage.MEETUP,
            Progression.DRAIN,
            PhaseHandler.MEETUP,
            GameManagerConfiguration::getMeetupMinuteOption
    ),

    FINAL_SHRINK(
            "Final Shrink",
            Stage.MEETUP,
            Progression.FILL,
            PhaseHandler.FINAL_SHRINK,
            GameManagerConfiguration::getFinalShrinkMinuteOption
    ),

    DEATHMATCH(
            "Deathmatch",
            Stage.DEATHMATCH,
            Progression.FULL,
            PhaseHandler.DEATHMATCH,
            GameManagerConfiguration::getDeathmatchMinuteOption
    )

    ;

    private final String label;
    private final Stage stage;
    private final Progression progression;
    private final PhaseHandler handler;
    private final ToIntFunction<GameManagerConfiguration> minute;

    Phase(
            String label,
            Stage stage,
            Progression progression,
            PhaseHandler handler
    ) {
        this(label, stage, progression, handler, (ToIntFunction<GameManagerConfiguration>) null);
    }

    Phase(
            String label,
            Stage stage,
            Progression progression,
            PhaseHandler handler,
            Function<GameManagerConfiguration, Option<Integer>> minuteOption
    ) {
        this(
                label,
                stage,
                progression,
                handler,
                (ToIntFunction<GameManagerConfiguration>)
                        configuration -> minuteOption.apply(configuration).getValue()
        );
    }

    Phase(
            String label,
            Stage stage,
            Progression progression,
            PhaseHandler handler,
            ToIntFunction<GameManagerConfiguration> minute
    ) {
        this.label = label;
        this.stage = stage;
        this.progression = progression;
        this.handler = handler;
        this.minute = minute;
    }

    public void onStart() {
        this.handler.onStart(this);
    }

    public void onStop() {
        this.handler.onStop(this);
    }

    public Optional<Phase> previous() {
        int index = this.ordinal() - 1;

        return index >= 0
                ? Optional.of(values()[index])
                : Optional.empty();
    }

    public Optional<Phase> next() {
        int index = this.ordinal() + 1;
        Phase[] phases = values();

        return index < phases.length
                ? Optional.of(phases[index])
                : Optional.empty();
    }

    public String getLabel() {
        return this.label;
    }

    public Stage getStage() {
        return this.stage;
    }

    public Progression getProgression() {
        return this.progression;
    }

    public Integer getMinute(GameManagerConfiguration configuration) {
        return this.minute == null
                ? null
                : this.minute.applyAsInt(configuration);
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

    public enum Stage {

        OFF_GAME(BossBar.Color.WHITE),
        SURVIVAL(BossBar.Color.PINK),
        MEETUP(BossBar.Color.RED),
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

    private static int calculateSurvivalDuration(GameManagerConfiguration configuration) {
        return configuration.getGracePeriodMinuteOption().getValue()
                + configuration.getPvpMinuteOption().getValue();
    }
}