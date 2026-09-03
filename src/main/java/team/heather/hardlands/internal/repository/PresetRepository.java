package team.heather.hardlands.internal.repository;

import java.util.List;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;
import org.bukkit.Material;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.json.JsonDataManager;

public final class PresetRepository extends JsonRepository<String> {

    public static final Material DEFAULT_ICON = Material.BOOK;
    public static final Pattern NAME_PATTERN = Pattern.compile("[\\p{L}\\p{N} _-]{1,32}");

    private final Hardlands hardlands;

    public PresetRepository(Hardlands hardlands) {
        super(hardlands, "presets");
        this.hardlands = hardlands;
    }

    public void save(String name, Material icon, String description) {
        String normalizedName = validateName(name);
        validateIcon(icon);

        this.managerFor(normalizedName).write(new Preset(
                icon.getKey().asString(),
                normalizeDescription(description),
                this.hardlands.getScenarioManager().toJson().getAsJsonObject(),
                this.hardlands.getGameManager().toJson().getAsJsonObject(),
                this.hardlands.getWorldManager().toJson().getAsJsonObject()
        ));
    }

    public void load(String name) {
        this.managerFor(validateName(name)).read().ifPresent(preset -> {
            this.hardlands.getScenarioManager().fromJson(preset.scenarios());
            this.hardlands.getGameManager().fromJson(preset.phase());
            this.hardlands.getWorldManager().fromJson(preset.world());
        });
    }

    public void update(String name, Material icon, String description) {
        String normalizedName = validateName(name);
        validateIcon(icon);

        JsonDataManager<Preset> manager = this.managerFor(normalizedName);

        manager.read().ifPresent(preset ->
                manager.write(preset.withMetadata(icon, description)));
    }

    @Override
    public void delete(String name) {
        super.delete(validateName(name));
    }

    @Override
    public boolean exists(String name) {
        return isNameValid(name) && super.exists(name.strip());
    }

    public List<PresetInfo> presets() {
        return this.entryNames().stream()
                .filter(this::isNameValid)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .flatMap(name -> this.managerFor(name).read().stream()
                        .map(preset -> preset.toInfo(name)))
                .toList();
    }

    public boolean isNameValid(String name) {
        return name != null && NAME_PATTERN.matcher(name.strip()).matches();
    }

    private JsonDataManager<Preset> managerFor(String name) {
        return super.managerFor(name, Preset.class);
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
        return icon != null && icon.isItem() && !icon.isAir();
    }

    private static String normalizeDescription(String description) {
        return description == null ? "" : description.strip();
    }

    public record PresetInfo(
            String name,
            Material icon,
            String description
    ) {}

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
                    this.world
            );
        }

        private PresetInfo toInfo(String name) {
            return new PresetInfo(
                    name,
                    parseIcon(this.icon),
                    normalizeDescription(this.description)
            );
        }

        private static Material parseIcon(String value) {
            Material material = value == null
                    ? null
                    : Material.matchMaterial(value);
            return isValidIcon(material) ? material : DEFAULT_ICON;
        }
    }
}