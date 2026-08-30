package team.heather.hardlands.module.enchantment.handler;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.event.block.BlockBreakEvent;

public final class WisdomHandler implements EnchantmentHandler<BlockBreakEvent> {

    private static final double EXPERIENCE_DELTA = 0.25D;

    @Override
    public void handle(BlockBreakEvent event, int amplifier) {
        int experience = event.getExpToDrop();
        if (experience <= 0) return;

        double increasedExperience = experience * (1.0D + amplifier * EXPERIENCE_DELTA);
        int result = (int) increasedExperience;

        if (ThreadLocalRandom.current().nextDouble() < increasedExperience - result) {
            result++;
        }

        event.setExpToDrop(result);
    }
}