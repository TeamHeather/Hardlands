package team.heather.hardlands.game;

import java.time.Duration;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntUnaryOperator;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.util.text.TimeFormatter;

public final class GameTimer {

    private final GameManager gameManager;
    private final BossBar bossBar = BossBar.bossBar(
            Component.empty(),
            1.0F,
            BossBar.Color.WHITE,
            BossBar.Overlay.PROGRESS
    );

    private int chronometer;
    private double progressDelta;
    private boolean ended;

    public GameTimer(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void updateProgress(Plugin plugin) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (this.progressDelta == 0.0D || this.ended) {
                return;
            }

            Phase phase = this.gameManager.getPhase();

            this.bossBar.name(Component.text(
                    phase.getLabel() + " >> " + TimeFormatter.format(Duration.ofSeconds(this.chronometer))
            ));

            this.modifyProgress(value -> value + this.progressDelta);
            this.modifyChronometer(value -> value + 1);

            double progress = this.bossBar.progress();
            if (progress == 0.0D || progress == 1.0D) {
                this.ended = true;
                phase.next().ifPresent(this.gameManager::changePhase);
            }
        });
    }

    public void updateState() {
        Phase phase = this.gameManager.getPhase();

        this.ended = false;
        this.bossBar.name(Component.text(phase.getLabel()));
        this.bossBar.color(phase.getStage().getBossBarColor());

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
                this.progressDelta = this.calculateProgressDelta(phase);
            }
            case DRAIN -> {
                this.bossBar.progress(1.0F);
                this.progressDelta = -this.calculateProgressDelta(phase);
            }
        }
    }

    void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    void modifyChronometer(IntUnaryOperator operation) {
        this.chronometer = operation.applyAsInt(chronometer);
    }

    void modifyProgress(DoubleUnaryOperator operation) {
        double progress = operation.applyAsDouble(bossBar.progress());
        this.bossBar.progress((float) Math.clamp(progress, 0.0D, 1.0D));
    }

    void resetChronometer() {
        this.modifyChronometer(_ -> 0);
    }

    private double calculateProgressDelta(Phase phase) {
        Option<Integer> startMinute = phase.getMinuteOption(this.gameManager);

        if (startMinute == null) {
            return 0.0D;
        }

        Phase[] phases = Phase.values();

        for (int index = phase.ordinal() + 1; index < phases.length; index++) {
            Option<Integer> nextMinute = phases[index].getMinuteOption(this.gameManager);

            if (nextMinute == null) {
                continue;
            }

            int duration = (nextMinute.getValue() - startMinute.getValue()) * 60;
            return duration > 0 ? 1.0D / duration : 0.0D;
        }

        return 0.0D;
    }
}