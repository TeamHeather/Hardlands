package team.heather.hardlands.game.world;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.logging.Level;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.internal.data.InternalDefinitions;
import team.heather.hardlands.util.TextFormatters;

public final class ScatterManager {

		private static final Duration TICK_DURATION = Duration.ofMillis(50);
		private static final Duration TELEPORT_DELAY = Duration.ofSeconds(3);

		private final Hardlands plugin;
		private final World world;
		private final Deque<UUID> scatterQueue;

		private boolean progressUpdatesEnabled;
		private boolean scatterAllPlayers;
		private int completedScatters;

		@Nullable private CompletableFuture<Void> activeFuture;

		public ScatterManager(Hardlands plugin, World world) {
				if (plugin == null) {
						throw new IllegalArgumentException("Plugin cannot be null");
				}

				if (world == null) {
						throw new IllegalArgumentException("World cannot be null");
				}

				this.plugin = plugin;
				this.world = world;
				this.scatterQueue = new ArrayDeque<>();
		}

		public void enqueue(Player player) {
				UUID playerId = player.getUniqueId();

				if (this.scatterQueue.contains(playerId)) {
						return;
				}

				if (!this.isScattering() && this.scatterQueue.isEmpty()) {
						this.completedScatters = 0;
				}

				this.scatterQueue.addLast(playerId);
				this.updateGameProgress();
		}

		public void remove(Player player) {
				UUID playerId = player.getUniqueId();

				if (!this.isActivePlayer(playerId) && this.scatterQueue.remove(playerId)) {
						this.updateGameProgress();
				}
		}

		public void scatterNext() {
				if (!this.scatterQueue.isEmpty() && !this.isScattering()) {
						this.startScatter(false);
				}
		}

		public void scatterAll() {
				this.scatterAllAsync();
		}

		public CompletableFuture<Void> scatterAllAsync() {
				return this.startScatter(true);
		}

		public void cancelScatter() {
				CompletableFuture<Void> future = this.activeFuture;

				if (future != null && !future.isDone()) {
						future.cancel(false);
				}

				this.clearActiveScatter();
		}

		public void setProgressUpdatesEnabled(boolean enabled) {
				this.progressUpdatesEnabled = enabled;

				if (enabled) {
						this.updateGameProgress();
				}
		}

		public float getScatterPercentage() {
				int totalScatters = this.completedScatters + this.scatterQueue.size();

				if (totalScatters == 0) {
						return 100.0F;
				}

				return Math.clamp(this.completedScatters * 100.0F / totalScatters, 0.0F, 100.0F);
		}

		public boolean isCompleted() {
				return this.scatterQueue.isEmpty() && !this.isScattering();
		}

		public boolean isScattering() {
				return this.activeFuture != null && !this.activeFuture.isDone();
		}

		public void clearSafetyEffects(Player player) {
				this.definitions().getScatterSafetyEffects().keySet().forEach(player::removePotionEffect);
		}

		private CompletableFuture<Void> startScatter(boolean scatterAllPlayers) {
				if (this.isScattering()) {
						this.scatterAllPlayers |= scatterAllPlayers;
						return this.activeFuture;
				}

				CompletableFuture<Void> future = new CompletableFuture<>();

				this.activeFuture = future;
				this.scatterAllPlayers = scatterAllPlayers;
				this.processNext(future);

				return future;
		}

		private void processNext(CompletableFuture<Void> future) {
				if (!this.isCurrentScatter(future)) {
						return;
				}

				UUID playerId = this.scatterQueue.peekFirst();

				if (playerId == null) {
						this.finishScatter(future);
						return;
				}

				Player player = Bukkit.getPlayer(playerId);

				if (player == null) {
						this.skipPlayer(future, playerId);
						return;
				}

				this.showTeleportNotice(player);

				CompletableFuture<Location> locationFuture = this.findLocationAsync(0);

				this.plugin.getThreadScheduler().schedule(
								() -> locationFuture.whenComplete((location, exception) -> Bukkit.getScheduler().runTask(
												this.plugin,
												() -> this.beginTeleport(future, player, playerId, location, exception)
								)),
								TELEPORT_DELAY
				);
		}

		private void beginTeleport(
						CompletableFuture<Void> future,
						Player player,
						UUID playerId,
						@Nullable Location location,
						@Nullable Throwable exception
		) {
				if (!this.isCurrentScatter(future)) {
						return;
				}

				if (exception != null) {
						this.failScatter(future, exception);
						return;
				}

				if (location == null || !this.canTeleport(future, player)) {
						this.handleFailedTeleport(future, player, playerId);
						return;
				}

				this.teleportAsync(player, location).whenComplete((result, teleportException) -> Bukkit.getScheduler().runTask(
								this.plugin,
								() -> this.completeTeleport(future, player, playerId, result, teleportException)
				));
		}

		private CompletableFuture<TeleportResult> teleportAsync(Player player, Location location) {
				this.applySafetyEffects(player);
				this.prepareDestination(location);

				long startedAt = System.nanoTime();

				return player.teleportAsync(location).thenApply(success ->
								new TeleportResult(Boolean.TRUE.equals(success), System.nanoTime() - startedAt));
		}

		private void completeTeleport(
						CompletableFuture<Void> future,
						Player player,
						UUID playerId,
						@Nullable TeleportResult result,
						@Nullable Throwable exception
		) {
				if (!this.isCurrentScatter(future)) {
						return;
				}

				if (exception != null) {
						this.failScatter(future, exception);
						return;
				}

				if (result == null || !result.success()) {
						this.handleFailedTeleport(future, player, playerId);
						return;
				}

				this.scatterQueue.removeFirstOccurrence(playerId);
				this.completedScatters++;

				this.updateGameProgress();
				this.continueScatter(future, this.calculateScatterDelay(result.elapsedNanos()));
		}

		private void handleFailedTeleport(CompletableFuture<Void> future, Player player, UUID playerId) {
				if (!player.isOnline()) {
						this.skipPlayer(future, playerId);
						return;
				}

				this.failScatter(
								future,
								new IllegalStateException("Unable to teleport player during scatter: " + player.getName())
				);
		}

		private void skipPlayer(CompletableFuture<Void> future, UUID playerId) {
				this.scatterQueue.removeFirstOccurrence(playerId);
				this.updateGameProgress();
				this.continueScatter(future, this.definitions().getScatterMinDelayTicks());
		}

		private void continueScatter(CompletableFuture<Void> future, long delayTicks) {
				this.plugin.getThreadScheduler().schedule(
								() -> Bukkit.getScheduler().runTask(this.plugin, () -> {
										if (!this.isCurrentScatter(future)) {
												return;
										}

										if (this.scatterAllPlayers) {
												this.processNext(future);
										} else {
												this.finishScatter(future);
										}
								}),
								ticks(delayTicks)
				);
		}

		private void finishScatter(CompletableFuture<Void> future) {
				if (!this.isCurrentScatter(future)) {
						return;
				}

				this.clearActiveScatter();
				this.updateGameProgress();
				future.complete(null);
		}

		private void scheduleBlindnessRemoval() {
				Bukkit.getScheduler().runTaskLater(
								this.plugin,
								() -> Bukkit.getOnlinePlayers().forEach(player -> player.removePotionEffect(PotionEffectType.BLINDNESS)),
								60L
				);
		}

		private void failScatter(CompletableFuture<Void> future, Throwable exception) {
				if (!this.isCurrentScatter(future)) {
						return;
				}

				this.clearActiveScatter();
				this.plugin.getLogger().log(Level.SEVERE, "Player scatter failed", exception);
				future.completeExceptionally(exception);
		}

		private void clearActiveScatter() {
				this.activeFuture = null;
				this.scatterAllPlayers = false;
		}

		private boolean isCurrentScatter(CompletableFuture<Void> future) {
				return this.activeFuture == future && !future.isDone();
		}

		private boolean isActivePlayer(UUID playerId) {
				return this.isScattering() && playerId.equals(this.scatterQueue.peekFirst());
		}

		private boolean canTeleport(CompletableFuture<Void> future, Player player) {
				return this.isCurrentScatter(future) && player.isOnline();
		}

		private void updateGameProgress() {
				if (this.progressUpdatesEnabled) {
						this.plugin.getGameManager().setScatterProgress(this.getScatterPercentage());
				}
		}

		private CompletableFuture<Location> findLocationAsync(int attempt) {
				if (attempt >= this.definitions().getScatterMaxAttempts()) {
						return CompletableFuture.failedFuture(new IllegalStateException("Unable to find a valid scatter location"));
				}

				return this.supplyMainThread(this::randomHorizontalLocation).thenCompose(candidate -> {
						int blockX = candidate.getBlockX();
						int blockZ = candidate.getBlockZ();

						return this.world.getChunkAtAsync(blockX >> 4, blockZ >> 4, true, false).thenCompose(ignored ->
										this.supplyMainThread(() -> {
												Location location = this.createLocation(blockX, blockZ);
												return this.isValidLocation(location) ? location : null;
										})
						);
				}).thenCompose(location ->
								location != null ? CompletableFuture.completedFuture(location) : this.findLocationAsync(attempt + 1));
		}

		private Location createLocation(int blockX, int blockZ) {
				int blockY = this.world.getHighestBlockYAt(blockX, blockZ) + 1;
				return new Location(this.world, blockX + 0.5D, blockY, blockZ + 0.5D);
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

				return new Location(
								this.world,
								random.nextInt(minX, maxX + 1) + 0.5D,
								0.0D,
								random.nextInt(minZ, maxZ + 1) + 0.5D
				);
		}

		private boolean isValidLocation(Location location) {
				Block feet = location.getBlock();
				Block head = feet.getRelative(0, 1, 0);
				Block ground = feet.getRelative(0, -1, 0);
				InternalDefinitions definitions = this.definitions();

				return ground.getType().isSolid()
								&& feet.isPassable()
								&& head.isPassable()
								&& !definitions.isScatterBlacklistedBiome(ground.getBiome())
								&& !definitions.isScatterBlacklistedBlock(ground.getType());
		}

		private void showTeleportNotice(Player player) {
				Component title = Component.text("◆ ", HardlandsColor.RED.secondary())
								.append(TextFormatters.TINY_CAPS.formatColored("Teleport").color(HardlandsColor.HARDLANDS))
								.append(Component.text(" ◆", HardlandsColor.RED.secondary()));

				Component subtitle = TextFormatters.TINY_CAPS.formatColored("serás teletransportado en unos segundos...")
								.color(HardlandsColor.LIGHT_GRAY);

				player.showTitle(
								Title.title(
												title,
												subtitle,
												Title.Times.times(Duration.ofMillis(150), Duration.ofSeconds(3), Duration.ofMillis(350))
								)
				);

				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_MIRROR_MOVE, 0.8F, 1.0F);
		}

		private void applySafetyEffects(Player player) {
				for (Map.Entry<PotionEffectType, Integer> effect : this.definitions().getScatterSafetyEffects().entrySet()) {
						player.addPotionEffect(
										new PotionEffect(
														effect.getKey(),
														PotionEffect.INFINITE_DURATION,
														effect.getValue(),
														false,
														false,
														false
										)
						);
				}
		}

		private void prepareDestination(Location location) {
				location.getBlock().getRelative(0, -1, 0).setType(this.definitions().getScatterSafetyBlock());
		}

		private long calculateScatterDelay(long elapsedNanos) {
				long elapsedTicks = Math.ceilDiv(elapsedNanos, TICK_DURATION.toNanos());

				return Math.clamp(
								elapsedTicks,
								this.definitions().getScatterMinDelayTicks(),
								this.definitions().getScatterMaxDelayTicks()
				);
		}

		private <T> CompletableFuture<T> supplyMainThread(Supplier<T> supplier) {
				CompletableFuture<T> future = new CompletableFuture<>();

				Bukkit.getScheduler().runTask(this.plugin, () -> {
						try {
								future.complete(supplier.get());
						} catch (RuntimeException exception) {
								future.completeExceptionally(exception);
						}
				});

				return future;
		}

		private InternalDefinitions definitions() {
				return this.plugin.getInternalDefinitions();
		}

		private static Duration ticks(long ticks) {
				return TICK_DURATION.multipliedBy(ticks);
		}

		private record TeleportResult(boolean success, long elapsedNanos) {}
}