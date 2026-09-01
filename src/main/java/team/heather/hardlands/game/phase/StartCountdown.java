package team.heather.hardlands.game.phase;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.world.ScatterManager;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.util.TextFormatters;

public final class StartCountdown implements AutoCloseable {

    private static final int START_SECONDS = 10;

    private static final long INITIAL_DELAY_TICKS = 0L;
    private static final long COUNTDOWN_PERIOD_TICKS = 20L;
    private static final long TYPEWRITER_PERIOD_TICKS = 2L;

    private static final float MIN_SOUND_VOLUME = 0.20F;
    private static final float MAX_SOUND_VOLUME = 0.85F;

    private static final float MIN_SOUND_PITCH = 1.00F;
    private static final float MAX_SOUND_PITCH = 1.08F;

    private static final float ACCENT_VOLUME_MULTIPLIER = 0.25F;
    private static final float ACCENT_PITCH_OFFSET = 0.05F;

    private static final float TYPEWRITER_VOLUME = 0.12F;
    private static final float TYPEWRITER_PITCH = 1.45F;

    private static final String HOST_LABEL = "Hosteado por:";

    private static final Component GAME_TITLE = MiniMessage.miniMessage().deserialize(Hardlands.LABEL);

    private static final Title.Times COUNTDOWN_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(900),
            Duration.ofMillis(100)
    );

    private static final Title.Times TYPEWRITER_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofMillis(250),
            Duration.ZERO
    );

    private static final Title.Times START_TIMES = Title.Times.times(
            Duration.ZERO,
            Duration.ofSeconds(2),
            Duration.ofMillis(300)
    );

    private final Hardlands plugin;
    private final ScatterManager scatterManager;

    @Nullable
    private BukkitTask countdownTask;

    @Nullable
    private BukkitTask typewriterTask;

    @Nullable
    private Player host;

    private int remainingSeconds;
    private int typedCharacters;

    public StartCountdown(
            Hardlands plugin,
            ScatterManager scatterManager
    ) {
        this.plugin = plugin;
        this.scatterManager = scatterManager;
    }

    public void start() {
        if (this.countdownTask != null) {
            throw new IllegalStateException(
                    "Scatter countdown is already running"
            );
        }

        this.host = Bukkit.getOnlinePlayers()
                .stream()
                .findFirst()
                .orElse(null);

        this.remainingSeconds = START_SECONDS;

        this.countdownTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                this::tickCountdown,
                INITIAL_DELAY_TICKS,
                COUNTDOWN_PERIOD_TICKS
        );
    }

    @Override
    public void close() {
        this.stopCountdown();
        this.stopTypewriter();
    }

    public boolean isRunning() {
        return this.countdownTask != null;
    }

    private void tickCountdown() {
        if (this.plugin.getGameManager().getPhase() != Phase.SCATTER) {
            this.stopCountdown();
            return;
        }

        if (!this.scatterManager.isCompleted()) {
            return;
        }

        if (this.remainingSeconds <= 0) {
            this.finish();
            return;
        }

        this.showCountdown();
        this.remainingSeconds--;
    }

    private void finish() {
        this.stopCountdown();

        this.plugin.getGameManager().transitionTo(Phase.SURVIVAL);
        this.startTypewriter();
    }

    private void showCountdown() {
        Title title = Title.title(
                Component.text(
                        this.remainingSeconds,
                        HardlandsColor.HARDLANDS
                ),
                TextFormatters.TINY_CAPS.formatColored("La partida comienza en")
                        .color(HardlandsColor.LIGHT_GRAY),
                COUNTDOWN_TIMES
        );

        float progress = this.computeProgress();
        float volume = this.computeSoundVolume(progress);
        float pitch = this.computeSoundPitch(progress);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            this.playCountdownSounds(player, volume, pitch);
        }
    }

    private void startTypewriter() {
        this.stopTypewriter();
        this.typedCharacters = 0;

        this.typewriterTask = Bukkit.getScheduler().runTaskTimer(
                this.plugin,
                this::tickTypewriter,
                INITIAL_DELAY_TICKS,
                TYPEWRITER_PERIOD_TICKS
        );
    }

    private void tickTypewriter() {
        if (this.typedCharacters < HOST_LABEL.length()) {
            this.typedCharacters++;

            String visibleText = HOST_LABEL.substring(
                    0,
                    this.typedCharacters
            );

            this.showStartTitle(
                    TextFormatters.TINY_CAPS.formatColored(visibleText)
                            .color(HardlandsColor.LIGHT_GRAY),
                    TYPEWRITER_TIMES
            );

            this.playTypewriterSound();
            return;
        }

        this.showFinalStartTitle();
        this.playStartSound();
        this.stopTypewriter();
    }

    private void showFinalStartTitle() {
        Component subtitle = TextFormatters.TINY_CAPS.formatColored(HOST_LABEL)
                .color(HardlandsColor.LIGHT_GRAY);

        if (this.host != null) {
            Component formattedHost = TextFormatters.MINI_MESSAGE.format(TextFormatters.USERNAME.format(this.host))
                    .color(HardlandsColor.LIGHT_GRAY);

            subtitle = subtitle
                    .append(Component.space())
                    .append(formattedHost);
        }

        this.showStartTitle(subtitle, START_TIMES);
    }

    private void showStartTitle(
            Component subtitle,
            Title.Times times
    ) {
        Title title = Title.title(
                GAME_TITLE,
                subtitle,
                times
        );

        Bukkit.getOnlinePlayers()
                .forEach(player -> player.showTitle(title));
    }

    private void playCountdownSounds(
            Player player,
            float volume,
            float pitch
    ) {
        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                volume,
                pitch
        );

        player.playSound(
                player.getLocation(),
                Sound.BLOCK_NOTE_BLOCK_HAT,
                volume * ACCENT_VOLUME_MULTIPLIER,
                pitch + ACCENT_PITCH_OFFSET
        );
    }

    private void playTypewriterSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    TYPEWRITER_VOLUME,
                    TYPEWRITER_PITCH
            );
        }
    }

    private void playStartSound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    0.75F,
                    1.12F
            );
        }
    }

    private void stopCountdown() {
        if (this.countdownTask == null) {
            return;
        }

        this.countdownTask.cancel();
        this.countdownTask = null;
    }

    private void stopTypewriter() {
        if (this.typewriterTask == null) {
            return;
        }

        this.typewriterTask.cancel();
        this.typewriterTask = null;
    }

    private float computeProgress() {
        return (START_SECONDS - this.remainingSeconds)
                / (float) (START_SECONDS - 1);
    }

    private float computeSoundVolume(float progress) {
        return MIN_SOUND_VOLUME
                + (MAX_SOUND_VOLUME - MIN_SOUND_VOLUME) * progress;
    }

    private float computeSoundPitch(float progress) {
        return MIN_SOUND_PITCH
                + (MAX_SOUND_PITCH - MIN_SOUND_PITCH) * progress;
    }
}