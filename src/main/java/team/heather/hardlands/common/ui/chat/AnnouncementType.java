package team.heather.hardlands.common.ui.chat;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import team.heather.hardlands.common.ui.HardlandsColor;

import java.util.List;

public enum AnnouncementType {

    NEUTRAL("⌛", HardlandsColor.GRAY, Sound.BLOCK_NOTE_BLOCK_HAT, 1.25F),
    GAMEPLAY("❣", HardlandsColor.GREEN, Sound.ITEM_TRIDENT_RETURN, 0.75F),
    DANGER("☠", HardlandsColor.RED, Sound.BLOCK_BELL_USE, 0.85F),
    STAR("★", HardlandsColor.YELLOW, Sound.BLOCK_NOTE_BLOCK_CHIME, 1.4F),

    ;

    private static final float VOLUME = 1.0F;

    private final String icon;
    private final HardlandsColor colors;
    private final Sound sound;
    private final float pitch;

    AnnouncementType(String icon, HardlandsColor colors, Sound sound, float pitch) {
        this.icon = icon;
        this.colors = colors;
        this.sound = sound;
        this.pitch = pitch;
    }

    public void broadcast(String message, Object... arguments) {
        this.broadcast(message, List.of(), arguments);
    }

    public void broadcast(String message, List<Sound> additionalSounds, Object... arguments) {
        ChatMessenger.broadcastFramed(this.icon, this.colors, message.formatted(arguments));

        Bukkit.getOnlinePlayers().forEach(player -> {
            this.playSound(player, this.sound);
            additionalSounds.forEach(sound -> this.playSound(player, sound));
        });
    }

    private void playSound(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, VOLUME, this.pitch);
    }
}