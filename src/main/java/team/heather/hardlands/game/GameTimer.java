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
import team.heather.hardlands.util.text.TinyCaps;

public final class GameTimer {

    private final GameManager gameManager;
    private final BossBar bossBar = BossBar.bossBar(
            Component.empty(),
            1.0F,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );

    private BooleanSupplier progressCondition = () -> true;
    private String prefix = "";
    private String suffix = "";
    private double progressDelta;
    private int chronometer;
    private boolean progressEnded;

    public GameTimer(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    private void tryAdvance(Phase phase) {
        if (!this.isProgressComplete(phase)) return;
        if (this.gameManager.getPhase() != phase) return;

        this.progressEnded = true;
        phase.next().ifPresent(this.gameManager::changePhase);
    }

    public synchronized void updateProgress() {
        Phase phase = this.gameManager.getPhase();

        if (this.shouldUpdateChronometer(phase)) {
            this.chronometer++;
        }

        this.updateName(phase);

        if (this.progressEnded || !this.progressCondition.getAsBoolean()) return;

        if (this.progressDelta != 0.0D) {
            this.modifyProgress(progress -> progress + this.progressDelta);
        }

        this.tryAdvance(phase);
    }

    private boolean shouldUpdateChronometer(Phase phase) {
        return switch (phase) {
            case OFF_GAME,
                 PRE_GENERATION,
                 SCATTER -> false;
            default -> true;
        };
    }

    public synchronized void updateState() {
        Phase phase = this.gameManager.getPhase();

        if (phase == Phase.OFF_GAME) {
            this.chronometer = 0;
        }

        this.progressEnded = false;
        this.progressCondition = () -> true;
        this.progressDelta = 0.0D;
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

        this.updateName(phase);
    }

    public synchronized void updatePregenerationProgress(float progress) {
        if (this.gameManager.getPhase() != Phase.PRE_GENERATION) return;

        this.setProgress(progress / 100.0F);
        this.suffix = "%.1f%%".formatted(progress);
        this.updateName(Phase.PRE_GENERATION);
    }

    public synchronized void updateScatterProgress(float progress) {
        if (this.gameManager.getPhase() != Phase.SCATTER) return;

        this.setProgress(progress / 100.0F);
        this.suffix = "%.1f%%".formatted(progress);
        this.updateName(Phase.SCATTER);
    }

    public synchronized void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    public synchronized void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    public synchronized void modifyChronometer(IntUnaryOperator operation) {
        this.chronometer = operation.applyAsInt(this.chronometer);
        this.updateName(this.gameManager.getPhase());
    }

    public synchronized void modifyProgress(DoubleUnaryOperator operation) {
        this.setProgress((float) operation.applyAsDouble(this.bossBar.progress()));
    }

    public synchronized void resetChronometer() {
        this.chronometer = 0;
        this.updateName(this.gameManager.getPhase());
    }

    public synchronized void setProgress(float progress) {
        this.bossBar.progress(Math.clamp(progress, 0.0F, 1.0F));
    }

    public synchronized void setPrefix(String prefix) {
        this.prefix = prefix;
        this.updateName(this.gameManager.getPhase());
    }

    public synchronized void setSuffix(String suffix) {
        this.suffix = suffix;
        this.updateName(this.gameManager.getPhase());
    }

    public synchronized void setProgressCondition(BooleanSupplier condition) {
        this.progressCondition = condition;
    }

    public synchronized void resetProgressCondition() {
        this.progressCondition = () -> true;
    }

    private void updateName(Phase phase) {
        if (phase == Phase.OFF_GAME) {
            this.bossBar.name(Component.text(phase.getLabel(), HardlandsColor.LIGHT_GRAY));
            return;
        }

        String label = this.prefix.isEmpty() ? phase.getLabel() : this.prefix;

        Component content = Component.text(label, HardlandsColor.LIGHT_GRAY)
                .append(Component.text(" » ", HardlandsColor.LIGHT_GRAY))
                .append(Component.text(this.computeSuffix(), HardlandsColor.LIGHT_GRAY))
                .append(Component.text(this.computeTransitionSuffix(phase), HardlandsColor.LIGHT_GRAY));

        if (phase != Phase.SURVIVAL) {
            this.bossBar.name(content);
            return;
        }

        TextColor swordColor = this.isPvpEnabled()
                ? HardlandsColor.PRIMARY
                : NamedTextColor.WHITE;

        this.bossBar.name(
                Component.text("☠ ", swordColor)
                        .append(content)
                        .append(Component.text(" ☠", swordColor))
        );
    }

    private String computePrefix(Phase phase) {
        return this.prefix.isEmpty()
                ? phase.getLabel()
                : this.prefix;
    }

    private Component computePrefixComponent(Phase phase) {
        String label = this.prefix.isEmpty() ? phase.getLabel() : this.prefix;
        if (phase != Phase.SURVIVAL) return Component.text(label);

        TextColor swordColor = this.isPvpEnabled()
                ? HardlandsColor.PRIMARY
                : HardlandsColor.LIGHT_GRAY;

        return Component.text("⚔ ", swordColor)
                .append(Component.text(label))
                .append(Component.text(" ⚔", swordColor));
    }

    private boolean isPvpEnabled() {
        int gracePeriodMinute = this.gameManager.getGracePeriodMinuteOption().getValue();
        int pvpMinute = this.gameManager.getPvpMinuteOption().getValue();
        int pvpStartSeconds = (pvpMinute - gracePeriodMinute) * 60;

        return this.chronometer >= pvpStartSeconds;
    }

    private String computeSuffix() {
        return this.suffix.isEmpty()
                ? TimeFormatter.format(Duration.ofSeconds(this.chronometer))
                : this.suffix;
    }

    private String computeTransitionSuffix(Phase phase) {
        if (phase == Phase.SURVIVAL) {
            return " » " + TimeFormatter.format(Duration.ofMinutes(this.getSurvivalTargetMinute()));
        }

        boolean showsTargetMinute = phase.getProgression() == Phase.Progression.DRAIN
                || phase == Phase.BORDER_SHRINK
                || phase == Phase.FINAL_SHRINK;

        if (!showsTargetMinute) return "";

        Integer minute = this.findNextMinute(phase);
        return minute == null
                ? ""
                : " » " + TimeFormatter.format(Duration.ofMinutes(minute));
    }

    private int getSurvivalTargetMinute() {
        int survivalMinute = this.gameManager.getGracePeriodMinuteOption().getValue();
        int pvpMinute = this.gameManager.getPvpMinuteOption().getValue();
        int borderShrinkMinute = this.gameManager.getBorderShrinkMinuteOption().getValue();

        int pvpStartSeconds = (pvpMinute - survivalMinute) * 60;

        return this.chronometer < pvpStartSeconds
                ? pvpMinute
                : borderShrinkMinute;
    }

    private Integer findNextMinute(Phase phase) {
        Phase[] phases = Phase.values();

        for (int index = phase.ordinal() + 1; index < phases.length; index++) {
            Integer minute = phases[index].getMinute(this.gameManager);
            if (minute != null) return minute;
        }

        return null;
    }

    private boolean isProgressComplete(Phase phase) {
        return switch (phase.getProgression()) {
            case FILL -> this.bossBar.progress() >= 1.0F;
            case DRAIN -> this.bossBar.progress() <= 0.0F;
            case EMPTY, FULL -> false;
        };
    }

    private double calculateProgressDelta(Phase phase) {
        Integer startMinute = phase.getMinute(this.gameManager);
        if (startMinute == null) return 0.0D;

        Integer nextMinute = this.findNextMinute(phase);
        if (nextMinute == null) return 0.0D;

        int duration = (nextMinute - startMinute) * 60;
        return duration > 0 ? 1.0D / duration : 0.0D;
    }

    public synchronized void completeCurrentPhase() {
        Phase phase = this.gameManager.getPhase();

        this.progressCondition = () -> true;

        switch (phase.getProgression()) {
            case FILL -> this.setProgress(1.0F);
            case DRAIN -> this.setProgress(0.0F);

            case EMPTY, FULL -> {
                phase.next().ifPresent(this.gameManager::changePhase);
                return;
            }
        }

        this.tryAdvance(phase);
    }

    public synchronized void setChronometer(int seconds) {
        this.chronometer = Math.max(0, seconds);
        this.updateName(this.gameManager.getPhase());
    }
}