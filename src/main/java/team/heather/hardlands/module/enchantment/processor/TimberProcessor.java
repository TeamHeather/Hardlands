package team.heather.hardlands.module.enchantment.processor;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.core.data.BoundedCounter;
import team.heather.hardlands.core.event.TimberBreakLeavesEvent;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;
import team.heather.hardlands.util.BlockUtils;

public final class TimberProcessor {

    private static final int LOG_LIMIT = 64;
    private static final int LEAF_LIMIT = 128;

    public boolean handle(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        Block origin = event.getBlock();

        if (!HardlandsEnchantment.TIMBER.matches(tool, item -> Tag.ITEMS_AXES.isTagged(item.getType()))
                || !Tag.LOGS.isTagged(origin.getType())) {
            return false;
        }

        event.setCancelled(true);

        BoundedCounter logs = new BoundedCounter(LOG_LIMIT);
        BoundedCounter leaves = new BoundedCounter(LEAF_LIMIT);

        BlockUtils.floodFill(origin, block -> {
            Material material = block.getType();

            if (Tag.LOGS.isTagged(material)) {
                if (!logs.tryAdvance()) return false;

                block.breakNaturally(tool);
                return true;
            }

            if (!Tag.LEAVES.isTagged(material) || !leaves.tryAdvance()) return false;

            TimberBreakLeavesEvent leavesEvent = new TimberBreakLeavesEvent(block);
            if (leavesEvent.callEvent()) {
                block.breakNaturally();
            }

            return true;
        });

        return true;
    }
}