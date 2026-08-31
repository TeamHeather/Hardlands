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

		@EventHandler
		private void onPlayerJoin(PlayerJoinEvent event) {
				Hardlands plugin = Hardlands.getInstance();
				GameManager gameManager = plugin.getGameManager();
				ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

				Player player = event.getPlayer();
				Phase phase = gameManager.getPhase();

				gameManager.addViewer(player);

				if (!phase.isScatterQueueOpen()) return;

				scatterManager.enqueue(player);

				if (phase == Phase.SCATTER) {
						scatterManager.scatterNext();
				}
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				Hardlands plugin = Hardlands.getInstance();
				GameManager gameManager = plugin.getGameManager();
				ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

				Player player = event.getPlayer();

				gameManager.removeViewer(player);

				if (gameManager.getPhase().isScatterQueueOpen()) {
						scatterManager.remove(player);
				}
		}

		@EventHandler
		private void onConfigurationChange(ConfigChangeEvent event) {
				Hardlands plugin = Hardlands.getInstance();
				GameManager gameManager = plugin.getGameManager();

				if (event.getConfiguration() != gameManager) return;
				if (!event.getOptionKey().equals(gameManager.getStartTimeOption().getKey())) return;

				if (Bukkit.isPrimaryThread()) {
						gameManager.refreshStartTime();
						return;
				}

				Bukkit.getScheduler().runTask(plugin, gameManager::refreshStartTime);
		}
}