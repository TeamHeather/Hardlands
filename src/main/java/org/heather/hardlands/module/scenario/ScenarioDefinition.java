package org.heather.hardlands.module.scenario;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.core.config.Option;
import org.heather.hardlands.module.scenario.scenarios.AppleGroveScenario;
import org.heather.hardlands.module.scenario.scenarios.BonanzaScenario;
import org.heather.hardlands.module.scenario.scenarios.MagicManScenario;

public enum ScenarioDefinition {

    APPLE_GROVE(
            "Apple Grove",
            AppleGroveScenario::new,
            Material.GOLDEN_APPLE,
            "Aumenta la obtención de {manzanas} y sus variantes."),

    BONANZA(
            "Bonanza",
            BonanzaScenario::new,
            Material.GOLD_ORE,
            "Multiplica los recursos obtenidos de {minerales}."),

    MAGIC_MAN(
            "Magic Man",
            MagicManScenario::new,
            Material.ENCHANTING_TABLE,
            "Configura los {encantamientos} disponibles.");

    private final String name;
    private final Supplier<Scenario> factory;
    private final ItemStack displayItem;

    public String identifier() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public Scenario createScenario() {
        return this.factory.get();
    }

    public ItemStack createDisplayItem(Scenario scenario, boolean enabled) {
        ItemBuilder builder = new ItemBuilder(this.displayItem).glint(enabled);
        boolean hasOptions = false;

        for (Option<?> option : scenario.getConfigurationOptions().values()) {
            if (!option.hasValue()) continue;

            if (!hasOptions) {
                builder.addFormattedLore("");
                hasOptions = true;
            }

            builder.addFormattedLore("%s: {%s}".formatted(option.getKey(), formatValue(option.getValue())));
        }

        builder.addFormattedLore(
                "",
                "Estado: {%s}".formatted(enabled ? "Activo" : "Inactivo"),
                "",
                "{Clic izquierdo} para alternar.");

        if (!scenario.getConfigurationOptions().isEmpty()) {
            builder.addFormattedLore("{Clic derecho} para configurar.");
        }

        return builder.build();
    }

    public String getName() {
        return this.name;
    }

    public static Optional<ScenarioDefinition> findByIdentifier(String identifier) {
        for (ScenarioDefinition definition : values()) {
            if (definition.identifier().equals(identifier)) return Optional.of(definition);
        }

        return Optional.empty();
    }

    private ScenarioDefinition(String name, Supplier<Scenario> factory, Material material, String description) {
        this.name = name;
        this.factory = factory;
        this.displayItem = InventoryItem.createDisplayStack(material, name, description);
    }

    private static String formatValue(Object value) {
        if (value instanceof Boolean booleanValue) return booleanValue ? "Activado" : "Desactivado";

        if (value instanceof Float || value instanceof Double) {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }

        if (value instanceof Map<?, ?> map) {
            return "%d configurado%s".formatted(map.size(), map.size() == 1 ? "" : "s");
        }

        return String.valueOf(value);
    }
}