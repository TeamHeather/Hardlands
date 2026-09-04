package team.heather.hardlands.common.ui.inventory.handler;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import team.heather.hardlands.common.item.InventoryItem;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;

public final class PlayerStatisticSelectionInventoryHandler implements InventoryHandler {

    private final HardlandsPlayer profile;
    private final Set<String> selected;
    private final BiConsumer<Player, Set<String>> selectionHandler;
    private final Consumer<Player> backHandler;

    public PlayerStatisticSelectionInventoryHandler(
            HardlandsPlayer profile,
            BiConsumer<Player, Set<String>> selectionHandler,
            Consumer<Player> backHandler
    ) {
        Set<String> statistics = profile.getPinnedStatisticsOption().getValue();

        this.profile = profile;
        this.selected = statistics == null ? new LinkedHashSet<>() : new LinkedHashSet<>(statistics);
        this.selectionHandler = selectionHandler;
        this.backHandler = backHandler;
    }

    @Override
    public void render(Inventory inventory) {
        DyeColor color = this.profile.getProfileColorOption().getValue();
        TextColor textColor = HardlandsColor.profile(color);

        HardlandsInventory.renderOutline(
                inventory,
                new ItemBuilder(Material.valueOf(color.name() + "_STAINED_GLASS_PANE")).name("").build()
        );

        for (int index = 0; index < PlayerStatistic.all().size(); index++) {
            PlayerStatistic statistic = PlayerStatistic.all().get(index);

            inventory.setItem(
                    statisticSlot(inventory, index, PlayerStatistic.all().size()),
                    statistic.createSelectionItem(this.profile, textColor, this.selected.contains(statistic.key()))
            );
        }

        inventory.setItem(inventory.getSize() - 5, InventoryItem.PREVIOUS.build());
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();

        if (event.getRawSlot() == inventory.getSize() - 5) {
            this.backHandler.accept(player);
            return Optional.of(true);
        }

        for (int index = 0; index < PlayerStatistic.all().size(); index++) {
            if (event.getRawSlot() != statisticSlot(inventory, index, PlayerStatistic.all().size())) {
                continue;
            }

            PlayerStatistic statistic = PlayerStatistic.all().get(index);

            if (!this.selected.remove(statistic.key())) {
                this.selected.add(statistic.key());
            }

            this.selectionHandler.accept(player, new LinkedHashSet<>(this.selected));

            event.setCurrentItem(
                    statistic.createSelectionItem(
                            this.profile,
                            HardlandsColor.profile(this.profile.getProfileColorOption().getValue()),
                            this.selected.contains(statistic.key())
                    )
            );

            return Optional.of(true);
        }

        return Optional.empty();
    }

    private static int statisticSlot(Inventory inventory, int index, int size) {
        int columns = 7;
        int rows = (size + columns - 1) / columns;
        int contentRows = inventory.getSize() / 9 - 2;
        int row = index / columns;
        int rowSize = Math.min(columns, size - row * columns);
        int startRow = 1 + Math.max(0, (contentRows - rows) / 2);
        int startColumn = (9 - rowSize) / 2;

        return (startRow + row) * 9 + startColumn + index % columns;
    }
}