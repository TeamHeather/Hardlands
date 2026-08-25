package org.heather.hardlands.enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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

    SMELTING_TOUCH(
            "Smelting Touch",
            "Funde automáticamente cualquier drop que tenga una receta válida de horno.",
            Tag.ITEMS_ENCHANTABLE_MINING),

    DEAD_EYE(
            "Dead Eye",
            "Aumenta ligeramente el daño de cada golpe consecutivo realizado en combo.",
            3,
            Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON),

    WISDOM(
            "Wisdom",
            "Incrementa en un 25% por nivel la experiencia obtenida al extraer bloques.",
            5,
            Tag.ITEMS_ENCHANTABLE_MINING),

    TIMBER(
            "Timber",
            "Al romper un tronco, rompe automáticamente todos los troncos conectados que pertenezcan al mismo árbol.",
            Tag.ITEMS_ENCHANTABLE_MINING),

    VEIN_MINER(
            "Vein Miner",
            "Al romper una mena, rompe automáticamente todas las menas conectadas que pertenezcan a la misma veta.",
            Tag.ITEMS_ENCHANTABLE_MINING);

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

    public static boolean has(ItemStack stack) {
        return Arrays.stream(values()).anyMatch(enchantment ->
                enchantment.find(stack).isPresent());
    }

    public void apply(ItemStack stack, int amplifier) {
        this.storeEnchantmentData(stack, amplifier).ifPresent(_ ->
                stack.editMeta(meta -> {
                    updateGlintOverride(stack);
                    this.prependEnchantmentLore(meta, amplifier);
                })
        );
    }

    public void remove(ItemStack stack) {
        this.storeEnchantmentData(stack, LeveledEnchantment.REMOVE).ifPresent(_ ->
                stack.editMeta(meta -> {
                    updateGlintOverride(stack);
                    removeMatchingEnchantmentLore(meta, this.name);
                }));
    }

    public Optional<LeveledEnchantment> find(ItemStack stack) {
        return PersistentData.find(stack.getItemMeta(), this.namespacedKey(), PersistentDataType.INTEGER)
                .filter(amplifier -> amplifier != LeveledEnchantment.REMOVE)
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
        return Hardlands.namespacedKey(this.name());
    }

    public Component beautifulName(int amplifier) {
        String level = amplifier == 0 ? "" : " " + RomanNumerals.format(amplifier);
        return Component.text(this.name + level, NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false);
    }

    public record LeveledEnchantment(HardlandsEnchantment enchantment, int amplifier) {

        private static final int REMOVE = -1;

        public LeveledEnchantment {
            if (amplifier > enchantment.maxLevel) {
                throw new IllegalArgumentException("The maximum level is %s for enchantment: %s"
                        .formatted(enchantment.maxLevel, enchantment.name()));
            }
        }

    }

    // Internal

    private void prependEnchantmentLore(ItemMeta meta, int amplifier) {
        List<Component> lore = new ArrayList<>();
        lore.add(beautifulName(amplifier));

        List<Component> currentLore = meta.lore();
        if (currentLore != null) lore.addAll(currentLore);

        meta.lore(lore);
    }

    private Optional<LeveledEnchantment> storeEnchantmentData(ItemStack stack, int amplifier) {
        if (!this.tag.isTagged(stack.getType())) {
            return Optional.empty();
        }

        LeveledEnchantment data = new LeveledEnchantment(this, amplifier);

        stack.editMeta(meta -> meta.getPersistentDataContainer().set(
                this.namespacedKey(),
                PersistentDataType.INTEGER,
                data.amplifier()
        ));

        return Optional.of(data);
    }

    private static void removeMatchingEnchantmentLore(ItemMeta meta, String display) {
        List<Component> lore = meta.lore();
        if (lore == null) return;

        lore.removeIf(component -> {
            if (!(component instanceof TextComponent text)) return false;

            String content = text.content();
            if (content.equals(display)) return true;

            return switch (content.substring(display.length() + 1)) {
                case "I", "II", "III", "IV", "V" -> true;
                default -> false;
            };
        });

        meta.lore(lore.isEmpty() ? null : lore);
    }

    private static void updateGlintOverride(ItemStack stack) {
        stack.editMeta(meta -> {
            boolean needsOverride = has(stack) && !meta.hasEnchants();
            meta.setEnchantmentGlintOverride(needsOverride ? true : null);
        });
    }
}