package team.heather.hardlands.feature.item;

import java.util.Locale;
import java.util.Map;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.module.enchantment.HardlandsEnchantment;
import team.heather.hardlands.module.scenario.Scenario;
import team.heather.hardlands.module.scenario.ScenarioProcessor;
import team.heather.hardlands.util.text.RomanNumerals;
import team.heather.hardlands.ui.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

public final class ScenarioItemFactory {

    private ScenarioItemFactory() {}

    public static ItemStack create(Scenario scenario, boolean enabled) {
        ScenarioProcessor processor = scenario.getProcessor();

        Component name = TextFormatter.tinyCaps(scenario.getLabel())
                .color(enabled ? HardlandsColor.HARDLANDS : NamedTextColor.DARK_GRAY);

        ItemBuilder builder = new ItemBuilder(scenario.getMaterial())
                .name(name)
                .glint(enabled)
                .addFormattedLore(scenario.getDescription(), "");

        if (!processor.getConfigurationOptions().isEmpty()) {
            builder.addFormattedLore(createConfigurationLore(processor), "");
        }

        return builder
                .addFormattedLore(createControls(processor, enabled))
                .build();
    }

    private static String createControls(ScenarioProcessor processor, boolean enabled) {
        String action = enabled ? "Desactivar" : "Activar";

        return processor.getConfigurationOptions().isEmpty()
                ? "<gray>Izq. <dark_gray>↔ {%s}".formatted(action)
                : "<gray>Izq. <dark_gray>↔ {%s}  <dark_gray>│  <gray>Der. <dark_gray>✎ {Configurar}".formatted(action);
    }

    private static String createConfigurationLore(ScenarioProcessor processor) {
        StringBuilder lore = new StringBuilder("{Configuración}");

        processor.getConfigurationOptions().forEach((key, option) -> {
            lore.append("\n\n<dark_gray>◆ <gray>").append(formatName(key));
            appendValue(lore, option.getValue());
        });

        return lore.toString();
    }

    private static void appendValue(StringBuilder lore, Object value) {
        if (value == null || value instanceof Map<?, ?> map && map.isEmpty()) {
            lore.append("\n  <dark_gray>Sin configurar");
            return;
        }

        if (value instanceof Map<?, ?> map) {
            map.forEach((key, entry) -> lore
                    .append("\n  <dark_gray>• <gray>")
                    .append(formatKey(key))
                    .append(" <dark_gray>· [")
                    .append(formatValue(entry))
                    .append(']'));

            return;
        }

        lore.append("\n  <dark_gray>• [").append(value).append(']');
    }

    private static String formatKey(Object value) {
        if (value instanceof HardlandsEnchantment enchantment) return enchantment.getLabel();

        String name = String.valueOf(value);
        int separator = name.indexOf(':');

        return formatName(separator >= 0 ? name.substring(separator + 1) : name);
    }

    private static String formatValue(Object value) {
        if (!(value instanceof Integer level)) return String.valueOf(value);

        return switch (level) {
            case -1 -> "Desactivado";
            case 0 -> "0";
            default -> RomanNumerals.format(level);
        };
    }

    private static String formatName(String value) {
        StringBuilder result = new StringBuilder();

        for (String word : value
                .replace('-', ' ')
                .replace('_', ' ')
                .toLowerCase(Locale.ROOT)
                .split(" ")) {

            if (word.isEmpty()) continue;
            if (!result.isEmpty()) result.append(' ');

            result.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1));
        }

        return result.toString();
    }
}