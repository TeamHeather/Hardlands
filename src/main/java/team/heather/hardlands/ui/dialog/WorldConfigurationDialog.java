package team.heather.hardlands.ui.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.ui.inventory.HardlandsInventory;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.ui.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

@SuppressWarnings("UnstableApiUsage")
public final class WorldConfigurationDialog {

    private static final int INPUT_WIDTH = 320;
    private static final int ACTION_WIDTH = 120;

    private static final float SURVIVAL_MIN_SIZE = 500F;
    private static final float SURVIVAL_MAX_SIZE = 5000F;
    private static final float MEETUP_MIN_SIZE = 200F;
    private static final float MEETUP_MAX_SIZE = 500F;
    private static final float DEATHMATCH_MIN_SIZE = 0F;
    private static final float DEATHMATCH_MAX_SIZE = 200F;
    private static final float BORDER_SIZE_STEP = 50F;

    private static final String DEFAULT = "default";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    private static final String WORLD_PREFIX = "world_";

    private static final String SURVIVAL_SIZE = "survivalSize";
    private static final String MEETUP_SIZE = "meetupSize";
    private static final String DEATHMATCH_SIZE = "deathmatchSize";
    private static final String SURFACE_TELEPORT = "surfaceTeleport";
    private static final String BORDER_DAMAGE = "borderDamage";
    private static final String CENTER_X = "centerX";
    private static final String CENTER_Z = "centerZ";

    private WorldConfigurationDialog() {}

    public static void show(Player player) {
        WorldManager manager = getWorldManager();
        List<World> worlds = Bukkit.getWorlds();

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(TextFormatter.tinyCaps("Configuración del mundo"))
                        .externalTitle(Component.text("Mundo"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .inputs(createInputs(manager, worlds))
                        .build())
                .type(DialogType.confirmation(createSaveButton(manager, worlds), createCancelButton()))));
    }

    private static List<DialogInput> createInputs(WorldManager manager, List<World> worlds) {
        List<DialogInput> inputs = new ArrayList<>(worlds.size() + 7);

        addWorldInputs(inputs, manager, worlds);

        inputs.add(booleanInput(
                SURFACE_TELEPORT,
                "↑",
                "Teletransportar a la superficie",
                manager.getSurfaceTeleportOption().getValue()));

        inputs.add(booleanInput(
                BORDER_DAMAGE,
                "☠",
                "Daño fuera del World Border",
                manager.getBorderDamageOption().getValue()));

        inputs.add(borderSizeInput(
                SURVIVAL_SIZE,
                "↔",
                "World Border de Supervivencia",
                manager.getSurvivalSizeOption().getValue(),
                SURVIVAL_MIN_SIZE,
                SURVIVAL_MAX_SIZE));

        inputs.add(borderSizeInput(
                MEETUP_SIZE,
                "↔",
                "World Border de Meetup",
                manager.getMeetupSizeOption().getValue(),
                MEETUP_MIN_SIZE,
                MEETUP_MAX_SIZE));

        inputs.add(borderSizeInput(
                DEATHMATCH_SIZE,
                "↔",
                "World Border de Deathmatch",
                manager.getDeathmatchSizeOption().getValue(),
                DEATHMATCH_MIN_SIZE,
                DEATHMATCH_MAX_SIZE));

        inputs.add(numberInput(
                CENTER_X,
                "Coordenada X del centro de los World Borders",
                manager.getCenterXOption().getValue()));

        inputs.add(numberInput(
                CENTER_Z,
                "Coordenada Z del centro de los World Borders",
                manager.getCenterZOption().getValue()));

        return inputs;
    }

    private static void addWorldInputs(List<DialogInput> inputs, WorldManager manager, List<World> worlds) {
        Option<Set<String>> option = manager.getEnabledWorldsOption();
        Set<String> enabledWorlds = option.hasValue() ? option.getValue() : Set.of();

        for (int index = 0; index < worlds.size(); index++) {
            World world = worlds.get(index);

            if (world.getEnvironment() == World.Environment.NORMAL) {
                continue;
            }

            inputs.add(DialogInput.bool(worldKey(index), worldLabel(world))
                    .initial(enabledWorlds.contains(world.getName()))
                    .build());
        }
    }

    private static DialogInput requiredWorldInput(int index, World world) {
        return DialogInput.singleOption(
                worldKey(index),
                INPUT_WIDTH,
                List.of(stateOption(TRUE, "Activado", NamedTextColor.GREEN, true)),
                worldLabel(world),
                true);
    }

    private static Component worldLabel(World world) {
        String name = switch (world.getEnvironment()) {
            case NORMAL -> "Overworld";
            case NETHER -> "The Nether";
            case THE_END -> "The End";
            default -> world.getName();
        };

        return TextFormatter.tinyCaps(name).color(NamedTextColor.WHITE);
    }

    private static DialogInput booleanInput(String key, String icon, String name, Boolean value) {
        return DialogInput.singleOption(
                key,
                INPUT_WIDTH,
                List.of(
                        stateOption(DEFAULT, "Sin configurar", NamedTextColor.DARK_GRAY, value == null),
                        stateOption(TRUE, "Activado", NamedTextColor.GREEN, Boolean.TRUE.equals(value)),
                        stateOption(FALSE, "Desactivado", NamedTextColor.RED, Boolean.FALSE.equals(value))),
                iconLabel(icon, name),
                true);
    }

    private static DialogInput borderSizeInput(
            String key, String icon, String name, Integer value, float min, float max) {

        Float initial = value == null ? null : Math.clamp(value.floatValue(), min, max);

        return DialogInput.numberRange(
                key,
                INPUT_WIDTH,
                iconLabel(icon, name),
                "%1$s: %2$s × %2$s",
                min,
                max,
                initial,
                BORDER_SIZE_STEP);
    }

    private static DialogInput numberInput(String key, String name, Double value) {
        return DialogInput.text(key, plainLabel(name))
                .width(INPUT_WIDTH)
                .initial(value == null ? "" : formatNumber(value))
                .maxLength(32)
                .build();
    }

    private static SingleOptionDialogInput.OptionEntry stateOption(
            String key, String name, TextColor color, boolean selected) {

        return SingleOptionDialogInput.OptionEntry.create(key, TextFormatter.tinyCaps(name).color(color), selected);
    }

    private static Component iconLabel(String icon, String name) {
        return Component.text(icon + "  ", HardlandsColor.HARDLANDS)
                .append(TextFormatter.tinyCaps(name).color(NamedTextColor.WHITE));
    }

    private static Component plainLabel(String name) {
        return TextFormatter.tinyCaps(name).color(NamedTextColor.WHITE);
    }

    private static ActionButton createSaveButton(WorldManager manager, List<World> worlds) {
        return ActionButton.create(
                TextFormatter.tinyCaps("Guardar").color(HardlandsColor.HARDLANDS),
                Component.text("Aplicar los cambios.", HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player player)) return;

                            try {
                                WorldSettings settings = readSettings(worlds, response);
                                validateSettings(manager, settings);
                                applySettings(manager, settings);
                            } catch (IllegalArgumentException exception) {
                                player.sendRichMessage("<red>" + exception.getMessage());
                                return;
                            }

                            HardlandsInventory.refreshPreparationItems();
                            player.closeDialog();
                            player.sendRichMessage("<green>Configuración del mundo guardada.");
                        },
                        ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                TextFormatter.tinyCaps("Cancelar").color(NamedTextColor.GRAY),
                Component.text("Cerrar sin guardar.", HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()));
    }

    private static WorldSettings readSettings(List<World> worlds, DialogResponseView response) {
        return new WorldSettings(
                readEnabledWorlds(worlds, response),
                readBorderSize(response, SURVIVAL_SIZE),
                readBorderSize(response, MEETUP_SIZE),
                readBorderSize(response, DEATHMATCH_SIZE),
                readBoolean(response, SURFACE_TELEPORT),
                readBoolean(response, BORDER_DAMAGE),
                parseDouble(response.getText(CENTER_X), "La coordenada X del centro de los World Borders"),
                parseDouble(response.getText(CENTER_Z), "La coordenada Z del centro de los World Borders"));
    }

    private static void validateSettings(WorldManager manager, WorldSettings settings) {
        validate(manager.getEnabledWorldsOption(), settings.enabledWorlds(), "La selección de mundos no es válida.");

        validateBorderSize(
                manager.getSurvivalSizeOption(),
                settings.survivalSize(),
                "El World Border de Supervivencia",
                SURVIVAL_MIN_SIZE,
                SURVIVAL_MAX_SIZE);

        validateBorderSize(
                manager.getMeetupSizeOption(),
                settings.meetupSize(),
                "El World Border de Meetup",
                MEETUP_MIN_SIZE,
                MEETUP_MAX_SIZE);

        validateBorderSize(
                manager.getDeathmatchSizeOption(),
                settings.deathmatchSize(),
                "El World Border de Deathmatch",
                DEATHMATCH_MIN_SIZE,
                DEATHMATCH_MAX_SIZE);

        validate(
                manager.getSurfaceTeleportOption(),
                settings.surfaceTeleport(),
                "La opción de teletransporte a la superficie no es válida.");

        validate(
                manager.getBorderDamageOption(),
                settings.borderDamage(),
                "La opción de daño fuera del World Border no es válida.");

        validate(manager.getCenterXOption(), settings.centerX(), "La coordenada X del centro no es válida.");
        validate(manager.getCenterZOption(), settings.centerZ(), "La coordenada Z del centro no es válida.");

        validateBorderSizes(settings.survivalSize(), settings.meetupSize(), settings.deathmatchSize());
    }

    private static void applySettings(WorldManager manager, WorldSettings settings) {
        manager.getEnabledWorldsOption().setValue(settings.enabledWorlds());

        manager.getSurfaceTeleportOption().setValue(settings.surfaceTeleport());
        manager.getBorderDamageOption().setValue(settings.borderDamage());

        manager.getSurvivalSizeOption().setValue(settings.survivalSize());
        manager.getMeetupSizeOption().setValue(settings.meetupSize());
        manager.getDeathmatchSizeOption().setValue(settings.deathmatchSize());

        manager.getCenterXOption().setValue(settings.centerX());
        manager.getCenterZOption().setValue(settings.centerZ());
    }

    private static Set<String> readEnabledWorlds(List<World> worlds, DialogResponseView response) {
        Set<String> enabledWorlds = new LinkedHashSet<>();

        for (int index = 0; index < worlds.size(); index++) {
            World world = worlds.get(index);

            if (world.getEnvironment() == World.Environment.NORMAL
                    || Boolean.TRUE.equals(response.getBoolean(worldKey(index)))) {
                enabledWorlds.add(world.getName());
            }
        }

        return enabledWorlds;
    }

    private static Integer readBorderSize(DialogResponseView response, String key) {
        Float value = response.getFloat(key);
        return value == null ? null : Math.round(value);
    }

    private static Boolean readBoolean(DialogResponseView response, String key) {
        String value = response.getText(key);

        if (value == null || value.equals(DEFAULT)) return null;
        if (value.equals(TRUE)) return true;
        if (value.equals(FALSE)) return false;

        throw new IllegalArgumentException("La opción seleccionada no es válida.");
    }

    private static Double parseDouble(String input, String name) {
        if (input == null || input.isBlank()) return null;

        try {
            double value = Double.parseDouble(input.strip());

            if (!Double.isFinite(value)) throw new NumberFormatException();

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " debe ser un número válido.");
        }
    }

    private static void validateBorderSize(
            Option<Integer> option, Integer value, String name, float min, float max) {

        if (value != null && (value < min || value > max)) {
            throw new IllegalArgumentException(
                    "%s debe estar entre %.0f × %.0f y %.0f × %.0f bloques.".formatted(name, min, min, max, max));
        }

        validate(option, value, name + " no es válido.");
    }

    private static void validateBorderSizes(Integer survival, Integer meetup, Integer deathmatch) {
        if (survival != null && meetup != null && survival < meetup) {
            throw new IllegalArgumentException(
                    "El World Border de Supervivencia no puede ser menor que el de Meetup.");
        }

        if (meetup != null && deathmatch != null && meetup < deathmatch) {
            throw new IllegalArgumentException(
                    "El World Border de Meetup no puede ser menor que el de Deathmatch.");
        }
    }

    private static <T> void validate(Option<T> option, T value, String error) {
        if (value != null && !option.getPredicate().test(value)) throw new IllegalArgumentException(error);
    }

    private static String formatNumber(double value) {
        return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
    }

    private static String worldKey(int index) {
        return WORLD_PREFIX + index;
    }

    private static WorldManager getWorldManager() {
        return Hardlands.getInstance().getWorldManager();
    }

    private record WorldSettings(
            Set<String> enabledWorlds,
            Integer survivalSize,
            Integer meetupSize,
            Integer deathmatchSize,
            Boolean surfaceTeleport,
            Boolean borderDamage,
            Double centerX,
            Double centerZ
    ) {}
}