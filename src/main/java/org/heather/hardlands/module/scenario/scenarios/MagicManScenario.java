package org.heather.hardlands.module.scenario.scenarios;

import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.heather.hardlands.config.ConfigBuilder;
import org.heather.hardlands.config.OptionDef;
import org.heather.hardlands.module.scenario.Scenario;

@ConfigBuilder(superclass = Scenario.class, options = {
        @OptionDef(
                type = Map.class,
                keyType = String.class,
                valueType = Integer.class,
                name = "enchantments"
        )
})
public class MagicManScenario extends MagicManScenarioConfiguration {

    public static final int DISABLED_LEVEL = -1;
    public static final int VANILLA_LEVEL = 0;
    private static final Registry<Enchantment> ENCHANTMENT_REGISTRY =
            RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);

    @FunctionalInterface
    private interface ItemUpdater {
        boolean update(ItemStack item, Player player);
    }

    @EventHandler
    private void onInventoryChange(PlayerInventorySlotChangeEvent event) {
        ItemStack item = event.getNewItemStack();
        if (!item.isEmpty() && applyEnchantments(item, event.getPlayer())) {
            event.getPlayer().getInventory().setItem(event.getSlot(), item);
        }
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        updateInventory(event.getPlayer().getInventory(), event.getPlayer());
    }

    public int getEnchantmentLevel(String identifier) {
        if (!super.enchantments.hasValue()) {
            return VANILLA_LEVEL;
        }
        return super.enchantments.getValue().getOrDefault(identifier, VANILLA_LEVEL);
    }

    public int getEnchantmentLevel(Enchantment enchantment) {
        return getEnchantmentLevel(enchantment.getKey().asString());
    }

    public int getEnchantmentLevel(EnchantmentDefinition enchantment) {
        return getEnchantmentLevel(enchantment.getKey().asString());
    }

    public boolean setEnchantmentLevel(Enchantment enchantment, int level) {
        String identifier = enchantment.getKey().asString();
        int normalized = Math.clamp(level, DISABLED_LEVEL, enchantment.getMaxLevel());
        return updateEnchantmentLevel(identifier, normalized,
                (item, player) -> applyVanillaEnchantment(item, enchantment, normalized, player));
    }

    public boolean setEnchantmentLevel(EnchantmentDefinition enchantment, int level) {
        String identifier = enchantment.getKey().asString();
        int normalized = Math.clamp(level, DISABLED_LEVEL, enchantment.getMaxLevel());
        return updateEnchantmentLevel(identifier, normalized,
                (item, player) -> applyCustomEnchantment(item, enchantment, normalized, player));
    }

    private boolean updateEnchantmentLevel(String identifier, int level,
                                           ItemUpdater updater) {
        Map<String, Integer> values = new LinkedHashMap<>(
                super.enchantments.hasValue() ? super.enchantments.getValue() : Map.of());

        if (level == VANILLA_LEVEL) {
            values.remove(identifier);
        } else {
            values.put(identifier, level);
        }

        if (!super.enchantments.getPredicate().test(values)) {
            return false;
        }

        super.enchantments.setValue(values);

        if (level != VANILLA_LEVEL) {
            updateOnlinePlayers(updater);
        }
        return true;
    }

    private boolean applyEnchantments(ItemStack item, Player player) {
        if (!super.enchantments.hasValue()) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<String, Integer> entry : super.enchantments.getValue().entrySet()) {
            changed |= applyEnchantmentByIdentifier(item, entry.getKey(), entry.getValue(),
                    player);
        }
        return changed;
    }

    private boolean applyEnchantmentByIdentifier(ItemStack item, String identifier, int level,
                                                 Player player) {
        NamespacedKey key = NamespacedKey.fromString(identifier);
        if (key == null || level == VANILLA_LEVEL) {
            return false;
        }

        EnchantmentDefinition customEnchantment = EnchantmentDefinition.find(key);
        if (customEnchantment != null) {
            return applyCustomEnchantment(item, customEnchantment, level, player);
        }

        Enchantment vanillaEnchantment = ENCHANTMENT_REGISTRY.get(key);
        if (vanillaEnchantment != null) {
            return applyVanillaEnchantment(item, vanillaEnchantment, level, player);
        }

        return false;
    }

    private static boolean applyVanillaEnchantment(ItemStack item, Enchantment enchantment,
                                                   int level, Player player) {
        int current = item.getEnchantmentLevel(enchantment);

        if (level == DISABLED_LEVEL) {
            if (current > 0 && item.removeEnchantment(enchantment) > 0) {
                playSound(player, Sound.ENTITY_ITEM_BREAK, 0.7f);
                return true;
            }
            return false;
        }

        if (level == VANILLA_LEVEL || current == level || !enchantment.canEnchantItem(item)) {
            return false;
        }

        item.addUnsafeEnchantment(enchantment, level);
        playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.25f);
        return true;
    }

    private static boolean applyCustomEnchantment(ItemStack item, EnchantmentDefinition enchantment, int level, Player player) {
        if (level == DISABLED_LEVEL) {
            if (enchantment.remove(item)) {
                playSound(player, Sound.BLOCK_GRINDSTONE_USE, 1.25f);
                return true;
            }
            return false;
        }

        if (level == VANILLA_LEVEL) {
            return false;
        }

        if (enchantment.apply(item, level)) {
            playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.85f);
            return true;
        }
        return false;
    }

    private static void playSound(Player player, Sound sound, float pitch) {
        if (player != null) {
            player.playSound(player.getLocation(), sound, 1.0f, pitch);
        }
    }

    private void updateOnlinePlayers(ItemUpdater updater) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateInventory(player.getInventory(), player, updater);
        }
    }

    private void updateInventory(PlayerInventory inventory, Player player) {
        updateInventory(inventory, player, this::applyEnchantments);
    }

    private static void updateInventory(PlayerInventory inventory, Player player, ItemUpdater updater) {
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            ItemStack item = inventory.getItem(slot);
            if (item != null && !item.isEmpty() && updater.update(item, player)) {
                inventory.setItem(slot, item);
            }
        }
    }
}