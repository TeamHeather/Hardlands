package team.heather.hardlands.game.preset;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Material;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.data.json.JsonDataManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class PresetRepository {

    public static final Material DEFAULT_ICON = Material.BOOK;
    private static final String FILE_EXTENSION = ".json";
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N} _-]{1,32}");

    private final Hardlands plugin;
    private final Path directory;

    public PresetRepository(Hardlands plugin, String directory) {
        this.plugin = plugin;
        this.directory = plugin.getDataPath().resolve(directory);
    }

    public void save(String name, Material icon, String description) {
        String normalizedName = validateName(name);
        validateIcon(icon);

        this.managerFor(normalizedName).write(new Preset(
                icon.getKey().asString(),
                normalizeDescription(description),
                this.plugin.getScenarioManager().toJson().getAsJsonObject(),
                this.plugin.getGameManager().toJson().getAsJsonObject(),
                this.plugin.getWorldManager().toJson().getAsJsonObject()));
    }

    public void load(String name) {
        String normalizedName = validateName(name);

        this.managerFor(normalizedName).read().ifPresent(preset -> {
            this.plugin.getScenarioManager().fromJson(preset.scenarios());
            this.plugin.getGameManager().fromJson(preset.phase());
            this.plugin.getWorldManager().fromJson(preset.world());
        });
    }

    public void update(String name, Material icon, String description) {
        String normalizedName = validateName(name);
        validateIcon(icon);

        JsonDataManager<Preset> manager = this.managerFor(normalizedName);
        manager.read().ifPresent(preset ->
                manager.write(preset.withMetadata(icon, description)));
    }

    public void delete(String name) {
        String normalizedName = validateName(name);

        try {
            Files.deleteIfExists(this.pathFor(normalizedName));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public List<PresetInfo> presets() {
        return this.presetNames().stream()
                .flatMap(name -> this.managerFor(name).read().stream()
                        .map(preset -> preset.toInfo(name)))
                .toList();
    }

    public boolean exists(String name) {
        if (!this.isNameValid(name)) return false;

        return Files.isRegularFile(this.pathFor(name.strip()));
    }

    public boolean isNameValid(String name) {
        return name != null && NAME_PATTERN.matcher(name.strip()).matches();
    }

    private List<String> presetNames() {
        if (Files.notExists(this.directory)) return List.of();

        try (Stream<Path> paths = Files.list(this.directory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(FILE_EXTENSION))
                    .map(name -> name.substring(0, name.length() - FILE_EXTENSION.length()))
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private JsonDataManager<Preset> managerFor(String name) {
        return new JsonDataManager<>(
                Hardlands.GSON,
                this.pathFor(name),
                Preset.class);
    }

    private Path pathFor(String name) {
        return this.directory.resolve(name + FILE_EXTENSION);
    }

    private static String validateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Preset name cannot be null");
        }

        String normalizedName = name.strip();

        if (!NAME_PATTERN.matcher(normalizedName).matches()) {
            throw new IllegalArgumentException("Invalid preset name: " + name);
        }

        return normalizedName;
    }

    private static void validateIcon(Material icon) {
        if (!isValidIcon(icon)) {
            throw new IllegalArgumentException("Invalid preset icon: " + icon);
        }
    }

    private static boolean isValidIcon(Material icon) {
        return icon != null
                && icon.isItem()
                && !icon.isAir();
    }

    private static String normalizeDescription(String description) {
        return description == null
                ? ""
                : description.strip();
    }

    public record PresetInfo(String name, Material icon, String description) {}

    private record Preset(
            @SerializedName("icon") String icon,
            @SerializedName("description") String description,
            @SerializedName("scenarios") JsonObject scenarios,
            @SerializedName("phase") JsonObject phase,
            @SerializedName("world") JsonObject world
    ) {

        private Preset withMetadata(Material icon, String description) {
            return new Preset(
                    icon.getKey().asString(),
                    normalizeDescription(description),
                    this.scenarios,
                    this.phase,
                    this.world);
        }

        private PresetInfo toInfo(String name) {
            return new PresetInfo(
                    name,
                    parseIcon(this.icon),
                    normalizeDescription(this.description));
        }

        private static Material parseIcon(String value) {
            Material material = value == null
                    ? null
                    : Material.matchMaterial(value);
            return isValidIcon(material)
                    ? material
                    : DEFAULT_ICON;
        }
    }
}