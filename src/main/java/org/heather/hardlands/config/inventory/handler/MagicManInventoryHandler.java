package org.heather.hardlands.config.inventory.handler;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.config.inventory.HardlandsInventory;
import org.heather.hardlands.config.inventory.HardlandsInventoryHolder;
import org.heather.hardlands.module.scenario.scenarios.MagicManScenario;
import org.heather.hardlands.util.text.TextFormatter;

public final class MagicManInventoryHandler implements InventoryHandler {

    private static final int INVENTORY_SIZE = 54;
    private static final int CONTENT_SIZE = 45;
    private static final int PREVIOUS_SLOT = 48;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 50;
    private static final String VANILLA_NAMESPACE = NamespacedKey.MINECRAFT;
    private static final String CLICK_HELP = "<dark_gray>Izq. Vanilla | Der. +1 | Shift + Der. -1";

    private static final EnchantmentDefinition[] CUSTOM_ENCHANTMENTS =
            EnchantmentDefinition.values();
    private static final List<Enchantment> VANILLA_ENCHANTMENTS =
            getVanillaEnchantments();
    private static final int ENCHANTMENT_COUNT =
            CUSTOM_ENCHANTMENTS.length + VANILLA_ENCHANTMENTS.size();

    private final MagicManScenario scenario;
    private int page;

    public MagicManInventoryHandler(MagicManScenario scenario) {
        this.scenario = scenario;
    }

    public void open(Player player) {
        HardlandsInventoryHolder holder = new HardlandsInventoryHolder(this);
        Inventory inventory = Bukkit.createInventory(holder, INVENTORY_SIZE,
                TextFormatter.formatTinyCaps("Magic Man | Encantamientos"));
        holder.setInventory(inventory);
        render(inventory);
        player.openInventory(inventory);
        onOpen(inventory, player);
    }

    @Override
    public void render(Inventory inventory) {
        inventory.clear();
        int start = page * CONTENT_SIZE;
        int end = Math.min(start + CONTENT_SIZE, ENCHANTMENT_COUNT);

        for (int i = start; i < end; i++) {
            inventory.setItem(i - start, createEnchantmentItem(i));
        }

        if (page > 0) {
            inventory.setItem(PREVIOUS_SLOT, InventoryItem.PREVIOUS.build());
        }
        if (page < getLastPage()) {
            inventory.setItem(NEXT_SLOT, InventoryItem.NEXT.build());
        }
        inventory.setItem(BACK_SLOT, createBackItem());
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        int slot = event.getRawSlot();

        if (slot == PREVIOUS_SLOT && page > 0) {
            page--;
            render(event.getView().getTopInventory());
            return Optional.of(true);
        }

        if (slot == NEXT_SLOT && page < getLastPage()) {
            page++;
            render(event.getView().getTopInventory());
            return Optional.of(true);
        }

        if (slot == BACK_SLOT) {
            HardlandsInventory.SCENARIOS.openInventory(player);
            return Optional.of(true);
        }

        if (slot < 0 || slot >= CONTENT_SIZE) {
            return Optional.empty();
        }

        int index = page * CONTENT_SIZE + slot;
        if (index >= ENCHANTMENT_COUNT) {
            return Optional.empty();
        }

        boolean changed = updateEnchantment(event, index);
        if (changed) {
            event.setCurrentItem(createEnchantmentItem(index));
        }

        return Optional.of(changed);
    }

    private boolean updateEnchantment(InventoryClickEvent event, int index) {
        if (index < CUSTOM_ENCHANTMENTS.length) {
            return updateCustomEnchantment(event, index);
        }
        return updateVanillaEnchantment(event, index);
    }

    private boolean updateCustomEnchantment(InventoryClickEvent event, int index) {
        EnchantmentDefinition enchantment = CUSTOM_ENCHANTMENTS[index];
        int currentLevel = scenario.getEnchantmentLevel(enchantment);
        int newLevel = calculateNewLevel(event, currentLevel, enchantment.getMaxLevel());

        if (newLevel != currentLevel) {
            return scenario.setEnchantmentLevel(enchantment, newLevel);
        }
        return false;
    }

    private boolean updateVanillaEnchantment(InventoryClickEvent event, int index) {
        Enchantment enchantment =
                VANILLA_ENCHANTMENTS.get(index - CUSTOM_ENCHANTMENTS.length);
        int currentLevel = scenario.getEnchantmentLevel(enchantment);
        int newLevel = calculateNewLevel(event, currentLevel, enchantment.getMaxLevel());

        if (newLevel != currentLevel) {
            return scenario.setEnchantmentLevel(enchantment, newLevel);
        }
        return false;
    }

    private ItemStack createEnchantmentItem(int index) {
        if (index < CUSTOM_ENCHANTMENTS.length) {
            EnchantmentDefinition enchantment = CUSTOM_ENCHANTMENTS[index];
            int level = scenario.getEnchantmentLevel(enchantment);
            return buildEnchantmentItem(enchantment.getDisplayName(), level,
                    enchantment.getMaxLevel(), true);
        }

        Enchantment enchantment =
                VANILLA_ENCHANTMENTS.get(index - CUSTOM_ENCHANTMENTS.length);
        int level = scenario.getEnchantmentLevel(enchantment);
        return buildEnchantmentItem(formatVanillaName(enchantment.getKey().getKey()),
                level, enchantment.getMaxLevel(), false);
    }

    private static ItemStack buildEnchantmentItem(String name, int level, int maxLevel,
                                                  boolean custom) {
        ItemBuilder builder = new ItemBuilder(getMaterialForLevel(level))
                .name(TextFormatter.formatTinyCaps(name).color(getColorForLevel(level)))
                .glint(level > 0)
                .formattedLore(
                        "Estado: [%s]".formatted(getStateForLevel(level)),
                        "Nivel: [%s]".formatted(level > 0 ? level : "-"),
                        "Máximo: [%d]".formatted(maxLevel));

        if (custom) {
            builder.addFormattedLore("Origen: {Hardlands}");
        }

        return builder.addFormattedLore("", CLICK_HELP).build();
    }

    private ItemStack createBackItem() {
        return new ItemBuilder(Material.BARRIER)
                .name(TextFormatter.formatTinyCaps("Volver").color(NamedTextColor.GRAY))
                .addFormattedLore("<dark_gray>Página %d/%d".formatted(page + 1,
                        getLastPage() + 1))
                .build();
    }

    private static int calculateNewLevel(InventoryClickEvent event, int level, int maxLevel) {
        if (event.isLeftClick()) {
            return MagicManScenario.VANILLA_LEVEL;
        }
        if (!event.isRightClick()) {
            return level;
        }
        return event.isShiftClick() ? Math.max(MagicManScenario.DISABLED_LEVEL, level - 1)
                : Math.min(maxLevel, level + 1);
    }

    private static Material getMaterialForLevel(int level) {
        if (level < 0) {
            return Material.BARRIER;
        }
        if (level == 0) {
            return Material.BOOK;
        }
        return Material.ENCHANTED_BOOK;
    }

    private static NamedTextColor getColorForLevel(int level) {
        if (level < 0) {
            return NamedTextColor.RED;
        }
        if (level == 0) {
            return NamedTextColor.GRAY;
        }
        return NamedTextColor.GREEN;
    }

    private static String getStateForLevel(int level) {
        if (level < 0) {
            return "Prohibido";
        }
        if (level == 0) {
            return "Vanilla";
        }
        return "Forzado";
    }

    private int getLastPage() {
        return Math.max(0, (ENCHANTMENT_COUNT - 1) / CONTENT_SIZE);
    }

    private static String formatVanillaName(String key) {
        StringBuilder name = new StringBuilder();
        boolean capitalize = true;

        for (char character : key.toCharArray()) {
            if (character == '_') {
                name.append(' ');
                capitalize = true;
            } else {
                name.append(capitalize ? Character.toUpperCase(character) : character);
                capitalize = false;
            }
        }

        return name.toString();
    }

    private static List<Enchantment> getVanillaEnchantments() {
        Registry<Enchantment> registry =
                RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        List<Enchantment> enchantments = new ArrayList<>();

        for (Enchantment enchantment : registry) {
            if (enchantment.getKey().getNamespace().equals(VANILLA_NAMESPACE)) {
                enchantments.add(enchantment);
            }
        }

        enchantments.sort(Comparator.comparing(e -> e.getKey().asString()));
        return List.copyOf(enchantments);
    }
}