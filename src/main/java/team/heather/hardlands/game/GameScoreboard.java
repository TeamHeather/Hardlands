package team.heather.hardlands.game;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.util.text.TextFormatter;

public final class GameScoreboard {

		private final GameManager manager;
		private final Scoreboard scoreboard;
		private final Objective objective;

		public GameScoreboard(GameManager manager) {
				this.manager = manager;
				this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
				this.objective = this.scoreboard.registerNewObjective(
								"game",
								Criteria.DUMMY,
								TextFormatter.parse(Hardlands.LABEL)
				);

				this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		}

		public void update() {
				Phase phase = this.manager.getPhase();

				this.objective.getScore( "phase")
								.customName(TextFormatter.parse("<gray>Phase: <white>" + phase.getLabel()));
		}

		void addViewer(Player player) {
				player.setScoreboard(scoreboard);
		}

		void removeViewer(Player player) {
				player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
		}
}
