package org.heather.hardlands.common.ui.inventory.handler;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.module.enchantment.HardlandsEnchantment;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.common.ui.inventory.HardlandsInventory;
import org.heather.hardlands.module.scenario.Scenario;
import org.heather.hardlands.module.scenario.ScenarioDefinition;
import org.heather.hardlands.module.scenario.implementation.MagicManScenario;
import org.heather.hardlands.util.RomanNumerals;
import org.heather.hardlands.util.text.HardlandsColor;
import org.heather.hardlands.util.text.TextFormatter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class MagicManInventoryHandler implements InventoryHandler {

    private static final int PROHIBITED_AMPLIFIER = -2;
    private static final int VANILLA_AMPLIFIER = -1;

    private final MagicManScenario scenario = getMagicManScenario();
    private final List<EnchantmentEntry> enchantments = createEnchantments();

    private int page;

    @Override
    public void render(Inventory inventory) {
        int capacity = HardlandsInventory.getContentCapacity(inventory);
        int startIndex = this.page * capacity;
        int endIndex = Math.min(startIndex + capacity, this.enchantments.size());

        clearContent(inventory);

        for (int index = startIndex; index < endIndex; index++) {
            EnchantmentEntry enchantment = this.enchantments.get(index);

            inventory.setItem(
                    HardlandsInventory.contentSlot(index - startIndex),
                    this.createDisplayItem(enchantment)
            );
        }
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();
        Optional<InventoryItem> footerItem = InventoryItem.findByStack(event.getCurrentItem());

        if (footerItem.isPresent()) {
            if (footerItem.get() == InventoryItem.PREVIOUS) return Optional.of(this.previousPage(player, inventory));
            if (footerItem.get() == InventoryItem.NEXT) return Optional.of(this.nextPage(inventory));

            return Optional.empty();
        }

        int contentIndex = HardlandsInventory.getContentIndex(inventory, event.getRawSlot());
        if (contentIndex < 0) return Optional.empty();

        int index = this.page * HardlandsInventory.getContentCapacity(inventory) + contentIndex;
        if (index >= this.enchantments.size()) return Optional.empty();

        EnchantmentEntry enchantment = this.enchantments.get(index);
        int amplifier = this.scenario.getEnchantmentAmplifier(enchantment.identifier());
        int newAmplifier;

        if (event.isLeftClick()) newAmplifier = Math.min(amplifier + 1, enchantment.maxAmplifier());
        else if (event.isRightClick()) newAmplifier = Math.max(amplifier - 1, PROHIBITED_AMPLIFIER);
        else return Optional.of(false);

        if (newAmplifier == amplifier) return Optional.of(false);

        this.scenario.setEnchantmentAmplifier(enchantment.identifier(), newAmplifier);
        event.setCurrentItem(this.createDisplayItem(enchantment));

        return Optional.of(true);
    }

    private ItemStack createDisplayItem(EnchantmentEntry enchantment) {
        int amplifier = this.scenario.getEnchantmentAmplifier(enchantment.identifier());

        if (enchantment.hardlandsEnchantment() != null) {
            return this.createHardlandsEnchantmentItem(enchantment.hardlandsEnchantment(), amplifier);
        }

        return createVanillaEnchantmentItem(enchantment, amplifier);
    }

    private ItemStack createHardlandsEnchantmentItem(HardlandsEnchantment enchantment, int amplifier) {
        ItemStack stack = enchantment.enchantedBook(0);
        ItemLore bookLore = stack.getData(DataComponentTypes.LORE);

        String description = bookLore != null && bookLore.lines().size() >= 3
                ? TextFormatter.toPlainText(bookLore.lines().get(2))
                : "";

        stack.setType(getMaterial(amplifier));

        ItemBuilder builder = new ItemBuilder(stack)
                .name(enchantment.createBeautifulName(Math.max(amplifier, 0)))
                .formattedLore(description, "");

        addEnchantmentStateLore(builder, amplifier, enchantment.getLimit().maxAmplifier(), "Hardlands");

        return builder
                .addFormattedLore("", "<dark_gray>Izq. + | Der. -")
                .build();
    }

    private static ItemStack createVanillaEnchantmentItem(EnchantmentEntry enchantment, int amplifier) {
        ItemBuilder builder = new ItemBuilder(getMaterial(amplifier))
                .name(TextFormatter.formatTinyCaps(enchantment.name()).color(getColor(amplifier)));

        addEnchantmentStateLore(builder, amplifier, enchantment.maxAmplifier(), "Vanilla");

        return builder
                .addFormattedLore("", "<dark_gray>Izq. + | Der. -")
                .build();
    }

    private static void addEnchantmentStateLore(ItemBuilder builder, int amplifier, int maxAmplifier, String origin) {
        builder.addFormattedLore("Estado: [%s]".formatted(formatState(amplifier)));

        if (maxAmplifier > 0) {
            builder.addFormattedLore("Nivel máximo: [%s]".formatted(RomanNumerals.format(maxAmplifier + 1)));
        }

        builder.addFormattedLore("Origen: [%s]".formatted(origin));
    }

    private boolean previousPage(Player player, Inventory inventory) {
        if (this.page == 0) {
            HardlandsInventory.SCENARIOS.openInventory(player);
            return true;
        }

        this.page--;
        this.render(inventory);

        return true;
    }

    private boolean nextPage(Inventory inventory) {
        int capacity = HardlandsInventory.getContentCapacity(inventory);
        if ((this.page + 1) * capacity >= this.enchantments.size()) return false;

        this.page++;
        this.render(inventory);

        return true;
    }

    private static Material getMaterial(int amplifier) {
        if (amplifier == PROHIBITED_AMPLIFIER) return Material.BARRIER;
        if (amplifier == VANILLA_AMPLIFIER) return Material.BOOK;

        return Material.ENCHANTED_BOOK;
    }

    private static TextColor getColor(int amplifier) {
        if (amplifier == PROHIBITED_AMPLIFIER) return NamedTextColor.RED;
        if (amplifier == VANILLA_AMPLIFIER) return NamedTextColor.GRAY;

        return HardlandsColor.PRIMARY;
    }

    private static String formatState(int amplifier) {
        if (amplifier == PROHIBITED_AMPLIFIER) return "Prohibido";
        if (amplifier == VANILLA_AMPLIFIER) return "Vanilla";

        return "Forzado · %s".formatted(RomanNumerals.format(amplifier + 1));
    }

    private static List<EnchantmentEntry> createEnchantments() {
        List<EnchantmentEntry> enchantments = new ArrayList<>();

        for (HardlandsEnchantment enchantment : HardlandsEnchantment.values()) {
            enchantments.add(new EnchantmentEntry(
                    enchantment.name(),
                    enchantment.getLabel(),
                    enchantment.getLimit().maxAmplifier(),
                    enchantment
            ));
        }

        Registry<Enchantment> registry = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
        List<EnchantmentEntry> vanillaEnchantments = new ArrayList<>();

        for (Enchantment enchantment : registry) {
            vanillaEnchantments.add(new EnchantmentEntry(
                    enchantment.getKey().toString(),
                    formatVanillaName(enchantment),
                    enchantment.getMaxLevel() - 1,
                    null
            ));
        }

        vanillaEnchantments.sort(Comparator.comparing(EnchantmentEntry::name, String.CASE_INSENSITIVE_ORDER));
        enchantments.addAll(vanillaEnchantments);

        return List.copyOf(enchantments);
    }

    private static String formatVanillaName(Enchantment enchantment) {
        String path = enchantment.getKey().getKey();
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!result.isEmpty()) result.append(' ');

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1).toLowerCase(Locale.ROOT));
        }

        if (!enchantment.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) {
            result.append(" (")
                    .append(enchantment.getKey().getNamespace())
                    .append(')');
        }

        return result.toString();
    }

    private static MagicManScenario getMagicManScenario() {
        Scenario scenario = Hardlands.getInstance()
                .getScenarioManager()
                .findRegisteredScenario(ScenarioDefinition.MAGIC_MAN.identifier())
                .orElseThrow(() -> new IllegalStateException("Magic Man scenario is not registered."));

        if (!(scenario instanceof MagicManScenario magicMan)) {
            throw new IllegalStateException("Magic Man definition is not backed by MagicManScenario.");
        }

        return magicMan;
    }

    private static void clearContent(Inventory inventory) {
        int capacity = HardlandsInventory.getContentCapacity(inventory);

        for (int index = 0; index < capacity; index++) {
            inventory.clear(HardlandsInventory.contentSlot(index));
        }
    }

    private record EnchantmentEntry(
            String identifier,
            String name,
            int maxAmplifier,
            HardlandsEnchantment hardlandsEnchantment
    ) {}
}