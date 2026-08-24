package org.heather.hardlands.common.inventory;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.common.inventory.handler.InventoryHandler;
import org.heather.hardlands.common.inventory.handler.PresetInventoryHandler;
import org.heather.hardlands.common.inventory.handler.ScenarioInventoryHandler;
import org.heather.hardlands.common.inventory.handler.WorldInventoryHandler;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;

public enum HardlandsInventory {

    MAIN("Hardlands", Material.RED_STAINED_GLASS_PANE, """
            -------
            -SPGFW-
            ---T---
            -------
            """, Map.of(
            'S', InventoryItem.SCENARIOS,
            'P', InventoryItem.PLAYERS,
            'G', InventoryItem.GENERAL,
            'F', InventoryItem.PHASES,
            'W', InventoryItem.WORLD,
            'T', InventoryItem.PRESETS)),

    SCENARIOS("Escenarios", Material.PINK_STAINED_GLASS_PANE, ScenarioInventoryHandler::new),
    PLAYERS("Jugadores", Material.YELLOW_STAINED_GLASS_PANE),
    GENERAL("General", Material.PURPLE_STAINED_GLASS_PANE),
    PHASES("Fases", Material.LIME_STAINED_GLASS_PANE),

    WORLD("Mundo", Material.LIGHT_BLUE_STAINED_GLASS_PANE, """
            -------
            ---W---
            -B-R-C-
            ---H---
            -------
            """, Map.of(
            'W', InventoryItem.WORLD_WORLDS,
            'B', InventoryItem.WORLD_BORDERS,
            'R', InventoryItem.WORLD_SHRINK,
            'C', InventoryItem.WORLD_CENTER,
            'H', InventoryItem.WORLD_BEHAVIOR
    ), WorldInventoryHandler::new),

    PRESETS("Plantillas", Material.PURPLE_STAINED_GLASS_PANE, PresetInventoryHandler::new);

    private static final int COLUMNS = 9;
    private static final int CONTENT_COLUMNS = COLUMNS - 2;
    private static final String EMPTY_LAYOUT = """
            -------
            -------
            -------
            -------
            """;

    private final String title;
    private final ItemStack outline;
    private final List<String> layout;
    private final Map<Character, InventoryItem> items;
    private final Supplier<InventoryHandler> handlerFactory;

    public void openInventory(Player player) {
        InventoryHandler handler = this.handlerFactory.get();
        Inventory inventory = this.createInventory(handler);

        player.openInventory(inventory);
        handler.onOpen(inventory, player);
    }

    public void renderLayout(Inventory inventory) {
        for (int row = 0; row < this.layout.size(); row++) {
            String line = this.layout.get(row);

            for (int column = 0; column < CONTENT_COLUMNS; column++) {
                char symbol = line.charAt(column);

                if (symbol == '-') continue;

                InventoryItem item = this.items.get(symbol);

                if (item == null) {
                    throw new IllegalStateException("No InventoryItem defined for layout symbol '%s'.".formatted(symbol));
                }

                inventory.setItem(slot(row + 2, column + 2), item.build());
            }
        }
    }

    public static int getContentCapacity(Inventory inventory) {
        return Math.max(0, getBottomRow(inventory) - 2) * CONTENT_COLUMNS;
    }

    public static int getContentIndex(Inventory inventory, int slot) {
        if (slot < 0 || slot >= inventory.getSize()) return -1;

        int row = slot / COLUMNS + 1;
        int column = slot % COLUMNS + 1;

        if (row < 2 || row >= getBottomRow(inventory)) return -1;
        if (column < 2 || column >= COLUMNS) return -1;

        return (row - 2) * CONTENT_COLUMNS + column - 2;
    }

    public static int contentSlot(int index) {
        return slot(index / CONTENT_COLUMNS + 2, index % CONTENT_COLUMNS + 2);
    }

    public static void refreshPreparationItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory inventory = player.getOpenInventory().getTopInventory();

            if (!(inventory.getHolder() instanceof HardlandsInventoryHolder)) continue;

            inventory.setItem(slot(getBottomRow(inventory), 5), InventoryItem.PREPARATION.build());
        }
    }

    private HardlandsInventory(String title, Material outline, String layout, Map<Character, InventoryItem> items,
                               Supplier<InventoryHandler> handlerFactory) {
        this.title = title;
        this.outline = new ItemBuilder(outline).name("").build();
        this.layout = layout.strip().lines().toList();
        this.items = items;
        this.handlerFactory = handlerFactory;
    }

    private HardlandsInventory(String title, Material outline, String layout, Map<Character, InventoryItem> items) {
        this(title, outline, layout, items, () -> InventoryHandler.EMPTY);
    }

    private HardlandsInventory(String title, Material outline, Supplier<InventoryHandler> handlerFactory) {
        this(title, outline, EMPTY_LAYOUT, Map.of(), handlerFactory);
    }

    private HardlandsInventory(String title, Material outline) {
        this(title, outline, EMPTY_LAYOUT, Map.of());
    }

    private Inventory createInventory(InventoryHandler handler) {
        HardlandsInventoryHolder holder = new HardlandsInventoryHolder(handler);
        Inventory inventory = Bukkit.createInventory(holder, (this.layout.size() + 2) * COLUMNS, Component.text(this.title));

        holder.setInventory(inventory);

        this.renderFrame(inventory);
        this.renderLayout(inventory);
        this.renderFooter(inventory);
        handler.render(inventory);

        return inventory;
    }

    private void renderFrame(Inventory inventory) {
        int bottomRow = getBottomRow(inventory);

        for (int column = 1; column <= COLUMNS; column++) {
            inventory.setItem(slot(1, column), this.outline);
            inventory.setItem(slot(bottomRow, column), this.outline);
        }

        for (int row = 2; row < bottomRow; row++) {
            inventory.setItem(slot(row, 1), this.outline);
            inventory.setItem(slot(row, COLUMNS), this.outline);
        }
    }

    private void renderFooter(Inventory inventory) {
        int row = getBottomRow(inventory);

        inventory.setItem(slot(row, 4), InventoryItem.PREVIOUS.build());
        inventory.setItem(slot(row, 5), InventoryItem.PREPARATION.build());
        inventory.setItem(slot(row, 6), InventoryItem.NEXT.build());
    }

    private static int getBottomRow(Inventory inventory) {
        return inventory.getSize() / COLUMNS;
    }

    private static int slot(int row, int column) {
        return (row - 1) * COLUMNS + column - 1;
    }
}