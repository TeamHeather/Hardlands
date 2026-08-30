package team.heather.hardlands.feature.ui.inventory.handler;

import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public interface InventoryHandler {

    InventoryHandler EMPTY = new InventoryHandler() {};

    default void render(Inventory inventory) {}

    default Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        return Optional.empty();
    }

    default void onOpen(Inventory inventory, Player player) {}

    default void onClose(Inventory inventory, Player player) {}
}