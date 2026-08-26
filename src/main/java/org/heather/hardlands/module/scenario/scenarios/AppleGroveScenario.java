package org.heather.hardlands.module.scenario.scenarios;

import java.util.Optional;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.gui.ConfigBuilder;
import org.heather.hardlands.gui.OptionDef;
import org.heather.hardlands.core.configuration.Validator;
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
        this.tryDropApple(event);
    }

    @EventHandler(ignoreCancelled = true)
    private void onBlockDropItem(BlockDropItemEvent event) {
        this.tryDropApple(event);
    }

    private void tryDropApple(BlockEvent event) {
        Block block = event.getBlock();

        if (!this.isEligibleLeaf(block.getType())) return;

        this.findAppleDrop().ifPresent(drop ->
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(drop)));
    }

    private Optional<Material> findAppleDrop() {
        float roll = Hardlands.RANDOM.nextFloat();

        float enchantedRate = super.enchantedRate.getValue();
        float goldenRate = enchantedRate + super.goldenRate.getValue();
        float appleRate = goldenRate + super.appleRate.getValue();

        if (roll < enchantedRate) return Optional.of(Material.ENCHANTED_GOLDEN_APPLE);
        if (roll < goldenRate) return Optional.of(Material.GOLDEN_APPLE);
        if (roll < appleRate) return Optional.of(Material.APPLE);

        return Optional.empty();
    }

    private boolean isEligibleLeaf(Material material) {
        return Tag.LEAVES.isTagged(material) && (super.allTrees.getValue() || material == Material.OAK_LEAVES);
    }
}
