package org.heather.hardlands.module.enchantment;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
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
import org.heather.hardlands.core.data.BoundedCounter;
import org.heather.hardlands.core.event.TimberBreakLeavesEvent;
import org.heather.hardlands.util.BlockUtils;
import org.heather.hardlands.util.SmeltingHelper;

public final class EnchantmentListener implements Listener {

    private static final int TIMBER_LOG_LIMIT = 64;
    private static final int TIMBER_LEAF_LIMIT = 128;
    private static final int VEIN_MINER_BLOCK_LIMIT = 64;

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
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (HardlandsEnchantment.SMELTING_TOUCH.matches(tool, _ -> BlockUtils.isOre(event.getBlockState().getType()))) {
            SmeltingHelper.smeltDrops(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) handleDeadEye(event, player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();

        if (HardlandsEnchantment.SMELTING_TOUCH.matches(tool)) {
            event.getDrops().forEach(SmeltingHelper::smeltFood);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        this.deadEyeHandler.reset(event.getPlayer());
    }

    //* Enchantment Handlers

    private static boolean handleTimber(BlockBreakEvent event, ItemStack tool) {
        Block origin = event.getBlock();

        if (!HardlandsEnchantment.TIMBER.matches(tool, item -> Tag.ITEMS_AXES.isTagged(item.getType()))
                || !Tag.LOGS.isTagged(origin.getType())) return false;

        event.setCancelled(true);

        BoundedCounter logs = new BoundedCounter(TIMBER_LOG_LIMIT);
        BoundedCounter leaves = new BoundedCounter(TIMBER_LEAF_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            Material material = block.getType();

            if (Tag.LOGS.isTagged(material)) {
                if (!logs.tryAdvance()) return false;

                block.breakNaturally(tool);
                return true;
            }

            if (!Tag.LEAVES.isTagged(material) || !leaves.tryAdvance()) return false;

            TimberBreakLeavesEvent leavesEvent = new TimberBreakLeavesEvent(block);
            if (leavesEvent.callEvent()) block.breakNaturally();

            return true;
        });

        return true;
    }

    private static boolean handleVeinMiner(BlockBreakEvent event, ItemStack tool) {
        Block origin = event.getBlock();

        if (!HardlandsEnchantment.VEIN_MINER.matches(tool, item -> Tag.ITEMS_PICKAXES.isTagged(item.getType()))
                || !BlockUtils.isOre(origin.getType())) return false;

        Material ore = origin.getType();
        boolean smeltingTouch = HardlandsEnchantment.SMELTING_TOUCH.matches(tool);
        BoundedCounter blocks = new BoundedCounter(VEIN_MINER_BLOCK_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            if (block.getType() != ore || !blocks.tryAdvance()) return false;
            if (block.equals(origin)) return true;

            if (smeltingTouch) SmeltingHelper.breakSmelted(block, tool);
            else block.breakNaturally(tool);

            return true;
        });

        return true;
    }

    private static void handleWisdom(BlockBreakEvent event, ItemStack tool) {
        HardlandsEnchantment.WISDOM.findMatchingLevel(tool).ifPresent(level -> {
            int experience = event.getExpToDrop();
            if (experience <= 0) return;

            double increasedExperience = experience * (1.0D + level * 0.25D);
            int result = (int) increasedExperience;

            if (ThreadLocalRandom.current().nextDouble() < increasedExperience - result) {
                result++;
            }

            event.setExpToDrop(result);
        });
    }

    private void handleDeadEye(EntityDamageByEntityEvent event, Player player) {
        ItemStack weapon = player.getInventory().getItemInMainHand();
        Optional<Integer> level = HardlandsEnchantment.DEAD_EYE.findMatchingLevel(weapon);

        if (level.isEmpty()) {
            this.deadEyeHandler.reset(player);
            return;
        }

        if (event.isCritical()) this.deadEyeHandler.handle(event, player, level.get());
    }
}