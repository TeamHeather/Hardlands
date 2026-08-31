package team.heather.hardlands.game;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.phase.Phase;

public final class GameListener implements Listener {

		private final GameManager manager;

		public GameListener(GameManager manager) {
				this.manager = manager;
		}

		@EventHandler
		private void onPlayerJoin(PlayerJoinEvent event) {
				Player player = event.getPlayer();

				this.manager.getTimerManager().addViewer(player);

				if (this.manager.getPhase().isScatterQueueOpen()) {
						Hardlands.getInstance()
										.getWorldManager()
										.getScatterManager()
										.enqueue(player);
				}
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				Player player = event.getPlayer();

				this.manager.getTimerManager().removeViewer(player);

				if (this.manager.getPhase().isScatterQueueOpen()) {
						Hardlands.getInstance()
										.getWorldManager()
										.getScatterManager()
										.remove(player);
				}
		}
}
