package org.heather.hardlands.common.enchantment;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.heather.hardlands.Hardlands;

import java.util.ArrayList;
import java.util.List;

public enum HardlandsEnchantment {

    DEAD_EYE(
            Hardlands.namespacedKey("dead_eye"),
            "Dead Eye",
            "Incrementa el daño de cada golpe crítico consecutivo realizado dentro del mismo combo.",
            Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON,
            Limit.fromLevel(2)
    ),

    SMELTING_TOUCH(
            Hardlands.namespacedKey("smelting_touch"),
            "Smelting Touch",
            "Funde automáticamente los ítems obtenidos siempre que dispongan de una receta de horno válida.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.SINGLE_LEVEL_ENCHANT
    ),

    WISDOM(
            Hardlands.namespacedKey("wisdom"),
            "Wisdom",
            "Incrementa la experiencia obtenida al extraer bloques.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.fromLevel(5)
    ),

    VEIN_MINER(
            Hardlands.namespacedKey("vein_miner"),
            "Vein Miner",
            "Al extraer una mena, rompe automáticamente todas las menas conectadas que formen parte de la misma veta.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.SINGLE_LEVEL_ENCHANT
    ),

    TIMBER(
            Hardlands.namespacedKey("timber"),
            "Timber",
            "Al talar un tronco, rompe automáticamente todos los troncos conectados que formen parte del mismo árbol.",
            Tag.ITEMS_AXES,
            Limit.SINGLE_LEVEL_ENCHANT
    );

    private final NamespacedKey namespacedKey;
    private final String label;
    private final String description;
    private final Tag<Material> tag;
    private final Limit limit;

    HardlandsEnchantment(NamespacedKey namespacedKey, String label, String description, Tag<Material> enchantables, Limit limit) {
        this.namespacedKey = namespacedKey;
        this.label = label;
        this.description = description;
        this.tag = enchantables;
        this.limit = limit;
    }

    public NamespacedKey getNamespacedKey() {
        return this.namespacedKey;
    }

    public String getIdentifier() {
        return this.namespacedKey.getKey();
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }

    public Tag<Material> getTag() {
        return this.tag;
    }

    public Limit getLimit() {
        return this.limit;
    }

    // Functions

    private LeveledEnchantment leveledEnchantment(int amplifier) {
        return new LeveledEnchantment(this, amplifier);
    }

    private boolean validateMaterial(Material material) {
        return this.tag.isTagged(material) || !material.isAir();
    }

    public void apply(ItemStack stack, int amplifier) {
        if (!this.validateMaterial(stack.getType())) {
            return;
        }

        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().set(
                    this.namespacedKey,
                    PersistentDataType.INTEGER,
                    leveledEnchantment(amplifier).amplifier()
            );

            removeMatchingEnchantmentLore(meta);
            this.prependEnchantmentLore(meta, amplifier);
            updateVisuals(stack, meta);
        });
    }

    public void remove(ItemStack stack) {
        if (stack.getType().isAir()) return;

        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(this.namespacedKey());
            removeMatchingEnchantmentLore(meta, this.label);
            updateVisuals(stack, meta);
        });
    }

    public Optional<LeveledEnchantment> find(ItemMeta meta) {
        return PersistentData.find(meta, this.namespacedKey(), PersistentDataType.INTEGER)
                .filter(amplifier -> amplifier >= 0 && amplifier < this.limit)
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

    public Component beautifulName(int amplifier) {
        String level = amplifier == 0 ? "" : " " + RomanNumerals.format(amplifier + 1);

        return Component.text(this.label + level, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    public static boolean has(ItemMeta meta) {
        return Arrays.stream(values()).anyMatch(enchantment -> enchantment.find(meta).isPresent());
    }

    public static boolean has(ItemStack stack) {
        return !stack.getType().isAir() && has(stack.getItemMeta());
    }

    public record Limit(int maxAmplifier) {

        public static final Limit SINGLE_LEVEL_ENCHANT = new Limit(0);

        public boolean check(int amplifier) {
            return amplifier <= this.maxAmplifier;
        }

        public static Limit fromLevel(int maxLevel) {
            return new Limit(maxLevel - 1);
        }
    }

    public record LeveledEnchantment(HardlandsEnchantment enchantment, int amplifier) {

        public LeveledEnchantment {
            Limit limit = enchantment.getLimit();

            if (amplifier < 0 || !limit.check(amplifier)) {
                throw new IllegalArgumentException("Invalid amplifier %d; max is %d.".formatted(
                        amplifier,
                        limit.maxAmplifier()
                ));
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

    private void removeMatchingEnchantmentLore(ItemMeta meta) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) return;

        List<Component> lore = new ArrayList<>(currentLore);
        String prefix = this.label + ' ';

        lore.removeIf(component -> {
            if (!(component instanceof TextComponent text)) return false;

            String content = text.content();

            if (content.equals(this.label)) return true;

            if (!content.startsWith(prefix)) return false;

            return switch (content.substring(prefix.length())) {
                case "I", "II", "III", "IV", "V" -> true;
                default -> false;
            };
        });

        meta.lore(lore.isEmpty() ? null : lore);
    }
}