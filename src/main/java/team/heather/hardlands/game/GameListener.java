package team.heather.hardlands.game;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.game.timeline.GameTimeline;
import team.heather.hardlands.internal.event.ConfigChangeEvent;

public final class GameListener implements Listener {

		private final Map<UUID, Set<UUID>> attackersByVictim = new HashMap<>();

		@EventHandler
		private void onConfigChange(ConfigChangeEvent event) {
				Hardlands hardlands = Hardlands.getInstance();
				GameManager gameManager = hardlands.getGameManager();

				if (this.changedStartTime(event, gameManager)) {
						this.refreshStartTime(hardlands, gameManager.getTimeline());
				}
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		private void onEntityDamage(EntityDamageEvent event) {
				if (!(event.getEntity() instanceof Player victim)
								|| event.getFinalDamage() <= 0.0) {
						return;
				}

				GameData data = Hardlands.getInstance().getGameManager().getData();
				DamageSource source = event.getDamageSource();

				if (data.running()) {
						this.trackAttacker(victim, source);
						data.recordFirstDamage(victim, source);
				}
		}

		@EventHandler(priority = EventPriority.MONITOR)
		private void onPlayerDeath(PlayerDeathEvent event) {
				GameData data = Hardlands.getInstance().getGameManager().getData();
				Player player = event.getPlayer();

				if (data.running()) {
						data.recordDeath(event);
						this.processKill(player, data);
				}
		}

		@EventHandler
		private void onPlayerJoin(PlayerJoinEvent event) {
				Player player = event.getPlayer();

				Hardlands hardlands = Hardlands.getInstance();
				GameManager gameManager = hardlands.getGameManager();
				Phase phase = gameManager.getPhase();

				gameManager.getTimeline().addViewer(player);

				if (phase.isScatterQueueEnabled()) {
						hardlands.getWorldManager()
										.getScatterManager()
										.enqueue(player);
				}
		}

		@EventHandler
		private void onPlayerQuit(PlayerQuitEvent event) {
				Player player = event.getPlayer();

				Hardlands hardlands = Hardlands.getInstance();
				GameManager gameManager = hardlands.getGameManager();

				this.clearCombatTracking(player.getUniqueId());
				gameManager.getTimeline().removeViewer(player);

				if (gameManager.getPhase().isScatterQueueEnabled()) {
						hardlands.getWorldManager()
										.getScatterManager()
										.remove(player);
				}
		}


		private boolean changedStartTime(ConfigChangeEvent event, GameManager gameManager) {
				return event.getConfig() == gameManager
								&& event.getOptionKey().equals(gameManager.getStartTimeOption().getKey());
		}

		private void refreshStartTime(Hardlands plugin, GameTimeline timeline) {
				if (Bukkit.isPrimaryThread()) {
						timeline.refreshStartTime();
						return;
				}

				Bukkit.getScheduler().runTask(plugin, timeline::refreshStartTime);
		}

		private void processKill(Player victim, GameData data) {
				Set<UUID> attackers = removeAttackers(victim);
				Player killer = victim.getKiller();

				if (!this.isValidKiller(victim, killer)) {
						return;
				}

				data.recordKill(killer);
				this.processAssists(attackers, killer, data);
		}

		private void processAssists(Set<UUID> attackers, Player killer, GameData data) {
				UUID killerId = killer.getUniqueId();

				for (UUID attackerId : attackers) {
						if (attackerId.equals(killerId)) {
								continue;
						}

						Player attacker = Bukkit.getPlayer(attackerId);

						if (attacker != null) {
								data.recordAssist(attacker);
						}
				}
		}

		private void trackAttacker(Player victim, DamageSource damageSource) {
				if (!(damageSource.getCausingEntity() instanceof Player attacker)) {
						return;
				}

				UUID victimId = victim.getUniqueId();
				UUID attackerId = attacker.getUniqueId();

				if (attackerId.equals(victimId)) {
						return;
				}

				this.attackersByVictim.computeIfAbsent(victimId, _ -> new HashSet<>()).add(attackerId);
		}

		private Set<UUID> removeAttackers(Player victim) {
				Set<UUID> attackers = this.attackersByVictim.remove(victim.getUniqueId());

				return attackers == null
								? Set.of()
								: attackers;
		}

		private boolean isValidKiller(Player victim, Player killer) {
				return killer != null && !killer.getUniqueId().equals(victim.getUniqueId());
		}

		private void clearCombatTracking(UUID playerId) {
				this.attackersByVictim.remove(playerId);

				for (Set<UUID> attackers : this.attackersByVictim.values()) {
						attackers.remove(playerId);
				}
		}
}