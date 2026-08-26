package org.heather.hardlands.module.scenario.scenarios;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.config.ConfigBuilder;
import org.heather.hardlands.config.OptionDef;
import org.heather.hardlands.common.enchantment.HardlandsEnchantment;
import org.heather.hardlands.module.scenario.Scenario;

@ConfigBuilder(
        superclass = Scenario.class,
        options = {
                @OptionDef(type = Map.class, keyType = String.class, valueType = Integer.class, name = "enchantments")
        }
)
public class MagicManScenario extends MagicManScenarioConfiguration {

    public static final int PROHIBITED = -1;
    public static final int VANILLA = 0;

    public MagicManScenario() {
        super.enchantments.setValue(Map.of());
    }

    @Override
    public boolean canEnable() {
        return super.canEnable()
                && super.enchantments.hasValue()
                && super.enchantments.getValue().values().stream().anyMatch(level -> level != VANILLA);
    }

    @EventHandler
    private void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        ItemStack original = event.getNewItemStack();
        if (original.getType().isAir()) return;

        ItemStack updated = original.clone();

        this.applyConfiguredEnchantments(updated);

        if (!updated.equals(original)) event.getPlayer().getInventory().setItem(event.getSlot(), updated);
    }

    public int getEnchantmentLevel(String identifier) {
        String normalized = normalizeIdentifier(identifier);
        return super.enchantments.getValue().getOrDefault(normalized, VANILLA);
    }

    public void setEnchantmentLevel(String identifier, int level) {
        String normalized = normalizeIdentifier(identifier);
        int maxLevel = getMaximumLevel(normalized);

        if (level < PROHIBITED || level > maxLevel) {
            throw new IllegalArgumentException("Enchantment level must be between -1 and %d: %s".formatted(maxLevel, normalized));
        }

        Map<String, Integer> enchantments = new LinkedHashMap<>(super.enchantments.getValue());

        if (level == VANILLA) enchantments.remove(normalized);
        else enchantments.put(normalized, level);

        super.enchantments.setValue(Map.copyOf(enchantments));
    }

    private void applyConfiguredEnchantments(ItemStack stack) {
        super.enchantments.getValue().forEach((identifier, level) -> {
            if (level == VANILLA) return;

            Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);

            if (hardlandsEnchantment.isPresent()) {
                applyHardlandsEnchantment(stack, hardlandsEnchantment.get(), level);
                return;
            }

            Enchantment enchantment = findVanillaEnchantment(identifier);
            if (enchantment != null) applyVanillaEnchantment(stack, enchantment, level);
        });
    }

    private static void applyVanillaEnchantment(ItemStack stack, Enchantment enchantment, int level) {
        if (level == PROHIBITED) {
            stack.removeEnchantment(enchantment);
            return;
        }

        if (level > VANILLA && level <= enchantment.getMaxLevel() && enchantment.canEnchantItem(stack)) {
            stack.addEnchantment(enchantment, level);
        }
    }

    private static void applyHardlandsEnchantment(ItemStack stack, HardlandsEnchantment enchantment, int level) {
        if (level == PROHIBITED) {
            enchantment.remove(stack);
            return;
        }

        if (level > VANILLA && level <= enchantment.getMaxLevel()) {
            enchantment.apply(stack, level - 1);
        }
    }

    private static int getMaximumLevel(String identifier) {
        Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);
        if (hardlandsEnchantment.isPresent()) return hardlandsEnchantment.get().getMaxLevel();

        Enchantment enchantment = findVanillaEnchantment(identifier);
        if (enchantment != null) return enchantment.getMaxLevel();

        throw new IllegalArgumentException("Unknown enchantment: " + identifier);
    }

    private static String normalizeIdentifier(String identifier) {
        Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);
        if (hardlandsEnchantment.isPresent()) return hardlandsEnchantment.get().name();

        Enchantment enchantment = findVanillaEnchantment(identifier);
        if (enchantment != null) return enchantment.getKey().toString();

        throw new IllegalArgumentException("Unknown enchantment: " + identifier);
    }

    private static Enchantment findVanillaEnchantment(String identifier) {
        String normalized = identifier.contains(":") ? identifier : "minecraft:" + identifier;
        NamespacedKey key = NamespacedKey.fromString(normalized.toLowerCase(Locale.ROOT));
        return key == null ? null : getEnchantmentRegistry().get(key);
    }

    private static Registry<Enchantment> getEnchantmentRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    }
}