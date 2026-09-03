package team.heather.hardlands.game.phase;

import java.time.Duration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.util.TextFormatters;

public final class StartCountdown implements AutoCloseable {

    private static final int START_SECONDS = 20;
    private static final int TITLE_SECONDS = 10;

    private final Hardlands plugin;
    private final String title;
    private final Component subtitle;

    @Nullable private BukkitTask task;

    private int remainingSeconds;
    private int glintFrame;
    private int glintDirection;

    public StartCountdown(Hardlands plugin, Player host, int matchNumber) {
        this.plugin = plugin;
        this.title = TextFormatters.TINY_CAPS.format("Hardlands") + " #" + matchNumber;
        this.subtitle = TextFormatters.TINY_CAPS.formatColored("hosted by")
                .color(HardlandsColor.LIGHT_GRAY)
                .append(Component.space())
                .append(TextFormatters.MINI_MESSAGE.format(TextFormatters.USERNAME.format(host)));
    }

    public void start() {
        if (this.task != null) {
            throw new IllegalStateException("Start countdown is already running");
        }

        this.remainingSeconds = START_SECONDS;
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickCountdown, 0L, 20L);
    }

    @Override
    public void close() {
        if (this.task != null) {
            this.task.cancel();
            this.task = null;
        }
    }

    public boolean isRunning() {
        return this.task != null;
    }

    private void tickCountdown() {
        if (this.plugin.getGameManager().getPhase() != Phase.SCATTER) {
            this.close();
            return;
        }

        if (this.remainingSeconds == 0) {
            this.startMatch();
            return;
        }

        this.showCountdown();
        this.remainingSeconds--;
    }

    private void showCountdown() {
        Component label = TextFormatters.TINY_CAPS.formatColored("la partida comienza en")
                .color(HardlandsColor.LIGHT_GRAY);

        Component actionBar = label
                .append(Component.space())
                .append(Component.text(this.remainingSeconds + "s", HardlandsColor.HARDLANDS));

        Title title = this.remainingSeconds <= TITLE_SECONDS
                ? Title.title(
                Component.text(this.remainingSeconds, HardlandsColor.HARDLANDS),
                label,
                Title.Times.times(
                        Duration.ZERO,
                        Duration.ofMillis(900),
                        Duration.ofMillis(100)
                )
        )
                : null;

        float progress = (TITLE_SECONDS - this.remainingSeconds) / (float) TITLE_SECONDS;
        float volume = 0.20F + 0.65F * Math.max(0.0F, progress);
        float pitch = 1.0F + 0.08F * Math.max(0.0F, progress);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(actionBar);

            if (title == null) {
                continue;
            }

            player.showTitle(title);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, volume, pitch);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, volume * 0.25F, pitch + 0.05F);
        }
    }

    private void startMatch() {
        this.close();
        this.plugin.getGameManager().transitionTo(Phase.SURVIVAL);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendActionBar(Component.empty());
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.75F, 1.12F);
        }

        this.glintFrame = -3;
        this.glintDirection = 1;
        this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::tickGlint, 0L, 1L);
    }

    private void tickGlint() {
        if (this.plugin.getGameManager().getPhase() != Phase.SURVIVAL) {
            this.close();
            return;
        }

        this.showTitle(
                this.createTitle(this.glintFrame),
                Title.Times.times(
                        Duration.ZERO,
                        Duration.ofMillis(100),
                        Duration.ZERO
                )
        );

        if (this.glintDirection > 0 && this.glintFrame >= this.title.length() + 2) {
            this.glintDirection = -1;
        } else if (this.glintDirection < 0 && this.glintFrame <= -3) {
            this.finishGlint();
            return;
        }

        this.glintFrame += this.glintDirection;
    }

    private void finishGlint() {
        this.close();

        this.showTitle(
                this.createTitle(),
                Title.Times.times(
                        Duration.ZERO,
                        Duration.ofSeconds(4),
                        Duration.ofMillis(300)
                )
        );
    }

    private Component createTitle() {
        return this.createTitle(-3);
    }

    private Component createTitle(int glintFrame) {
        TextComponent.Builder result = Component.text();

        for (int index = 0; index < this.title.length(); index++) {
            float position = index * 2.0F / (this.title.length() - 1);

            TextColor color = position <= 1.0F
                    ? TextColor.lerp(position, HardlandsColor.RED.primary(), HardlandsColor.RED.secondary())
                    : TextColor.lerp(
                    position - 1.0F,
                    HardlandsColor.RED.secondary(),
                    HardlandsColor.RED.tertiary()
            );

            float strength = Math.max(0.0F, 1.0F - Math.abs(index - glintFrame) / 3.0F);

            if (strength > 0.0F) {
                color = TextColor.lerp(strength * 0.85F, color, NamedTextColor.WHITE);
            }

            result.append(Component.text(this.title.charAt(index), color));
        }

        return result.build();
    }

    private void showTitle(Component title, Title.Times times) {
        Title display = Title.title(title, this.subtitle, times);
        Bukkit.getOnlinePlayers().forEach(player -> player.showTitle(display));
    }
}