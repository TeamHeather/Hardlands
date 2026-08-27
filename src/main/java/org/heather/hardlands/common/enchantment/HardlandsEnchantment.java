package org.heather.hardlands.common.enchantment;

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
import org.heather.hardlands.core.data.PersistentData;
import org.heather.hardlands.util.RomanNumerals;
import org.heather.hardlands.util.text.HardlandsColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

public enum HardlandsEnchantment {

    DEAD_EYE(
            Hardlands.createNamespacedKey("dead_eye"),
            "Dead Eye",
            "Incrementa el daño de cada golpe crítico consecutivo realizado dentro del mismo combo.",
            Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON,
            Limit.fromLevel(2)
    ),

    SMELTING_TOUCH(
            Hardlands.createNamespacedKey("smelting_touch"),
            "Smelting Touch",
            "Funde automáticamente los ítems obtenidos siempre que dispongan de una receta de horno válida.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.SINGLE_LEVEL_ENCHANT
    ),

    WISDOM(
            Hardlands.createNamespacedKey("wisdom"),
            "Wisdom",
            "Incrementa la experiencia obtenida al extraer bloques.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.fromLevel(5)
    ),

    VEIN_MINER(
            Hardlands.createNamespacedKey("vein_miner"),
            "Vein Miner",
            "Al extraer una mena, rompe automáticamente todas las menas conectadas que formen parte de la misma veta.",
            Tag.ITEMS_ENCHANTABLE_MINING,
            Limit.SINGLE_LEVEL_ENCHANT
    ),

    TIMBER(
            Hardlands.createNamespacedKey("timber"),
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

    //* Public API

    public static Optional<HardlandsEnchantment> fromString(String value) {
        return Arrays.stream(values())
                .filter(enchantment -> enchantment.createIdentifier().equalsIgnoreCase(value))
                .findFirst();
    }

    public static boolean containsHardlandsEnchantment(ItemMeta meta) {
        return Arrays.stream(values())
                .anyMatch(enchantment -> enchantment.findLevel(meta).isPresent());
    }

    public void apply(ItemStack stack, int amplifier) {
        this.editIfCompatible(stack, meta -> {
            meta.getPersistentDataContainer().set(
                    this.namespacedKey,
                    PersistentDataType.INTEGER,
                    createLeveledEnchantment(amplifier).amplifier()
            );

            this.removeMatchingEnchantmentLore(meta);
            this.prependEnchantmentLore(meta, amplifier);

            updateVisuals(stack, meta);
        });
    }

    public void remove(ItemStack stack) {
        this.editIfCompatible(stack, meta -> {
            meta.getPersistentDataContainer().remove(this.namespacedKey);

            this.removeMatchingEnchantmentLore(meta);

            updateVisuals(stack, meta);
        });
    }

    public Optional<Integer> findLevel(ItemStack stack) {
        return this.findLevel(stack.getItemMeta());
    }

    public Optional<Integer> findLevel(ItemMeta meta) {
        return PersistentData.find(meta, this.namespacedKey, PersistentDataType.INTEGER);
    }

    public ItemStack enchantedBook(int amplifier) {
        ItemBuilder builder = new ItemBuilder(Material.ENCHANTED_BOOK);
        builder.addLore(
                this.createBeautifulName(amplifier),
                Component.empty(),
                Component.text(this.description, HardlandsColor.LIGHT_GRAY)
        );
        return builder.build();
    }

    public Component createBeautifulName(int amplifier) {
        String level = amplifier != 0
                ? " " + RomanNumerals.format(amplifier + 1)
                : "";
        return Component.text(
                this.label + level,
                NamedTextColor.GRAY
        ).decoration(TextDecoration.ITALIC, false);
    }

    public String createIdentifier() {
        return this.namespacedKey.getKey();
    }

    @SafeVarargs
    public final Optional<Integer> findMatchingLevel(ItemStack stack, Predicate<ItemStack>... predicates) {
        if (!this.tag.isTagged(stack.getType())) return Optional.empty();

        return this.findLevel(stack)
                .filter(_ -> Arrays.stream(predicates).allMatch(predicate -> predicate.test(stack)));
    }

    @SafeVarargs
    public final boolean matches(ItemStack stack, Predicate<ItemStack>... predicates) {
        return this.findMatchingLevel(stack, predicates).isPresent();
    }

    // Getters
    public NamespacedKey getNamespacedKey() {
        return this.namespacedKey;
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

    //* Subclasses

    public record Limit(int maxAmplifier) {

        public static final Limit SINGLE_LEVEL_ENCHANT = new Limit(0);

        public boolean check(int amplifier) {
            return amplifier <= this.maxAmplifier;
        }

        public static Limit fromLevel(int maxLevel) {
            return new Limit(maxLevel - 1);
        }
    }

    public record LeveledEnchantment(
            HardlandsEnchantment enchantment,
            int amplifier
    ) {

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

    //* Internal Class Utilities

    private static void updateVisuals(ItemStack stack, ItemMeta meta) {
        if (!containsHardlandsEnchantment(meta) || meta.hasEnchants()) return;

        meta.setEnchantmentGlintOverride(true);

        Component customName = meta.customName();
        if (customName != null) {
            meta.customName(customName.color(NamedTextColor.AQUA));
            return;
        }

        meta.itemName(meta.hasItemName()
                ? meta.itemName()
                : Component.translatable(stack.translationKey()).color(NamedTextColor.AQUA));
    }

    private void editIfCompatible(ItemStack stack, Consumer<ItemMeta> operation) {
        if (this.tag.isTagged(stack.getType())) {
            stack.editMeta(operation);
        }
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

    private void prependEnchantmentLore(ItemMeta meta, int amplifier) {
        List<Component> lore = new ArrayList<>();
        lore.add(this.createBeautifulName(amplifier));

        List<Component> currentLore = meta.lore();
        if (currentLore != null) {
            lore.addAll(currentLore);
        }

        meta.lore(lore);
    }

    private LeveledEnchantment createLeveledEnchantment(int amplifier) {
        return new LeveledEnchantment(this, amplifier);
    }
}