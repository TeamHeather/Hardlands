package team.heather.hardlands.internal.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.json.JsonDataManager;

public final class PlayerRepository extends JsonRepository<UUID> {

    public PlayerRepository(Hardlands hardlands) {
        super(hardlands, "players");
    }

    public team.heather.hardlands.common.player.PlayerData create(@NotNull Player player) {
        return this.create(player.getName(), player.getUniqueId());
    }

    public team.heather.hardlands.common.player.PlayerData create(@NotNull String name, @NotNull UUID uuid) {
        if (name.isBlank()) {
            throw new IllegalArgumentException("Player name cannot be blank");
        }

        if (this.exists(uuid)) {
            throw new IllegalStateException("Player data already exists: " + uuid);
        }

        team.heather.hardlands.common.player.PlayerData player = team.heather.hardlands.common.player.PlayerData.from(name, uuid);

        this.save(player);

        return player;
    }

    public void save(@NotNull team.heather.hardlands.common.player.PlayerData player) {
        this.managerFor(player.getUniqueId()).write(PlayerRepository.PlayerData.from(player));
    }

    public Optional<team.heather.hardlands.common.player.PlayerData> load(@NotNull UUID uuid) {
        return this.managerFor(uuid).read().map(data -> data.toPlayer(uuid));
    }

    public Optional<team.heather.hardlands.common.player.PlayerData> load(@NotNull Player player) {
        return this.managerFor(player.getUniqueId()).read().map(data -> data.toPlayer(player));
    }

    public team.heather.hardlands.common.player.PlayerData loadOrCreate(@NotNull Player player) {
        return this.load(player).orElseGet(() -> this.create(player));
    }

    public List<PlayerInfo> players() {
        List<PlayerInfo> players = new ArrayList<>();

        for (String entry : this.entryNames()) {
            Optional<UUID> uuid = parseUuid(entry);

            if (uuid.isEmpty()) {
                continue;
            }

            this.managerFor(uuid.get()).read().map(data -> data.toInfo(uuid.get())).ifPresent(players::add);
        }

        players.sort((first, second) -> first.uuid().compareTo(second.uuid()));

        return players;
    }

    private JsonDataManager<PlayerData> managerFor(UUID uuid) {
        return super.managerFor(uuid, PlayerData.class);
    }

    private static Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public record PlayerInfo(String name, UUID uuid) {}

    private record PlayerData(
            @SerializedName("name") String name,
            @SerializedName("uuid") UUID uuid,
            @SerializedName("options") JsonObject options
    ) {

        private static PlayerData from(team.heather.hardlands.common.player.PlayerData player) {
            return new PlayerData(player.getName(), player.getUniqueId(), player.toJson().getAsJsonObject());
        }

        private team.heather.hardlands.common.player.PlayerData toPlayer(UUID expectedUuid) {
            this.validate(expectedUuid);

            team.heather.hardlands.common.player.PlayerData player = team.heather.hardlands.common.player.PlayerData.from(this.name, this.uuid);

            player.fromJson(this.options);

            return player;
        }

        private team.heather.hardlands.common.player.PlayerData toPlayer(Player player) {
            this.validate(player.getUniqueId());

            team.heather.hardlands.common.player.PlayerData playerData = team.heather.hardlands.common.player.PlayerData.from(player);

            playerData.fromJson(this.options);

            return playerData;
        }

        private PlayerInfo toInfo(UUID expectedUuid) {
            this.validate(expectedUuid);

            return new PlayerInfo(this.name, this.uuid);
        }

        private void validate(UUID expectedUuid) {
            if (this.name == null || this.name.isBlank()) {
                throw new IllegalStateException("Stored player name cannot be null or blank: " + expectedUuid);
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