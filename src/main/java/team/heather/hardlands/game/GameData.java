package team.heather.hardlands.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.common.player.PlayerManager;

public class GameData {

		private final SequencedMap<UUID, DamageContext> firstDamageByPlayer;
		private final Map<UUID, Integer> assistsByPlayer;
		private final Map<UUID, Integer> killsByPlayer;
		private final List<DamageContext> deathHistory;
		private final PlayerManager playerManager;

		@Nullable private Set<UUID> winners;
		@Nullable private Instant startedAt;
		@Nullable private Instant endedAt;
		@Nullable private UUID paperMan;
		@Nullable private UUID ironMan;
		@Nullable private Host host;

		public GameData(PlayerManager playerManager) {
				this.firstDamageByPlayer = new LinkedHashMap<>();
				this.assistsByPlayer = new HashMap<>();
				this.killsByPlayer = new HashMap<>();
				this.deathHistory = new ArrayList<>();
				this.playerManager = playerManager;
		}

		// Recorders

		public int recordAssist(Player player) {
				return this.assistsByPlayer.merge(
								player.getUniqueId(),
								1,
								Integer::sum
				);
		}

		public DamageContext recordDeath(PlayerDeathEvent event) {
				DamageContext context = DamageContext.death(
								event.getDamageSource(),
								event.deathMessage()
				);

				this.deathHistory.add(context);

				return context;
		}

		public boolean recordFirstDamage(Player player, DamageSource source) {
				UUID playerId = player.getUniqueId();
				DamageContext context = DamageContext.damage(source);

				if (this.firstDamageByPlayer.putIfAbsent(playerId, context) != null) {
						return false;
				}

				if (this.paperMan == null) {
						this.paperMan = playerId;
				}

				this.ironMan = playerId;

				return true;
		}

		public int recordKill(Player player) {
				return this.killsByPlayer.merge(
								player.getUniqueId(),
								1,
								Integer::sum
				);
		}

		// Lifecycle

		public boolean markEnded() {
				if (!this.running()) {
						return false;
				}

				this.endedAt = Instant.now();
				return true;
		}

		public boolean markStarted() {
				if (this.startedAt != null) {
						return false;
				}

				this.startedAt = Instant.now();
				this.endedAt = null;

				return true;
		}

		// Mutators

		public boolean host(@NotNull Host host) {
				if (Objects.equals(this.host, host)) {
						return false;
				}

				this.host = host;
				return true;
		}

		public boolean ironMan(Player player) {
				UUID playerId = player.getUniqueId();

				if (Objects.equals(this.ironMan, playerId)) {
						return false;
				}

				this.ironMan = playerId;
				return true;
		}

		public boolean paperMan(Player player) {
				UUID playerId = player.getUniqueId();

				if (Objects.equals(this.paperMan, playerId)) {
						return false;
				}

				this.paperMan = playerId;
				return true;
		}

		public boolean winners(Player... players) {
				Set<UUID> winnerIds = new HashSet<>();

				for (Player player : players) {
						winnerIds.add(player.getUniqueId());
				}

				Set<UUID> updatedWinners = Set.copyOf(winnerIds);

				if (Objects.equals(this.winners, updatedWinners)) {
						return false;
				}

				this.winners = updatedWinners;
				return true;
		}

		// State

		public boolean ended() {
				return this.endedAt != null;
		}

		public boolean running() {
				return this.startedAt != null && this.endedAt == null;
		}

		public boolean started() {
				return this.startedAt != null;
		}

		// Accessors

		public int assistCount(Player player) {
				return this.assistsByPlayer.getOrDefault(player.getUniqueId(), 0);
		}

		public List<DamageContext> deathHistory() {
				return List.copyOf(this.deathHistory);
		}

		public @Nullable Instant endedAt() {
				return this.endedAt;
		}

		public Map<UUID, DamageContext> firstDamageHistory() {
				return Map.copyOf(this.firstDamageByPlayer);
		}

		public @Nullable Host host() {
				return this.host;
		}

		public @Nullable HardlandsPlayer ironMan() {
				if (this.ironMan == null) {
						return null;
				}

				return this.playerManager.get(this.ironMan);
		}

		public int killCount(Player player) {
				return this.killsByPlayer.getOrDefault(player.getUniqueId(), 0);
		}

		public List<KillRanking> killLeaderboard() {
				List<KillRanking> leaderboard = new ArrayList<>();

				this.killsByPlayer.forEach((playerId, kills) ->
								leaderboard.add(new KillRanking(playerId, kills)));

				leaderboard.sort(null);

				return leaderboard;
		}

		public @Nullable HardlandsPlayer paperMan() {
				if (this.paperMan == null) {
						return null;
				}

				return this.playerManager.get(this.paperMan);
		}

		public @Nullable Instant startedAt() {
				return this.startedAt;
		}

		public @Nullable Set<HardlandsPlayer> winners() {
				if (this.winners == null) {
						return null;
				}

				Set<HardlandsPlayer> players = new HashSet<>();

				for (UUID playerId : this.winners) {
						HardlandsPlayer player = this.playerManager.get(playerId);

						if (player != null) {
								players.add(player);
						}
				}

				return Set.copyOf(players);
		}

		// Data Types

		public record Host(int number, UUID hoster) {}

		public record DamageContext(Instant occurredAt, DamageSource source, Fatal fatal) {

				public static DamageContext damage(DamageSource source) {
						return new DamageContext(Instant.now(), source, null);
				}

				public static DamageContext death(DamageSource damageSource, Component component) {
				}

				public DamageType damageType() {
						return this.source.getDamageType();
				}

				public String deathMessage() {
						return this.fatal == null ? "" : this.fatal.deathMessage();
				}

				public boolean isFatal() {
						return this.fatal != null;
				}

				public boolean isFromPlayer() {
						return this.source.getCausingEntity() instanceof Player;
				}

				public boolean isFromEnvironment() {
						return !this.isFromPlayer();
				}

				public record Fatal(String string) {


				}
		}

		public record KillRanking(UUID killer, int kills) implements Comparable<KillRanking> {

				@Override
				public int compareTo(KillRanking other) {
						int result = Integer.compare(other.kills, this.kills);
						return result != 0
										? result
										: this.killer.compareTo(other.killer);
				}
		}
}