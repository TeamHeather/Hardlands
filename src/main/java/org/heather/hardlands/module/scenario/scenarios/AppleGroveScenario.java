package org.heather.hardlands.module.scenario.scenarios;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.configuration.OptionDef;
import org.heather.hardlands.core.configuration.Validator;
import org.heather.hardlands.core.event.TimberBreakLeavesEvent;
import org.heather.hardlands.module.scenario.Scenario;

@ConfigBuilder(superclass = Scenario.class, options = {
        @OptionDef(type = Boolean.class, name = "allTrees"),
        @OptionDef(type = Float.class, validators = Validator.Keys.UNIT_INTERVAL, name = "appleRate"),
        @OptionDef(type = Float.class, validators = Validator.Keys.UNIT_INTERVAL, name = "goldenRate"),
        @OptionDef(type = Float.class, validators = Validator.Keys.UNIT_INTERVAL, name = "enchantedRate")
})
public class AppleGroveScenario extends AppleGroveScenarioConfiguration {

    @EventHandler(ignoreCancelled = true)
    private void onLeavesDecay(LeavesDecayEvent event) {
        this.tryDropApple(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockDropItem(BlockDropItemEvent event) {
        this.tryDropApple(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    private void onTimberBreakLeaves(TimberBreakLeavesEvent event) {
        this.tryDropApple(event.getBlock());
    }

    private void tryDropApple(Block block) {
        if (!this.isEligibleLeaf(block.getType())) return;

        this.findAppleDrop().ifPresent(drop ->
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(drop)));
    }

    private Optional<Material> findAppleDrop() {
        float roll = ThreadLocalRandom.current().nextFloat();

        float enchantedRate = super.enchantedRate.getValue();
        float goldenRate = enchantedRate + super.goldenRate.getValue();
        float appleRate = goldenRate + super.appleRate.getValue();

        if (roll < enchantedRate) return Optional.of(Material.ENCHANTED_GOLDEN_APPLE);
        if (roll < goldenRate) return Optional.of(Material.GOLDEN_APPLE);
        if (roll < appleRate) return Optional.of(Material.APPLE);

        return Optional.empty();
    }

    private boolean isEligibleLeaf(Material material) {
        return Tag.LEAVES.isTagged(material)
                && (super.allTrees.getValue() || material == Material.OAK_LEAVES);
    }
}