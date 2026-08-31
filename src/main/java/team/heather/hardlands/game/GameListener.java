package team.heather.hardlands.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.event.ConfigChangeEvent;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.game.world.ScatterManager;

public final class GameListener implements Listener {

		private static final Hardlands PLUGIN = Hardlands.getInstance();

		@EventHandler
		private void onPlayerJoin(PlayerJoinEvent event) {
				GameManager gameManager = PLUGIN.getGameManager();
				ScatterManager scatterManager = PLUGIN.getWorldManager().getScatterManager();

				Player player = event.getPlayer();
				Phase phase = gameManager.getPhase();

				gameManager.addViewer(player);

				if (!phase.isScatterQueueOpen()) {
						return;
				}

				scatterManager.enqueue(player);

				if (phase == Phase.SCATTER) {
						scatterManager.scatterNext();
				}
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				GameManager gameManager = PLUGIN.getGameManager();
				ScatterManager scatterManager = PLUGIN.getWorldManager().getScatterManager();

				Player player = event.getPlayer();

				gameManager.removeViewer(player);

				if (gameManager.getPhase().isScatterQueueOpen()) {
						scatterManager.remove(player);
				}
		}

		@EventHandler
		private void onConfigurationChange(ConfigChangeEvent event) {
				GameManager gameManager = PLUGIN.getGameManager();

				if (event.getConfiguration() != gameManager
								|| !event.getOptionKey().equals(gameManager.getStartTimeOption().getKey())) {
						return;
				}

				Bukkit.getScheduler().runTask(PLUGIN, gameManager::refreshStartTime);
		}
}