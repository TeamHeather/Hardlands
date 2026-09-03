package team.heather.hardlands.internal.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.internal.json.JsonDataManager;

public final class PlayerRepository extends JsonRepository<UUID> {

    public PlayerRepository(Hardlands hardlands) {
        super(hardlands, "players");
    }

    public HardlandsPlayer create(@NotNull Player player) {
        UUID uuid = player.getUniqueId();

        if (this.exists(uuid)) {
            throw new IllegalStateException("Player data already exists: " + uuid);
        }

        HardlandsPlayer hardlandsPlayer = HardlandsPlayer.from(player);
        this.save(hardlandsPlayer);

        return hardlandsPlayer;
    }

    public void save(@NotNull HardlandsPlayer player) {
        this.managerFor(player.getUniqueId()).write(PlayerData.from(player));
    }

    public Optional<HardlandsPlayer> load(@NotNull UUID uuid) {
        return this.managerFor(uuid).read()
                .map(data -> data.toPlayer(uuid));
    }

    public Optional<HardlandsPlayer> load(@NotNull Player player) {
        return this.managerFor(player.getUniqueId()).read()
                .map(data -> data.toPlayer(player));
    }

    public HardlandsPlayer loadOrCreate(@NotNull Player player) {
        return this.load(player).orElseGet(() -> this.create(player));
    }

    public List<PlayerInfo> players() {
        return this.entryNames().stream()
                .map(PlayerRepository::parseUuid)
                .flatMap(Optional::stream)
                .sorted()
                .flatMap(uuid -> this.managerFor(uuid).read().stream()
                        .map(data -> data.toInfo(uuid)))
                .toList();
    }

    private JsonDataManager<PlayerData> managerFor(UUID uuid) {
        return super.managerFor(uuid, PlayerData.class);
    }

    private static Optional<UUID> parseUuid(String value) {
        return Optional.of(UUID.fromString(value));
    }

    public record PlayerInfo(String name, UUID uuid) {}

    private record PlayerData(
            @SerializedName("name") String name,
            @SerializedName("uuid") UUID uuid,
            @SerializedName("options") JsonObject options
    ) {

        private static PlayerData from(HardlandsPlayer player) {
            return new PlayerData(
                    player.getName(),
                    player.getUniqueId(),
                    player.toJson().getAsJsonObject()
            );
        }

        private HardlandsPlayer toPlayer(UUID expectedUuid) {
            this.validate(expectedUuid);

            HardlandsPlayer player = HardlandsPlayer.from(this.name, this.uuid);
            player.fromJson(this.options);

            return player;
        }

        private HardlandsPlayer toPlayer(Player player) {
            this.validate(player.getUniqueId());

            HardlandsPlayer hardlandsPlayer = HardlandsPlayer.from(player);
            hardlandsPlayer.fromJson(this.options);

            return hardlandsPlayer;
        }

        private PlayerInfo toInfo(UUID expectedUuid) {
            this.validate(expectedUuid);
            return new PlayerInfo(this.name, this.uuid);
        }

        private void validate(UUID expectedUuid) {
            if (this.name == null || this.name.isBlank()) {
                throw new IllegalStateException("Stored player name cannot be null or blank");
            }

            if (this.uuid == null || !this.uuid.equals(expectedUuid)) {
                throw new IllegalStateException("Stored UUID does not match player file: " + expectedUuid);
            }

            if (this.options == null) {
                throw new IllegalStateException("Stored player options cannot be null: " + expectedUuid);
            }
        }
    }
}