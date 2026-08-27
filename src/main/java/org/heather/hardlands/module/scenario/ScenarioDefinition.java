package org.heather.hardlands.module.scenario;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.core.configuration.Option;
import org.heather.hardlands.module.enchantment.HardlandsEnchantment;
import org.heather.hardlands.module.scenario.implementation.AppleGroveScenario;
import org.heather.hardlands.module.scenario.implementation.BonanzaScenario;
import org.heather.hardlands.module.scenario.implementation.MagicManScenario;
import org.heather.hardlands.util.text.HardlandsColor;
import org.heather.hardlands.util.text.TextFormatter;

public enum ScenarioDefinition {

    APPLE_GROVE("Apple Grove", AppleGroveScenario::new, Material.APPLE,
            "Aumenta la obtención de manzanas y sus variantes."),

    BONANZA("Bonanza", BonanzaScenario::new, Material.GOLD_ORE,
            "Multiplica los recursos obtenidos de minerales."),

    MAGIC_MAN("Magic Man", MagicManScenario::new, Material.ENCHANTING_TABLE,
            "Aplica los encantamientos configurados automáticamente a sus herramientas respectivas.");

    private final String name;
    private final Supplier<Scenario> factory;
    private final ItemStack baseDisplayItem;

    ScenarioDefinition(String name, Supplier<Scenario> factory, Material material, String description) {
        this.name = name;
        this.factory = factory;
        this.baseDisplayItem = InventoryItem.createDisplayStack(material, name, description);
    }

    public String identifier() {
        return this.name.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return this.name;
    }

    public Scenario createScenario() {
        return this.factory.get();
    }

    public ItemStack createDisplayItem(Scenario scenario, boolean enabled) {
        ItemBuilder builder = new ItemBuilder(this.baseDisplayItem)
                .name(TextFormatter.formatTinyCaps(this.name).color(enabled
                        ? HardlandsColor.PRIMARY
                        : NamedTextColor.DARK_GRAY))
                .glint(enabled)
                .addFormattedLore("", "{Estado}: [%s]".formatted(enabled ? "Activo" : "Inactivo"));

        if (this == MAGIC_MAN) addMagicManOptions(builder, scenario);
        else addConfiguredOptions(builder, scenario);

        boolean configurable = !scenario.getConfigurationOptions().isEmpty();

        String actions = configurable
                ? "<dark_gray>Izq. ↔ Alternar | Der. ✎ Configurar"
                : "<dark_gray>Izq. ↔ Alternar";

        return builder.addFormattedLore("", actions).build();
    }

    public static Optional<ScenarioDefinition> findByIdentifier(String identifier) {
        for (ScenarioDefinition definition : values()) {
            if (definition.identifier().equals(identifier)) return Optional.of(definition);
        }

        return Optional.empty();
    }

    private static void addMagicManOptions(ItemBuilder builder, Scenario scenario) {
        Option<?> option = scenario.getConfigurationOptions().get("enchantments");
        if (option == null || !(option.getValue() instanceof Map<?, ?> enchantments)) return;

        boolean headerAdded = false;

        for (Map.Entry<?, ?> entry : enchantments.entrySet()) {
            if (!(entry.getKey() instanceof String identifier)) continue;
            if (!(entry.getValue() instanceof Integer level)) continue;
            if (level == MagicManScenario.VANILLA_AMPLIFIER || level < MagicManScenario.PROHIBITED_AMPLIFIER) continue;

            if (!headerAdded) {
                builder.addFormattedLore("", "{Configuración}:");
                headerAdded = true;
            }

            builder.addFormattedLore(
                    "▶ %s: [%s]".formatted(
                            formatEnchantmentName(identifier),
                            level == MagicManScenario.PROHIBITED_AMPLIFIER ? "REMOVED" : level));
        }
    }

    private static void addConfiguredOptions(ItemBuilder builder, Scenario scenario) {
        boolean headerAdded = false;

        for (Option<?> option : scenario.getConfigurationOptions().values()) {
            if (!option.hasValue()) continue;

            if (!headerAdded) {
                builder.addFormattedLore("", "{Configuración}:");
                headerAdded = true;
            }

            builder.addFormattedLore(
                    "▶ %s: [%s]".formatted(
                            formatOptionName(option.getKey()),
                            formatValue(option.getValue())));
        }
    }

    private static String formatEnchantmentName(String identifier) {
        Optional<HardlandsEnchantment> hardlandsEnchantment = HardlandsEnchantment.fromString(identifier);

        if (hardlandsEnchantment.isPresent()) {
            return hardlandsEnchantment.get().getLabel();
        }

        int namespaceSeparator = identifier.indexOf(':');
        String name = namespaceSeparator >= 0
                ? identifier.substring(namespaceSeparator + 1)
                : identifier;

        return formatOptionName(name);
    }

    private static String formatOptionName(String key) {
        String name = key.replace('-', ' ')
                .replace('_', ' ')
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ");

        return name.isEmpty()
                ? name
                : Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String formatValue(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "Activado" : "Desactivado";
        }

        if (value instanceof Float || value instanceof Double) {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }

        if (value instanceof Map<?, ?> map) {
            return "%d configurado%s".formatted(map.size(), map.size() == 1 ? "" : "s");
        }

        return String.valueOf(value);
    }
}