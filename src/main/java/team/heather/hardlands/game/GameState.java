package team.heather.hardlands.game;

import java.util.Optional;
import java.util.function.Function;

import net.kyori.adventure.bossbar.BossBar;
import team.heather.hardlands.core.config.Option;

public enum GameState {

    IDLE(
            "Idle",
            Category.NEUTRAL,
            ProgressMode.EMPTY
    ),

    PRE_GENERATION(
            "Pre-Generation",
            Category.NEUTRAL,
            ProgressMode.FILL
    ),

    WAITING(
            "Waiting",
            Category.NEUTRAL,
            ProgressMode.DRAIN
    ),

    SCATTER(
            "Scatter",
            Category.EARLY_GAME,
            ProgressMode.FILL
    ),

    GRACE_PERIOD(
            "Grace Period",
            Category.EARLY_GAME,
            ProgressMode.DRAIN,
            GameFlowConfiguration::getGracePeriodMinuteOption
    ),

    PVP(
            "PvP",
            Category.COMBAT,
            ProgressMode.DRAIN,
            GameFlowConfiguration::getPvpMinuteOption
    ),

    BORDER_SHRINK(
            "Border Shrink",
            Category.COMBAT,
            ProgressMode.FILL,
            GameFlowConfiguration::getBorderShrinkMinuteOption
    ),

    MEETUP(
            "Meetup",
            Category.COMBAT,
            ProgressMode.DRAIN,
            GameFlowConfiguration::getMeetupMinuteOption
    ),

    FINAL_SHRINK(
            "Final Shrink",
            Category.COMBAT,
            ProgressMode.FILL,
            GameFlowConfiguration::getFinalShrinkMinuteOption
    ),

    DEATHMATCH(
            "Deathmatch",
            Category.DEATHMATCH,
            ProgressMode.FULL,
            GameFlowConfiguration::getDeathmatchMinuteOption
    ),

    POST_GAME(
            "Post-Game",
            Category.NEUTRAL,
            ProgressMode.EMPTY
    );

    private final String displayName;
    private final Category category;
    private final ProgressMode progressMode;
    private final Function<GameFlow, Option<Integer>> minuteOption;

    GameState(String displayName, Category category, ProgressMode progressMode) {
        this(displayName, category, progressMode, null);
    }

    GameState(String displayName, Category category, ProgressMode progressMode, Function<GameFlow, Option<Integer>> minuteOption) {
        this.displayName = displayName;
        this.category = category;
        this.progressMode = progressMode;
        this.minuteOption = minuteOption;
    }

    public Optional<GameState> previous() {
        return this.ordinal() == 0
                ? Optional.empty()
                : Optional.of(values()[this.ordinal() - 1]);
    }

    public Optional<GameState> next() {
        return this.ordinal() == 0
                ? Optional.empty()
                : Optional.of(values()[this.ordinal() + 1]);
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

    public Option<Integer> getMinuteOption(GameFlow flow) {
        return this.minuteOption == null ? null : this.minuteOption.apply(flow);
    }

    public boolean isRunning() {
        return switch (this) {
            case SCATTER, GRACE_PERIOD, PVP, BORDER_SHRINK, MEETUP, FINAL_SHRINK, DEATHMATCH -> true;
            default -> false;
        };
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
}