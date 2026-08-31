package team.heather.hardlands.game.world;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import team.heather.hardlands.Hardlands;

public final class ScatterManager {

		private static final int MAX_SCATTER_ATTEMPTS = 100;

		private static final Material SAFETY_BLOCK =
						Material.REINFORCED_DEEPSLATE;

		private static final Map<PotionEffectType, Integer> SAFETY_EFFECTS =
						Map.of(
										PotionEffectType.RESISTANCE, 5,
										PotionEffectType.MINING_FATIGUE, 3,
										PotionEffectType.WEAKNESS, 1,
										PotionEffectType.SLOW_FALLING, 0,
										PotionEffectType.BLINDNESS, 0
						);

		private static final Set<Material> BLACKLISTED_BLOCKS =
						Set.of(
										Material.WATER,
										Material.LAVA,
										Material.FIRE,
										Material.SOUL_FIRE,
										Material.MAGMA_BLOCK,
										Material.CACTUS,
										Material.POWDER_SNOW,
										Material.CAMPFIRE,
										Material.SOUL_CAMPFIRE,
										Material.SWEET_BERRY_BUSH,
										Material.WITHER_ROSE,
										Material.POINTED_DRIPSTONE
						);

		private static final Set<Biome> BLACKLISTED_BIOMES =
						Set.of(
										Biome.OCEAN,
										Biome.DEEP_OCEAN,
										Biome.COLD_OCEAN,
										Biome.DEEP_COLD_OCEAN,
										Biome.LUKEWARM_OCEAN,
										Biome.DEEP_LUKEWARM_OCEAN,
										Biome.WARM_OCEAN,
										Biome.FROZEN_OCEAN,
										Biome.DEEP_FROZEN_OCEAN
						);

		private final Deque<UUID> scatterQueue =
						new ArrayDeque<>();

		private final World world;

		private boolean progressUpdatesEnabled;

		public ScatterManager(World world) {
				this.world = world;
		}

		public void enqueue(Player player) {
				UUID playerId = player.getUniqueId();

				if (this.scatterQueue.contains(playerId)) {
						return;
				}

				this.scatterQueue.addLast(playerId);
				this.updateGameProgress();
		}

		public void remove(Player player) {
				if (!this.scatterQueue.remove(
								player.getUniqueId()
				)) {
						return;
				}

				this.updateGameProgress();
		}

		public void scatterNext() {
				this.scatterNext(true);
		}

		public void scatterAll() {
				while (!this.scatterQueue.isEmpty()) {
						this.scatterNext(false);
				}

				this.updateGameProgress();
		}

		public void setProgressUpdatesEnabled(
						boolean enabled
		) {
				this.progressUpdatesEnabled = enabled;

				if (enabled) {
						this.updateGameProgress();
				}
		}

		public float getScatterPercentage() {
				int onlinePlayers =
								Bukkit.getOnlinePlayers().size();

				if (onlinePlayers == 0) {
						return 100.0F;
				}

				float progress =
								(onlinePlayers - this.scatterQueue.size())
												* 100.0F
												/ onlinePlayers;

				return Math.clamp(
								progress,
								0.0F,
								100.0F
				);
		}

		public boolean isCompleted() {
				return this.scatterQueue.isEmpty();
		}

		private void scatterNext(boolean updateProgress) {
				UUID playerId =
								this.scatterQueue.pollFirst();

				if (playerId == null) {
						return;
				}

				Player player =
								Bukkit.getPlayer(playerId);

				if (player != null) {
						teleportPlayerSafely(
										player,
										this.findLocation()
						);
				}

				if (updateProgress) {
						this.updateGameProgress();
				}
		}

		private void updateGameProgress() {
				if (!this.progressUpdatesEnabled) {
						return;
				}

				float progress =
								this.getScatterPercentage();

				Runnable action = () ->
								Hardlands.getInstance()
												.getGameManager()
												.setScatterProgress(progress);

				if (Bukkit.isPrimaryThread()) {
						action.run();
						return;
				}

				Bukkit.getScheduler().runTask(
								Hardlands.getInstance(),
								action
				);
		}

		private Location findLocation() {
				for (int attempt = 0;
				     attempt < MAX_SCATTER_ATTEMPTS;
				     attempt++) {

						Location location =
										this.randomLocation();

						if (validateLocation(location)) {
								return location;
						}
				}

				throw new IllegalStateException(
								"Unable to find a valid scatter location"
				);
		}

		private Location randomLocation() {
				WorldBorder border =
								this.world.getWorldBorder();

				Location center =
								border.getCenter();

				double radius =
								border.getSize() / 2.0D - 1.0D;

				int minX = (int) Math.ceil(
								center.getX() - radius
				);

				int maxX = (int) Math.floor(
								center.getX() + radius
				);

				int minZ = (int) Math.ceil(
								center.getZ() - radius
				);

				int maxZ = (int) Math.floor(
								center.getZ() + radius
				);

				ThreadLocalRandom random =
								ThreadLocalRandom.current();

				int x = random.nextInt(
								minX,
								maxX + 1
				);

				int z = random.nextInt(
								minZ,
								maxZ + 1
				);

				int y = this.world
								.getHighestBlockYAt(x, z)
								+ 1;

				return new Location(
								this.world,
								x + 0.5D,
								y,
								z + 0.5D
				);
		}

		private static boolean validateLocation(
						Location location
		) {
				Block feet = location.getBlock();
				Block head = feet.getRelative(0, 1, 0);
				Block ground = feet.getRelative(0, -1, 0);

				if (BLACKLISTED_BIOMES.contains(
								ground.getBiome()
				)) {
						return false;
				}

				if (BLACKLISTED_BLOCKS.contains(
								ground.getType()
				)) {
						return false;
				}

				return ground.getType().isSolid()
								&& feet.isPassable()
								&& head.isPassable();
		}

		private static void teleportPlayerSafely(
						Player player,
						Location location
		) {
				location.getBlock()
								.getRelative(0, -1, 0)
								.setType(SAFETY_BLOCK);

				for (Map.Entry<PotionEffectType, Integer> entry
								: SAFETY_EFFECTS.entrySet()) {

						player.addPotionEffect(
										new PotionEffect(
														entry.getKey(),
														PotionEffect.INFINITE_DURATION,
														entry.getValue(),
														false,
														false,
														false
										)
						);
				}

				player.teleport(location);
		}
}