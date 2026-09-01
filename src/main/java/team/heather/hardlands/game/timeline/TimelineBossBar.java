package team.heather.hardlands.game.timeline;

import java.time.Duration;
import java.time.LocalTime;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import team.heather.hardlands.core.data.json.LocalTimeAdapter;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.ui.HardlandsColor;
import team.heather.hardlands.util.text.TimeFormatter;
import team.heather.hardlands.util.text.TinyCaps;

final class TimelineBossBar {

    private final GameManager gameManager;
    private final BossBar bossBar;

    TimelineBossBar(GameManager gameManager) {
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
        this.bossBar.progress(phase.getProgression().apply(0.0F));
        this.bossBar.name(Component.text(phase.getLabel(), NamedTextColor.WHITE));
    }

    void updatePercentage(Phase phase, float percentage) {
        float progress = Math.clamp(percentage / 100.0F, 0.0F, 1.0F);

        this.bossBar.progress(phase.getProgression().apply(progress));
        this.bossBar.name(this.buildLabel(phase.getLabel(), "%.1f%%".formatted(percentage), ""));
    }

    void updateClock(Phase phase, float progress, LocalTime currentTime, LocalTime targetTime) {
        this.bossBar.progress(phase.getProgression().apply(progress));
        this.bossBar.name(this.buildLabel(
                phase.getLabel(),
                LocalTimeAdapter.HHMMSS_FORMATTER.format(currentTime),
                LocalTimeAdapter.format(targetTime)
        ));
    }

    void updateCounter(Phase phase, int seconds) {
        float progress = this.computeCounterProgress(phase, seconds);
        Component label = this.buildLabel(
                phase.getLabel(),
                TimeFormatter.format(Duration.ofSeconds(seconds)),
                this.computeTarget(phase, seconds)
        );

        this.bossBar.progress(phase.getProgression().apply(progress));

        if (phase != Phase.SURVIVAL) {
            this.bossBar.name(label);
            return;
        }

        TextColor skullColor = this.isPvpEnabled(seconds)
                ? HardlandsColor.HARDLANDS
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

    private float computeCounterProgress(Phase phase, int seconds) {
        Integer startMinute = phase.getMinute(this.gameManager);
        Integer endMinute = phase.getNextMinute(this.gameManager);

        if (startMinute == null || endMinute == null) return 0.0F;
        return computeProgress(seconds, startMinute, endMinute);
    }

    private String computeTarget(Phase phase, int seconds) {
        if (phase == Phase.SURVIVAL) {
            int targetMinute = this.isPvpEnabled(seconds)
                    ? this.gameManager.getBorderShrinkMinuteOption().getValue()
                    : this.gameManager.getPvpMinuteOption().getValue();

            return formatMinute(targetMinute);
        }

        if (phase.getProgression() == Phase.Progression.FULL) return "";

        Integer endMinute = phase.getNextMinute(this.gameManager);
        return endMinute == null ? "" : formatMinute(endMinute);
    }

    private boolean isPvpEnabled(int seconds) {
        int pvpMinute = this.gameManager.getPvpMinuteOption().getValue();
        return seconds >= pvpMinute * 60;
    }

    private Component buildLabel(String label, String value, String target) {
        Component component = Component.text(TinyCaps.format(label))
                .append(Component.text(" » ", NamedTextColor.WHITE))
                .append(Component.text(value, HardlandsColor.HARDLANDS));

        if (target.isEmpty()) return component;

        return component
                .append(Component.text(" → ", NamedTextColor.WHITE))
                .append(Component.text(target, NamedTextColor.WHITE));
    }

    private static float computeProgress(int seconds, int startMinute, int endMinute) {
        int start = startMinute * 60;
        int end = endMinute * 60;

        if (end <= start) return 1.0F;
        return Math.clamp((float) (seconds - start) / (end - start), 0.0F, 1.0F);
    }

    private static String formatMinute(int minute) {
        return TimeFormatter.format(Duration.ofMinutes(minute));
    }
}