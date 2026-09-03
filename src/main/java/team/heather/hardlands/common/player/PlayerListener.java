package team.heather.hardlands.common.player;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.util.TextFormatters;

public class PlayerListener implements Listener {

    private static final String KILL_MESSAGE = "☠ {¡Has eliminado a} %s{!} ☠";

    @EventHandler
    private void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Entity causingEntity = event.getDamageSource().getCausingEntity();

        updateDeathMessage(event, player, causingEntity);
        sendKillMessage(player, causingEntity);
        playDeathSounds(player.getLocation());
        placeTombstone(player);
    }

    private static void updateDeathMessage(PlayerDeathEvent event, Player player, Entity causingEntity) {
        Component deathMessage = event.deathMessage();
        if (deathMessage == null) {
            return;
        }

        String text = TextFormatters.PLAIN_TEXT.format(deathMessage);

        if (causingEntity instanceof Player killer) {
            text = TextFormatters.USERNAME.format(text, killer);
        }

        text = TextFormatters.USERNAME.format(text, player);
        event.deathMessage(TextFormatters.MINI_MESSAGE.format("<#B22222>" + text));
    }

    private static void sendKillMessage(Player victim, Entity causingEntity) {
        if (!(causingEntity instanceof Player killer) || killer == victim) {
            return;
        }

        String victimName = TextFormatters.USERNAME.format(victim);
        killer.sendActionBar(TextFormatters.HIGHLIGHT.format(KILL_MESSAGE.formatted(victimName)));
    }

    private static void playDeathSounds(Location location) {
        location.getWorld().playSound(location, Sound.ITEM_TRIDENT_THUNDER, 0.75F, 1.75F);

        Bukkit.getOnlinePlayers().forEach(player -> {
            player.playSound(player, Sound.ENTITY_ELDER_GUARDIAN_DEATH, 1.0F, 0.65F);
            player.playSound(player, Sound.ENTITY_GUARDIAN_DEATH, 1.0F, 0.5F);
        });
    }

    private static void placeTombstone(Player player) {
        Location location = player.getLocation();

        location.getBlock().setType(Material.GOLD_BLOCK);
        location.clone().add(0, 1, 0).getBlock().setType(Material.IRON_BARS);

        Block skullBlock = location.clone().add(0, 2, 0).getBlock();
        skullBlock.setType(Material.PLAYER_HEAD);

        Skull skull = (Skull) skullBlock.getState();
        skull.setProfile(ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
        skull.update(true, false);
    }
}