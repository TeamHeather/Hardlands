package team.heather.hardlands.common.ui.inventory.handler;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.common.item.InventoryItem;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;
import team.heather.hardlands.module.scenario.Scenario;
import team.heather.hardlands.module.scenario.implementation.EnchantiaScenario;

public final class EnchantiaInventoryHandler implements InventoryHandler {

    private final EnchantiaScenario scenario = (EnchantiaScenario) Scenario.ENCHANTIA.getProcessor();
    private final List<Entry> entries = this.createEntries();

    private int page;

    @Override
    public void render(Inventory inventory) {
        int capacity = HardlandsInventory.getContentCapacity(inventory);
        int start = this.page * capacity;
        int end = Math.min(start + capacity, this.entries.size());

        for (int index = 0; index < capacity; index++) {
            inventory.setItem(HardlandsInventory.contentSlot(index), null);
        }

        for (int index = start; index < end; index++) {
            inventory.setItem(
                    HardlandsInventory.contentSlot(index - start),
                    this.entries.get(index).createItem()
            );
        }
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();

        Optional<Boolean> navigation = this.handleNavigation(event, player, inventory);
        if (navigation.isPresent()) return navigation;

        int slot = HardlandsInventory.getContentIndex(inventory, event.getRawSlot());
        if (slot < 0) return Optional.empty();

        int index = this.page * HardlandsInventory.getContentCapacity(inventory) + slot;
        if (index >= this.entries.size()) return Optional.of(false);

        int delta;

        if (event.isLeftClick()) {
            delta = 1;
        } else if (event.isRightClick()) {
            delta = -1;
        } else {
            return Optional.of(false);
        }

        Entry entry = this.entries.get(index);
        if (!entry.changeLevel(delta)) return Optional.of(false);

        event.setCurrentItem(entry.createItem());
        return Optional.of(true);
    }

    private Optional<Boolean> handleNavigation(
            InventoryClickEvent event,
            Player player,
            Inventory inventory
    ) {
        InventoryItem item = InventoryItem.findByStack(event.getCurrentItem()).orElse(null);

        if (item == InventoryItem.PREVIOUS) {
            if (this.page > 0) {
                this.page--;
                this.render(inventory);
            } else {
                HardlandsInventory.SCENARIOS.openInventory(player);
            }

            return Optional.of(true);
        }

        if (item != InventoryItem.NEXT) return Optional.empty();

        int pages = Math.ceilDiv(
                this.entries.size(),
                HardlandsInventory.getContentCapacity(inventory)
        );

        if (this.page + 1 >= pages) return Optional.of(false);

        this.page++;
        this.render(inventory);

        return Optional.of(true);
    }

    private List<Entry> createEntries() {
        List<Entry> entries = new ArrayList<>();

        for (HardlandsEnchantment enchantment : HardlandsEnchantment.values()) {
            entries.add(new Entry(
                    enchantment.getLabel(),
                    enchantment.getDescription(),
                    "Hardlands",
                    enchantment.createMaxLevel(),
                    () -> this.scenario.level(enchantment),
                    level -> this.scenario.level(enchantment, level)
            ));
        }

        Registry<Enchantment> registry = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        registry.stream()
                .filter(enchantment -> enchantment.getKey().getNamespace().equals("minecraft"))
                .sorted(Comparator.comparing(enchantment -> enchantment.getKey().getKey()))
                .map(enchantment -> new Entry(
                        formatName(enchantment.getKey().getKey()),
                        "Encantamiento original de Minecraft.",
                        "Vanilla",
                        enchantment.getMaxLevel(),
                        () -> this.scenario.level(enchantment),
                        level -> this.scenario.level(enchantment, level)
                ))
                .forEach(entries::add);

        return List.copyOf(entries);
    }

    private static String formatName(String identifier) {
        StringBuilder result = new StringBuilder();

        for (String word : identifier.split("_")) {
            if (!result.isEmpty()) result.append(' ');

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word, 1, word.length());
        }

        return result.toString();
    }

    private record Entry(
            String name,
            String description,
            String source,
            int maxLevel,
            IntSupplier levelSupplier,
            IntConsumer levelConsumer
    ) {

        private int getLevel() {
            return this.levelSupplier.getAsInt();
        }

        private boolean changeLevel(int delta) {
            int current = this.getLevel();
            int level = Math.clamp(current + delta, -1, this.maxLevel);

            if (level == current) return false;

            this.levelConsumer.accept(level);
            return true;
        }

        private ItemStack createItem() {
            int level = this.getLevel();

            Material material = switch (level) {
                case -1 -> Material.BARRIER;
                case 0 -> Material.BOOK;
                default -> Material.ENCHANTED_BOOK;
            };

            return new ItemBuilder(material)
                    .name(TextFormatter.tinyCaps(this.name))
                    .glint(level > 0)
                    .formattedLore(
                            this.description,
                            "",
                            "{Tipo}: [%s]".formatted(this.source),
                            "{Nivel}: [%s]".formatted(formatLevel(level)),
                            "{Estado}: %s".formatted(status(level)),
                            "",
                            "<dark_gray>Izq. ＋ Nivel | Der. － Nivel"
                    )
                    .build();
        }

        private static String formatLevel(int level) {
            return level == -1 ? "—" : Integer.toString(level);
        }

        private static String status(int level) {
            return switch (level) {
                case -1 -> "<red>Prohibido";
                case 0 -> "<gray>Sin modificar";
                default -> "<green>Forzado";
            };
        }
    }
}