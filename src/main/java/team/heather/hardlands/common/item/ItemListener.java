package team.heather.hardlands.common.item;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ItemListener implements Listener {

    private static final int DUALITY_COOLDOWN_TICKS = 100;

    @EventHandler
    private void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) return;

        applyDuality(event.getPlayer());
    }

    @EventHandler
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isDualitySpear(item)) return;

        if (player.hasCooldown(item)) {
            event.setCancelled(true);
            return;
        }

        applyDuality(player);
    }

    private void applyDuality(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();

        if (!isDualitySpear(item) || player.hasCooldown(item)) return;

        Material opposite = switch (item.getType()) {
            case GOLDEN_SPEAR -> Material.IRON_SPEAR;
            case IRON_SPEAR -> Material.GOLDEN_SPEAR;
            default -> null;
        };

        if (opposite == null) return;

        player.setCooldown(opposite, 0);
        player.setCooldown(item, DUALITY_COOLDOWN_TICKS);
    }

    private boolean isDualitySpear(ItemStack item) {
        return item.getType() == Material.GOLDEN_SPEAR
                || item.getType() == Material.IRON_SPEAR;
    }
}