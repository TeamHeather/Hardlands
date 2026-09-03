package team.heather.hardlands.internal;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.internal.config.Option;
import team.heather.hardlands.internal.config.Validator;
import team.heather.hardlands.internal.json.JsonDataManager;

@ConfigBuilder(
				identifier = "internal",
				options = {
								@OptionDef(name = "knownOres", type = Set.class, elementType = Material.class),
								@OptionDef(name = "scatterBlacklistedBiomes", type = Set.class, elementType = String.class),
								@OptionDef(name = "scatterBlacklistedBlocks", type = Set.class, elementType = Material.class),
								@OptionDef(name = "scatterSafetyBlock", type = Material.class),
								@OptionDef(name = "scatterMaxAttempts", type = Integer.class, validators = Validator.Keys.POSITIVE),
								@OptionDef(name = "scatterMinDelayTicks", type = Long.class, validators = Validator.Keys.POSITIVE),
								@OptionDef(name = "scatterMaxDelayTicks", type = Long.class, validators = Validator.Keys.POSITIVE),
								@OptionDef(
												name = "scatterSafetyEffects",
												type = Map.class,
												keyType = String.class,
												valueType = Integer.class,
												validators = Validator.Keys.NON_EMPTY
								)
				}
)
public final class InternalDefinitions extends InternalDefinitionsConfiguration {

		private final JsonDataManager<JsonObject> dataManager;

		public InternalDefinitions(Plugin plugin, String path) {
				this.dataManager = new JsonDataManager<>(
								Hardlands.GSON,
								plugin.getDataPath().resolve(path + ".json"),
								JsonObject.class
				);
		}

		public void load() {
				this.applyDefaults();
				this.dataManager.read().ifPresent(this::fromJson);

				if (!this.isConfigValid()) {
						throw new IllegalStateException("Internal definitions contain invalid values");
				}

				this.save();
		}

		public void save() {
				this.dataManager.write(toJson().getAsJsonObject());
		}

		public boolean isKnownOre(Material material) {
				return contains(super.knownOres, material);
		}

		public boolean isScatterBlacklistedBiome(Biome biome) {
				return biome != null && contains(super.scatterBlacklistedBiomes, biome.getKey().toString());
		}

		public boolean isScatterBlacklistedBlock(Material material) {
				return contains(super.scatterBlacklistedBlocks, material);
		}

		public Material getScatterSafetyBlock() {
				return requireValue(super.scatterSafetyBlock);
		}

		public int getScatterMaxAttempts() {
				return requireValue(super.scatterMaxAttempts);
		}

		public long getScatterMinDelayTicks() {
				return requireValue(super.scatterMinDelayTicks);
		}

		public long getScatterMaxDelayTicks() {
				return requireValue(super.scatterMaxDelayTicks);
		}

		public Map<PotionEffectType, Integer> getScatterSafetyEffects() {
				Map<String, Integer> configuredEffects = requireValue(super.scatterSafetyEffects);
				Map<PotionEffectType, Integer> resolvedEffects = LinkedHashMap.newLinkedHashMap(configuredEffects.size());

				for (Map.Entry<String, Integer> entry : configuredEffects.entrySet()) {
						PotionEffectType type = resolveEffectType(entry.getKey());

						if (type == null) {
								throw new IllegalStateException("Unknown scatter safety effect: " + entry.getKey());
						}

						resolvedEffects.put(type, entry.getValue());
				}

				return Map.copyOf(resolvedEffects);
		}

		@Override
		protected boolean onConfigValidation() {
				return getScatterMinDelayTicks() <= getScatterMaxDelayTicks()
								&& getScatterSafetyBlock().isBlock()
								&& getScatterSafetyBlock().isSolid()
								&& hasValidSafetyEffects();
		}

		private void applyDefaults() {
				knownOres.changeValue(Set.of(
								Material.COAL_ORE,
								Material.DEEPSLATE_COAL_ORE,
								Material.COPPER_ORE,
								Material.DEEPSLATE_COPPER_ORE,
								Material.IRON_ORE,
								Material.DEEPSLATE_IRON_ORE,
								Material.GOLD_ORE,
								Material.DEEPSLATE_GOLD_ORE,
								Material.REDSTONE_ORE,
								Material.DEEPSLATE_REDSTONE_ORE,
								Material.EMERALD_ORE,
								Material.DEEPSLATE_EMERALD_ORE,
								Material.LAPIS_ORE,
								Material.DEEPSLATE_LAPIS_ORE,
								Material.DIAMOND_ORE,
								Material.DEEPSLATE_DIAMOND_ORE,
								Material.NETHER_GOLD_ORE,
								Material.NETHER_QUARTZ_ORE,
								Material.ANCIENT_DEBRIS
				));

				scatterBlacklistedBiomes.changeValue(Set.of(
								"minecraft:ocean",
								"minecraft:deep_ocean",
								"minecraft:cold_ocean",
								"minecraft:deep_cold_ocean",
								"minecraft:lukewarm_ocean",
								"minecraft:deep_lukewarm_ocean",
								"minecraft:warm_ocean",
								"minecraft:frozen_ocean",
								"minecraft:deep_frozen_ocean"
				));

				scatterBlacklistedBlocks.changeValue(Set.of(
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
				));

				scatterSafetyBlock.changeValue(Material.REINFORCED_DEEPSLATE);
				scatterMaxAttempts.changeValue(100);
				scatterMinDelayTicks.changeValue(1L);
				scatterMaxDelayTicks.changeValue(20L);

				scatterSafetyEffects.changeValue(Map.of(
								"minecraft:resistance", 5,
								"minecraft:slowness", 4,
								"minecraft:mining_fatigue", 3,
								"minecraft:weakness", 1,
								"minecraft:slow_falling", 0,
								"minecraft:blindness", 0
				));
		}

		private boolean hasValidSafetyEffects() {
				for (Map.Entry<String, Integer> entry : requireValue(scatterSafetyEffects).entrySet()) {
						if (entry.getValue() == null || entry.getValue() < 0 || resolveEffectType(entry.getKey()) == null) {
								return false;
						}
				}

				return true;
		}

		private static @Nullable PotionEffectType resolveEffectType(String value) {
				NamespacedKey key = NamespacedKey.fromString(value);
				return key == null ? null : Registry.MOB_EFFECT.get(key);
		}

		private static <T> boolean contains(Option<Set<T>> option, T value) {
				return value != null && option.hasValue() && option.getValue().contains(value);
		}

		private static <T> T requireValue(Option<T> option) {
				T value = option.getValue();

				if (value == null) {
						throw new IllegalStateException("Internal definition has no value: " + option.getKey());
				}

				return value;
		}
}