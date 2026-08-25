package org.heather.hardlands.enchantment;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.util.data.PersistentData;

import java.util.Arrays;
import java.util.Optional;

public enum HardlandsEnchantment {

    SMELTING_TOUCH(Tag.ITEMS_ENCHANTABLE_MINING, "Smelting Touch",
            "Funde automáticamente cualquier drop que tenga una receta válida de horno.")

    ;

    private final Tag<Material> enchantable;
    private final String display;
    private final String description;
    private final int maxLevel;

    HardlandsEnchantment(Tag<Material> enchantable, String display, String description, int maxLevel) {
        this.display = display;
        this.description = description;
        this.maxLevel = maxLevel;
        this.enchantable = enchantable;
    }

    HardlandsEnchantment(Tag<Material> enchantable, String display, String description) {
        this(enchantable, display, description, 1);
    }


    public static boolean has(ItemStack stack) {
        return Arrays.stream(HardlandsEnchantment.values())
                .anyMatch(enchantment -> enchantment.find(stack).isPresent());
    }

    public void apply(ItemStack stack, int amplifier) {
        EnchantmentContext context = new EnchantmentContext(this, amplifier);
        context.applyData(stack);

        stack.editMeta(meta -> {
            boolean glint = !meta.hasEnchants() && !has(stack);
            meta.setEnchantmentGlintOverride(glint);
        });
    }

    public Optional<EnchantmentContext> find(ItemStack stack) {
        return PersistentData.find(stack.getItemMeta(), this.namespacedKey(), PersistentDataType.INTEGER)
                .filter(amplifier -> amplifier != EnchantmentContext.REMOVE)
                .map(level -> new EnchantmentContext(this, level));
    }

    public NamespacedKey namespacedKey() {
        return Hardlands.namespacedKey(this.name());
    }


    public record EnchantmentContext(HardlandsEnchantment enchantment, int amplifier) {

        public static final int REMOVE = -1;

        public EnchantmentContext {
            if (this.amplifier() > this.enchantment().maxLevel) {
                throw new IllegalArgumentException("The maximum level is %s for enchantment: %s".formatted(
                        this.enchantment().maxLevel,
                        this.enchantment().name()));
            }
        }

        public void applyData(ItemStack stack) {
            stack.editMeta(meta -> meta.getPersistentDataContainer().set(
                    this.enchantment.namespacedKey(),
                    PersistentDataType.INTEGER,
                    this.amplifier()));
        }
    }
}