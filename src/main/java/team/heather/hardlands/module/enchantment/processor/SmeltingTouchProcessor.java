package team.heather.hardlands.module.enchantment.processor;

import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;
import team.heather.hardlands.util.BlockUtils;
import team.heather.hardlands.util.SmeltingHelper;

public final class SmeltingTouchProcessor {

    public void handle(BlockDropItemEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        if (HardlandsEnchantment.SMELTING_TOUCH.matches(
                tool,
                _ -> BlockUtils.isOre(event.getBlockState().getType())
        )) {
            SmeltingHelper.smeltDrops(event);
        }
    }

    public void handle(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof Animals)) return;

        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        ItemStack tool = killer.getInventory().getItemInMainHand();
        if (!HardlandsEnchantment.SMELTING_TOUCH.matches(tool)) return;

        event.getDrops().forEach(SmeltingHelper::smeltFood);
    }
}