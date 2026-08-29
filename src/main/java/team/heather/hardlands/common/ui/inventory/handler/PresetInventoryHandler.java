package team.heather.hardlands.common.ui.inventory.handler;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;
import team.heather.hardlands.common.item.InventoryItem;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.module.preset.PresetRepository;
import team.heather.hardlands.module.preset.PresetRepository.PresetInfo;
import team.heather.hardlands.util.text.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

public final class PresetInventoryHandler implements InventoryHandler {

    private static final String NAME_INPUT = "name";
    private static final String ICON_INPUT = "icon";
    private static final String DESCRIPTION_INPUT = "description";

    private static final int SAVE_INDEX = 0;
    private static final int PREVIOUS_OFFSET = 2;
    private static final int NEXT_OFFSET = 1;
    private static final int RESERVED_SLOTS = 3;

    private int page;

    @Override
    public void render(Inventory inventory) {
        List<PresetInfo> presets = getRepository().presets();
        int capacity = HardlandsInventory.getContentCapacity(inventory);
        int pageSize = capacity - RESERVED_SLOTS;
        int lastPage = Math.max(0, presets.size() - 1) / pageSize;

        this.page = Math.min(this.page, lastPage);

        for (int index = 0; index < capacity; index++) {
            inventory.clear(HardlandsInventory.contentSlot(index));
        }

        inventory.setItem(
                HardlandsInventory.contentSlot(SAVE_INDEX),
                createSaveItem());

        int start = this.page * pageSize;

        for (int index = 0;
             index < pageSize && start + index < presets.size();
             index++) {

            inventory.setItem(
                    HardlandsInventory.contentSlot(index + 1),
                    createPresetItem(presets.get(start + index)));
        }

        if (this.page > 0) {
            inventory.setItem(
                    HardlandsInventory.contentSlot(capacity - PREVIOUS_OFFSET),
                    InventoryItem.PREVIOUS.build());
        }

        if (this.page < lastPage) {
            inventory.setItem(
                    HardlandsInventory.contentSlot(capacity - NEXT_OFFSET),
                    InventoryItem.NEXT.build());
        }
    }

    @Override
    public Optional<Boolean> onClick(
            InventoryClickEvent event,
            Player player
    ) {
        Inventory inventory = event.getView().getTopInventory();
        List<PresetInfo> presets = getRepository().presets();

        int capacity = HardlandsInventory.getContentCapacity(inventory);
        int pageSize = capacity - RESERVED_SLOTS;
        int index = HardlandsInventory.getContentIndex(
                inventory,
                event.getRawSlot());

        if (index < 0) return Optional.empty();

        if (index == SAVE_INDEX) {
            this.showSaveDialog(
                    player,
                    inventory,
                    "",
                    PresetRepository.DEFAULT_ICON.getKey().asString(),
                    "",
                    null);

            return Optional.of(true);
        }

        if (index == capacity - PREVIOUS_OFFSET && this.page > 0) {
            this.page--;
            this.render(inventory);
            return Optional.of(true);
        }

        int lastPage = Math.max(0, presets.size() - 1) / pageSize;

        if (index == capacity - NEXT_OFFSET && this.page < lastPage) {
            this.page++;
            this.render(inventory);
            return Optional.of(true);
        }

        int presetIndex = this.page * pageSize + index - 1;

        if (presetIndex < 0 || presetIndex >= presets.size()) {
            return Optional.empty();
        }

        PresetInfo preset = presets.get(presetIndex);

        if (event.isLeftClick()) {
            getRepository().load(preset.name());

            HardlandsInventory.refreshPreparationItems();

            player.sendRichMessage(
                    "<green>Preset <white>%s</white> aplicado."
                            .formatted(preset.name()));

            return Optional.of(true);
        }

        if (event.isRightClick()) {
            this.showEditDialog(
                    player,
                    inventory,
                    preset,
                    preset.icon().getKey().asString(),
                    preset.description(),
                    null);

            return Optional.of(true);
        }

        return Optional.of(false);
    }

    private void showSaveDialog(
            Player player,
            Inventory inventory,
            String name,
            String icon,
            String description,
            String error
    ) {
        DialogBase.Builder base = DialogBase.builder(Component.text("Crear preset"))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .inputs(List.of(
                        DialogInput.text(
                                        NAME_INPUT,
                                        Component.text("Nombre"))
                                .width(300)
                                .initial(name)
                                .maxLength(32)
                                .build(),

                        DialogInput.text(
                                        ICON_INPUT,
                                        Component.text("Icono"))
                                .width(300)
                                .initial(icon)
                                .maxLength(128)
                                .build(),

                        DialogInput.text(
                                        DESCRIPTION_INPUT,
                                        Component.text("Descripción"))
                                .width(400)
                                .initial(description)
                                .maxLength(256)
                                .multiline(
                                        TextDialogInput.MultilineOptions.create(
                                                4,
                                                80))
                                .build()));

        if (error != null) {
            base.body(List.of(
                    DialogBody.plainMessage(Component.text(error))));
        }

        ActionButton save = ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarde el preset."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player target)) return;

                            this.savePreset(
                                    target,
                                    inventory,
                                    response.getText(NAME_INPUT),
                                    response.getText(ICON_INPUT),
                                    response.getText(DESCRIPTION_INPUT));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()));

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(base.build())
                .type(DialogType.confirmation(
                        save,
                        createCancelButton()))));
    }

    private void showEditDialog(
            Player player,
            Inventory inventory,
            PresetInfo preset,
            String icon,
            String description,
            String error
    ) {
        DialogBase.Builder base = DialogBase.builder(Component.text("Editar preset"))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .inputs(List.of(
                        DialogInput.text(
                                        ICON_INPUT,
                                        Component.text("Icono"))
                                .width(300)
                                .initial(icon)
                                .maxLength(128)
                                .build(),

                        DialogInput.text(
                                        DESCRIPTION_INPUT,
                                        Component.text("Descripción"))
                                .width(400)
                                .initial(description)
                                .maxLength(256)
                                .multiline(
                                        TextDialogInput.MultilineOptions.create(
                                                4,
                                                80))
                                .build()));

        if (error != null) {
            base.body(List.of(
                    DialogBody.plainMessage(Component.text(error))));
        }

        ActionButton save = ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarde los cambios."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player target)) return;

                            this.updatePreset(
                                    target,
                                    inventory,
                                    preset,
                                    response.getText(ICON_INPUT),
                                    response.getText(DESCRIPTION_INPUT));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()));

        ActionButton delete = ActionButton.create(
                Component.text("Eliminar", NamedTextColor.RED),
                Component.text("Elimine el preset."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player target)) return;

                            getRepository().delete(preset.name());
                            this.render(inventory);
                            target.closeDialog();

                            target.sendRichMessage(
                                    "<red>Preset <white>%s</white> eliminado."
                                            .formatted(preset.name()));
                        },
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()));

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(base.build())
                .type(DialogType.multiAction(
                        List.of(save, delete),
                        createCancelButton(),
                        2))));
    }

    private void savePreset(
            Player player,
            Inventory inventory,
            String nameInput,
            String iconInput,
            String descriptionInput
    ) {
        String name = normalize(nameInput);
        String iconName = normalize(iconInput);
        String description = normalize(descriptionInput);

        PresetRepository repository = getRepository();

        if (!repository.isNameValid(name)) {
            this.showSaveDialog(
                    player,
                    inventory,
                    name,
                    iconName,
                    description,
                    "Use entre 1 y 32 caracteres: "
                            + "letras, números, espacios, _ o -.");

            return;
        }

        if (repository.exists(name)) {
            this.showSaveDialog(
                    player,
                    inventory,
                    name,
                    iconName,
                    description,
                    "Ya existe un preset llamado \"%s\"."
                            .formatted(name));

            return;
        }

        Material icon = parseIcon(iconName);

        if (icon == null) {
            this.showSaveDialog(
                    player,
                    inventory,
                    name,
                    iconName,
                    description,
                    "El icono \"%s\" no corresponde a un item válido."
                            .formatted(iconName));

            return;
        }

        repository.save(name, icon, description);
        this.render(inventory);
        player.closeDialog();

        player.sendRichMessage(
                "<green>Preset <white>%s</white> guardado."
                        .formatted(name));
    }

    private void updatePreset(
            Player player,
            Inventory inventory,
            PresetInfo preset,
            String iconInput,
            String descriptionInput
    ) {
        String iconName = normalize(iconInput);
        String description = normalize(descriptionInput);
        Material icon = parseIcon(iconName);

        if (icon == null) {
            this.showEditDialog(
                    player,
                    inventory,
                    preset,
                    iconName,
                    description,
                    "El icono \"%s\" no corresponde a un item válido."
                            .formatted(iconName));

            return;
        }

        getRepository().update(
                preset.name(),
                icon,
                description);

        this.render(inventory);
        player.closeDialog();

        player.sendRichMessage(
                "<green>Preset <white>%s</white> actualizado."
                        .formatted(preset.name()));
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                Component.text("Cancelar"),
                null,
                100,
                DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()));
    }

    private static ItemStack createSaveItem() {
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .name(TextFormatter.formatTinyCaps("Crear preset"))
                .formattedLore(
                        "Guarda la [configuración actual].",
                        "",
                        "{Clic} para crear.")
                .build();
    }

    private static ItemStack createPresetItem(PresetInfo preset) {
        ItemBuilder builder = new ItemBuilder(preset.icon())
                .name(TextFormatter.formatTinyCaps(preset.name()));

        if (!preset.description().isBlank()) {
            builder.addLore(Component.text(
                    preset.description(),
                    HardlandsColor.LIGHT_GRAY));
        }

        return builder
                .addFormattedLore(
                        "",
                        "{Clic izquierdo} para aplicar.",
                        "[Clic derecho] para editar.")
                .build();
    }

    private static Material parseIcon(String input) {
        String value = normalize(input);

        if (value.isEmpty()) return PresetRepository.DEFAULT_ICON;

        Material material = Material.matchMaterial(value);

        if (material == null && !value.contains(":")) {
            material = Material.matchMaterial(
                    "minecraft:" + value.toLowerCase(Locale.ROOT));
        }

        return material;
    }

    private static String normalize(String input) {
        return input == null ? "" : input.strip();
    }

    private static PresetRepository getRepository() {
        return Hardlands.getInstance().getPresetRepository();
    }
}