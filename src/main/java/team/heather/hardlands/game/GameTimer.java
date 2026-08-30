package team.heather.hardlands.game;

import java.time.Duration;
import java.util.function.BooleanSupplier;
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

    private BooleanSupplier condition;
    private String prefix = "";
    private String suffix = "";
    private double progressDelta;
    private int chronometer;
    private boolean ended;

    public GameTimer(GameManager gameManager) {
        this.gameManager = gameManager;
        this.condition = () -> this.progressDelta != 0.0D;
    }

    public void updateProgress(Plugin plugin) {
        Bukkit.getScheduler().runTask(plugin, this::tick);
    }

    public synchronized void updateState() {
        Phase phase = this.gameManager.getPhase();

        this.ended = false;
        this.bossBar.color(phase.getStage().getBossBarColor());
        this.setName(this.computePrefix(phase), this.computeSuffix());

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

    synchronized void addViewer(Player player) {
        this.bossBar.addViewer(player);
    }

    synchronized void removeViewer(Player player) {
        this.bossBar.removeViewer(player);
    }

    synchronized void modifyChronometer(IntUnaryOperator operation) {
        this.chronometer = operation.applyAsInt(this.chronometer);
    }

    synchronized void modifyProgress(DoubleUnaryOperator operation) {
        double progress = operation.applyAsDouble(this.bossBar.progress());
        this.setProgress((float) progress);
    }

    synchronized void resetChronometer() {
        this.chronometer = 0;
    }

    synchronized void setProgress(float progress) {
        this.bossBar.progress(Math.clamp(progress, 0.0F, 1.0F));
    }

    synchronized void setPrefix(String prefix) {
        this.prefix = prefix;
        this.updateName();
    }

    synchronized void setSuffix(String suffix) {
        this.suffix = suffix;
        this.updateName();
    }

    synchronized void setCondition(BooleanSupplier condition) {
        this.condition = condition;
    }

    synchronized void resetTickCondition() {
        this.condition = () -> this.progressDelta != 0.0D;
    }

    private synchronized void tick() {
        if (this.ended || !this.condition.getAsBoolean()) return;

        Phase phase = this.gameManager.getPhase();

        if (this.progressDelta != 0.0D) {
            this.chronometer++;
            this.modifyProgress(value -> value + this.progressDelta);
            this.setName(this.computePrefix(phase), this.computeSuffix());
        }

        if (!this.isProgressComplete(phase)) return;

        this.ended = true;
        phase.next().ifPresent(this.gameManager::changePhase);
    }

    private void setName(String prefix, String suffix) {
        this.prefix = prefix;
        this.suffix = suffix;
        this.updateName();
    }

    private void updateName() {
        this.bossBar.name(Component.text(this.prefix + " >> " + this.suffix));
    }

    private String computePrefix(Phase phase) {
        return phase.getLabel();
    }

    private String computeSuffix() {
        return TimeFormatter.format(Duration.ofSeconds(this.chronometer));
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

        Phase[] phases = Phase.values();

        for (int index = phase.ordinal() + 1; index < phases.length; index++) {
            Integer nextMinute = phases[index].getMinute(this.gameManager);
            if (nextMinute == null) continue;

            int duration = (nextMinute - startMinute) * 60;
            return duration > 0 ? 1.0D / duration : 0.0D;
        }

        return 0.0D;
    }
}