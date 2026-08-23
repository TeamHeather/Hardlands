package org.heather.hardlands.common.inventory;

import java.util.Optional;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.heather.hardlands.common.item.InventoryItem;

public final class InventoryListener implements Listener {

    @EventHandler
    private void onInventoryClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory.getHolder() instanceof HardlandsInventoryHolder holder)
                || !(event.getWhoClicked() instanceof Player player)
                || !prepareClick(event, topInventory)) return;

        Optional<Boolean> succeeded = holder.getHandler().handleClick(event, player);

        if (succeeded.isEmpty()) {
            succeeded = InventoryItem.findByStack(event.getCurrentItem())
                    .map(item -> item.onClick(event));
        }

        succeeded.ifPresent(result -> playFeedback(player, result));
    }

    @EventHandler
    private void onInventoryDrag(InventoryDragEvent event) {
        Inventory inventory = event.getView().getTopInventory();

        if (!(inventory.getHolder() instanceof HardlandsInventoryHolder)) return;

        if (event.getRawSlots().stream().anyMatch(slot -> slot < inventory.getSize())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    private void onInventoryClose(InventoryCloseEvent event) {
        Inventory inventory = event.getView().getTopInventory();

        if (!(inventory.getHolder() instanceof HardlandsInventoryHolder holder)) return;
        if (!(event.getPlayer() instanceof Player player)) return;

        holder.getHandler().onClose(inventory, player);
    }

    private static boolean prepareClick(InventoryClickEvent event, Inventory topInventory) {
        boolean topClick = event.getClickedInventory() == topInventory;
        boolean affectsTop = event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY
                || event.getAction() == InventoryAction.COLLECT_TO_CURSOR;

        if (!topClick && !affectsTop) return false;

        event.setCancelled(true);
        return topClick;
    }

    private static void playFeedback(Player player, boolean succeeded) {
        player.playSound(player,
                succeeded ? Sound.UI_BUTTON_CLICK : Sound.BLOCK_NOTE_BLOCK_BIT,
                0.5F,
                succeeded ? 1.5F : 0.5F);
    }
}