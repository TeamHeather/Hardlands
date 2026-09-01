package team.heather.hardlands.module.scenario.implementation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.config.ScenarioConfigBuilder;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;

@ScenarioConfigBuilder(options = {
    @OptionDef(
        name = "hardlandsEnchantments",
        type = Map.class,
        keyType = HardlandsEnchantment.class,
        valueType = Integer.class
    ),
    @OptionDef(
        name = "vanillaEnchantments",
        type = Map.class,
        keyType = String.class,
        valueType = Integer.class
    )
})
public final class EnchantiaScenario extends EnchantiaScenarioConfiguration {

        @EventHandler
        private void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
                ItemStack stack = event.getNewItemStack();
                if (stack.getType().isAir()) return;

                boolean hardlandsChanged = this.applyHardlandsEnchantments(stack);
                boolean vanillaChanged = this.applyVanillaEnchantments(stack);

                if (hardlandsChanged || vanillaChanged) {
                        event.getPlayer().getInventory().setItem(event.getSlot(), stack);
                }
        }

        public int level(HardlandsEnchantment enchantment) {
                return this.hardlandsEnchantments.getValue()
                        .getOrDefault(enchantment, 0);
        }

        public int level(Enchantment enchantment) {
                return this.vanillaEnchantments.getValue()
                        .getOrDefault(enchantment.getKey().toString(), 0);
        }

        public void level(HardlandsEnchantment enchantment, int level) {
                Map<HardlandsEnchantment, Integer> values = new LinkedHashMap<>(this.hardlandsEnchantments.getValue());

                updateLevel(values, enchantment, level);
                this.hardlandsEnchantments.changeValue(values);
        }

        public void level(Enchantment enchantment, int level) {
                Map<String, Integer> values = new LinkedHashMap<>(this.vanillaEnchantments.getValue());

                updateLevel(values, enchantment.getKey().toString(), level);
                this.vanillaEnchantments.changeValue(values);
        }

        private boolean applyHardlandsEnchantments(ItemStack stack) {
                boolean changed = false;

                for (Map.Entry<HardlandsEnchantment, Integer> entry : this.hardlandsEnchantments.getValue().entrySet()) {
                        changed |= applyHardlandsEnchantment(stack, entry.getKey(), entry.getValue());
                }

                return changed;
        }

        private boolean applyVanillaEnchantments(ItemStack stack) {
                boolean changed = false;

                for (Map.Entry<String, Integer> entry : this.vanillaEnchantments.getValue().entrySet()) {
                        changed |= applyVanillaEnchantment(stack, entry.getKey(), entry.getValue());
                }

                return changed;
        }

        private static boolean applyHardlandsEnchantment(
            ItemStack stack,
            HardlandsEnchantment enchantment,
            int level
        ) {
                if (level == -1) return enchantment.remove(stack);
                if (level < 1 || level > enchantment.createMaxLevel()) return false;

                int amplifier = level - 1;
                Optional<Integer> currentAmplifier = enchantment.findLevel(stack);

                if (currentAmplifier.isPresent() && currentAmplifier.get() == amplifier) return false;

                return enchantment.applyIfCompatible(stack, amplifier);
        }

        private static boolean applyVanillaEnchantment(
            ItemStack stack,
            String identifier,
            int level
        ) {
                NamespacedKey key = NamespacedKey.fromString(identifier);
                if (key == null) return false;

                Enchantment enchantment = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.ENCHANTMENT)
                    .get(key);

                if (enchantment == null) return false;

                int currentLevel = stack.getEnchantmentLevel(enchantment);

                if (level == -1) {
                        if (currentLevel == 0) return false;

                        stack.removeEnchantment(enchantment);
                        return true;
                }

                if (level < 1
                    || level > enchantment.getMaxLevel()
                    || currentLevel >= level
                    || !enchantment.canEnchantItem(stack)) return false;

                stack.addEnchantment(enchantment, level);
                return true;
        }

        private static <K> void updateLevel(Map<K, Integer> values, K key, int level) {
                if (level == 0) {
                        values.remove(key);
                        return;
                }

                values.put(key, level);
        }
}