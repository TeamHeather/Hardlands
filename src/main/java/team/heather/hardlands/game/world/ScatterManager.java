package team.heather.hardlands.game.world;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

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
import org.bukkit.scheduler.BukkitTask;
import team.heather.hardlands.Hardlands;

public final class ScatterManager {

		private static final Material SAFETY_BLOCK = Material.REINFORCED_DEEPSLATE;
		private static final int MAX_SCATTER_ATTEMPTS = 100;

		private static final long NANOS_PER_TICK = Duration.ofMillis(50).toNanos();
		private static final long MIN_SCATTER_DELAY_TICKS = 1L;
		private static final long MAX_SCATTER_DELAY_TICKS = 20L;

		private static final Map<PotionEffectType, Integer> SAFETY_EFFECTS = Map.of(
						PotionEffectType.RESISTANCE, 5,
						PotionEffectType.SLOWNESS, 4,
						PotionEffectType.MINING_FATIGUE, 3,
						PotionEffectType.WEAKNESS, 1,
						PotionEffectType.SLOW_FALLING, 0,
						PotionEffectType.BLINDNESS, 0
		);

		private static final Set<Material> BLACKLISTED_BLOCKS = Set.of(
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

		private static final Set<Biome> BLACKLISTED_BIOMES = Set.of(
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

		private final Deque<UUID> scatterQueue;
		private final World world;

		private boolean progressUpdatesEnabled;
		private boolean scatterAllMode;
		private int completedScatters;
		private long scatterGeneration;

		private UUID activePlayerId;
		private CompletableFuture<Void> activeScatterFuture;
		private BukkitTask continuationTask;

		public ScatterManager(World world) {
				this.scatterQueue = new ArrayDeque<>();
				this.world = world;
		}

		public void enqueue(Player player) {
				UUID uuid = player.getUniqueId();
				if (this.scatterQueue.contains(uuid)) return;

				if (!this.isScattering() && this.scatterQueue.isEmpty()) this.completedScatters = 0;

				this.scatterQueue.addLast(uuid);
				this.updateGameProgress();
		}

		public void remove(Player player) {
				UUID uuid = player.getUniqueId();
				if (uuid.equals(this.activePlayerId) || !this.scatterQueue.remove(uuid)) return;

				this.updateGameProgress();
		}

		public void scatterNext() {
				if (this.scatterQueue.isEmpty() || this.isScattering()) return;
				this.startScatter(false);
		}

		public void scatterAll() {
				this.scatterAllAsync();
		}

		public CompletableFuture<Void> scatterAllAsync() {
				return this.startScatter(true);
		}

		public void cancelScatter() {
				this.scatterGeneration++;

				if (this.continuationTask != null) {
						this.continuationTask.cancel();
						this.continuationTask = null;
				}

				if (this.activeScatterFuture != null && !this.activeScatterFuture.isDone()) {
						this.activeScatterFuture.cancel(false);
				}

				this.activeScatterFuture = null;
				this.activePlayerId = null;
				this.scatterAllMode = false;
		}

		public void setProgressUpdatesEnabled(boolean enabled) {
				this.progressUpdatesEnabled = enabled;
				if (enabled) this.updateGameProgress();
		}

		public float getScatterPercentage() {
				int totalScatters = this.completedScatters + this.scatterQueue.size();
				if (totalScatters == 0) return 100.0F;

				float progress = this.completedScatters * 100.0F / totalScatters;
				return Math.clamp(progress, 0.0F, 100.0F);
		}

		public boolean isCompleted() {
				return this.scatterQueue.isEmpty() && !this.isScattering();
		}

		public boolean isScattering() {
				return this.activeScatterFuture != null && !this.activeScatterFuture.isDone();
		}

		private CompletableFuture<Void> startScatter(boolean scatterAll) {
				if (this.isScattering()) {
						if (scatterAll) this.scatterAllMode = true;
						return this.activeScatterFuture;
				}

				CompletableFuture<Void> future = new CompletableFuture<>();

				this.activeScatterFuture = future;
				this.scatterAllMode = scatterAll;

				long generation = ++this.scatterGeneration;
				this.processNext(generation, future);

				return future;
		}

		private void processNext(long generation, CompletableFuture<Void> future) {
				if (!this.isCurrentScatter(generation, future)) return;

				UUID uuid = this.scatterQueue.peekFirst();

				if (uuid == null) {
						this.finishScatter(generation, future);
						return;
				}

				Player player = Bukkit.getPlayer(uuid);

				if (player == null) {
						this.scatterQueue.removeFirst();
						this.updateGameProgress();
						this.continueScatter(generation, future, MIN_SCATTER_DELAY_TICKS);
						return;
				}

				this.activePlayerId = uuid;
				long startedAt = System.nanoTime();

				this.findLocationAsync(0)
								.thenCompose(location -> {
										if (!this.isCurrentScatter(generation, future) || !player.isOnline()) {
												return CompletableFuture.completedFuture(false);
										}

										return teleportPlayerSafelyAsync(player, location);
								})
								.whenComplete((success, exception) -> {
										if (!this.isCurrentScatter(generation, future)) return;

										this.activePlayerId = null;

										if (exception != null) {
												this.failScatter(generation, future, exception);
												return;
										}

										if (!Boolean.TRUE.equals(success)) {
												if (!player.isOnline()) {
														this.scatterQueue.removeFirstOccurrence(uuid);
														this.updateGameProgress();
														this.continueScatter(
																		generation,
																		future,
																		MIN_SCATTER_DELAY_TICKS
														);
														return;
												}

												this.failScatter(
																generation,
																future,
																new IllegalStateException(
																				"Unable to teleport player during scatter: "
																								+ player.getName()
																)
												);
												return;
										}

										this.scatterQueue.removeFirstOccurrence(uuid);
										this.completedScatters++;
										this.updateGameProgress();

										long elapsedNanos = System.nanoTime() - startedAt;
										long delayTicks = calculateScatterDelayTicks(elapsedNanos);

										this.continueScatter(generation, future, delayTicks);
								});
		}

		private void continueScatter(
						long generation,
						CompletableFuture<Void> future,
						long delayTicks
		) {
				this.continuationTask = Bukkit.getScheduler().runTaskLater(
								Hardlands.getInstance(),
								() -> {
										this.continuationTask = null;
										if (!this.isCurrentScatter(generation, future)) return;

										if (!this.scatterAllMode) {
												this.finishScatter(generation, future);
												return;
										}

										this.processNext(generation, future);
								},
								delayTicks
				);
		}

		private void finishScatter(long generation, CompletableFuture<Void> future) {
				if (!this.isCurrentScatter(generation, future)) return;

				this.activeScatterFuture = null;
				this.activePlayerId = null;
				this.scatterAllMode = false;

				this.updateGameProgress();
				future.complete(null);
		}

		private void failScatter(
						long generation,
						CompletableFuture<Void> future,
						Throwable exception
		) {
				if (!this.isCurrentScatter(generation, future)) return;

				this.activeScatterFuture = null;
				this.activePlayerId = null;
				this.scatterAllMode = false;

				Hardlands.getInstance().getLogger().log(Level.SEVERE, "Player scatter failed", exception);
				future.completeExceptionally(exception);
		}

		private boolean isCurrentScatter(long generation, CompletableFuture<Void> future) {
				return this.scatterGeneration == generation
								&& this.activeScatterFuture == future
								&& !future.isDone();
		}

		private void updateGameProgress() {
				if (!this.progressUpdatesEnabled) return;
				Hardlands.getInstance().getGameManager().setScatterProgress(this.getScatterPercentage());
		}

		private CompletableFuture<Location> findLocationAsync(int attempt) {
				if (attempt >= MAX_SCATTER_ATTEMPTS) {
						return CompletableFuture.failedFuture(
										new IllegalStateException("Unable to find a valid scatter location")
						);
				}

				Location candidate = this.randomHorizontalLocation();
				int blockX = candidate.getBlockX();
				int blockZ = candidate.getBlockZ();

				return this.world.getChunkAtAsync(blockX >> 4, blockZ >> 4, true, false)
								.thenCompose(ignored -> {
										int blockY = this.world.getHighestBlockYAt(blockX, blockZ) + 1;

										Location location = new Location(
														this.world,
														blockX + 0.5D,
														blockY,
														blockZ + 0.5D
										);

										return validateLocation(location)
														? CompletableFuture.completedFuture(location)
														: this.findLocationAsync(attempt + 1);
								});
		}

		private Location randomHorizontalLocation() {
				WorldBorder border = this.world.getWorldBorder();
				Location center = border.getCenter();

				double radius = border.getSize() / 2.0D - 1.0D;
				int minX = (int) Math.ceil(center.getX() - radius);
				int maxX = (int) Math.floor(center.getX() + radius);
				int minZ = (int) Math.ceil(center.getZ() - radius);
				int maxZ = (int) Math.floor(center.getZ() + radius);

				ThreadLocalRandom random = ThreadLocalRandom.current();
				int x = random.nextInt(minX, maxX + 1);
				int z = random.nextInt(minZ, maxZ + 1);

				return new Location(this.world, x + 0.5D, 0.0D, z + 0.5D);
		}

		private static boolean validateLocation(Location location) {
				Block feet = location.getBlock();
				Block head = feet.getRelative(0, 1, 0);
				Block ground = feet.getRelative(0, -1, 0);

				if (BLACKLISTED_BIOMES.contains(ground.getBiome())
								|| BLACKLISTED_BLOCKS.contains(ground.getType())) {
						return false;
				}

				return ground.getType().isSolid() && feet.isPassable() && head.isPassable();
		}

		private static CompletableFuture<Boolean> teleportPlayerSafelyAsync(
						Player player,
						Location location
		) {
				location.getBlock().getRelative(0, -1, 0).setType(SAFETY_BLOCK);

				SAFETY_EFFECTS.forEach((type, amplifier) -> player.addPotionEffect(
								new PotionEffect(
												type,
												PotionEffect.INFINITE_DURATION,
												amplifier,
												false,
												false,
												false
								)
				));

				return player.teleportAsync(location);
		}

		private static long calculateScatterDelayTicks(long elapsedNanos) {
				long elapsedTicks = Math.ceilDiv(elapsedNanos, NANOS_PER_TICK);
				return Math.clamp(elapsedTicks, MIN_SCATTER_DELAY_TICKS, MAX_SCATTER_DELAY_TICKS);
		}
}