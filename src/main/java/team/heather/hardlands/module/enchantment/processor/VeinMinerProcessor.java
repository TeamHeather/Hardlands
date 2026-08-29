package team.heather.hardlands.module.enchantment.processor;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.core.data.BoundedCounter;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;
import team.heather.hardlands.util.BlockUtils;
import team.heather.hardlands.util.SmeltingHelper;

public final class VeinMinerProcessor {

    private static final int BLOCK_LIMIT = 64;

    public boolean handle(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Block origin = event.getBlock();

        if (!HardlandsEnchantment.VEIN_MINER.matches(tool, item -> Tag.ITEMS_PICKAXES.isTagged(item.getType()))
                || !BlockUtils.isOre(origin.getType())) {
            return false;
        }

        Material ore = origin.getType();
        boolean smeltingTouch = HardlandsEnchantment.SMELTING_TOUCH.matches(tool);
        BoundedCounter blocks = new BoundedCounter(BLOCK_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            if (block.getType() != ore || !blocks.tryAdvance()) return false;
            if (block.equals(origin)) return true;

            if (smeltingTouch) {
                SmeltingHelper.breakSmelted(block, tool);
            } else {
                block.breakNaturally(tool);
            }

            return true;
        });

        return true;
    }
}