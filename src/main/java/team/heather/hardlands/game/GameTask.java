package team.heather.hardlands.game;

import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.timeline.GameTimeline;

public final class GameTask implements AutoCloseable {

		private final Hardlands plugin;
		private final GameTimeline timeline;

		@Nullable private BukkitTask task;

		public GameTask(Hardlands plugin, GameTimeline timeline) {
				this.plugin = plugin;
				this.timeline = timeline;
		}

		public void start() {
				if (this.task != null) {
						throw new IllegalStateException("Game task is already running");
				}

				this.task = Bukkit.getScheduler().runTaskTimer(
								this.plugin,
								this.timeline::tick,
								20L,
								20L
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