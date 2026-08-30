package team.heather.hardlands.module.scenario.implementation;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.config.ScenarioConfigBuilder;
import team.heather.hardlands.core.event.ConfigChangeEvent;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;

import java.util.LinkedHashMap;
import java.util.Map;

@ScenarioConfigBuilder(options = {
        @OptionDef(type = Map.class, keyType = HardlandsEnchantment.class, valueType = Integer.class, name = "hardlandsEnchantments"),
        @OptionDef(type = Map.class, keyType = String.class, valueType = Integer.class, name = "vanillaEnchantments")
})
public final class EnchantiaScenario extends EnchantiaScenarioConfiguration {

        public void save() {
                getPluginOrThrow().getEnchantmentManager().activate(hardlandsEnchantments.getValue().keySet().toArray(HardlandsEnchantment[]::new));
        }

        public int getLevel(HardlandsEnchantment enchantment) {
                return hardlandsEnchantments.getValue().getOrDefault(enchantment, 0);
        }

        public int getLevel(Enchantment enchantment) {
                return vanillaEnchantments.getValue().getOrDefault(
                        enchantment.getKey().toString(),
                        0
                );
        }

        public void setLevel(HardlandsEnchantment enchantment, int level) {
                Map<HardlandsEnchantment, Integer> values =
                        new LinkedHashMap<>(hardlandsEnchantments.getValue());

                updateLevel(values, enchantment, level);
                hardlandsEnchantments.setValue(values);
        }

        public void setLevel(Enchantment enchantment, int level) {
                Map<String, Integer> values =
                        new LinkedHashMap<>(vanillaEnchantments.getValue());

                updateLevel(values, enchantment.getKey().toString(), level);
                vanillaEnchantments.setValue(values);
        }

        private static <K> void updateLevel(Map<K, Integer> values, K key, int level) {
                if (level == 0) {
                        values.remove(key);
                } else {
                        values.put(key, level);
                }
        }

        @EventHandler
        private void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
                ItemStack stack = event.getOldItemStack();

                if (stack.getType().isAir()) return;

                boolean changed = false;

                changed |= applyHardlandsEnchantments(stack);
                changed |= applyVanillaEnchantments(stack);

                if (changed) {
                        event.getPlayer().getInventory().setItem(event.getSlot(), stack);
                }
        }

        private boolean applyHardlandsEnchantments(ItemStack stack) {
                boolean changed = false;

                for (Map.Entry<HardlandsEnchantment, Integer> entry : hardlandsEnchantments.getValue().entrySet()) {
                        changed |= entry.getKey().applyIfCompatible(stack, entry.getValue());
                }

                return changed;
        }

        private boolean applyVanillaEnchantments(ItemStack stack) {
                boolean changed = false;

                for (Map.Entry<String, Integer> entry : vanillaEnchantments.getValue().entrySet()) {
                        changed |= applyVanillaEnchantment(stack, entry.getKey(), entry.getValue());
                }

                return changed;
        }

        @Override
        public boolean canEnable() {
                return super.canEnable()
                        && (hardlandsEnchantments.hasValue() || vanillaEnchantments.hasValue());
        }

        private static boolean applyVanillaEnchantment(ItemStack stack, String identifier, int amplifier) {
                NamespacedKey key = NamespacedKey.fromString(identifier);
                if (key == null) {
                        return false;
                }

                Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
                if (enchantment == null) {
                        return false;
                }

                if (amplifier == -1) {
                        stack.removeEnchantment(enchantment);
                        return true;
                }

                int level = amplifier + 1;
                if (!enchantment.canEnchantItem(stack) || stack.getEnchantmentLevel(enchantment) >= level) {
                        return false;
                }

                stack.addEnchantment(enchantment, level);
                return true;
        }

        private boolean isHardlandsEnchantActive(HardlandsEnchantment enchantment) {
                return hardlandsEnchantments.getValue().containsKey(enchantment);
        }
}