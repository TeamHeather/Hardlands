package team.heather.hardlands.module.enchantment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.feature.item.ItemBuilder;
import team.heather.hardlands.core.data.pdc.PersistentData;
import team.heather.hardlands.module.enchantment.handler.*;
import team.heather.hardlands.util.text.RomanNumerals;
import team.heather.hardlands.util.HardlandsColor;

public enum HardlandsEnchantment {

    DEAD_EYE(
            Hardlands.createKey("dead_eye"),
            "Dead Eye",
            "Incrementa el daño de cada golpe crítico consecutivo realizado dentro del mismo combo.",
            Limit.fromLevel(2),
            Tag.ITEMS_ENCHANTABLE_SHARP_WEAPON,
            new DeadEyeHandler()
    ),

    SMELTING_TOUCH(
            Hardlands.createKey("smelting_touch"),
            "Smelting Touch",
            "Funde automáticamente los ítems obtenidos siempre que dispongan de una receta de horno válida.",
            Limit.SINGLE_LEVEL_ENCHANT,
            Tag.ITEMS_ENCHANTABLE_MINING,
            new SmeltingTouchHandler()
    ),

    WISDOM(
            Hardlands.createKey("wisdom"),
            "Wisdom",
            "Incrementa la experiencia obtenida al extraer bloques.",
            Limit.fromLevel(5),
            Tag.ITEMS_ENCHANTABLE_MINING,
            new WisdomHandler()
    ),

    VEIN_MINER(
            Hardlands.createKey("vein_miner"),
            "Vein Miner",
            "Al extraer una mena, rompe automáticamente todas las menas conectadas que formen parte de la misma veta.",
            Limit.SINGLE_LEVEL_ENCHANT,
            Tag.ITEMS_ENCHANTABLE_MINING,
            new VeinMinerHandler()
    ),

    TIMBER(
            Hardlands.createKey("timber"),
            "Timber",
            "Al talar un tronco, rompe automáticamente todos los troncos conectados que formen parte del mismo árbol.",
            Limit.SINGLE_LEVEL_ENCHANT,
            Tag.ITEMS_AXES,
            new TimberHandler()
    );

    private final NamespacedKey namespacedKey;
    private final String label;
    private final String description;
    private final Limit limit;
    private final Tag<Material> tag;
    private final EnchantmentHandler<?> handler;

    HardlandsEnchantment(
            NamespacedKey namespacedKey,
            String label,
            String description,
            Limit limit,
            Tag<Material> tag,
            EnchantmentHandler<?> handler
    ) {
        this.namespacedKey = namespacedKey;
        this.label = label;
        this.description = description;
        this.limit = limit;
        this.tag = tag;
        this.handler = handler;
    }

    public NamespacedKey getNamespacedKey() {
        return namespacedKey;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public Limit getLimit() {
        return limit;
    }

    public Tag<Material> getTag() {
        return tag;
    }

    public EnchantmentHandler<?> getHandler() {
        return handler;
    }

    public int createMaxLevel() {
        return this.limit.maxAmplifier() + 1;
    }

    public boolean applyIfCompatible(ItemStack stack, int amplifier) {
        if (amplifier == -1) return remove(stack);

        return editIfCompatible(stack, meta -> {
            meta.getPersistentDataContainer().set(
                    namespacedKey,
                    PersistentDataType.INTEGER,
                    createLeveledEnchantment(amplifier).amplifier()
            );

            removeMatchingEnchantmentLore(meta);
            prependEnchantmentLore(meta, amplifier);

            updateVisuals(stack, meta);
        });
    }

    public boolean remove(ItemStack stack) {
        if (findLevel(stack).isEmpty()) return false;

        stack.editMeta(meta -> {
            meta.getPersistentDataContainer().remove(namespacedKey);
            removeMatchingEnchantmentLore(meta);
            updateVisuals(stack, meta);
        });

        return true;
    }

    public Optional<Integer> findLevel(ItemStack stack) {
        return findLevel(stack.getItemMeta());
    }

    public Optional<Integer> findLevel(ItemMeta meta) {
        return PersistentData.find(meta, namespacedKey, PersistentDataType.INTEGER);
    }

    public ItemStack createEnchantedBook(int amplifier) {
        ItemBuilder builder = new ItemBuilder(Material.ENCHANTED_BOOK);

        builder.addLore(
                createBeautifulName(amplifier),
                Component.empty(),
                Component.text(description, HardlandsColor.LIGHT_GRAY)
        );

        return builder.build();
    }

    public Component createBeautifulName(int amplifier) {
        String level = amplifier != 0
                ? " " + RomanNumerals.format(amplifier + 1)
                : "";

        return Component.text(label + level, NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    public String createIdentifier() {
        return namespacedKey.getKey();
    }

    @SafeVarargs
    public final Optional<Integer> findMatchingLevel(ItemStack stack, Predicate<ItemStack>... predicates) {
        if (!tag.isTagged(stack.getType())) return Optional.empty();

        return findLevel(stack)
                .filter(_ -> Arrays.stream(predicates).allMatch(predicate -> predicate.test(stack)));
    }

    @SafeVarargs
    public final boolean matches(ItemStack stack, Predicate<ItemStack>... predicates) {
        return findMatchingLevel(stack, predicates).isPresent();
    }

    public static Optional<HardlandsEnchantment> findByKey(String value) {
        for (HardlandsEnchantment enchantment : values()) {
            if (enchantment.createIdentifier().equalsIgnoreCase(value)) {
                return Optional.of(enchantment);
            }
        }

        return Optional.empty();
    }

    public static boolean matches(ItemMeta meta) {
        for (HardlandsEnchantment enchantment : values()) {
            if (enchantment.findLevel(meta).isPresent()) return true;
        }

        return false;
    }

    private boolean editIfCompatible(ItemStack stack, Consumer<ItemMeta> operation) {
        if (tag.isTagged(stack.getType())) {
            stack.editMeta(operation);
            return true;
        }
        return false;
    }

    private void removeMatchingEnchantmentLore(ItemMeta meta) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) return;

        List<Component> lore = new ArrayList<>(currentLore);
        String prefix = label + ' ';

        lore.removeIf(component -> {
            if (!(component instanceof TextComponent text)) return false;

            String content = text.content();

            if (content.equals(label)) return true;
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
        lore.add(createBeautifulName(amplifier));

        List<Component> currentLore = meta.lore();
        if (currentLore != null) {
            lore.addAll(currentLore);
        }

        meta.lore(lore);
    }

    private LeveledEnchantment createLeveledEnchantment(int amplifier) {
        return new LeveledEnchantment(this, amplifier);
    }

    private static void updateVisuals(ItemStack stack, ItemMeta meta) {
        if (!matches(meta) || meta.hasEnchants()) return;

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

    public record Limit(int maxAmplifier) {

        public static final Limit SINGLE_LEVEL_ENCHANT = new Limit(0);

        public boolean check(int amplifier) {
            return amplifier <= maxAmplifier;
        }

        public static Limit fromLevel(int maxLevel) {
            return new Limit(maxLevel - 1);
        }
    }

    public record LeveledEnchantment(HardlandsEnchantment enchantment, int amplifier) {

        public LeveledEnchantment {
            Limit limit = enchantment.getLimit();

            if (amplifier < 0 || !limit.check(amplifier)) {
                throw new IllegalArgumentException("Invalid amplifier %d; max is %d.".formatted(amplifier, limit.maxAmplifier()));
            }
        }
    }
}