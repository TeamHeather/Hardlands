package team.heather.hardlands.game.phase;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Function;

import net.kyori.adventure.bossbar.BossBar;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.config.Option;
import team.heather.hardlands.game.GameManagerConfiguration;

public enum Phase {

    OFF_GAME("Fuera de partida", Stage.LOBBY, Progression.EMPTY, ProgressSource.STATIC, false,
            PhaseBehavior.OFF_GAME),

    PREPARATION("Preparación", Stage.LOBBY, Progression.FILL, ProgressSource.PERCENTAGE, true,
            PhaseBehavior.PREPARATION),

    WAITING("Esperando", Stage.LOBBY, Progression.DRAIN, ProgressSource.CLOCK, true,
            PhaseBehavior.WAITING),

    SCATTER("Dispersión", Stage.LOBBY, Progression.FILL, ProgressSource.PERCENTAGE, false,
            PhaseBehavior.SCATTER),

    SURVIVAL("Supervivencia", Stage.SURVIVAL, Progression.FILL, ProgressSource.COUNTER, true,
            PhaseBehavior.SURVIVAL),

    BORDER_SHRINK("Reducción del borde", Stage.MEETUP, Progression.FILL, ProgressSource.COUNTER,
            true, PhaseBehavior.BORDER_SHRINK,
            GameManagerConfiguration::getBorderShrinkMinuteOption),

    MEETUP("Encuentro", Stage.MEETUP, Progression.DRAIN, ProgressSource.COUNTER, true,
            PhaseBehavior.MEETUP, GameManagerConfiguration::getMeetupMinuteOption),

    FINAL_SHRINK("Reducción final", Stage.DEATHMATCH, Progression.FILL, ProgressSource.COUNTER,
            true, PhaseBehavior.FINAL_SHRINK,
            GameManagerConfiguration::getFinalShrinkMinuteOption),

    DEATHMATCH("Combate a muerte", Stage.DEATHMATCH, Progression.FULL, ProgressSource.COUNTER,
            false, PhaseBehavior.DEATHMATCH,
            GameManagerConfiguration::getDeathmatchMinuteOption);

    private static final Phase[] VALUES = values();

    private final String label;
    private final Stage stage;
    private final Progression progression;
    private final ProgressSource progressSource;
    private final boolean automaticAdvance;
    private final PhaseBehavior.Handler handler;
    private final Function<GameManagerConfiguration, Option<Integer>> minuteOption;

    Phase(String label, Stage stage, Progression progression, ProgressSource progressSource,
          boolean automaticAdvance, PhaseBehavior.Handler handler) {
        this(label, stage, progression, progressSource, automaticAdvance, handler, null);
    }

    Phase(String label, Stage stage, Progression progression, ProgressSource progressSource,
          boolean automaticAdvance, PhaseBehavior.Handler handler,
          Function<GameManagerConfiguration, Option<Integer>> minuteOption) {
        this.label = label;
        this.stage = stage;
        this.progression = progression;
        this.progressSource = progressSource;
        this.automaticAdvance = automaticAdvance;
        this.handler = handler;
        this.minuteOption = minuteOption;
    }

    public void onStart(Hardlands plugin) {
        this.handler.onStart(plugin, this);
    }

    public void onStop(Hardlands plugin) {
        this.handler.onStop(plugin, this);
    }

    public Optional<Phase> previous() {
        int index = this.ordinal() - 1;
        return index >= 0 ? Optional.of(VALUES[index]) : Optional.empty();
    }

    public Optional<Phase> next() {
        int index = this.ordinal() + 1;
        return index < VALUES.length ? Optional.of(VALUES[index]) : Optional.empty();
    }

    public Integer getMinute(GameManagerConfiguration configuration) {
        if (this == SURVIVAL) return 0;
        return this.minuteOption == null ? null : this.minuteOption.apply(configuration).getValue();
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

        if (startMinute == null || endMinute == null) return Duration.ZERO;
        return Duration.ofMinutes(Math.max(0, endMinute - startMinute));
    }

    public boolean isScatterQueueEnabled() {
        return this.stage == Stage.LOBBY;
    }

    public boolean isAutomaticAdvanceEnabled() {
        return this.automaticAdvance;
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

    public ProgressSource getProgressSource() {
        return this.progressSource;
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

    public enum ProgressSource {
        STATIC,
        PERCENTAGE,
        CLOCK,
        COUNTER
    }

    public enum Progression {

        EMPTY,
        FILL,
        DRAIN,
        FULL;

        public float apply(float progress) {
            float normalized = Math.clamp(progress, 0.0F, 1.0F);

            return switch (this) {
                case EMPTY -> 0.0F;
                case FILL -> normalized;
                case DRAIN -> 1.0F - normalized;
                case FULL -> 1.0F;
            };
        }
    }
}