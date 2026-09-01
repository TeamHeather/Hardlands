package team.heather.hardlands.module.scenario.implementation;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.config.ScenarioConfigBuilder;
import team.heather.hardlands.internal.config.Validator;
import team.heather.hardlands.internal.event.TimberBreakLeavesEvent;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ScenarioConfigBuilder(options = {
        @OptionDef(name = "allTrees", type = Boolean.class),
        @OptionDef(name = "appleRate", type = Float.class, validators = Validator.Keys.UNIT_INTERVAL),
        @OptionDef(name = "goldenRate", type = Float.class, validators = Validator.Keys.UNIT_INTERVAL),
        @OptionDef(name = "enchantedRate", type = Float.class, validators = Validator.Keys.UNIT_INTERVAL)
})
public final class AppleGroveScenario extends AppleGroveScenarioConfiguration {

    @EventHandler
    private void onLeavesDecay(LeavesDecayEvent event) {
        tryDropApple(event.getBlock());
    }

    @EventHandler
    private void onBlockDropItem(BlockDropItemEvent event) {
        tryDropApple(event.getBlock());
    }

    @EventHandler
    private void onTimberBreakLeaves(TimberBreakLeavesEvent event) {
        tryDropApple(event.getBlock());
    }

    private void tryDropApple(Block block) {
        if (!isEligibleLeaf(block.getType())) return;

        findAppleDrop().ifPresent(material ->
                block.getWorld().dropItemNaturally(
                        block.getLocation(),
                        new ItemStack(material)
                )
        );
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