package org.heather.hardlands.common.inventory.handler;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.inventory.InventoryDefinition;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.core.config.Option;
import org.heather.hardlands.module.scenario.Scenario;
import org.heather.hardlands.module.scenario.ScenarioDefinition;
import org.heather.hardlands.module.scenario.ScenarioManager;

public final class ScenarioInventoryHandler implements InventoryHandler {

    private static final int SEARCH_LIMIT = 1_000;

    @Override
    public void render(Inventory inventory) {
        ScenarioManager manager = getScenarioManager();
        List<Scenario> scenarios = manager.getRegisteredScenarios();
        int capacity = InventoryDefinition.getContentCapacity(inventory);

        if (scenarios.size() > capacity) {
            throw new IllegalStateException(
                    "The scenario inventory supports at most %d scenarios.".formatted(capacity));
        }

        clearContent(inventory);

        for (int index = 0; index < scenarios.size(); index++) {
            Scenario scenario = scenarios.get(index);
            String identifier = scenario.getConfigurationIdentifier();

            inventory.setItem(
                    InventoryDefinition.contentSlot(index),
                    getScenarioDefinition(identifier)
                            .createDisplayItem(manager.isScenarioEnabled(identifier)));
        }
    }

    @Override
    public Optional<Boolean> handleClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();
        ScenarioManager manager = getScenarioManager();
        List<Scenario> scenarios = manager.getRegisteredScenarios();

        int index = InventoryDefinition.getContentIndex(inventory, event.getRawSlot());

        if (index < 0 || index >= scenarios.size()) return Optional.empty();

        Scenario scenario = scenarios.get(index);
        String identifier = scenario.getConfigurationIdentifier();
        ScenarioDefinition definition = getScenarioDefinition(identifier);

        if (event.isLeftClick()) {
            boolean enabled = manager.isScenarioEnabled(identifier);

            manager.toggleScenario(identifier);

            boolean succeeded = enabled != manager.isScenarioEnabled(identifier);

            if (succeeded) {
                event.setCurrentItem(
                        definition.createDisplayItem(manager.isScenarioEnabled(identifier)));
            }

            return Optional.of(succeeded);
        }

        if (event.isRightClick()) {
            openOptions(player, definition, scenario);
            return Optional.of(true);
        }

        return Optional.of(false);
    }

    private static void openOptions(
            Player player,
            ScenarioDefinition definition,
            Scenario scenario
    ) {
        InventoryDefinition.SCENARIOS.openInventory(
                player,
                new OptionsHandler(definition, scenario),
                InventoryDefinition.getSizeForContent(
                        scenario.getConfigurationOptions().size()),
                Component.text(definition.getName() + " - Opciones"));
    }

    private static void openEnchantments(
            Player player,
            ScenarioDefinition definition,
            Scenario scenario,
            Option<Map<Enchantment, Integer>> option
    ) {
        InventoryDefinition.SCENARIOS.openInventory(
                player,
                new EnchantmentMapHandler(definition, scenario, option),
                InventoryDefinition.MAX_SIZE,
                Component.text(definition.getName() + " - Encantamientos"));
    }

    private static void clearContent(Inventory inventory) {
        int capacity = InventoryDefinition.getContentCapacity(inventory);

        for (int index = 0; index < capacity; index++) {
            inventory.clear(InventoryDefinition.contentSlot(index));
        }
    }

    private static ScenarioManager getScenarioManager() {
        return Hardlands.getInstance().getScenarioManager();
    }

    private static ScenarioDefinition getScenarioDefinition(String identifier) {
        return ScenarioDefinition.findByIdentifier(identifier)
                .orElseThrow(() ->
                        new IllegalStateException("Missing ScenarioDefinition: " + identifier));
    }

    private static ItemStack createOptionItem(Option<?> option) {
        Object value = option.getValue();

        ItemBuilder builder = new ItemBuilder(getMaterial(option))
                .name("<yellow>" + formatName(option.getKey()))
                .lore("<gray>Valor: <white>" + formatValue(value));

        if (isEnchantmentMap(option)) {
            return builder
                    .addLore(
                            "",
                            "<yellow>Click <gray>para editar.")
                    .build();
        }

        if (option.getDataType() == Boolean.class) {
            return builder
                    .glint(Boolean.TRUE.equals(value))
                    .addLore(
                            "",
                            "<yellow>Click <gray>para alternar.")
                    .build();
        }

        if (isNumber(option)) {
            return builder
                    .addLore(
                            "",
                            "<yellow>Click izquierdo <gray>para aumentar.",
                            "<yellow>Click derecho <gray>para reducir.",
                            "<yellow>Shift + Click <gray>para cambiar más rápido.")
                    .build();
        }

        return builder
                .addLore(
                        "",
                        "<red>Este tipo todavía no tiene editor.")
                .build();
    }

    @SuppressWarnings("unchecked")
    private static boolean editOption(Option<?> option, InventoryClickEvent event) {
        Type type = option.getDataType();

        if (type == Boolean.class) return toggleBoolean((Option<Boolean>) option, event);
        if (type == Integer.class) return adjustInteger((Option<Integer>) option, event);
        if (type == Long.class) return adjustLong((Option<Long>) option, event);
        if (type == Float.class) return adjustFloat((Option<Float>) option, event);
        if (type == Double.class) return adjustDouble((Option<Double>) option, event);

        return false;
    }

    private static boolean toggleBoolean(
            Option<Boolean> option,
            InventoryClickEvent event
    ) {
        if (getDirection(event) == 0) return false;

        return setIfValid(option, !Boolean.TRUE.equals(option.getValue()));
    }

    private static boolean adjustInteger(
            Option<Integer> option,
            InventoryClickEvent event
    ) {
        int direction = getDirection(event);

        if (direction == 0) return false;

        int step = event.isShiftClick() ? 10 : 1;
        Integer current = option.getValue();

        if (current != null) return setIfValid(option, current + direction * step);

        return findInitialValue(
                option,
                index -> direction * step * index,
                0,
                direction < 0);
    }

    private static boolean adjustLong(
            Option<Long> option,
            InventoryClickEvent event
    ) {
        int direction = getDirection(event);

        if (direction == 0) return false;

        long step = event.isShiftClick() ? 10L : 1L;
        Long current = option.getValue();

        if (current != null) return setIfValid(option, current + direction * step);

        return findInitialValue(
                option,
                index -> direction * step * index,
                0L,
                direction < 0);
    }

    private static boolean adjustFloat(
            Option<Float> option,
            InventoryClickEvent event
    ) {
        int direction = getDirection(event);

        if (direction == 0) return false;

        float step = event.isShiftClick() ? 0.1F : 0.01F;
        Float current = option.getValue();

        if (current != null) return setIfValid(option, round(current + direction * step));

        return findInitialValue(
                option,
                index -> round(direction * step * index),
                0.0F,
                direction < 0);
    }

    private static boolean adjustDouble(
            Option<Double> option,
            InventoryClickEvent event
    ) {
        int direction = getDirection(event);

        if (direction == 0) return false;

        double step = event.isShiftClick() ? 0.1D : 0.01D;
        Double current = option.getValue();

        if (current != null) return setIfValid(option, round(current + direction * step));

        return findInitialValue(
                option,
                index -> round(direction * step * index),
                0.0D,
                direction < 0);
    }

    private static <T> boolean findInitialValue(
            Option<T> option,
            IntFunction<T> factory,
            T zero,
            boolean zeroFirst
    ) {
        if (zeroFirst && setIfValid(option, zero)) return true;

        for (int index = 1; index <= SEARCH_LIMIT; index++) {
            if (setIfValid(option, factory.apply(index))) return true;
        }

        return setIfValid(option, zero);
    }

    private static <T> boolean setIfValid(Option<T> option, T value) {
        if (!option.getPredicate().test(value)) return false;

        option.setValue(value);
        return true;
    }

    private static int getDirection(InventoryClickEvent event) {
        if (event.isLeftClick()) return 1;
        if (event.isRightClick()) return -1;

        return 0;
    }

    private static boolean isNumber(Option<?> option) {
        Type type = option.getDataType();

        return type == Integer.class
                || type == Long.class
                || type == Float.class
                || type == Double.class;
    }

    private static boolean isEnchantmentMap(Option<?> option) {
        if (!(option.getDataType() instanceof ParameterizedType type)) return false;

        Type[] arguments = type.getActualTypeArguments();

        return type.getRawType() == Map.class
                && arguments.length == 2
                && arguments[0] == Enchantment.class
                && arguments[1] == Integer.class;
    }

    @SuppressWarnings("unchecked")
    private static Option<Map<Enchantment, Integer>> asEnchantmentMap(Option<?> option) {
        return (Option<Map<Enchantment, Integer>>) option;
    }

    private static Material getMaterial(Option<?> option) {
        if (option.getDataType() == Boolean.class) return Material.LEVER;
        if (isNumber(option)) return Material.COMPARATOR;
        if (isEnchantmentMap(option)) return Material.ENCHANTED_BOOK;

        return Material.PAPER;
    }

    private static String formatValue(Object value) {
        if (value == null) return "<red>No configurado";
        if (value instanceof Boolean booleanValue) {
            return booleanValue ? "<green>Activado" : "<red>Desactivado";
        }
        if (value instanceof Float floatValue) return formatDecimal(Float.toString(floatValue));
        if (value instanceof Double doubleValue) return formatDecimal(Double.toString(doubleValue));
        if (value instanceof Map<?, ?> map) return "%d entradas".formatted(map.size());

        return String.valueOf(value);
    }

    private static String formatDecimal(String value) {
        return new BigDecimal(value)
                .stripTrailingZeros()
                .toPlainString();
    }

    private static String formatName(String value) {
        String formatted = value
                .replace('_', ' ')
                .replaceAll("(?<=[a-z0-9])(?=[A-Z])", " ")
                .toLowerCase(Locale.ROOT);

        if (formatted.isEmpty()) return value;

        return Character.toUpperCase(formatted.charAt(0)) + formatted.substring(1);
    }

    private static float round(float value) {
        return Math.round(value * 10_000.0F) / 10_000.0F;
    }

    private static double round(double value) {
        return Math.round(value * 10_000.0D) / 10_000.0D;
    }

    private static int getPreviousSlot(Inventory inventory) {
        return InventoryDefinition.slot(InventoryDefinition.getBottomRow(inventory), 4);
    }

    private static int getNextSlot(Inventory inventory) {
        return InventoryDefinition.slot(InventoryDefinition.getBottomRow(inventory), 6);
    }

    private static int getLastPage(int itemCount, int pageSize) {
        return Math.max(0, (itemCount - 1) / pageSize);
    }

    private static List<Enchantment> getEnchantments() {
        Registry<Enchantment> registry = RegistryAccess.registryAccess()
                .getRegistry(RegistryKey.ENCHANTMENT);

        List<Enchantment> enchantments = new ArrayList<>();
        registry.forEach(enchantments::add);
        enchantments.sort(Comparator.comparing(enchantment -> enchantment.getKey().toString()));

        return enchantments;
    }

    private static final class OptionsHandler implements InventoryHandler {

        private final ScenarioDefinition definition;
        private final Scenario scenario;

        private OptionsHandler(
                ScenarioDefinition definition,
                Scenario scenario
        ) {
            this.definition = definition;
            this.scenario = scenario;
        }

        @Override
        public void render(Inventory inventory) {
            List<Option<?>> options = this.getOptions();
            int capacity = InventoryDefinition.getContentCapacity(inventory);

            if (options.size() > capacity) {
                throw new IllegalStateException(
                        "Scenario supports at most %d options.".formatted(capacity));
            }

            clearContent(inventory);

            for (int index = 0; index < options.size(); index++) {
                inventory.setItem(
                        InventoryDefinition.contentSlot(index),
                        createOptionItem(options.get(index)));
            }
        }

        @Override
        public Optional<Boolean> handleClick(InventoryClickEvent event, Player player) {
            Inventory inventory = event.getView().getTopInventory();

            if (event.getRawSlot() == getPreviousSlot(inventory)) {
                InventoryDefinition.SCENARIOS.openInventory(player);
                return Optional.of(true);
            }

            List<Option<?>> options = this.getOptions();
            int index = InventoryDefinition.getContentIndex(inventory, event.getRawSlot());

            if (index < 0 || index >= options.size()) return Optional.empty();

            Option<?> option = options.get(index);

            if (isEnchantmentMap(option)) {
                openEnchantments(
                        player,
                        this.definition,
                        this.scenario,
                        asEnchantmentMap(option));

                return Optional.of(true);
            }

            boolean succeeded = editOption(option, event);

            if (succeeded) event.setCurrentItem(createOptionItem(option));

            return Optional.of(succeeded);
        }

        private List<Option<?>> getOptions() {
            return List.copyOf(this.scenario.getConfigurationOptions().values());
        }
    }

    private static final class EnchantmentMapHandler implements InventoryHandler {

        private final ScenarioDefinition definition;
        private final Scenario scenario;
        private final Option<Map<Enchantment, Integer>> option;
        private final List<Enchantment> enchantments = getEnchantments();

        private int page;

        private EnchantmentMapHandler(
                ScenarioDefinition definition,
                Scenario scenario,
                Option<Map<Enchantment, Integer>> option
        ) {
            this.definition = definition;
            this.scenario = scenario;
            this.option = option;
        }

        @Override
        public void render(Inventory inventory) {
            int capacity = InventoryDefinition.getContentCapacity(inventory);
            int lastPage = getLastPage(this.enchantments.size(), capacity);

            this.page = Math.min(this.page, lastPage);

            clearContent(inventory);

            int start = this.page * capacity;

            for (int index = 0;
                 index < capacity && start + index < this.enchantments.size();
                 index++) {
                inventory.setItem(
                        InventoryDefinition.contentSlot(index),
                        this.createEnchantmentItem(this.enchantments.get(start + index)));
            }
        }

        @Override
        public Optional<Boolean> handleClick(InventoryClickEvent event, Player player) {
            Inventory inventory = event.getView().getTopInventory();
            int capacity = InventoryDefinition.getContentCapacity(inventory);

            if (event.getRawSlot() == getPreviousSlot(inventory)) {
                if (this.page > 0) {
                    this.page--;
                    this.render(inventory);
                } else {
                    openOptions(player, this.definition, this.scenario);
                }

                return Optional.of(true);
            }

            if (event.getRawSlot() == getNextSlot(inventory)) {
                if (this.page >= getLastPage(this.enchantments.size(), capacity)) {
                    return Optional.of(false);
                }

                this.page++;
                this.render(inventory);

                return Optional.of(true);
            }

            int index = InventoryDefinition.getContentIndex(inventory, event.getRawSlot());

            if (index < 0) return Optional.empty();

            int enchantmentIndex = this.page * capacity + index;

            if (enchantmentIndex >= this.enchantments.size()) return Optional.empty();
            if (!event.isLeftClick() && !event.isRightClick()) return Optional.of(false);

            Enchantment enchantment = this.enchantments.get(enchantmentIndex);
            Map<Enchantment, Integer> values = this.option.hasValue()
                    ? new LinkedHashMap<>(this.option.getValue())
                    : new LinkedHashMap<>();

            int currentLevel = values.getOrDefault(enchantment, 0);
            int step = event.isShiftClick() ? 5 : 1;
            int direction = event.isLeftClick() ? 1 : -1;
            int level = Math.clamp(currentLevel + direction * step, 0, 255);

            if (level == currentLevel) return Optional.of(false);

            if (level == 0) values.remove(enchantment);
            else values.put(enchantment, level);

            if (!this.option.getPredicate().test(values)) return Optional.of(false);

            this.option.setValue(values);
            event.setCurrentItem(this.createEnchantmentItem(enchantment));

            return Optional.of(true);
        }

        private ItemStack createEnchantmentItem(Enchantment enchantment) {
            int level = this.option.hasValue()
                    ? this.option.getValue().getOrDefault(enchantment, 0)
                    : 0;

            return new ItemBuilder(Material.ENCHANTED_BOOK)
                    .name("<yellow>" + formatName(enchantment.getKey().getKey()))
                    .glint(level > 0)
                    .lore(
                            "<gray>Nivel: <white>" + level,
                            "",
                            "<yellow>Click izquierdo <gray>+1",
                            "<yellow>Click derecho <gray>-1",
                            "<yellow>Shift + Click <gray>×5")
                    .build();
        }
    }
}