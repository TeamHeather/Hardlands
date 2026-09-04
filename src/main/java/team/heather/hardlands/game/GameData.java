package team.heather.hardlands.game;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GameData {

		private final LinkedHashMap<UUID, FirstDamage> firstDamageByPlayer = new LinkedHashMap<>();
		private final HashMap<UUID, Integer> killCountByPlayer = new HashMap<>();
		private final HashMap<UUID, Integer> assistCountByPlayer = new HashMap<>();
		private final ArrayList<DeathContext> deathHistory = new ArrayList<>();

		private Set<UUID> winnerIds = Set.of();

		@Nullable private Instant startedAt;
		@Nullable private Instant endedAt;
		@Nullable private Host host;

		public boolean recordFirstDamage(UUID playerId, DamageType damageType) {
				return this.firstDamageByPlayer.putIfAbsent(
								playerId,
								new FirstDamage(Instant.now(), playerId, damageType)
				) == null;
		}

		public int recordKill(UUID playerId) {
				return this.killCountByPlayer.merge(playerId, 1, Integer::sum);
		}

		public int recordAssist(UUID playerId) {
				return this.assistCountByPlayer.merge(playerId, 1, Integer::sum);
		}

		public void recordDeath(UUID playerId, String deathMessage, DamageSource damageSource) {
				this.deathHistory.add(
								new DeathContext(Instant.now(), playerId, deathMessage, damageSource)
				);
		}

		public void markStarted() {
				this.startedAt = Instant.now();
				this.endedAt = null;
		}

		public void markEnded() {
				if (this.startedAt == null) {
						throw new IllegalStateException("The game has not started.");
				}

				this.endedAt = Instant.now();
		}

		public void setWinners(@NotNull Set<UUID> winnerIds) {
				this.winnerIds = Set.copyOf(winnerIds);
		}

		public void setHost(@NotNull Host host) {
				this.host = host;
		}

		public int getKillCount(UUID playerId) {
				return this.killCountByPlayer.getOrDefault(playerId, 0);
		}

		public int getAssistCount(UUID playerId) {
				return this.assistCountByPlayer.getOrDefault(playerId, 0);
		}

		public List<KillRanking> getKillLeaderboard() {
				List<KillRanking> leaderboard = new ArrayList<>(this.killCountByPlayer.size());

				this.killCountByPlayer.forEach((playerId, killCount) ->
								leaderboard.add(new KillRanking(playerId, killCount)));

				leaderboard.sort(null);
				return leaderboard;
		}

		public List<FirstDamage> getFirstDamageHistory() {
				return List.copyOf(this.firstDamageByPlayer.values());
		}

		public List<DeathContext> getDeathHistory() {
				return List.copyOf(this.deathHistory);
		}

		public Set<UUID> getWinners() {
				return this.winnerIds;
		}

		public @Nullable FirstDamage getPaperMan() {
				Map.Entry<UUID, FirstDamage> entry = this.firstDamageByPlayer.firstEntry();

				return entry != null
								? entry.getValue()
								: null;
		}

		public @Nullable FirstDamage getIronMan() {
				Map.Entry<UUID, FirstDamage> entry = this.firstDamageByPlayer.lastEntry();

				return entry != null
								? entry.getValue()
								: null;
		}

		public @Nullable Instant getStartedAt() {
				return this.startedAt;
		}

		public @Nullable Instant getEndedAt() {
				return this.endedAt;
		}

		public @Nullable Host getHost() {
				return this.host;
		}

		public record Host(
						int hostNumber,
						UUID playerId
		) {}

		public record FirstDamage(
						Instant occurredAt,
						UUID playerId,
						DamageType damageType
		) {}

		public record DeathContext(
						Instant occurredAt,
						UUID playerId,
						String deathMessage,
						DamageSource damageSource
		) {

				public boolean isPvP() {
						return this.damageSource.getCausingEntity() instanceof Player;
				}

				public boolean isPvE() {
						return !this.isPvP();
				}
		}

		public record KillRanking(
						UUID playerId,
						int killCount
		) implements Comparable<KillRanking> {

				@Override
				public int compareTo(KillRanking other) {
						int result = Integer.compare(other.killCount, this.killCount);

						return result != 0
										? result
										: this.playerId.compareTo(other.playerId);
				}
		}
}