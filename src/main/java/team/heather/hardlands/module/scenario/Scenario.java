package team.heather.hardlands.module.scenario;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.item.ScenarioItemFactory;
import team.heather.hardlands.module.scenario.implementation.AppleGroveScenario;
import team.heather.hardlands.module.scenario.implementation.BonanzaScenario;
import team.heather.hardlands.module.scenario.implementation.EnchantiaScenario;

public enum Scenario {

    APPLE_GROVE(
        Hardlands.createKey("apple_grove"),
        "Apple Grove",
        "Permite obtener manzanas especiales al romper o deteriorarse las hojas.",
        Material.APPLE,
        new AppleGroveScenario()
    ),

    BONANZA(
        Hardlands.createKey("bonanza"),
        "Bonanza",
        "Multiplica la cantidad de recursos obtenidos al minar minerales.",
        Material.GOLD_ORE,
        new BonanzaScenario()
    ),

    ENCHANTIA(
        Hardlands.createKey("enchantia"),
        "Enchantia",
        "Gestiona la aplicación y generación de encantamientos vanilla y de Hardlands.",
        Material.ENCHANTING_TABLE,
        new EnchantiaScenario()
    ),

    ;

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

    public void initialize(Hardlands plugin) {
        this.processor.initializeScenario(plugin, this.namespacedKey.getKey());
    }

    public void enable() {
        this.processor.enableScenario();
    }

    public void disable() {
        this.processor.disableScenario();
    }

    public ItemStack createItem(boolean enabled) {
        return ScenarioItemFactory.create(this, enabled);
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

    public String getDescription() {
        return this.description;
    }

    public  Material getMaterial() {
        return this.material;
    }

    public static Optional<Scenario> findByKey(String value) {
        for (Scenario scenario : values()) {
            if (scenario.getNamespacedKey().getKey().equalsIgnoreCase(value)) {
                return Optional.of(scenario);
            }
        }

        return Optional.empty();
    }
}