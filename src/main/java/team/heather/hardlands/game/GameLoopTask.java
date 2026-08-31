package team.heather.hardlands.game;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import team.heather.hardlands.game.timeline.GameTimeline;

public final class GameLoopTask
				implements AutoCloseable {

		private static final long INITIAL_DELAY_TICKS = 20L;
		private static final long PERIOD_TICKS = 20L;

		private final Plugin plugin;
		private final GameTimeline timeline;

		private BukkitTask task;

		public GameLoopTask(
						Plugin plugin,
						GameTimeline timeline
		) {
				this.plugin = plugin;
				this.timeline = timeline;
		}

		public void start() {
				if (this.task != null) {
						throw new IllegalStateException(
										"Game loop is already running"
						);
				}

				this.task = Bukkit.getScheduler().runTaskTimer(
								this.plugin,
								this.timeline::tick,
								INITIAL_DELAY_TICKS,
								PERIOD_TICKS
				);
		}

		@Override
		public void close() {
				if (this.task == null) {
						return;
				}

				this.task.cancel();
				this.task = null;
		}

		public boolean isRunning() {
				return this.task != null;
		}
}