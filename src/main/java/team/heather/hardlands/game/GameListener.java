package team.heather.hardlands.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.game.world.ScatterManager;

public final class GameListener implements Listener {

		private final GameManager gameManager;
		private final ScatterManager scatterManager;

		public GameListener(GameManager gameManager, ScatterManager scatterManager) {
				this.gameManager = gameManager;
				this.scatterManager = scatterManager;
		}

		@EventHandler
		private void onPlayerJoin(PlayerJoinEvent event) {
				Player player = event.getPlayer();
				Phase phase = this.gameManager.getPhase();

				this.gameManager.addViewer(player);

				if (!phase.isScatterQueueOpen()) return;

				this.scatterManager.enqueue(player);

				if (phase == Phase.SCATTER) {
						this.scatterManager.scatterNext();
				}
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				Player player = event.getPlayer();

				this.gameManager.removeViewer(player);

				if (this.gameManager.getPhase().isScatterQueueOpen()) {
						this.scatterManager.remove(player);
				}
		}
}