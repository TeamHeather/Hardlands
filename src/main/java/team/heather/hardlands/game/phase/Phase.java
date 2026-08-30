package team.heather.hardlands.game.phase;

import java.util.Optional;
import java.util.function.Function;

import net.kyori.adventure.bossbar.BossBar;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.GameManagerConfiguration;

public enum Phase {

    IDLE(
            "Idle",
            Stage.NEUTRAL,
            Progression.EMPTY,
            new IdleHandler()
    ),

    PRE_GENERATION(
            "Pre-Generation",
            Stage.NEUTRAL,
            Progression.FILL,
            new PreGenerationHandler()
    ),

    WAITING(
            "Waiting",
            Stage.NEUTRAL,
            Progression.DRAIN,
            new WaitingHandler()
    ),

    SCATTER(
            "Scatter",
            Stage.EARLY_GAME,
            Progression.FILL,
            new PreGenerationHandler() //TODO CAMBIAR
    ),

    GRACE_PERIOD(
            "Grace Period",
            Stage.EARLY_GAME,
            Progression.DRAIN,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getGracePeriodMinuteOption
    ),

    PVP(
            "PvP",
            Stage.COMBAT,
            Progression.DRAIN,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getPvpMinuteOption
    ),

    BORDER_SHRINK(
            "Border Shrink",
            Stage.COMBAT,
            Progression.FILL,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getBorderShrinkMinuteOption
    ),

    MEETUP(
            "Meetup",
            Stage.COMBAT,
            Progression.DRAIN,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getMeetupMinuteOption
    ),

    FINAL_SHRINK(
            "Final Shrink",
            Stage.COMBAT,
            Progression.FILL,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getFinalShrinkMinuteOption
    ),

    DEATHMATCH(
            "Deathmatch",
            Stage.DEATHMATCH,
            Progression.FULL,
            new PreGenerationHandler(), //TODO CAMBIAR
            GameManagerConfiguration::getDeathmatchMinuteOption
    ),

    POST_GAME(
            "Post-Game",
            Stage.NEUTRAL,
            Progression.EMPTY,
            new PreGenerationHandler() //TODO CAMBI
    );

    private final String label;
    private final Stage stage;
    private final Progression progression;
    private final PhaseHandler handler;
    private final Function<GameManager, Option<Integer>> minuteOption;

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
            Function<GameManager, Option<Integer>> minuteOption
    ) {
        this.label = label;
        this.stage = stage;
        this.progression = progression;
        this.handler = handler;
        this.minuteOption = minuteOption;
    }

    public void onStart() {
        this.handler.onStart(this);
    }

    public void onStop() {
        this.handler.onStop(this);
    }

    public Optional<Phase> previous() {
        int index = ordinal() - 1;

        return index >= 0
                ? Optional.of(values()[index])
                : Optional.empty();
    }

    public Optional<Phase> next() {
        int index = ordinal() + 1;
        Phase[] phases = values();

        return index < phases.length
                ? Optional.of(phases[index])
                : Optional.empty();
    }

    public String getLabel() {
        return this.label;
    }

    public Stage getCategory() {
        return this.stage;
    }

    public Progression getProgression() {
        return this.progression;
    }

    public Option<Integer> getMinuteOption(GameManager flow) {
        return this.minuteOption == null ? null : this.minuteOption.apply(flow);
    }

    public boolean isRunning() {
        return switch (this) {
            case SCATTER, GRACE_PERIOD, PVP, BORDER_SHRINK, MEETUP, FINAL_SHRINK, DEATHMATCH -> true;
            default -> false;
        };
    }

    public enum Stage {

        NEUTRAL(BossBar.Color.WHITE),
        EARLY_GAME(BossBar.Color.PINK),
        COMBAT(BossBar.Color.RED),
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