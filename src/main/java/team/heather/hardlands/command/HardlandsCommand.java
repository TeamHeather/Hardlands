package team.heather.hardlands.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import org.bukkit.entity.Player;
import team.heather.hardlands.ui.inventory.HardlandsInventory;

@CommandAlias("hardlands|hl")
@CommandPermission("hardlands.admin")
public final class HardlandsCommand extends BaseCommand {

    @Default
    private void onDefault(Player player) {
        HardlandsInventory.MAIN.openInventory(player);
    }
}
