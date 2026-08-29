package team.heather.hardlands.module.scenario;

import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.module.scenario.implementation.AppleGroveScenario;
import team.heather.hardlands.module.scenario.implementation.BonanzaScenario;
import team.heather.hardlands.module.scenario.implementation.EnchantiaScenario;
import team.heather.hardlands.util.text.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

public enum Scenario {

    APPLE_GROVE(
            Hardlands.createKey("apple_grove"),
            "Apple Grove",
            "Permite obtener manzanas especiales al romper o deteriorarse las hojas.",
            Material.GOLDEN_APPLE,
            new AppleGroveScenario()
    ),

    BONANZA(
            Hardlands.createKey("bonanza"),
            "Bonanza",
            "Multiplica la cantidad de recursos obtenidos al minar minerales.",
            Material.DIAMOND_ORE,
            new BonanzaScenario()
    ),

    ENCHANTIA(
            Hardlands.createKey("enchantia"),
            "Enchantia",
            "Gestiona la aplicación y generación de encantamientos vanilla y de Hardlands.",
            Material.ENCHANTING_TABLE,
            new EnchantiaScenario()
    );

    private final NamespacedKey namespacedKey;
    private final String label;
    private final String description;
    private final Material material;
    private final ScenarioProcessor processor;

    Scenario(
            NamespacedKey namespacedKey,
            String label,
            String description,
            Material material,
            ScenarioProcessor processor
    ) {
        this.namespacedKey = namespacedKey;
        this.label = label;
        this.description = description;
        this.material = material;
        this.processor = processor;
    }

    void initialize(Hardlands plugin) {
        this.processor.initializeScenario(plugin, this.namespacedKey.getKey());
    }

    void enable() {
        this.processor.enableScenario();
    }

    void disable() {
        this.processor.disableScenario();
    }

    public ItemStack createItem(boolean enabled) {
        Component name = TextFormatter.formatTinyCaps(this.label).color(
                enabled
                        ? HardlandsColor.PRIMARY
                        : NamedTextColor.DARK_GRAY
        );

        String action = enabled ? "Desactivar" : "Activar";
        String controls = !this.processor.getConfigurationOptions().isEmpty()
                ? "<dark_gray>Izq. ↔ %s | Der. ✎ Configurar".formatted(action)
                : "<dark_gray>Izq. ↔ %s".formatted(action);

        return new ItemBuilder(this.material)
                .name(name)
                .glint(enabled)
                .addFormattedLore(
                        this.description,
                        "",
                        this.createConfigurationLore(),
                        "",
                        controls
                )
                .build();
    }

    public NamespacedKey getNamespacedKey() {
        return this.namespacedKey;
    }

    public String getLabel() {
        return this.label;
    }

    public ScenarioProcessor getProcessor() {
        return this.processor;
    }

    public static Optional<Scenario> findByKey(String value) {
        for (Scenario scenario : values()) {
            if (scenario.namespacedKey.getKey().equalsIgnoreCase(value)) {
                return Optional.of(scenario);
            }
        }

        return Optional.empty();
    }

    private String createConfigurationLore() {
        StringBuilder lore = new StringBuilder("{Configuración}:\n");

        this.processor.getConfigurationOptions().forEach((key, option) ->
                lore.append("[→] {%s}: [%s]\n".formatted(
                        key,
                        option.getValue()
                ))
        );

        return lore.toString();
    }
}