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

				gameManager.addTimelineViewer(player);

				if (!phase.isScatterQueueEnabled()) return;

				scatterManager.enqueue(player);
				if (phase == Phase.SCATTER) scatterManager.scatterAll();
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				Hardlands plugin = Hardlands.getInstance();
				GameManager gameManager = plugin.getGameManager();
				ScatterManager scatterManager = plugin.getWorldManager().getScatterManager();

				Player player = event.getPlayer();

				gameManager.removeTimelineViewer(player);
				if (gameManager.getPhase().isScatterQueueEnabled()) scatterManager.remove(player);
		}

		@EventHandler
		private void onConfigurationChange(ConfigChangeEvent event) {
				Hardlands plugin = Hardlands.getInstance();
				GameManager gameManager = plugin.getGameManager();

				if (event.getConfiguration() != gameManager
								|| !event.getOptionKey().equals(gameManager.getStartTimeOption().getKey())) {
						return;
				}

				if (Bukkit.isPrimaryThread()) {
						gameManager.refreshTimelineStartTime();
						return;
				}

				Bukkit.getScheduler().runTask(plugin, gameManager::refreshTimelineStartTime);
		}
}