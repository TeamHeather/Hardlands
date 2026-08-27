package org.heather.hardlands.module.scenario.implementation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.configuration.OptionDef;
import org.heather.hardlands.module.scenario.Scenario;
import org.heather.hardlands.util.BlockUtils;

@ConfigBuilder(superclass = Scenario.class, options = {
        @OptionDef(type = Float.class, validators = "at-least:1.0", name = "dropMultiplier")
})
public class BonanzaScenario extends BonanzaScenarioConfiguration {

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private void onBlockDropItem(BlockDropItemEvent event) {
        if (!BlockUtils.isOre(event.getBlockState().getType())) return;

        float multiplier = super.dropMultiplier.getValue();

        event.getItems().forEach(item -> {
            ItemStack itemStack = item.getItemStack();
            itemStack.setAmount(Math.round(itemStack.getAmount() * multiplier));
        });
    }
}