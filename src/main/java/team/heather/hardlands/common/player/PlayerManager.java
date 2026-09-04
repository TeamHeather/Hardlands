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
		private final Map<UUID, PlayerData> playersById;

		public PlayerManager(Hardlands hardlands) {
				this.repository = hardlands.getRepositories().player();
				this.playersById = new HashMap<>();
		}

		public void load() {
				this.playersById.clear();

				for (PlayerRepository.PlayerInfo info : this.repository.players()) {
						this.repository.load(info.uuid()).ifPresent(player -> this.playersById.put(info.uuid(), player));
				}
		}

		public void save() {
				for (PlayerData player : this.playersById.values()) {
						this.repository.save(player);
				}
		}

		public @Nullable PlayerData get(Player player) {
				return this.get(player.getUniqueId());
		}

		public @Nullable PlayerData get(UUID uuid) {
				return this.playersById.get(uuid);
		}

		public void register(Player player) {
				this.register(player.getName(), player.getUniqueId());
		}

		public void register(String name, UUID uuid) {
				if (this.playersById.containsKey(uuid)) {
						throw new IllegalStateException("Player is already registered: " + name);
				}

				PlayerData player = this.repository.load(uuid).orElseGet(() -> this.repository.create(name, uuid));

				this.playersById.put(uuid, player);
		}

		public void unregister(Player player) {
				PlayerData playerData = this.playersById.remove(player.getUniqueId());

				if (playerData == null) {
						throw new IllegalStateException("Player is not registered: " + player.getName());
				}

				this.repository.save(playerData);
		}
}