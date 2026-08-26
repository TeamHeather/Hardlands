package org.heather.hardlands.common.enchantment;

import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public final class EnchantmentListener implements Listener {

    private final DeadEyeHandler deadEyeHandler = new DeadEyeHandler();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (handleTimber(event, tool)) return;
        if (handleVeinMiner(event, tool)) return;

        handleWisdom(event, tool);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockDropItem(BlockDropItemEvent event) {
        handleSmeltingTouch(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) handleDeadEye(event, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDeath(EntityDeathEvent event) {
        handleSmeltingTouch(event);
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        this.deadEyeHandler.reset(event.getPlayer());
    }

    private static boolean handleTimber(BlockBreakEvent event, ItemStack tool) {
        Block block = event.getBlock();

        if (HardlandsEnchantment.TIMBER.level(tool) <= 0 || !Tag.ITEMS_AXES.isTagged(tool.getType()) || !Tag.LOGS.isTagged(block.getType())) {
            return false;
        }

        event.setCancelled(true);
        TimberHandler.handle(block, tool);
        return true;
    }

    private static boolean handleVeinMiner(BlockBreakEvent event, ItemStack tool) {
        Block block = event.getBlock();

        if (HardlandsEnchantment.VEIN_MINER.level(tool) <= 0
                || !Tag.ITEMS_PICKAXES.isTagged(tool.getType())
                || !VeinMinerHandler.isOre(block)) {
            return false;
        }

        event.setCancelled(true);
        VeinMinerHandler.handle(block, tool);
        return true;
    }

    private static void handleWisdom(BlockBreakEvent event, ItemStack tool) {
        int level = HardlandsEnchantment.WISDOM.level(tool);
        if (level > 0) WisdomHandler.handle(event, level);
    }

    private static void handleSmeltingTouch(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (HardlandsEnchantment.SMELTING_TOUCH.level(tool) > 0) SmeltingTouchHandler.handle(event);
    }

    private static void handleSmeltingTouch(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (HardlandsEnchantment.SMELTING_TOUCH.level(tool) > 0) SmeltingTouchHandler.handle(event);
    }

    private void handleDeadEye(EntityDamageByEntityEvent event, Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int level = HardlandsEnchantment.DEAD_EYE.level(weapon);

        if (level <= 0 || !Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON.isTagged(weapon.getType())) {
            this.deadEyeHandler.reset(player);
            return;
        }

        if (event.isCritical()) this.deadEyeHandler.handle(event, player, level);
    }
}