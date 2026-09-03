package team.heather.hardlands.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;

@CommandAlias("hardlands|hl")
public final class HardlandsCommand extends BaseCommand {

    @Default
    private void onDefault(Player player) {
        HardlandsInventory.MAIN.openInventory(player);
    }
}
