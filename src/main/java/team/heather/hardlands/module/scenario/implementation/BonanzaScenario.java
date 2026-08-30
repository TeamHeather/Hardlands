package team.heather.hardlands.module.scenario.implementation;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.config.ScenarioConfigBuilder;
import team.heather.hardlands.core.config.Validator;
import team.heather.hardlands.util.BlockUtils;

@ScenarioConfigBuilder(options = {
        @OptionDef(
                name = "multiplier",
                type = Integer.class,
                validators = Validator.Keys.POSITIVE
        )
})
public final class BonanzaScenario extends BonanzaScenarioConfiguration {

    @EventHandler(priority = EventPriority.LOWEST)
    private void onBlockDropItem(BlockDropItemEvent event) {
        if (!BlockUtils.isOre(event.getBlockState().getType())) {
            return;
        }

        event.getItems().forEach(item -> {
            ItemStack stack = item.getItemStack();
            stack.setAmount(stack.getAmount() * super.multiplier.getValue());
        });
    }
}