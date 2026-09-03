package team.heather.hardlands.game.team;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class TeamManager {

    private final Map<UUID, String> teams = new HashMap<>();

    public void set(UUID playerId, @Nullable String team) {
        if (team == null || team.isBlank()) {
            teams.remove(playerId);
            return;
        }

        teams.put(playerId, team.strip());
    }

    public void set(Player player, @Nullable String team) {
        if (team == null || team.isBlank()) {
            teams.remove(player.getUniqueId());
            return;
        }

        teams.put(player.getUniqueId(), team.strip());
    }

    public @Nullable String get(Player player) {
        return teams.get(player.getUniqueId());
    }

    public @Nullable String get(UUID playerId) {
        return teams.get(playerId);
    }

    public boolean areTeammates(Player first, Player second) {
        String team = get(first);
        return team != null && team.equals(get(second));
    }

    public void clear() {
        teams.clear();
    }
}