package org.heather.hardlands.common.enchantment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
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
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.util.BlockUtils;
import org.heather.hardlands.util.SmeltingHelper;
import org.heather.hardlands.util.data.BoundedCounter;

public final class EnchantmentListener implements Listener {

    private static final int TIMBER_LOG_LIMIT = 64;
    private static final int TIMBER_LEAF_LIMIT = 128;
    private static final int VEIN_SIZE_LIMIT = 64;

    private static final long DEAD_EYE_COMBO_TIMEOUT = 1_500L;
    private static final double DEAD_EYE_DAMAGE_PER_HIT = 0.02D;
    private static final double WISDOM_EXPERIENCE_PER_LEVEL = 0.25D;

    private final Map<UUID, DeadEyeCombo> deadEyeCombos = new HashMap<>();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (getEnchantmentLevel(tool, HardlandsEnchantment.TIMBER) > 0
                && Tag.ITEMS_AXES.isTagged(tool.getType())
                && Tag.LOGS.isTagged(block.getType())) {
            event.setCancelled(true);
            handleTimber(block, tool);
            return;
        }

        if (getEnchantmentLevel(tool, HardlandsEnchantment.VEIN_MINER) > 0
                && Tag.ITEMS_PICKAXES.isTagged(tool.getType())
                && BlockUtils.isOre(block.getType())) {
            event.setCancelled(true);
            handleVeinMiner(block, tool);
            return;
        }

        int wisdomLevel = getEnchantmentLevel(tool, HardlandsEnchantment.WISDOM);
        if (wisdomLevel > 0) handleWisdom(event, wisdomLevel);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onBlockDropItem(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (getEnchantmentLevel(tool, HardlandsEnchantment.SMELTING_TOUCH) > 0
                && BlockUtils.isOre(event.getBlockState().getType())) {
            SmeltingHelper.smeltDrops(event);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;

        ItemStack weapon = player.getInventory().getItemInMainHand();
        int level = getEnchantmentLevel(weapon, HardlandsEnchantment.DEAD_EYE);

        if (level <= 0 || !Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON.isTagged(weapon.getType())) {
            deadEyeCombos.remove(player.getUniqueId());
            return;
        }

        if (event.isCritical()) handleDeadEye(event, player, level);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private void onEntityDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (getEnchantmentLevel(tool, HardlandsEnchantment.SMELTING_TOUCH) > 0) {
            event.getDrops().forEach(SmeltingHelper::smeltFood);
        }
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        deadEyeCombos.remove(event.getPlayer().getUniqueId());
    }

    private static void handleTimber(Block origin, ItemStack tool) {
        BoundedCounter logs = new BoundedCounter(TIMBER_LOG_LIMIT);
        BoundedCounter leaves = new BoundedCounter(TIMBER_LEAF_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            Material material = block.getType();
            boolean breakBlock = Tag.LOGS.isTagged(material)
                    ? logs.tryAdvance()
                    : Tag.LEAVES.isTagged(material) && leaves.tryAdvance();

            if (breakBlock) block.breakNaturally(tool);
            return breakBlock;
        });
    }

    private static void handleVeinMiner(Block origin, ItemStack tool) {
        Material ore = origin.getType();
        BoundedCounter blocks = new BoundedCounter(VEIN_SIZE_LIMIT);
        boolean smeltingTouch =
                getEnchantmentLevel(tool, HardlandsEnchantment.SMELTING_TOUCH) > 0;

        BlockUtils.floodFill(origin, block -> {
            if (block.getType() != ore || !blocks.tryAdvance()) return false;

            if (smeltingTouch) SmeltingHelper.breakSmelted(block, tool);
            else block.breakNaturally(tool);

            return true;
        });
    }

    private void handleDeadEye(EntityDamageByEntityEvent event, Player player, int level) {
        UUID playerId = player.getUniqueId();
        UUID targetId = event.getEntity().getUniqueId();
        long currentTime = System.currentTimeMillis();

        DeadEyeCombo combo = deadEyeCombos.get(playerId);
        boolean continuesCombo = combo != null
                && combo.targetId().equals(targetId)
                && currentTime - combo.lastHitTime() <= DEAD_EYE_COMBO_TIMEOUT;

        int hitCount = continuesCombo ? combo.hitCount() + 1 : 1;
        double bonus = (hitCount - 1) * level * DEAD_EYE_DAMAGE_PER_HIT;
        double damage = event.getDamage() * (1.0D + bonus);

        event.setDamage(damage);

        displayDeadEyeActionBar(player, hitCount, bonus, damage);
        playDeadEyeEffect(player, hitCount);

        deadEyeCombos.put(playerId, new DeadEyeCombo(targetId, hitCount, currentTime));
    }

    private static void playDeadEyeEffect(Player player, int hitCount) {
        float pitch = Math.min(0.85F + (hitCount - 1) * 0.15F, 2.0F);
        int particles = Math.min(4 + (hitCount - 1) * 3, 32);

        World world = player.getWorld();
        world.playSound(player.getLocation(), "minecraft:block.end_portal_frame.fill", 5.0F, pitch);
        world.spawnParticle(Particle.FLAME, player.getLocation().add(0, 1, 0), particles, 0.4, 1.0, 0.4, 0.03);
    }

    private static void displayDeadEyeActionBar(Player player, int hitCount, double bonus, double damage) {
        player.sendActionBar(
                Component.text("◆ ", NamedTextColor.DARK_AQUA)
                        .append(Component.text("DEAD EYE", NamedTextColor.AQUA))
                        .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("×" + hitCount, NamedTextColor.YELLOW))
                        .append(Component.text("  +", NamedTextColor.DARK_GRAY))
                        .append(Component.text(Math.round(bonus * 100) + "%", NamedTextColor.GREEN))
                        .append(Component.text("  |  ", NamedTextColor.DARK_GRAY))
                        .append(Component.text(String.format("%.1f", damage), NamedTextColor.RED))
                        .append(Component.text(" ❤", NamedTextColor.DARK_RED)));
    }

    private static void handleWisdom(BlockBreakEvent event, int level) {
        int experience = event.getExpToDrop();
        if (experience <= 0) return;

        double increasedExperience =
                experience * (1.0D + level * WISDOM_EXPERIENCE_PER_LEVEL);

        int result = (int) increasedExperience;
        if (Hardlands.RANDOM.nextDouble() < increasedExperience - result) result++;

        event.setExpToDrop(result);
    }

    private static int getEnchantmentLevel(
            ItemStack item,
            HardlandsEnchantment enchantment) {
        if (!item.hasItemMeta()) return 0;

        return enchantment.find(item.getItemMeta())
                .map(context -> context.amplifier() + 1)
                .orElse(0);
    }

    private record DeadEyeCombo(UUID targetId, int hitCount, long lastHitTime) {}
}