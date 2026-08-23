package org.heather.hardlands.common.inventory.handler;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.inventory.InventoryDefinition;
import org.heather.hardlands.common.item.InventoryItem;
import org.heather.hardlands.common.item.ItemBuilder;
import org.heather.hardlands.module.PresetRepository;

@SuppressWarnings("UnstableApiUsage")
public final class PresetInventoryHandler implements InventoryHandler {

    private static final String NAME_INPUT = "name";

    private static final int SAVE_INDEX = 0;
    private static final int PREVIOUS_OFFSET = 2;
    private static final int NEXT_OFFSET = 1;
    private static final int RESERVED_SLOTS = 3;

    private int page;

    @Override
    public void onOpen(Inventory inventory, Player player) {
        this.page = 0;
        this.render(inventory);
    }

    @Override
    public void render(Inventory inventory) {
        List<String> presets = getRepository().getPresetNames();
        int capacity = InventoryDefinition.getContentCapacity(inventory);
        int pageSize = capacity - RESERVED_SLOTS;
        int lastPage = getLastPage(presets.size(), pageSize);

        this.page = Math.min(this.page, lastPage);

        for (int index = 0; index < capacity; index++) {
            inventory.clear(InventoryDefinition.contentSlot(index));
        }

        inventory.setItem(
                InventoryDefinition.contentSlot(SAVE_INDEX),
                createSaveItem());

        int start = this.page * pageSize;

        for (int index = 0; index < pageSize && start + index < presets.size(); index++) {
            inventory.setItem(
                    InventoryDefinition.contentSlot(index + 1),
                    createPresetItem(presets.get(start + index)));
        }

        if (this.page > 0) {
            inventory.setItem(
                    InventoryDefinition.contentSlot(capacity - PREVIOUS_OFFSET),
                    InventoryItem.PREVIOUS.build());
        }

        if (this.page < lastPage) {
            inventory.setItem(
                    InventoryDefinition.contentSlot(capacity - NEXT_OFFSET),
                    InventoryItem.NEXT.build());
        }
    }

    @Override
    public Optional<Boolean> handleClick(InventoryClickEvent event, Player player) {
        Inventory inventory = event.getView().getTopInventory();
        List<String> presets = getRepository().getPresetNames();

        int capacity = InventoryDefinition.getContentCapacity(inventory);
        int pageSize = capacity - RESERVED_SLOTS;
        int index = InventoryDefinition.getContentIndex(inventory, event.getRawSlot());

        if (index < 0) return Optional.empty();

        if (index == SAVE_INDEX) {
            this.showNameDialog(player, inventory, "", null);
            return Optional.of(true);
        }

        if (index == capacity - PREVIOUS_OFFSET && this.page > 0) {
            this.page--;
            this.render(inventory);
            return Optional.of(true);
        }

        if (index == capacity - NEXT_OFFSET && this.page < getLastPage(presets.size(), pageSize)) {
            this.page++;
            this.render(inventory);
            return Optional.of(true);
        }

        int presetIndex = this.page * pageSize + index - 1;

        if (presetIndex < 0 || presetIndex >= presets.size()) return Optional.empty();

        String preset = presets.get(presetIndex);

        getRepository().load(preset);
        player.sendRichMessage("<green>Preset <white>%s</white> cargado.".formatted(preset));

        return Optional.of(true);
    }

    private void showNameDialog(
            Player player,
            Inventory inventory,
            String initialName,
            String error
    ) {
        DialogBase.Builder base = DialogBase.builder(Component.text("Guardar preset"))
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .inputs(List.of(
                        DialogInput.text(NAME_INPUT, Component.text("Nombre"))
                                .width(300)
                                .initial(initialName)
                                .maxLength(32)
                                .build()));

        if (error != null) {
            base.body(List.of(DialogBody.plainMessage(Component.text(error))));
        }

        ActionButton save = ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarda la configuración actual."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player target) {
                                this.savePreset(
                                        target,
                                        inventory,
                                        response.getText(NAME_INPUT));
                            }
                        },
                        ClickCallback.Options.builder().uses(1).build()));

        ActionButton cancel = ActionButton.create(
                Component.text("Cancelar"),
                null,
                100,
                DialogAction.customClick(
                        (response, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()));

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(base.build())
                .type(DialogType.confirmation(save, cancel))));
    }

    private void savePreset(Player player, Inventory inventory, String input) {
        String name = input == null ? "" : input.strip();
        PresetRepository repository = getRepository();

        if (!repository.isValidName(name)) {
            this.showNameDialog(player, inventory, name,
                    "El nombre debe tener entre 1 y 32 caracteres y solo puede contener letras, números, espacios, _ o -.");
            return;
        }

        if (repository.exists(name)) {
            this.showNameDialog(
                    player,
                    inventory,
                    name,
                    "Ya existe un preset llamado \"%s\".".formatted(name));
            return;
        }

        repository.save(name);
        this.render(inventory);

        player.sendRichMessage("<green>Preset <white>%s</white> guardado.".formatted(name));
        player.closeDialog();
    }

    private static ItemStack createSaveItem() {
        return new ItemBuilder(Material.WRITABLE_BOOK)
                .name("<green>Guardar preset")
                .lore(
                        "<gray>Guarda la configuración actual",
                        "<gray>como una nueva plantilla.",
                        "",
                        "<yellow>Click <gray>para guardar.")
                .build();
    }

    private static ItemStack createPresetItem(String name) {
        return new ItemBuilder(Material.BOOK)
                .name("<yellow>" + name)
                .lore(
                        "<gray>Carga esta configuración.",
                        "",
                        "<yellow>Click <gray>para cargar.")
                .build();
    }

    private static PresetRepository getRepository() {
        return Hardlands.getInstance().getPresetRepository();
    }

    private static int getLastPage(int presetCount, int pageSize) {
        return Math.max(0, presetCount - 1) / pageSize;
    }
}