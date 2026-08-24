package org.heather.hardlands.module;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.core.data.json.JsonDataManager;

public final class PresetRepository {

    public static final String DEFAULT_NAME = "DEFAULT";
    public static final Material DEFAULT_ICON = Material.BOOK;

    private static final String EXTENSION = ".json";
    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N} _-]{1,32}");

    private final Hardlands plugin;
    private final Path directory;

    public PresetRepository(Hardlands plugin) {
        this.plugin = plugin;
        this.directory = plugin.getDataPath().resolve("presets");
    }

    public void save(String name, Material icon, String description) {
        this.validateName(name);
        this.validateIcon(icon);

        try {
            Files.createDirectories(this.directory);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        this.managerFor(name).write(new Preset(
                icon.getKey().asString(),
                cleanDescription(description),
                this.plugin.getGeneralConfiguration().toJson().getAsJsonObject(),
                this.plugin.getWorldManagerOrThrow().toJson().getAsJsonObject(),
                this.plugin.getScenarioManager().toJson().getAsJsonObject(),
                this.plugin.getPhaseController().toJson().getAsJsonObject()));
    }

    public void load(String name) {
        this.validateName(name);

        this.managerFor(name).read().ifPresent(preset -> {
            this.plugin.getGeneralConfiguration().fromJson(preset.general());
            this.plugin.getWorldManagerOrThrow().fromJson(preset.world());
            this.plugin.getScenarioManager().fromJson(preset.scenarios());
            this.plugin.getPhaseController().fromJson(preset.phase());
        });
    }

    public void loadDefault() {
        this.findPresetName(DEFAULT_NAME).ifPresent(this::load);
    }

    public void updateMetadata(String name, Material icon, String description) {
        this.validateName(name);
        this.validateIcon(icon);

        JsonDataManager<Preset> manager = this.managerFor(name);

        manager.read().ifPresent(preset -> manager.write(new Preset(
                icon.getKey().asString(),
                cleanDescription(description),
                preset.general(),
                preset.world(),
                preset.scenarios(),
                preset.phase())));
    }

    public void delete(String name) {
        this.validateName(name);

        try {
            Files.deleteIfExists(this.pathFor(name));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public List<PresetInfo> getPresets() {
        List<PresetInfo> presets = new ArrayList<>();

        for (String name : this.getPresetNames()) {
            this.managerFor(name).read().ifPresent(preset ->
                    presets.add(new PresetInfo(name, parseIcon(preset.icon()), cleanDescription(preset.description()))));
        }

        return presets;
    }

    public boolean exists(String name) {
        return this.findPresetName(name).isPresent();
    }

    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.strip()).matches();
    }

    public static boolean isValidIcon(Material material) {
        return material != null && material.isItem() && !material.isAir();
    }

    public record PresetInfo(String name, Material icon, String description) {}

    private List<String> getPresetNames() {
        if (Files.notExists(this.directory)) return List.of();

        try (Stream<Path> files = Files.list(this.directory)) {
            return files
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(EXTENSION))
                    .map(name -> name.substring(0, name.length() - EXTENSION.length()))
                    .filter(this::isValidName)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private Optional<String> findPresetName(String name) {
        if (!this.isValidName(name)) return Optional.empty();

        String target = name.strip();

        for (String preset : this.getPresetNames()) {
            if (preset.equalsIgnoreCase(target)) return Optional.of(preset);
        }

        return Optional.empty();
    }

    private JsonDataManager<Preset> managerFor(String name) {
        return new JsonDataManager<>(Hardlands.GSON, this.pathFor(name), Preset.class);
    }

    private Path pathFor(String name) {
        return this.directory.resolve(name.strip() + EXTENSION);
    }

    private void validateName(String name) {
        if (!this.isValidName(name)) throw new IllegalArgumentException("Invalid preset name: " + name);
    }

    private void validateIcon(Material icon) {
        if (!isValidIcon(icon)) throw new IllegalArgumentException("Invalid preset icon: " + icon);
    }

    private static Material parseIcon(String value) {
        Material material = value == null ? null : Material.matchMaterial(value);
        return isValidIcon(material) ? material : DEFAULT_ICON;
    }

    private static String cleanDescription(String description) {
        return description == null ? "" : description.strip();
    }

    private record Preset(
            @SerializedName("icon") String icon,
            @SerializedName("description") String description,
            @SerializedName("general") JsonObject general,
            @SerializedName("world") JsonObject world,
            @SerializedName("scenarios") JsonObject scenarios,
            @SerializedName("phase") JsonObject phase
    ) {}
}