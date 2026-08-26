package org.heather.hardlands.common.enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.util.RomanNumerals;
import org.heather.hardlands.util.data.PersistentData;
import org.heather.hardlands.util.text.HardlandsColor;

public enum HardlandsEnchantment {

    DEAD_EYE(
            "Dead Eye",
            "Incrementa en un 10% el daño de cada golpe consecutivo realizado dentro de un mismo combo.",
            3,
            Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON),
    SMELTING_TOUCH(
            "Smelting Touch",
            "Funde automáticamente los objetos obtenidos siempre que dispongan de una receta de horno válida.",
            Tag.ITEMS_ENCHANTABLE_MINING),
    WISDOM(
            "Wisdom",
            "Incrementa en un 25% por nivel la experiencia obtenida al extraer bloques.",
            5,
            Tag.ITEMS_ENCHANTABLE_MINING),
    VEIN_MINER(
            "Vein Miner",
            "Al extraer una mena, rompe automáticamente todas las menas conectadas que formen parte de la misma veta.",
            Tag.ITEMS_ENCHANTABLE_MINING),
    TIMBER(
            "Timber",
            "Al talar un tronco, rompe automáticamente todos los troncos conectados que formen parte del mismo árbol.",
            Tag.ITEMS_AXES),

    ;

    private final String name;
    private final String description;
    private final int maxLevel;
    private final Tag<Material> tag;

    HardlandsEnchantment(String name, String description, int maxLevel, Tag<Material> tag) {
        this.name = name;
        this.description = description;
        this.maxLevel = maxLevel;
        this.tag = tag;
    }

    HardlandsEnchantment(String name, String description, Tag<Material> tag) {
        this(name, description, 1, tag);
    }

    public static Optional<HardlandsEnchantment> fromString(String value) {
        String normalized = value.trim()
                .replace(' ', '_')
                .replace('-', '_')
                .toUpperCase();

        return Arrays.stream(values())
                .filter(enchantment -> enchantment.name().equals(normalized))
                .findFirst();
    }

    public void apply(ItemStack stack, int amplifier) {
        if (!this.tag.isTagged(stack.getType())) return;

        LeveledEnchantment data = new LeveledEnchantment(this, amplifier);

        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(this.namespacedKey(), PersistentDataType.INTEGER, data.amplifier());
            removeMatchingEnchantmentLore(meta, this.name);
            this.prependEnchantmentLore(meta, amplifier);
            updateVisuals(stack, meta);
        });
    }

    public void remove(ItemStack stack) {
        if (stack.getType().isAir()) return;

        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(this.namespacedKey());
            removeMatchingEnchantmentLore(meta, this.name);
            updateVisuals(stack, meta);
        });
    }

    public Optional<LeveledEnchantment> find(ItemMeta meta) {
        return PersistentData.find(meta, this.namespacedKey(), PersistentDataType.INTEGER)
                .filter(amplifier -> amplifier >= 0 && amplifier < this.maxLevel)
                .map(amplifier -> new LeveledEnchantment(this, amplifier));
    }

    public ItemStack enchantedBook(int amplifier) {
        ItemBuilder builder = new ItemBuilder(Material.ENCHANTED_BOOK);

        builder.addLore(
                this.beautifulName(amplifier),
                Component.empty(),
                Component.text(this.description, HardlandsColor.LIGHT_GRAY));

        return builder.build();
    }

    public NamespacedKey namespacedKey() {
        return Hardlands.namespacedKey(this.name().toLowerCase(Locale.ROOT));
    }

    public Component beautifulName(int amplifier) {
        String level = amplifier == 0 ? "" : " " + RomanNumerals.format(amplifier + 1);

        return Component.text(this.name + level, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    public String getName() {
        return this.name;
    }

    public int getMaxLevel() {
        return this.maxLevel;
    }

    public Tag<Material> getTag() {
        return this.tag;
    }

    public static boolean has(ItemMeta meta) {
        return Arrays.stream(values()).anyMatch(enchantment -> enchantment.find(meta).isPresent());
    }

    public static boolean has(ItemStack stack) {
        return !stack.getType().isAir() && has(stack.getItemMeta());
    }

    public record LeveledEnchantment(HardlandsEnchantment enchantment, int amplifier) {

        public LeveledEnchantment {
            if (amplifier < 0 || amplifier >= enchantment.maxLevel) {
                throw new IllegalArgumentException("Enchantment amplifier must be between 0 and %d: %s"
                        .formatted(enchantment.maxLevel - 1, enchantment.name()));
            }
        }
    }

    // Internal

    private static void updateVisuals(ItemStack stack, ItemMeta meta) {
        if (!has(meta) || meta.hasEnchants()) return;

        meta.setEnchantmentGlintOverride(true);

        if (meta.hasCustomName()) {
            meta.customName(meta.customName().color(NamedTextColor.AQUA));
            return;
        }

        Component name = meta.hasItemName()
                ? meta.itemName()
                : Component.translatable(stack.translationKey());

        meta.itemName(name.color(NamedTextColor.AQUA));
    }

    private void prependEnchantmentLore(ItemMeta meta, int amplifier) {
        List<Component> lore = new ArrayList<>();
        lore.add(this.beautifulName(amplifier));

        List<Component> currentLore = meta.lore();
        if (currentLore != null) lore.addAll(currentLore);

        meta.lore(lore);
    }

    private static void removeMatchingEnchantmentLore(ItemMeta meta, String display) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) return;

        List<Component> lore = new ArrayList<>(currentLore);
        String prefix = display + ' ';

        lore.removeIf(component -> {
            if (!(component instanceof TextComponent text)) return false;

            String content = text.content();
            if (content.equals(display)) return true;
            if (!content.startsWith(prefix)) return false;

            return switch (content.substring(prefix.length())) {
                case "I", "II", "III", "IV", "V" -> true;
                default -> false;
            };
        });

        meta.lore(lore.isEmpty() ? null : lore);
    }
}