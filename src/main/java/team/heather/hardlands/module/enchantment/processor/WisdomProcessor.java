package team.heather.hardlands.module.enchantment.processor;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;

public final class WisdomProcessor {

    private static final double EXPERIENCE_DELTA = 0.25D;

    public void handle(BlockBreakEvent event) {
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();

        HardlandsEnchantment.WISDOM.findMatchingLevel(tool).ifPresent(level -> {
            int experience = event.getExpToDrop();
            if (experience <= 0) return;

            double increasedExperience = experience * (1.0D + level * EXPERIENCE_DELTA);
            int result = (int) increasedExperience;

            if (ThreadLocalRandom.current().nextDouble() < increasedExperience - result) {
                result++;
            }

            event.setExpToDrop(result);
        });
    }
}