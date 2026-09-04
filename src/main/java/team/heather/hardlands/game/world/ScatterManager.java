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
import team.heather.hardlands.internal.InternalDefinitions;
import team.heather.hardlands.util.TextFormatters;

public final class ScatterManager {

		private final Hardlands hardlands;
		private final Deque<UUID> queue;
		private final World world;

		@Nullable private CompletableFuture<Void> activeScatter;
		private int completedScatters;
		private boolean progressUpdatesEnabled;
		private boolean scatterAllPlayers;

		public ScatterManager(Hardlands hardlands, World world) {
				this.hardlands = hardlands;
				this.queue = new ArrayDeque<>();
				this.world = world;
		}

		public void enqueue(Player player) {
				UUID playerId = player.getUniqueId();

				if (this.queue.contains(playerId)) {
						return;
				}

				if (this.completed()) {
						this.completedScatters = 0;
				}

				this.queue.addLast(playerId);
				this.updateGameProgress();
		}

		public void remove(Player player) {
				UUID playerId = player.getUniqueId();

				if (this.scattering() && playerId.equals(this.queue.peekFirst())) {
						return;
				}

				if (this.queue.remove(playerId)) {
						this.updateGameProgress();
				}
		}

		public void cancelScatter() {
				CompletableFuture<Void> scatter = this.activeScatter;

				if (scatter == null) {
						return;
				}

				if (this.scattering()) {
						scatter.cancel(false);
				}

				this.clearActiveScatter();
		}

		public void clearEffects(Player player) {
				this.hardlands.getInternalDefinitions()
								.getScatterSafetyEffects()
								.keySet()
								.forEach(player::removePotionEffect);
		}

		public void scatterAll() {
				this.scatterAllAsync();
		}

		public CompletableFuture<Void> scatterAllAsync() {
				return this.startScatter();
		}

		public boolean completed() {
				return this.queue.isEmpty() && !this.scattering();
		}

		public boolean progressUpdatesEnabled() {
				return this.progressUpdatesEnabled;
		}

		public void progressUpdatesEnabled(boolean enabled) {
				if (this.progressUpdatesEnabled == enabled) {
						return;
				}

				this.progressUpdatesEnabled = enabled;

				if (enabled) {
						this.updateGameProgress();
				}
		}

		public float scatterPercentage() {
				int totalScatters = this.completedScatters + this.queue.size();

				if (totalScatters == 0) {
						return 0.0F;
				}

				return Math.clamp(this.completedScatters * 100.0F / totalScatters, 0.0F, 100.0F);
		}

		public boolean scattering() {
				return this.activeScatter != null && !this.activeScatter.isDone();
		}

		// Private Workflow

		private void applySafetyEffects(Player player) {
				for (Map.Entry<PotionEffectType, Integer> effect
								: this.hardlands.getInternalDefinitions().getScatterSafetyEffects().entrySet()) {
						player.addPotionEffect(new PotionEffect(
										effect.getKey(),
										PotionEffect.INFINITE_DURATION,
										effect.getValue(),
										false,
										false,
										false
						));
				}
		}

		private void beginTeleport(
						CompletableFuture<Void> scatter,
						Player player,
						UUID playerId,
						@Nullable Location location,
						@Nullable Throwable exception
		) {
				if (!this.isCurrentScatter(scatter)) {
						return;
				}

				if (exception != null) {
						this.failScatter(scatter, exception);
						return;
				}

				if (location == null || !player.isOnline()) {
						this.handleFailedTeleport(scatter, player, playerId);
						return;
				}

				this.applySafetyEffects(player);

				location.getBlock()
								.getRelative(0, -1, 0)
								.setType(this.hardlands.getInternalDefinitions().getScatterSafetyBlock());

				long startedAt = System.nanoTime();

				player.teleportAsync(location)
								.thenApply(success ->
												new TeleportResult(Boolean.TRUE.equals(success), System.nanoTime() - startedAt))
								.whenComplete((result, teleportException) ->
												Bukkit.getScheduler().runTask(
																this.hardlands,
																() -> this.completeTeleport(scatter, player, playerId, result, teleportException)
												)
								);
		}

		private void clearActiveScatter() {
				this.activeScatter = null;
				this.scatterAllPlayers = false;
		}

		private void completeTeleport(
						CompletableFuture<Void> scatter,
						Player player,
						UUID playerId,
						@Nullable TeleportResult result,
						@Nullable Throwable exception
		) {
				if (!this.isCurrentScatter(scatter)) {
						return;
				}

				if (exception != null) {
						this.failScatter(scatter, exception);
						return;
				}

				if (result == null || !result.success()) {
						this.handleFailedTeleport(scatter, player, playerId);
						return;
				}

				this.queue.removeFirstOccurrence(playerId);
				this.completedScatters++;

				this.updateGameProgress();
				this.continueScatter(scatter, this.calculateScatterDelay(result.elapsedNanos()));
		}

		private void continueScatter(CompletableFuture<Void> scatter, long delayTicks) {
				this.hardlands.getThreadScheduler().schedule(
								() -> Bukkit.getScheduler().runTask(this.hardlands, () -> {
										if (!this.isCurrentScatter(scatter)) {
												return;
										}

										if (this.scatterAllPlayers) {
												this.processNext(scatter);
										} else {
												this.finishScatter(scatter);
										}
								}),
								Duration.ofMillis(delayTicks * 50L)
				);
		}

		private void failScatter(CompletableFuture<Void> scatter, Throwable exception) {
				if (!this.isCurrentScatter(scatter)) {
						return;
				}

				this.clearActiveScatter();
				this.hardlands.getLogger().log(Level.SEVERE, "Player scatter failed", exception);
				scatter.completeExceptionally(exception);
		}

		private void finishScatter(CompletableFuture<Void> scatter) {
				if (!this.isCurrentScatter(scatter)) {
						return;
				}

				this.clearActiveScatter();
				this.updateGameProgress();
				scatter.complete(null);
		}

		private void handleFailedTeleport(
						CompletableFuture<Void> scatter,
						Player player,
						UUID playerId
		) {
				if (!player.isOnline()) {
						this.skipPlayer(scatter, playerId);
						return;
				}

				this.failScatter(
								scatter,
								new IllegalStateException("Unable to teleport player during scatter: " + player.getName())
				);
		}

		private void processNext(CompletableFuture<Void> scatter) {
				if (!this.isCurrentScatter(scatter)) {
						return;
				}

				UUID playerId = this.queue.peekFirst();

				if (playerId == null) {
						this.finishScatter(scatter);
						return;
				}

				Player player = Bukkit.getPlayer(playerId);

				if (player == null) {
						this.skipPlayer(scatter, playerId);
						return;
				}

				this.showTeleportNotice(player);

				CompletableFuture<Location> location = this.findLocationAsync(0);

				this.hardlands.getThreadScheduler().schedule(
								() -> location.whenComplete((destination, exception) ->
												Bukkit.getScheduler().runTask(
																this.hardlands,
																() -> this.beginTeleport(scatter, player, playerId, destination, exception)
												)
								),
								Duration.ofSeconds(3)
				);
		}

		private void showTeleportNotice(Player player) {
				Component title = Component.text("◆ ", HardlandsColor.RED.secondary())
								.append(TextFormatters.TINY_CAPS.formatColored("Teleport").color(HardlandsColor.HARDLANDS))
								.append(Component.text(" ◆", HardlandsColor.RED.secondary()));

				Component subtitle = TextFormatters.TINY_CAPS
								.formatColored("serás teletransportado en unos segundos...")
								.color(HardlandsColor.LIGHT_GRAY);

				player.showTitle(Title.title(
								title,
								subtitle,
								Title.Times.times(Duration.ofMillis(150), Duration.ofSeconds(3), Duration.ofMillis(350))
				));

				player.getWorld().playSound(
								player.getLocation(),
								Sound.ENTITY_ILLUSIONER_MIRROR_MOVE,
								0.8F,
								1.0F
				);
		}

		private void skipPlayer(CompletableFuture<Void> scatter, UUID playerId) {
				this.queue.removeFirstOccurrence(playerId);
				this.updateGameProgress();

				this.continueScatter(
								scatter,
								this.hardlands.getInternalDefinitions().getScatterMinDelayTicks()
				);
		}

		private CompletableFuture<Void> startScatter() {
				if (this.scattering()) {
						this.scatterAllPlayers = true;
						return this.activeScatter;
				}

				CompletableFuture<Void> scatter = new CompletableFuture<>();

				this.activeScatter = scatter;
				this.scatterAllPlayers = true;
				this.processNext(scatter);

				return scatter;
		}

		// Private Location Methods

		private CompletableFuture<Location> findLocationAsync(int attempt) {
				if (attempt >= this.hardlands.getInternalDefinitions().getScatterMaxAttempts()) {
						return CompletableFuture.failedFuture(
										new IllegalStateException("Unable to find a valid scatter location")
						);
				}

				return this.supplyMainThread(this::randomHorizontalLocation).thenCompose(candidate -> {
						int blockX = candidate.getBlockX();
						int blockZ = candidate.getBlockZ();

						return this.world.getChunkAtAsync(blockX >> 4, blockZ >> 4, true, false).thenCompose(ignored ->
										this.supplyMainThread(() -> {
												int blockY = this.world.getHighestBlockYAt(blockX, blockZ) + 1;
												Location location = new Location(this.world, blockX + 0.5D, blockY, blockZ + 0.5D);

												return this.isValidLocation(location) ? location : null;
										})
						);
				}).thenCompose(location ->
								location == null
												? this.findLocationAsync(attempt + 1)
												: CompletableFuture.completedFuture(location)
				);
		}

		private boolean isValidLocation(Location location) {
				Block feet = location.getBlock();
				Block head = feet.getRelative(0, 1, 0);
				Block ground = feet.getRelative(0, -1, 0);
				InternalDefinitions definitions = this.hardlands.getInternalDefinitions();

				return ground.getType().isSolid()
								&& feet.isPassable()
								&& head.isPassable()
								&& !definitions.isScatterBlacklistedBiome(ground.getBiome())
								&& !definitions.isScatterBlacklistedBlock(ground.getType());
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

		// Private Utilities

		private long calculateScatterDelay(long elapsedNanos) {
				long elapsedTicks = Math.ceilDiv(elapsedNanos, Duration.ofMillis(50).toNanos());
				InternalDefinitions definitions = this.hardlands.getInternalDefinitions();

				return Math.clamp(
								elapsedTicks,
								definitions.getScatterMinDelayTicks(),
								definitions.getScatterMaxDelayTicks()
				);
		}

		private boolean isCurrentScatter(CompletableFuture<Void> scatter) {
				return this.activeScatter == scatter && !scatter.isDone();
		}

		private <T> CompletableFuture<T> supplyMainThread(Supplier<T> supplier) {
				CompletableFuture<T> future = new CompletableFuture<>();

				Bukkit.getScheduler().runTask(this.hardlands, () -> {
						try {
								future.complete(supplier.get());
						} catch (RuntimeException e) {
								future.completeExceptionally(e);
						}
				});

				return future;
		}

		private void updateGameProgress() {
				if (this.progressUpdatesEnabled()) {
						this.hardlands.getGameManager().setScatterProgress(this.scatterPercentage());
				}
		}

		private record TeleportResult(boolean success, long elapsedNanos) {}
}