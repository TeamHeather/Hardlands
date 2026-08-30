package team.heather.hardlands.game;

import java.util.Optional;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.phase.Phase;

public final class GameTimer {

    private final GameManager gameManager;
    private final BossBar bossBar = BossBar.bossBar(
            Component.empty(),
            1.0F,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );

    private double progressDelta;
    private boolean ended;

    public GameTimer(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public synchronized void updateProgress(Plugin plugin) {
        Bukkit.getScheduler().runTask(plugin, () ->         {
            if (this.progressDelta == 0.0D || this.ended) {
                return;
            }

            double progress = this.bossBar.progress() + this.progressDelta;
            double clampedProgress = Math.clamp(progress, 0.0D, 1.0D);

            this.bossBar.progress((float) clampedProgress);

            if (clampedProgress == 0.0D || clampedProgress == 1.0D) {
                this.ended = true;
                this.gameManager.getPhase()
                        .next()
                        .ifPresent(this.gameManager::changePhase);
            }
        });
    }

    public void updateState() {
        Phase phase = this.gameManager.getPhase();
        double phaseProgressDelta = calculateProgressDelta(this.gameManager, phase);

        this.ended = false;

        this.bossBar.name(Component.text(phase.getLabel()));
        this.bossBar.color(phase.getCategory().getBossBarColor());

        switch (phase.getProgression()) {
            case EMPTY -> {
                this.bossBar.progress(0.0F);
                this.progressDelta = 0.0D;
            }
            case FULL -> {
                this.bossBar.progress(1.0F);
                this.progressDelta = 0.0D;
            }
            case FILL -> {
                this.bossBar.progress(0.0F);
                this.progressDelta = phaseProgressDelta;
            }
            case DRAIN -> {
                this.bossBar.progress(1.0F);
                this.progressDelta = -phaseProgressDelta;
            }
        }
    }

    public void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    public void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    private static double calculateProgressDelta(GameManager gameManager, Phase phase) {
        return getDurationInSeconds(gameManager, phase)
                .filter(duration -> duration > 0)
                .map(duration -> 1.0D / duration)
                .orElse(0.0D);
    }

    private static Optional<Integer> getDurationInSeconds(GameManager gameManager, Phase phase) {
        Option<Integer> startMinute = phase.getMinuteOption(gameManager);

        if (startMinute == null) {
            return Optional.empty();
        }

        Phase[] phases = Phase.values();

        for (int index = phase.ordinal() + 1; index < phases.length; index++) {
            Option<Integer> nextStartMinute = phases[index].getMinuteOption(gameManager);

            if (nextStartMinute != null) {
                int duration = nextStartMinute.getValue() - startMinute.getValue();
                return Optional.of(duration * 60);
            }
        }

        return Optional.empty();
    }
}