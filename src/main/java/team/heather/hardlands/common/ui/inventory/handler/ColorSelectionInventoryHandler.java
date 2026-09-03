package team.heather.hardlands.common.ui.inventory.handler;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.common.item.InventoryItem;
import team.heather.hardlands.common.item.ItemBuilder;

public final class ColorSelectionInventoryHandler implements InventoryHandler {

    private static final DyeColor[] COLORS = DyeColor.values();

    private final DyeColor selected;
    private final BiConsumer<Player, DyeColor> selectionHandler;
    private final Consumer<Player> backHandler;

    public ColorSelectionInventoryHandler(
            DyeColor selected,
            BiConsumer<Player, DyeColor> selectionHandler,
            Consumer<Player> backHandler
    ) {
        this.selected = selected;
        this.selectionHandler = selectionHandler;
        this.backHandler = backHandler;
    }

    @Override
    public void render(Inventory inventory) {
        for (int index = 0; index < COLORS.length; index++) {
            DyeColor color = COLORS[index];

            inventory.setItem(
                    colorSlot(index),
                    createColorItem(color, color == this.selected)
            );
        }

        inventory.setItem(inventory.getSize() - 5, InventoryItem.PREVIOUS.build());
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        if (event.getRawSlot() == event.getView().getTopInventory().getSize() - 5) {
            this.backHandler.accept(player);
            return Optional.of(true);
        }

        for (int index = 0; index < COLORS.length; index++) {
            if (event.getRawSlot() != colorSlot(index)) {
                continue;
            }

            this.selectionHandler.accept(player, COLORS[index]);
            return Optional.of(true);
        }

        return Optional.empty();
    }

    private static ItemStack createColorItem(DyeColor color, boolean selected) {
        ItemBuilder builder = new ItemBuilder(Material.valueOf(color.name() + "_DYE")).glint(selected);

        return InventoryItem.createDisplayStack(
                builder,
                colorName(color),
                selected ? "[Seleccionado]" : "",
                "",
                "{Clic} para seleccionar."
        );
    }

    private static int colorSlot(int index) {
        if (index < 5) {
            return 11 + index;
        }

        if (index < 11) {
            return 19 + index - 5;
        }

        return 29 + index - 11;
    }

    public static String colorName(DyeColor color) {
        return switch (color) {
            case WHITE -> "Blanco";
            case ORANGE -> "Naranja";
            case MAGENTA -> "Magenta";
            case LIGHT_BLUE -> "Azul claro";
            case YELLOW -> "Amarillo";
            case LIME -> "Lima";
            case PINK -> "Rosa";
            case GRAY -> "Gris";
            case LIGHT_GRAY -> "Gris claro";
            case CYAN -> "Cian";
            case PURPLE -> "Morado";
            case BLUE -> "Azul";
            case BROWN -> "Marrón";
            case GREEN -> "Verde";
            case RED -> "Rojo";
            case BLACK -> "Negro";
        };
    }
}