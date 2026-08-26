package org.heather.hardlands.config.inventory.handler;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.config.Option;
import org.heather.hardlands.config.Validator;
import org.heather.hardlands.config.inventory.HardlandsInventory;
import org.heather.hardlands.module.scenario.Scenario;
import org.heather.hardlands.module.scenario.ScenarioDefinition;
import org.heather.hardlands.module.scenario.ScenarioManager;

public final class ScenarioInventoryHandler implements InventoryHandler {

    private static final int INPUT_WIDTH = 300;
    private static final int ENCHANTMENT_INPUT_WIDTH = 400;

    private static final String DEFAULT = "default";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String INPUT_PREFIX = "input";

    @Override
    public void render(Inventory inventory) {
        ScenarioManager manager = getScenarioManager();
        List<Scenario> scenarios = manager.getRegisteredScenarios();
        int capacity = HardlandsInventory.getContentCapacity(inventory);

        if (scenarios.size() > capacity) {
            throw new IllegalStateException("The scenario inventory supports at most %d scenarios.".formatted(capacity));
        }

        clearContent(inventory);

        for (int index = 0; index < scenarios.size(); index++) {
            Scenario scenario = scenarios.get(index);
            String identifier = scenario.getConfigurationIdentifier();
            ScenarioDefinition definition = getScenarioDefinition(identifier);

            inventory.setItem(
                    HardlandsInventory.contentSlot(index),
                    definition.createDisplayItem(scenario, manager.isScenarioEnabled(identifier)));
        }
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();
        ScenarioManager manager = getScenarioManager();
        List<Scenario> scenarios = manager.getRegisteredScenarios();
        int index = HardlandsInventory.getContentIndex(inventory, event.getRawSlot());

        if (index < 0 || index >= scenarios.size()) return Optional.empty();

        Scenario scenario = scenarios.get(index);
        String identifier = scenario.getConfigurationIdentifier();
        ScenarioDefinition definition = getScenarioDefinition(identifier);

        if (event.isLeftClick()) return Optional.of(toggleScenario(event, manager, definition, scenario, identifier));

        if (event.isRightClick()) {
            if (definition == ScenarioDefinition.MAGIC_MAN) {
                HardlandsInventory.MAGIC_MAN.openInventory(player);
                return Optional.of(true);
            }

            if (scenario.getConfigurationOptions().isEmpty()) return Optional.of(false);

            this.showOptionsDialog(player, inventory, definition, scenario);
            return Optional.of(true);
        }

        return Optional.of(false);
    }

    private static boolean toggleScenario(
            InventoryClickEvent event,
            ScenarioManager manager,
            ScenarioDefinition definition,
            Scenario scenario,
            String identifier
    ) {
        if (!manager.toggleScenario(identifier)) return false;

        event.setCurrentItem(definition.createDisplayItem(scenario, manager.isScenarioEnabled(identifier)));
        return true;
    }

    private void showOptionsDialog(Player player, Inventory inventory, ScenarioDefinition definition, Scenario scenario) {
        List<OptionBinding> bindings = createBindings(scenario);

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text("Configuración · " + definition.getName()))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(createBody(scenario))
                        .inputs(createInputs(bindings))
                        .build())
                .type(DialogType.confirmation(
                        createSaveButton(inventory, definition, scenario, bindings),
                        createCancelButton()))));
    }

    private ActionButton createSaveButton(
            Inventory inventory,
            ScenarioDefinition definition,
            Scenario scenario,
            List<OptionBinding> bindings
    ) {
        return ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarde la configuración del escenario."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player player) {
                                this.saveOptions(player, inventory, definition, scenario, bindings, response);
                            }
                        },
                        ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                Component.text("Cancelar"),
                Component.text("Cierre sin guardar los cambios."),
                100,
                DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()));
    }

    private void saveOptions(
            Player player,
            Inventory inventory,
            ScenarioDefinition definition,
            Scenario scenario,
            List<OptionBinding> bindings,
            DialogResponseView response
    ) {
        Map<Option<?>, Object> values;

        try {
            values = readValues(bindings, response);
        } catch (IllegalArgumentException exception) {
            player.sendRichMessage("<red>" + exception.getMessage());
            return;
        }

        values.forEach(ScenarioInventoryHandler::setValue);

        ScenarioManager manager = getScenarioManager();
        String identifier = scenario.getConfigurationIdentifier();
        boolean disabled = manager.isScenarioEnabled(identifier) && !scenario.isConfigurationValid();

        if (disabled) manager.disableScenario(identifier);

        this.render(inventory);
        player.closeDialog();

        if (disabled) {
            player.sendRichMessage(
                    "<yellow>Configuración guardada. El escenario se desactivó porque su configuración no es válida.");
            return;
        }

        player.sendRichMessage("<green>Configuración de <white>%s</white> guardada.".formatted(definition.getName()));
    }

    private static List<OptionBinding> createBindings(Scenario scenario) {
        List<OptionBinding> bindings = new ArrayList<>();
        int index = 0;

        for (Option<?> option : scenario.getConfigurationOptions().values()) {
            bindings.add(new OptionBinding(option, INPUT_PREFIX + index++));
        }

        return bindings;
    }

    private static List<DialogInput> createInputs(List<OptionBinding> bindings) {
        List<DialogInput> inputs = new ArrayList<>(bindings.size());

        for (OptionBinding binding : bindings) inputs.add(createInput(binding));

        return inputs;
    }

    private static DialogInput createInput(OptionBinding binding) {
        Option<?> option = binding.option();
        Component label = Component.text(formatLabel(option));
        Type type = option.getDataType();

        if (type == Boolean.class) return createBooleanInput(binding, label);
        if (isEnchantmentMap(option)) return createEnchantmentInput(binding, label);
        if (isNumber(type) || type == String.class) return createTextInput(binding, label);

        throw new IllegalStateException("Unsupported dialog option enchantment: " + type.getTypeName());
    }

    private static DialogInput createBooleanInput(OptionBinding binding, Component label) {
        Object value = binding.option().getValue();

        return DialogInput.singleOption(
                binding.inputKey(),
                INPUT_WIDTH,
                List.of(
                        SingleOptionDialogInput.OptionEntry.create(DEFAULT, Component.text("Sin configurar"), value == null),
                        SingleOptionDialogInput.OptionEntry.create(TRUE, Component.text("Activado"), Boolean.TRUE.equals(value)),
                        SingleOptionDialogInput.OptionEntry.create(FALSE, Component.text("Desactivado"), Boolean.FALSE.equals(value))),
                label,
                true);
    }

    private static DialogInput createTextInput(OptionBinding binding, Component label) {
        Option<?> option = binding.option();
        String initial = option.hasValue() ? formatValue(option.getValue()) : "";

        return DialogInput.text(binding.inputKey(), INPUT_WIDTH, label, true, initial, 64, null);
    }

    private static DialogInput createEnchantmentInput(OptionBinding binding, Component label) {
        return DialogInput.text(
                binding.inputKey(),
                ENCHANTMENT_INPUT_WIDTH,
                label,
                true,
                formatEnchantmentMap(asEnchantmentMap(binding.option())),
                4096,
                TextDialogInput.MultilineOptions.create(64, 120));
    }

    private static List<DialogBody> createBody(Scenario scenario) {
        List<DialogBody> body = new ArrayList<>();

        body.add(DialogBody.plainMessage(Component.text("Deje un campo vacío para mantenerlo sin configurar.")));

        if (hasEnchantmentMap(scenario)) {
            body.add(DialogBody.plainMessage(
                    Component.text("Encantamientos: introduzca una entrada por línea. Ejemplo: minecraft:sharpness=5")));
        }

        return body;
    }

    private static Map<Option<?>, Object> readValues(List<OptionBinding> bindings, DialogResponseView response) {
        Map<Option<?>, Object> values = new LinkedHashMap<>();

        for (OptionBinding binding : bindings) {
            Option<?> option = binding.option();
            Object value = readValue(binding, response);

            if (value != null && !isValid(option, value)) throw new IllegalArgumentException(getValidationError(option));

            values.put(option, value);
        }

        return values;
    }

    private static Object readValue(OptionBinding binding, DialogResponseView response) {
        Option<?> option = binding.option();
        Type type = option.getDataType();

        if (type == Boolean.class) return readBoolean(binding, response);

        String input = response.getText(binding.inputKey());

        if (input == null || input.isBlank()) return null;
        if (isEnchantmentMap(option)) return parseEnchantmentMap(option, input);

        return parseValue(option, type, input.strip());
    }

    private static Object parseValue(Option<?> option, Type type, String value) {
        try {
            if (type == Integer.class) return Integer.valueOf(value);
            if (type == Long.class) return Long.valueOf(value);
            if (type == Float.class) return parseFloat(option, value);
            if (type == Double.class) return parseDouble(option, value);
            if (type == String.class) return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("\"%s\" debe ser un número válido.".formatted(option.getKey()));
        }

        throw new IllegalStateException("Unsupported dialog option enchantment: " + type.getTypeName());
    }

    private static Boolean readBoolean(OptionBinding binding, DialogResponseView response) {
        String value = response.getText(binding.inputKey());

        if (value == null || value.equals(DEFAULT)) return null;
        if (value.equals(TRUE)) return true;
        if (value.equals(FALSE)) return false;

        throw new IllegalArgumentException("Valor inválido para \"%s\".".formatted(binding.option().getKey()));
    }

    private static Float parseFloat(Option<?> option, String value) {
        float result = Float.parseFloat(value);

        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("\"%s\" debe ser un número finito.".formatted(option.getKey()));
        }

        return result;
    }

    private static Double parseDouble(Option<?> option, String value) {
        double result = Double.parseDouble(value);

        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("\"%s\" debe ser un número finito.".formatted(option.getKey()));
        }

        return result;
    }

    private static Map<Enchantment, Integer> parseEnchantmentMap(Option<?> option, String input) {
        Registry<Enchantment> registry = getEnchantmentRegistry();
        Map<Enchantment, Integer> values = new LinkedHashMap<>();

        for (String rawEntry : input.split("\\R|,")) {
            String entry = rawEntry.strip();

            if (entry.isEmpty()) continue;

            String[] parts = entry.split("=", 2);

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Formato inválido para \"%s\". Use enchantment=nivel.".formatted(option.getKey()));
            }

            String identifier = normalizeEnchantmentIdentifier(parts[0]);
            NamespacedKey key = NamespacedKey.fromString(identifier);

            if (key == null) throw new IllegalArgumentException("Encantamiento inválido: \"%s\".".formatted(identifier));

            Enchantment enchantment = registry.get(key);

            if (enchantment == null) {
                throw new IllegalArgumentException("No existe el encantamiento \"%s\".".formatted(identifier));
            }

            int level = parseEnchantmentLevel(identifier, parts[1]);

            if (values.putIfAbsent(enchantment, level) != null) {
                throw new IllegalArgumentException("El encantamiento \"%s\" está repetido.".formatted(identifier));
            }
        }

        return values;
    }

    private static String normalizeEnchantmentIdentifier(String input) {
        String identifier = input.strip();
        return identifier.contains(":") ? identifier : "minecraft:" + identifier;
    }

    private static int parseEnchantmentLevel(String identifier, String input) {
        int level;

        try {
            level = Integer.parseInt(input.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Nivel inválido para \"%s\".".formatted(identifier));
        }

        if (level < 1 || level > 255) {
            throw new IllegalArgumentException("El nivel de \"%s\" debe estar entre 1 y 255.".formatted(identifier));
        }

        return level;
    }

    private static String formatEnchantmentMap(Option<Map<Enchantment, Integer>> option) {
        if (!option.hasValue()) return "";

        StringBuilder result = new StringBuilder();

        for (Map.Entry<Enchantment, Integer> entry : option.getValue().entrySet()) {
            if (!result.isEmpty()) result.append('\n');
            result.append(entry.getKey().getKey()).append('=').append(entry.getValue());
        }

        return result.toString();
    }

    private static String formatLabel(Option<?> option) {
        String constraint = getConstraint(option);
        return constraint == null ? option.getKey() : "%s (%s)".formatted(option.getKey(), constraint);
    }

    private static String getValidationError(Option<?> option) {
        String constraint = getConstraint(option);

        return constraint == null
                ? "Valor inválido para \"%s\".".formatted(option.getKey())
                : "\"%s\" debe ser %s.".formatted(option.getKey(), constraint);
    }

    private static String getConstraint(Option<?> option) {
        if (!(option.getPredicate() instanceof Validator<?> validator)) return null;

        String key = validator.key();

        if (key.equals(Validator.Keys.UNIT_INTERVAL)) return "entre 0 y 1";
        if (key.equals(Validator.Keys.POSITIVE)) return "mayor que 0";
        if (key.equals(Validator.Keys.NON_NEGATIVE)) return "0 o mayor";
        if (key.equals(Validator.Keys.NEGATIVE)) return "menor que 0";
        if (key.equals(Validator.Keys.NON_POSITIVE)) return "0 o menor";
        if (key.startsWith(Validator.Keys.AT_LEAST + ":")) return "mínimo " + getValidatorArguments(key)[0];
        if (key.startsWith(Validator.Keys.AT_MOST + ":")) return "máximo " + getValidatorArguments(key)[0];

        if (key.startsWith(Validator.Keys.BETWEEN + ":")) {
            String[] arguments = getValidatorArguments(key);

            if (arguments.length == 2) return "entre %s y %s".formatted(arguments[0], arguments[1]);
        }

        return null;
    }

    private static String[] getValidatorArguments(String key) {
        String[] parts = key.split(":");
        return parts.length <= 1 ? new String[0] : Arrays.copyOfRange(parts, 1, parts.length);
    }

    private static String formatValue(Object value) {
        if (value instanceof Float || value instanceof Double) {
            return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
        }

        return String.valueOf(value);
    }

    private static boolean hasEnchantmentMap(Scenario scenario) {
        for (Option<?> option : scenario.getConfigurationOptions().values()) {
            if (isEnchantmentMap(option)) return true;
        }

        return false;
    }

    private static boolean isNumber(Type type) {
        return type == Integer.class || type == Long.class || type == Float.class || type == Double.class;
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

    @SuppressWarnings("unchecked")
    private static boolean isValid(Option<?> option, Object value) {
        return ((Option<Object>) option).getPredicate().test(value);
    }

    @SuppressWarnings("unchecked")
    private static void setValue(Option<?> option, Object value) {
        ((Option<Object>) option).setValue(value);
    }

    private static Registry<Enchantment> getEnchantmentRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    }

    private static void clearContent(Inventory inventory) {
        int capacity = HardlandsInventory.getContentCapacity(inventory);

        for (int index = 0; index < capacity; index++) {
            inventory.clear(HardlandsInventory.contentSlot(index));
        }
    }

    private static ScenarioManager getScenarioManager() {
        return Hardlands.getInstance().getScenarioManager();
    }

    private static ScenarioDefinition getScenarioDefinition(String identifier) {
        return ScenarioDefinition.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Missing ScenarioDefinition: " + identifier));
    }

    private record OptionBinding(Option<?> option, String inputKey) {}
}