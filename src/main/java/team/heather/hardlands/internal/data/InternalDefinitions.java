package team.heather.hardlands.internal.data;

import java.util.Set;

import com.google.gson.JsonObject;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.plugin.Plugin;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.config.ConfigBuilder;
import team.heather.hardlands.config.OptionDef;
import team.heather.hardlands.internal.config.Option;
import team.heather.hardlands.internal.data.json.JsonDataManager;

@ConfigBuilder(
				identifier = "internal",
				options = {
								@OptionDef(name = "knownOres", type = Set.class, elementType = Material.class),
								@OptionDef(name = "scatterBlacklistedBiomes", type = Set.class, elementType = NamespacedKey.class),
								@OptionDef(name = "scatterBlacklistedBlocks", type = Set.class, elementType = Material.class)
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

		public boolean isKnownOre(Material material) {
				return contains(super.knownOres, material);
		}

		public boolean isScatterBlacklistedBiome(Biome biome) {
				return biome != null && contains(super.scatterBlacklistedBiomes, biome.getKey());
		}

		public boolean isScatterBlacklistedBlock(Material material) {
				return contains(super.scatterBlacklistedBlocks, material);
		}

		public void load() {
				this.dataManager.read().ifPresentOrElse(this::fromJson, this::save);
		}

		public void save() {
				this.dataManager.write(super.toJson().getAsJsonObject());
		}

		private static <T> boolean contains(Option<Set<T>> option, T value) {
				return value != null && option.hasValue() && option.getValue().contains(value);
		}
}