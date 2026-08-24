package org.heather.hardlands.common.inventory.handler;

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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.inventory.HardlandsInventory;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.core.config.Option;
import org.heather.hardlands.module.world.WorldManager;

@SuppressWarnings("UnstableApiUsage")
public final class WorldInventoryHandler implements InventoryHandler {

    private static final String DEFAULT = "default";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private static final String SURVIVAL_SIZE = "survivalSize";
    private static final String MEETUP_SIZE = "meetupSize";
    private static final String DEATHMATCH_SIZE = "deathmatchSize";
    private static final String MEETUP_SHRINK = "meetupShrink";
    private static final String DEATHMATCH_SHRINK = "deathmatchShrink";
    private static final String CENTER_X = "centerX";
    private static final String CENTER_Z = "centerZ";
    private static final String SURFACE_TELEPORT = "surfaceTeleport";
    private static final String BORDER_DAMAGE = "borderDamage";

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player player) {
        Optional<InventoryItem> item = InventoryItem.findByStack(event.getCurrentItem());

        if (item.isEmpty()) return Optional.empty();

        Inventory inventory = event.getView().getTopInventory();

        switch (item.get()) {
            case WORLD_WORLDS -> this.showWorldsDialog(player, inventory);
            case WORLD_BORDERS -> this.showBordersDialog(player, inventory);
            case WORLD_SHRINK -> this.showShrinkDialog(player, inventory);
            case WORLD_CENTER -> this.showCenterDialog(player, inventory);
            case WORLD_BEHAVIOR -> this.showBehaviorDialog(player, inventory);
            default -> {
                return Optional.empty();
            }
        }

        return Optional.of(true);
    }

    private void showWorldsDialog(Player player, Inventory inventory) {
        Option<Set<String>> option = getWorldManager().getEnabledWorldsOption();
        Set<String> enabled = option.hasValue() ? option.getValue() : Set.of();
        List<World> worlds = Bukkit.getWorlds();
        List<DialogInput> inputs = new ArrayList<>(worlds.size());

        for (int index = 0; index < worlds.size(); index++) {
            World world = worlds.get(index);

            inputs.add(DialogInput.bool("world" + index, Component.text(world.getName()))
                    .initial(enabled.contains(world.getName()))
                    .build());
        }

        this.showDialog(player, inventory, "Mundos", inputs, response -> {
            Set<String> selected = new LinkedHashSet<>();

            for (int index = 0; index < worlds.size(); index++) {
                if (Boolean.TRUE.equals(response.getBoolean("world" + index))) {
                    selected.add(worlds.get(index).getName());
                }
            }

            option.setValue(selected);
        });
    }

    private void showBordersDialog(Player player, Inventory inventory) {
        WorldManager manager = getWorldManager();
        Option<Integer> survival = manager.getSurvivalSizeOption();
        Option<Integer> meetup = manager.getMeetupSizeOption();
        Option<Integer> deathmatch = manager.getDeathmatchSizeOption();

        List<DialogInput> inputs = List.of(
                textInput(SURVIVAL_SIZE, "Supervivencia", survival.getValue()),
                textInput(MEETUP_SIZE, "Meetup", meetup.getValue()),
                textInput(DEATHMATCH_SIZE, "Deathmatch", deathmatch.getValue()));

        this.showDialog(player, inventory, "Límites", inputs, response -> {
            Integer survivalValue = parseInteger(response.getText(SURVIVAL_SIZE), "Supervivencia");
            Integer meetupValue = parseInteger(response.getText(MEETUP_SIZE), "Meetup");
            Integer deathmatchValue = parseInteger(response.getText(DEATHMATCH_SIZE), "Deathmatch");

            validate(survival, survivalValue, "Supervivencia debe ser mayor que 0.");
            validate(meetup, meetupValue, "Meetup debe ser mayor que 0.");
            validate(deathmatch, deathmatchValue, "Deathmatch debe ser mayor que 0.");

            if (survivalValue != null && meetupValue != null && survivalValue < meetupValue) {
                throw new IllegalArgumentException("Supervivencia no puede ser menor que Meetup.");
            }

            if (meetupValue != null && deathmatchValue != null && meetupValue < deathmatchValue) {
                throw new IllegalArgumentException("Meetup no puede ser menor que Deathmatch.");
            }

            survival.setValue(survivalValue);
            meetup.setValue(meetupValue);
            deathmatch.setValue(deathmatchValue);
        });
    }

    private void showShrinkDialog(Player player, Inventory inventory) {
        WorldManager manager = getWorldManager();
        Option<Integer> meetup = manager.getMeetupShrinkTimeOption();
        Option<Integer> deathmatch = manager.getDeathmatchShrinkTimeOption();

        List<DialogInput> inputs = List.of(
                textInput(MEETUP_SHRINK, "Meetup", meetup.getValue()),
                textInput(DEATHMATCH_SHRINK, "Deathmatch", deathmatch.getValue()));

        this.showDialog(player, inventory, "Reducción", inputs, response -> {
            Integer meetupValue = parseInteger(response.getText(MEETUP_SHRINK), "Meetup");
            Integer deathmatchValue = parseInteger(response.getText(DEATHMATCH_SHRINK), "Deathmatch");

            validate(meetup, meetupValue, "Meetup debe ser 0 o mayor.");
            validate(deathmatch, deathmatchValue, "Deathmatch debe ser 0 o mayor.");

            meetup.setValue(meetupValue);
            deathmatch.setValue(deathmatchValue);
        });
    }

    private void showCenterDialog(Player player, Inventory inventory) {
        WorldManager manager = getWorldManager();
        List<DialogInput> inputs = List.of(
                textInput(CENTER_X, "Centro X", manager.getCenterXOption().getValue()),
                textInput(CENTER_Z, "Centro Z", manager.getCenterZOption().getValue()));

        this.showDialog(player, inventory, "Centro", inputs, response -> {
            Double x = parseDouble(response.getText(CENTER_X), "Centro X");
            Double z = parseDouble(response.getText(CENTER_Z), "Centro Z");

            manager.getCenterXOption().setValue(x);
            manager.getCenterZOption().setValue(z);
        });
    }

    private void showBehaviorDialog(Player player, Inventory inventory) {
        WorldManager manager = getWorldManager();
        Option<Boolean> surfaceTeleport = manager.getSurfaceTeleportOption();
        Option<Boolean> borderDamage = manager.getBorderDamageOption();

        List<DialogInput> inputs = List.of(
                booleanInput(SURFACE_TELEPORT, "Teletransporte a superficie", surfaceTeleport.getValue()),
                booleanInput(BORDER_DAMAGE, "Daño fuera del límite", borderDamage.getValue()));

        this.showDialog(player, inventory, "Comportamiento", inputs, response -> {
            Boolean surfaceValue = readBoolean(response, SURFACE_TELEPORT);
            Boolean damageValue = readBoolean(response, BORDER_DAMAGE);

            surfaceTeleport.setValue(surfaceValue);
            borderDamage.setValue(damageValue);
        });
    }

    private void showDialog(Player player, Inventory inventory, String title, List<DialogInput> inputs,
                            Consumer<DialogResponseView> saveAction) {
        ActionButton save = ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarde los cambios."),
                100,
                DialogAction.customClick((response, audience) -> {
                    if (!(audience instanceof Player target)) return;

                    try {
                        saveAction.accept(response);
                    } catch (IllegalArgumentException exception) {
                        target.sendRichMessage("<red>" + exception.getMessage());
                        return;
                    }

                    HardlandsInventory.WORLD.renderLayout(inventory);
                    HardlandsInventory.refreshPreparationItems();
                    target.closeDialog();
                }, ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()));

        ActionButton cancel = ActionButton.create(
                Component.text("Cancelar"),
                null,
                100,
                DialogAction.customClick((response, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()));

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(Component.text(title))
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .inputs(inputs)
                        .build())
                .type(DialogType.confirmation(save, cancel))));
    }

    private static DialogInput textInput(String key, String label, Number value) {
        return DialogInput.text(key, Component.text(label))
                .width(300)
                .initial(value == null ? "" : value.toString())
                .maxLength(32)
                .build();
    }

    private static DialogInput booleanInput(String key, String label, Boolean value) {
        return DialogInput.singleOption(
                key,
                300,
                List.of(
                        SingleOptionDialogInput.OptionEntry.create(DEFAULT, Component.text("Sin configurar"), value == null),
                        SingleOptionDialogInput.OptionEntry.create(TRUE, Component.text("Activado"), Boolean.TRUE.equals(value)),
                        SingleOptionDialogInput.OptionEntry.create(FALSE, Component.text("Desactivado"), Boolean.FALSE.equals(value))),
                Component.text(label),
                true);
    }

    private static Integer parseInteger(String input, String name) {
        if (input == null || input.isBlank()) return null;

        try {
            return Integer.valueOf(input.strip());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("%s debe ser un número entero.".formatted(name));
        }
    }

    private static Double parseDouble(String input, String name) {
        if (input == null || input.isBlank()) return null;

        try {
            double value = Double.parseDouble(input.strip());

            if (!Double.isFinite(value)) throw new NumberFormatException();

            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("%s debe ser un número válido.".formatted(name));
        }
    }

    private static Boolean readBoolean(DialogResponseView response, String key) {
        String value = response.getText(key);

        if (value == null || value.equals(DEFAULT)) return null;
        if (value.equals(TRUE)) return true;
        if (value.equals(FALSE)) return false;

        throw new IllegalArgumentException("Valor booleano inválido.");
    }

    private static <T> void validate(Option<T> option, T value, String error) {
        if (value != null && !option.getPredicate().test(value)) throw new IllegalArgumentException(error);
    }

    private static WorldManager getWorldManager() {
        return Hardlands.getInstance().getWorldManagerOrThrow();
    }
}