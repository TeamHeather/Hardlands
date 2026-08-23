package org.heather.hardlands.module;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.core.data.json.JsonDataManager;

public final class PresetRepository {

    private static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N} _-]{1,32}");
    private static final String EXTENSION = ".json";

    private final Hardlands plugin;
    private final Path directory;

    private record Preset(
            @SerializedName("general") JsonObject general,
            @SerializedName("world") JsonObject world,
            @SerializedName("scenarios") JsonObject scenarios,
            @SerializedName("phase") JsonObject phase
    ) {}

    private PresetRepository(Hardlands plugin) {
        this.plugin = plugin;
        this.directory = plugin.getDataPath().resolve("presets");
    }

    public static PresetRepository create(Hardlands plugin) {
        return new PresetRepository(plugin);
    }

    public void save(String name) {
        this.validateName(name);
        this.managerFor(name).write(new Preset(
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

    public List<String> getPresetNames() {
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

    public boolean exists(String name) {
        return this.isValidName(name)
                && this.getPresetNames().stream().anyMatch(name.strip()::equalsIgnoreCase);
    }

    public boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name.strip()).matches();
    }

    private JsonDataManager<Preset> managerFor(String name) {
        return new JsonDataManager<>(
                Hardlands.GSON,
                this.directory.resolve(name.strip() + EXTENSION),
                Preset.class);
    }

    private void validateName(String name) {
        if (!this.isValidName(name)) {
            throw new IllegalArgumentException("Invalid preset name: " + name);
        }
    }
}