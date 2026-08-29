package team.heather.hardlands.common.ui.inventory;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import team.heather.hardlands.common.ui.inventory.handler.InventoryHandler;
import org.jspecify.annotations.NonNull;

public final class HardlandsInventoryHolder implements InventoryHolder {

    private final InventoryHandler handler;

    private Inventory inventory;

    public HardlandsInventoryHolder(InventoryHandler handler) {
        this.handler = handler;
    }

    @Override
    public @NonNull Inventory getInventory() {
        if (this.inventory == null) {
            throw new IllegalStateException("Inventory has not been initialized.");
        }

        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public InventoryHandler getHandler() {
        return this.handler;
    }
}