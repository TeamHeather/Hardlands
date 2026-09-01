package team.heather.hardlands.module.enchantment.handler;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.internal.data.BoundedCounter;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;

public final class VeinMinerHandler implements EnchantmentHandler<BlockBreakEvent> {

    private static final int BLOCK_LIMIT = 64;

    @Override
    public void handle(BlockBreakEvent event, int amplifier) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Block origin = event.getBlock();

        if (!BlockUtils.isOre(origin.getType())
                || !HardlandsEnchantment.VEIN_MINER.matches(tool, item -> Tag.ITEMS_PICKAXES.isTagged(item.getType()))) {
            return;
        }

        Material ore = origin.getType();
        boolean smeltingTouch = HardlandsEnchantment.SMELTING_TOUCH.matches(tool);
        BoundedCounter blocks = new BoundedCounter(BLOCK_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            if (block.getType() != ore || !blocks.tryAdvance()) return false;
            if (block.equals(origin)) return true;

            if (smeltingTouch) {
                SmeltingTouchHandler.breakSmelted(block, tool);
            } else {
                block.breakNaturally(tool);
            }

            return true;
        });
    }
}