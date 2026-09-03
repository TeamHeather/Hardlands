package team.heather.hardlands.common.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.repository.PlayerRepository;

public final class PlayerManager {

		private final PlayerRepository repository;
		private final Map<UUID, HardlandsPlayer> playersById;

		public PlayerManager(Hardlands hardlands) {
				this.repository = hardlands.getRepositories().player();
				this.playersById = new HashMap<>();
		}

		/**
		 * Loads every persisted player into memory.
		 */
		public void load() {
				this.playersById.clear();
				this.repository.players().forEach(info ->
								this.repository.load(info.uuid()).ifPresent(player ->
												this.playersById.put(info.uuid(), player)));
		}

		/**
		 * Saves every cached player.
		 */
		public void save() {
				this.playersById.values().forEach(this.repository::save);
		}

		public @Nullable HardlandsPlayer get(Player player) {
				return this.get(player.getUniqueId());
		}

		public @Nullable HardlandsPlayer get(UUID uuid) {
				return this.playersById.get(uuid);
		}

		public void register(Player player) {
				UUID uuid = player.getUniqueId();
				if (this.playersById.containsKey(uuid)) {
						throw new IllegalStateException("Player is already registered: " + player.getName());
				}
				this.playersById.put(uuid, this.repository.loadOrCreate(player));
		}

		public void unregister(Player player) {
				UUID uuid = player.getUniqueId();
				HardlandsPlayer hardlandsPlayer = this.playersById.remove(uuid);
				if (hardlandsPlayer == null) {
						throw new IllegalStateException("Player is not registered: " + player.getName());
				}
				this.repository.save(hardlandsPlayer);
		}
}