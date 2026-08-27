package org.heather.hardlands.module.scenario.implementation;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.module.enchantment.HardlandsEnchantment;
import org.heather.hardlands.configuration.ConfigBuilder;
import org.heather.hardlands.configuration.OptionDef;
import org.heather.hardlands.module.scenario.Scenario;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@ConfigBuilder(
        superclass = Scenario.class,
        options = {
                @OptionDef(type = Map.class, keyType = String.class, valueType = Integer.class, name = "enchantments")
        }
)
public class MagicManScenario extends MagicManScenarioConfiguration {

    public static final int PROHIBITED_AMPLIFIER = -2;
    public static final int VANILLA_AMPLIFIER = -1;

    public MagicManScenario() {
        super.enchantments.setValue(Map.of());
    }

    @Override
    public boolean canEnable() {
        return super.canEnable()
                && super.enchantments.hasValue()
                && super.enchantments.getValue().values().stream().anyMatch(amplifier -> amplifier != VANILLA_AMPLIFIER);
    }

    @EventHandler
    private void onPlayerInventorySlotChange(PlayerInventorySlotChangeEvent event) {
        ItemStack original = event.getNewItemStack();
        if (original.getType().isAir()) return;

        ItemStack updated = original.clone();
        this.applyConfiguredEnchantments(updated);

        if (!updated.equals(original)) {
            event.getPlayer().getInventory().setItem(event.getSlot(), updated);
        }
    }

    public int getEnchantmentAmplifier(String identifier) {
        return super.enchantments.getValue().getOrDefault(normalizeIdentifier(identifier), VANILLA_AMPLIFIER);
    }

    public void setEnchantmentAmplifier(String identifier, int amplifier) {
        String normalized = normalizeIdentifier(identifier);
        int maxAmplifier = getMaximumAmplifier(normalized);

        if (amplifier < PROHIBITED_AMPLIFIER || amplifier > maxAmplifier) {
            throw new IllegalArgumentException(
                    "Enchantment amplifier must be between %d and %d: %s"
                            .formatted(PROHIBITED_AMPLIFIER, maxAmplifier, normalized)
            );
        }

        Map<String, Integer> enchantments = new LinkedHashMap<>(super.enchantments.getValue());

        if (amplifier == VANILLA_AMPLIFIER) enchantments.remove(normalized);
        else enchantments.put(normalized, amplifier);

        super.enchantments.setValue(Map.copyOf(enchantments));
    }

    private void applyConfiguredEnchantments(ItemStack stack) {
        super.enchantments.getValue().forEach((identifier, amplifier) -> {
            Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);

            if (hardlandsEnchantment.isPresent()) {
                applyHardlandsEnchantment(stack, hardlandsEnchantment.get(), amplifier);
                return;
            }

            Enchantment vanillaEnchantment = findVanillaEnchantment(identifier);
            if (vanillaEnchantment != null) applyVanillaEnchantment(stack, vanillaEnchantment, amplifier);
        });
    }

    private static void applyVanillaEnchantment(ItemStack stack, Enchantment enchantment, int amplifier) {
        if (amplifier == PROHIBITED_AMPLIFIER) {
            stack.removeEnchantment(enchantment);
            return;
        }

        if (amplifier >= 0 && amplifier < enchantment.getMaxLevel() && enchantment.canEnchantItem(stack)) {
            stack.addEnchantment(enchantment, amplifier + 1);
        }
    }

    private static void applyHardlandsEnchantment(ItemStack stack, HardlandsEnchantment enchantment, int amplifier) {
        if (amplifier == PROHIBITED_AMPLIFIER) {
            enchantment.remove(stack);
            return;
        }

        if (amplifier >= 0 && enchantment.getLimit().check(amplifier)) {
            enchantment.apply(stack, amplifier);
        }
    }

    private static int getMaximumAmplifier(String identifier) {
        Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);
        if (hardlandsEnchantment.isPresent()) {
            return hardlandsEnchantment.get().getLimit().maxAmplifier();
        }

        Enchantment enchantment = findVanillaEnchantment(identifier);
        if (enchantment != null) return enchantment.getMaxLevel() - 1;

        throw new IllegalArgumentException("Unknown enchantment: " + identifier);
    }

    private static String normalizeIdentifier(String identifier) {
        Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);
        if (hardlandsEnchantment.isPresent()) {
            return hardlandsEnchantment.get().createIdentifier();
        }

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