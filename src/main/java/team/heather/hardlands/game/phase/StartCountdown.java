package team.heather.hardlands.game.phase;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.world.ScatterManager;

public final class StartCountdown implements AutoCloseable {

    private static final int START_SECONDS = 10;

    private static final long INITIAL_DELAY_TICKS = 0L;
    private static final long PERIOD_TICKS = 20L;

    private static final float MIN_SOUND_VOLUME = 0.15F;
    private static final float MAX_SOUND_VOLUME = 1.0F;
    private static final float SOUND_PITCH = 1.0F;

    private static final Title.Times COUNTDOWN_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ZERO
    );

    private static final Title.Times START_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ZERO
    );

    private final Hardlands plugin;
    private final ScatterManager scatterManager;

    private BukkitTask task;
    private int remainingSeconds;

    public StartCountdown(Hardlands plugin, ScatterManager scatterManager) {
        this.plugin = plugin;
        this.scatterManager = scatterManager;
    }

    public void start() {
        if (this.task != null) {
            throw new IllegalStateException("Scatter countdown is already running");
        }

        this.remainingSeconds = START_SECONDS;
        this.task = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                this::tick,
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS
        );
    }

    @Override
    public void close() {
        if (this.task == null) return;

        this.task.cancel();
        this.task = null;
    }

    public boolean isRunning() {
        return this.task != null;
    }

    private void tick() {
        if (this.plugin.getGameManager().getPhase() != Phase.SCATTER) {
            this.close();
            return;
        }

        if (!this.scatterManager.isCompleted()) return;

        if (this.remainingSeconds <= 0) {
            this.finish();
            return;
        }

        this.showCountdown();
        this.remainingSeconds--;
    }

    private void finish() {
        this.close();

        Title title = Title.title(
                Component.text("¡La partida ha comenzado!"),
                Component.empty(),
                START_TIMES
        );

        Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(title));

        this.plugin.getGameManager().changePhase(Phase.SURVIVAL);
    }

    private void showCountdown() {
        Title title = Title.title(
                Component.text(this.remainingSeconds),
                Component.text("La partida comienza en"),
                COUNTDOWN_TIMES
        );

        float volume = this.computeSoundVolume();

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.showTitle(title);
            this.playCountdownSound(player, volume);
        });
    }

    private void playCountdownSound(Player player, float volume) {
        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                volume,
                SOUND_PITCH
        );
    }

    private float computeSoundVolume() {
        float progress = (START_SECONDS - this.remainingSeconds)
                / (float) (START_SECONDS - 1);

        return MIN_SOUND_VOLUME
                + (MAX_SOUND_VOLUME - MIN_SOUND_VOLUME) * progress;
    }
}