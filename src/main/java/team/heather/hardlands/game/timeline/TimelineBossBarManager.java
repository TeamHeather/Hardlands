package team.heather.hardlands.game.timeline;

import java.time.Duration;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import team.heather.hardlands.core.data.json.LocalTimeAdapter;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.util.HardlandsColor;
import team.heather.hardlands.util.text.TimeFormatter;

final class TimelineBossBarManager {

    private final GameManager gameManager;
    private final BossBar bossBar;

    private String labelPrefix = "";
    private String labelSuffix = "";

    TimelineBossBarManager(GameManager gameManager) {
        this.gameManager = gameManager;
        this.bossBar = BossBar.bossBar(
                Component.empty(),
                0.0F,
                BossBar.Color.WHITE,
                BossBar.Overlay.PROGRESS
        );
    }

    void reset(Phase phase) {
        this.bossBar.color(phase.getStage().getBossBarColor());
        this.labelPrefix = "";
        this.labelSuffix = "";

        if (phase.isPercentageProgressDisplayEnabled()) {
            this.labelSuffix = "0.0%";
        }
    }

    void updateForStaticProgression(Phase phase, TimelineCounterTracker timelineCounterTracker) {
        this.setProgress(switch (phase.getProgression()) {
            case EMPTY, FILL -> 0.0F;
            case DRAIN, FULL -> 1.0F;
        });

        this.updateLabel(phase, timelineCounterTracker);
    }

    void updateForTimedProgression(Phase phase, TimelineCounterTracker timelineCounterTracker) {
        this.updateProgressForTimedProgression(phase, timelineCounterTracker);
        this.updateLabel(phase, timelineCounterTracker);
    }

    void updateForPercentageProgression(Phase phase, float percentage, TimelineCounterTracker timelineCounterTracker) {
        this.setProgress(percentage / 100.0F);
        this.labelSuffix = "%.1f%%".formatted(percentage);
        this.updateLabel(phase, timelineCounterTracker);
    }

    void updateForWaitingPhase(Phase phase, TimelineClockProgress.State state) {
        this.setProgress(state.progress());
        this.bossBar.name(this.buildLabel(
                phase.getLabel(),
                LocalTimeAdapter.format(state.currentTime()),
                LocalTimeAdapter.format(state.targetTime())
        ));
    }

    void updateLabel(Phase phase, TimelineCounterTracker timelineCounterTracker) {
        if (phase.isWaitingCountdownEnabled()) return;

        if (phase.isChronometerAdvanceEnabled()) {
            this.bossBar.name(Component.text(phase.getLabel(), NamedTextColor.WHITE));
            return;
        }

        Component label = this.buildLabel(
                this.labelPrefix.isEmpty() ? phase.getLabel() : this.labelPrefix,
                this.computeSuffix(timelineCounterTracker),
                this.computeTarget(phase, timelineCounterTracker)
        );

        if (!phase.isPvpTransitionEnabled()) {
            this.bossBar.name(label);
            return;
        }

        TextColor skullColor = timelineCounterTracker.isPvpEnabled()
                ? HardlandsColor.PRIMARY
                : HardlandsColor.LIGHT_GRAY;

        this.bossBar.name(
                Component.text("☠ ", skullColor)
                        .append(label)
                        .append(Component.text(" ☠", skullColor))
        );
    }

    void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    void setLabelPrefix(String labelPrefix, Phase phase, TimelineCounterTracker timelineCounterTracker) {
        this.labelPrefix = labelPrefix;
        this.updateLabel(phase, timelineCounterTracker);
    }

    void setLabelSuffix(String labelSuffix, Phase phase, TimelineCounterTracker timelineCounterTracker) {
        this.labelSuffix = labelSuffix;
        this.updateLabel(phase, timelineCounterTracker);
    }

    private void updateProgressForTimedProgression(Phase phase, TimelineCounterTracker timelineCounterTracker) {
        if (phase.isPvpTransitionEnabled()) {
            this.updateProgressForSurvival(timelineCounterTracker);
            return;
        }

        Integer startMinute = phase.getMinute(this.gameManager);
        Integer endMinute = phase.getNextMinute(this.gameManager);

        if (startMinute == null || endMinute == null) {
            this.setProgress(phase.getProgression() == Phase.Progression.FULL
                    ? 1.0F
                    : 0.0F);
            return;
        }

        float progress = timelineCounterTracker.getProgress(startMinute, endMinute);

        this.setProgress(switch (phase.getProgression()) {
            case EMPTY -> 0.0F;
            case FILL -> progress;
            case DRAIN -> 1.0F - progress;
            case FULL -> 1.0F;
        });
    }

    private void updateProgressForSurvival(TimelineCounterTracker timelineCounterTracker) {
        int gracePeriodMinute = this.gameManager.getGracePeriodMinuteOption().getValue();
        int pvpMinute = this.gameManager.getPvpMinuteOption().getValue();
        int borderShrinkMinute = this.gameManager.getBorderShrinkMinuteOption().getValue();

        float progress = timelineCounterTracker.isPvpEnabled()
                ? timelineCounterTracker.getProgress(pvpMinute, borderShrinkMinute)
                : timelineCounterTracker.getProgress(gracePeriodMinute, pvpMinute);

        this.setProgress(1.0F - progress);
    }

    private Component buildLabel(String label, String value, String target) {
        Component component = Component.text(label, NamedTextColor.WHITE)
                .append(Component.text(" » ", NamedTextColor.WHITE))
                .append(Component.text(value, NamedTextColor.WHITE));

        return target.isEmpty()
                ? component
                : component.append(Component.text(" → ", NamedTextColor.WHITE))
                  .append(Component.text(target, NamedTextColor.WHITE));
    }

    private String computeSuffix(TimelineCounterTracker timelineCounterTracker) {
        return this.labelSuffix.isEmpty()
                ? TimeFormatter.format(Duration.ofSeconds(timelineCounterTracker.get()))
                : this.labelSuffix;
    }

    private String computeTarget(Phase phase, TimelineCounterTracker timelineCounterTracker) {
        if (phase.isPvpTransitionEnabled()) {
            return TimeFormatter.format(Duration.ofMinutes(
                    timelineCounterTracker.getSurvivalTargetMinute()
            ));
        }

        if (!phase.isTargetMinuteDisplayEnabled()) return "";

        Integer minute = phase.getNextMinute(this.gameManager);

        return minute != null
                ? TimeFormatter.format(Duration.ofMinutes(minute))
                : "";
    }

    private void setProgress(float progress) {
        this.bossBar.progress(Math.clamp(progress, 0.0F, 1.0F));
    }
}