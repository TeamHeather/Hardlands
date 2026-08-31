package team.heather.hardlands.game;

import java.time.Duration;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.util.HardlandsColor;
import team.heather.hardlands.util.text.TimeFormatter;

public final class GameTimer {

    private static final BooleanSupplier ALWAYS_TRUE = () -> true;
    private static final String SEPARATOR = " » ";
    private static final String TRANSITION_SEPARATOR = " → ";

    private final GameManager gameManager;
    private final BossBar bossBar;

    private BooleanSupplier progressCondition = ALWAYS_TRUE;
    private String prefix = "";
    private String suffix = "";

    private double progressDelta;
    private int chronometer;
    private boolean progressEnded;

    public GameTimer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.bossBar = BossBar.bossBar(
                Component.empty(),
                1.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );
    }

    // Lifecycle

    public void updateProgress() {
        Phase phase = this.gameManager.getPhase();

        if (phase.advancesChronometer()) {
            this.chronometer++;
        }

        if (!this.progressEnded
                && this.progressCondition.getAsBoolean()
                && this.progressDelta != 0.0D) {
            this.modifyProgress(progress -> progress + this.progressDelta);
        }

        this.updateName(phase);
        this.tryAdvance(phase);
    }

    public void updateState() {
        Phase phase = this.gameManager.getPhase();

        if (phase == Phase.OFF_GAME) {
            this.chronometer = 0;
        }

        this.progressCondition = ALWAYS_TRUE;
        this.progressDelta = 0.0D;
        this.progressEnded = false;
        this.prefix = "";
        this.suffix = "";

        this.bossBar.color(phase.getStage().getBossBarColor());

        switch (phase.getProgression()) {
            case EMPTY -> this.bossBar.progress(0.0F);
            case FULL -> this.bossBar.progress(1.0F);

            case FILL -> {
                this.bossBar.progress(0.0F);
                this.progressDelta = this.calculateProgressDelta(phase);
            }

            case DRAIN -> {
                this.bossBar.progress(1.0F);
                this.progressDelta = -this.calculateProgressDelta(phase);
            }
        }

        if (phase == Phase.PRE_GENERATION || phase == Phase.SCATTER) {
            this.suffix = "0.0%";
        }

        this.updateName(phase);
    }

    // External progress

    public void updatePregenerationProgress(float progress) {
        if (this.gameManager.getPhase() != Phase.PRE_GENERATION) return;

        float clampedProgress = Math.clamp(progress, 0.0F, 100.0F);

        this.setProgress(clampedProgress / 100.0F);
        this.suffix = "%.1f%%".formatted(clampedProgress);
        this.updateName(Phase.PRE_GENERATION);

        this.tryAdvance(Phase.PRE_GENERATION);
    }

    public void updateScatterProgress(float progress) {
        if (this.gameManager.getPhase() != Phase.SCATTER) return;

        float clampedProgress = Math.clamp(progress, 0.0F, 100.0F);

        this.setProgress(clampedProgress / 100.0F);
        this.suffix = "%.1f%%".formatted(clampedProgress);
        this.updateName(Phase.SCATTER);
    }

    // Control API

    public void completeCurrentPhase() {
        Phase phase = this.gameManager.getPhase();

        this.progressCondition = ALWAYS_TRUE;

        switch (phase.getProgression()) {
            case FILL -> this.setProgress(1.0F);
            case DRAIN -> this.setProgress(0.0F);

            case EMPTY, FULL -> {
                if (phase.allowsForcedAdvance()) {
                    this.advance(phase);
                }

                return;
            }
        }

        this.updateName(phase);

        if (phase.allowsForcedAdvance()) {
            this.tryAdvance(phase, true);
        }
    }

    public void setChronometer(int seconds) {
        this.chronometer = Math.max(0, seconds);
        this.updateName(this.gameManager.getPhase());
    }

    public void resetChronometer() {
        this.setChronometer(0);
    }

    public void modifyChronometer(IntUnaryOperator operation) {
        this.setChronometer(operation.applyAsInt(this.chronometer));
    }

    public void setProgress(float progress) {
        this.bossBar.progress(Math.clamp(progress, 0.0F, 1.0F));
    }

    public void modifyProgress(DoubleUnaryOperator operation) {
        this.setProgress((float) operation.applyAsDouble(this.bossBar.progress()));
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        this.updateName(this.gameManager.getPhase());
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
        this.updateName(this.gameManager.getPhase());
    }

    public void setProgressCondition(BooleanSupplier condition) {
        this.progressCondition = condition;
    }

    public void resetProgressCondition() {
        this.progressCondition = ALWAYS_TRUE;
    }

    // Viewers

    public void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    public void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    // Phase progression

    private void tryAdvance(Phase phase) {
        this.tryAdvance(phase, false);
    }

    private void tryAdvance(Phase phase, boolean forced) {
        if (this.progressEnded) return;
        if (this.gameManager.getPhase() != phase) return;
        if (!forced && !phase.advancesAutomatically()) return;
        if (!this.progressCondition.getAsBoolean()) return;
        if (!this.isProgressComplete(phase)) return;

        this.advance(phase);
    }

    private void advance(Phase phase) {
        if (this.gameManager.getPhase() != phase) return;

        this.progressEnded = true;
        phase.next().ifPresent(this.gameManager::changePhase);
    }

    private boolean isProgressComplete(Phase phase) {
        return switch (phase.getProgression()) {
            case FILL -> this.bossBar.progress() >= 1.0F;
            case DRAIN -> this.bossBar.progress() <= 0.0F;
            case EMPTY, FULL -> false;
        };
    }

    private double calculateProgressDelta(Phase phase) {
        long durationSeconds = phase.getDuration(this.gameManager).toSeconds();
        return durationSeconds > 0L ? 1.0D / durationSeconds : 0.0D;
    }

    // Bossbar rendering

    private void updateName(Phase phase) {
        if (phase == Phase.OFF_GAME) {
            this.bossBar.name(Component.text(phase.getLabel(), NamedTextColor.WHITE));
            return;
        }

        String label = this.prefix.isEmpty()
                ? phase.getLabel()
                : this.prefix;

        Component content = Component.text(label, NamedTextColor.WHITE)
                .append(Component.text(SEPARATOR, NamedTextColor.WHITE))
                .append(Component.text(this.computeSuffix(), NamedTextColor.WHITE))
                .append(Component.text(this.computeTransitionSuffix(phase), NamedTextColor.WHITE));

        if (phase != Phase.SURVIVAL) {
            this.bossBar.name(content);
            return;
        }

        TextColor skullColor = this.isPvpEnabled()
                ? HardlandsColor.PRIMARY
                : HardlandsColor.LIGHT_GRAY;

        this.bossBar.name(
                Component.text("☠ ", skullColor)
                        .append(content)
                        .append(Component.text(" ☠", skullColor))
        );
    }

    private String computeSuffix() {
        return this.suffix.isEmpty()
                ? TimeFormatter.format(Duration.ofSeconds(this.chronometer))
                : this.suffix;
    }

    private String computeTransitionSuffix(Phase phase) {
        if (phase == Phase.SURVIVAL) {
            return TRANSITION_SEPARATOR
                    + TimeFormatter.format(Duration.ofMinutes(this.getSurvivalTargetMinute()));
        }

        if (!phase.showsTargetMinute()) return "";

        Integer minute = phase.getNextMinute(this.gameManager);

        return minute == null
                ? ""
                : TRANSITION_SEPARATOR + TimeFormatter.format(Duration.ofMinutes(minute));
    }

    // Survival state

    private boolean isPvpEnabled() {
        int gracePeriodMinute = this.gameManager.getGracePeriodMinuteOption().getValue();
        int pvpMinute = this.gameManager.getPvpMinuteOption().getValue();
        int pvpStartSeconds = (pvpMinute - gracePeriodMinute) * 60;

        return this.chronometer >= pvpStartSeconds;
    }

    private int getSurvivalTargetMinute() {
        return this.isPvpEnabled()
                ? this.gameManager.getBorderShrinkMinuteOption().getValue()
                : this.gameManager.getPvpMinuteOption().getValue();
    }
}