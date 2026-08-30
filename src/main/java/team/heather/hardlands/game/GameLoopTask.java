package team.heather.hardlands.game;

import java.time.Duration;

import team.heather.hardlands.Hardlands;

public final class GameLoopTask {

		private static final Duration TICK_INTERVAL = Duration.ofSeconds(1);

		private final Hardlands plugin;
		private final GameManager gameManager;

		public GameLoopTask(Hardlands plugin, GameManager gameManager) {
				this.plugin = plugin;
				this.gameManager = gameManager;
		}

		public void start() {
				this.plugin.getSingleThreadScheduler().loop(
								_ -> this.tick(),
								TICK_INTERVAL
				);
		}

		private void tick() {
				this.gameManager.updateTimer();
		}
}