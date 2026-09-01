package team.heather.hardlands.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;

@CommandAlias("phase")
public final class PhaseCommand extends BaseCommand {

    private final GameManager gameManager;

    public PhaseCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Subcommand("start")
    public void start() {
        this.gameManager.transitionTo(Phase.PREPARATION);
    }

    @Subcommand("next")
    public void next() {
        this.gameManager.completePhase();
    }

    @Subcommand("previous")
    public void previous() {
        this.gameManager.getPhase()
                .previous()
                .ifPresent(this.gameManager::transitionTo);
    }

    @Subcommand("time")
    public void setTime(int seconds) {
        this.gameManager.setElapsedSeconds(seconds);
    }
}