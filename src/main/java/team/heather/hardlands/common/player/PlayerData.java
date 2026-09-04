package team.heather.hardlands.common.player;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.OptionDef;

@ConfigBuilder(
				identifier = "player",
				options = {
								@OptionDef(name = "profileColor", type = DyeColor.class),
								@OptionDef(name = "pinnedStatistics", type = Set.class, elementType = String.class),

								@OptionDef(name = "bestFriend", type = UUID.class),
								@OptionDef(name = "mostPlayedHost", type = UUID.class),
								@OptionDef(name = "primaryKiller", type = UUID.class),
								@OptionDef(name = "primaryVictim", type = UUID.class),
								@OptionDef(name = "mostPlayedPreset", type = String.class),

								@OptionDef(name = "averageSurvivalTime", type = Double.class, value = "0"),
								@OptionDef(name = "gamesPlayed", type = Integer.class, value = "0"),
								@OptionDef(name = "ironManAwards", type = Integer.class, value = "0"),
								@OptionDef(name = "killTopAwards", type = Integer.class, value = "0"),
								@OptionDef(name = "paperManAwards", type = Integer.class, value = "0"),
								@OptionDef(name = "totalDeaths", type = Integer.class, value = "0"),
								@OptionDef(name = "totalKills", type = Integer.class, value = "0"),
								@OptionDef(name = "totalWins", type = Integer.class, value = "0")
				}
)
public class PlayerData extends PlayerDataConfiguration {

		private static final DyeColor[] PROFILE_COLORS = DyeColor.values();
		private final String name;
		private final UUID uuid;

		private PlayerData(String name, UUID uuid) {
				this.name = name;
				this.uuid = uuid;

				this.getProfileColorOption().changeValue(PROFILE_COLORS[ThreadLocalRandom.current().nextInt(PROFILE_COLORS.length)]);
				this.getPinnedStatisticsOption().changeValue(new LinkedHashSet<>());
		}

		public static PlayerData from(@NotNull Player player) {
				return new PlayerData(player.getName(), player.getUniqueId());
		}

		public static PlayerData from(String name, UUID uuid) {
				return new PlayerData(name, uuid);
		}

		public String getName() {
				return this.name;
		}

		public UUID getUniqueId() {
				return this.uuid;
		}

		public @Nullable Player getPlayer() {
				return Bukkit.getPlayer(this.uuid);
		}
}