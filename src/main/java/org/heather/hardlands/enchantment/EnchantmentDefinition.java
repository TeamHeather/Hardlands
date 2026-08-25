package org.heather.hardlands.enchantment;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.papermc.paper.persistence.PersistentDataContainerView;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.util.RomanNumerals;

public enum EnchantmentDefinition {

    DEAD_EYE("Dead Eye",
            "Aumenta ligeramente el daño de cada golpe consecutivo realizado en combo.",
            3, Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON),

    WISDOM("Wisdom",
            "Incrementa en un 25% por nivel la experiencia obtenida al extraer bloques.",
            5, Tag.ITEMS_ENCHANTABLE_MINING),

    SMELTING_TOUCH("Smelting Touch",
            "Funde automáticamente cualquier drop que tenga una receta válida de horno.",
            1, Tag.ITEMS_ENCHANTABLE_MINING),

    TIMBER("Timber",
            "Al romper un tronco, rompe automáticamente todos los troncos conectados que pertenezcan al mismo árbol.",
            1, Tag.ITEMS_ENCHANTABLE_MINING),

    VEIN_MINER("Vein Miner",
            "Al romper una mena, rompe automáticamente todas las menas conectadas que pertenezcan a la misma veta.",
            1, Tag.ITEMS_ENCHANTABLE_MINING);

    public static final String NAMESPACE = "hardlands";
    private static final String GLINT_MARKER = "hardlands_enchantment_glint";

    private static final PlainTextComponentSerializer PLAIN_TEXT =
            PlainTextComponentSerializer.plainText();
    private static final Map<String, EnchantmentDefinition> BY_IDENTIFIER =
            Arrays.stream(values())
                    .collect(Collectors.toUnmodifiableMap(
                            EnchantmentDefinition::getIdentifier,
                            Function.identity()));

    private final String identifier;
    private final String displayName;
    private final String description;
    private final int maxLevel;
    private final Tag<Material> enchantable;
    private final NamespacedKey key;

    EnchantmentDefinition(String displayName, String description, int maxLevel,
                          Tag<Material> enchantable) {
        this.identifier = name().toLowerCase(Locale.ROOT);
        this.displayName = displayName;
        this.description = description;
        this.maxLevel = maxLevel;
        this.enchantable = enchantable;
        this.key = new NamespacedKey(NAMESPACE, this.identifier);
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public NamespacedKey getKey() {
        return this.key;
    }

    public boolean canEnchant(Material material) {
        return this.enchantable.isTagged(material);
    }

    public int getLevel(ItemStack item) {
        Integer storedLevel = item.getPersistentDataContainer().get(this.key,
                PersistentDataType.INTEGER);
        if (storedLevel != null && storedLevel > 0) {
            return Math.clamp(storedLevel, 1, this.maxLevel);
        }
        return 0;
    }

    public boolean apply(ItemStack item, int level) {
        if (level < 1 || level > this.maxLevel || !canEnchant(item.getType())) {
            return false;
        }

        int current = getLevel(item);
        if (current == level) {
            return false;
        }

        if (current > 0) {
            remove(item);
        }

        item.editPersistentDataContainer(
                container -> container.set(this.key, PersistentDataType.INTEGER, level));

        applyGlintEffect(item);

        ItemLore currentLore = item.getData(DataComponentTypes.LORE);
        List<Component> lore =
                new ArrayList<>(currentLore == null ? List.of() : currentLore.lines());

        lore.add(0, createEnchantmentLoreLine(level));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return true;
    }

    public boolean remove(ItemStack item) {
        PersistentDataContainerView container = item.getPersistentDataContainer();
        ItemLore currentLore = item.getData(DataComponentTypes.LORE);
        boolean stored = container.has(this.key, PersistentDataType.INTEGER);
        boolean loreChanged = false;

        if (stored) {
            item.editPersistentDataContainer(data -> data.remove(this.key));
        }

        if (currentLore != null) {
            List<Component> lore = new ArrayList<>(currentLore.lines());
            loreChanged = lore.removeIf(this::isEnchantmentLine);

            if (loreChanged) {
                if (lore.isEmpty()) {
                    item.unsetData(DataComponentTypes.LORE);
                } else {
                    item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
                }
            }
        }

        removeGlintEffect(item);
        return stored || loreChanged;
    }

    public ItemStack createBook(int level) {
        if (level < 1 || level > this.maxLevel) {
            throw new IllegalArgumentException(
                    "Invalid level %d for %s".formatted(level, this.displayName));
        }

        ItemStack book = new ItemBuilder(Material.ENCHANTED_BOOK)
                .formattedLore(formatLore(level), this.description)
                .build();

        book.editPersistentDataContainer(
                container -> container.set(this.key, PersistentDataType.INTEGER, level));

        ItemLore currentLore = book.getData(DataComponentTypes.LORE);
        List<Component> lore =
                new ArrayList<>(currentLore == null ? List.of() : currentLore.lines());
        lore.set(0, createEnchantmentLoreLine(level));
        book.setData(DataComponentTypes.LORE, ItemLore.lore(lore));

        return book;
    }

    private Component createEnchantmentLoreLine(int level) {
        return Component.text(formatLore(level))
                .decoration(TextDecoration.ITALIC, true);
    }

    private boolean isEnchantmentLine(Component component) {
        String plainText = PLAIN_TEXT.serialize(component);
        for (int level = 1; level <= this.maxLevel; level++) {
            if (plainText.equals(formatLore(level))) {
                return true;
            }
        }
        return false;
    }

    private String formatLore(int level) {
        return "%s %s".formatted(this.displayName, RomanNumerals.format(level));
    }

    private static void applyGlintEffect(ItemStack item) {
        if (item.getEnchantments().size() > 0) {
            return;
        }

        try {
            Registry<Enchantment> registry = Bukkit.getRegistry(Enchantment.class);
            if (registry != null) {
                Enchantment marker = registry.get(
                        NamespacedKey.minecraft("unbreaking"));
                if (marker != null) {
                    item.addUnsafeEnchantment(marker, 1);
                    item.editPersistentDataContainer(
                            container -> container.set(
                                    new NamespacedKey("hardlands", GLINT_MARKER),
                                    PersistentDataType.BYTE, (byte) 1));
                }
            }
        } catch (Exception ignored) {
            // Silently fail if enchantment registration fails
        }
    }

    private static void removeGlintEffect(ItemStack item) {
        NamespacedKey glintKey = new NamespacedKey("hardlands", GLINT_MARKER);
        if (!item.getPersistentDataContainer().has(glintKey, PersistentDataType.BYTE)) {
            return;
        }

        try {
            Registry<Enchantment> registry = Bukkit.getRegistry(Enchantment.class);
            if (registry != null) {
                Enchantment marker = registry.get(
                        NamespacedKey.minecraft("unbreaking"));
                if (marker != null && item.containsEnchantment(marker)) {
                    item.removeEnchantment(marker);
                }
            }
        } catch (Exception ignored) {
            // Silently fail if enchantment removal fails
        }

        item.editPersistentDataContainer(
                container -> container.remove(glintKey));
    }

    public static EnchantmentDefinition find(NamespacedKey key) {
        if (!key.getNamespace().equals(NAMESPACE)) {
            return null;
        }
        return BY_IDENTIFIER.get(key.getKey());
    }
}