package team.heather.hardlands.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.command.CommandSender;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;

@CommandAlias("phase")
public final class PhaseCommand extends BaseCommand {

    private final GameManager gameManager;

    public PhaseCommand() {
        this(Hardlands.getInstance().getGameManager());
    }

    public PhaseCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Subcommand("start")
    public void onStart(CommandSender sender) {
        gameManager.changePhase(Phase.PRE_GENERATION);
    }

    @Subcommand("next")
    public void onNext(CommandSender sender) {
        gameManager.getPhase()
                .next()
                .ifPresent(gameManager::changePhase);
    }

    @Subcommand("previous")
    public void onPrevious(CommandSender sender) {
        gameManager.getPhase()
                .previous()
                .ifPresent(gameManager::changePhase);
    }
}