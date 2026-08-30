package team.heather.hardlands.ui.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.phase.Phase;
import team.heather.hardlands.util.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

@SuppressWarnings("UnstableApiUsage")
public final class PhaseConfigurationDialog {

    private static final int INPUT_WIDTH = 320;
    private static final int ACTION_WIDTH = 120;

    private static final float MIN_MINUTE = 0F;
    private static final float MAX_MINUTE = 180F;
    private static final float MINUTE_STEP = 1F;

    private PhaseConfigurationDialog() {}

    public static void show(Player player) {
        GameManager manager = Hardlands.getInstance().getGameManager();
        List<PhaseSetting> settings = createSettings(manager);

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(TextFormatter.tinyCaps("Configuración de fases"))
                        .externalTitle(Component.text("Fases"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .inputs(createInputs(settings))
                        .build())
                .type(DialogType.confirmation(
                        createSaveButton(settings),
                        createCancelButton()
                ))));
    }

    private static List<PhaseSetting> createSettings(GameManager manager) {
        List<PhaseSetting> settings = new ArrayList<>();

        settings.add(new PhaseSetting(
                Phase.WAITING,
                manager.getWaitingMinuteOption(),
                inputKey(Phase.WAITING)
        ));

        for (Phase phase : Phase.values()) {
            if (phase == Phase.WAITING) continue;

            Option<Integer> option = phase.getMinuteOption(manager);
            if (option != null) {
                settings.add(new PhaseSetting(phase, option, inputKey(phase)));
            }
        }

        return settings;
    }


    private static List<DialogInput> createInputs(List<PhaseSetting> settings) {
        List<DialogInput> inputs = new ArrayList<>(settings.size());

        for (PhaseSetting setting : settings) {
            Integer value = setting.option().getValue();

            inputs.add(DialogInput.numberRange(
                    setting.inputKey(),
                    INPUT_WIDTH,
                    TextFormatter.tinyCaps(setting.phase().getLabel()).color(NamedTextColor.WHITE),
                    "%1$s: %2$s min",
                    MIN_MINUTE,
                    MAX_MINUTE,
                    value == null ? null : value.floatValue(),
                    MINUTE_STEP
            ));
        }

        return inputs;
    }

    private static ActionButton createSaveButton(List<PhaseSetting> settings) {
        return ActionButton.create(
                TextFormatter.tinyCaps("Guardar").color(HardlandsColor.PRIMARY),
                Component.text("Aplicar los minutos de inicio.", HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player player)) return;

                            try {
                                List<PhaseMinute> values = readValues(settings, response);
                                validateValues(values);
                                applyValues(settings, values);
                            } catch (IllegalArgumentException exception) {
                                player.sendRichMessage("<red>" + exception.getMessage());
                                return;
                            }

                            player.closeDialog();
                            player.sendRichMessage("<green>Configuración de fases guardada.");
                        },
                        ClickCallback.Options.builder()
                                .uses(ClickCallback.UNLIMITED_USES)
                                .build()
                )
        );
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                TextFormatter.tinyCaps("Cancelar").color(NamedTextColor.GRAY),
                Component.text("Cerrar sin guardar.", HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (_, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()
                )
        );
    }

    private static List<PhaseMinute> readValues(List<PhaseSetting> settings, DialogResponseView response) {
        List<PhaseMinute> values = new ArrayList<>(settings.size());

        for (PhaseSetting setting : settings) {
            Float value = response.getFloat(setting.inputKey());

            if (value == null) {
                throw new IllegalArgumentException(
                        "Debe configurar el minuto de inicio de %s.".formatted(setting.phase().getLabel())
                );
            }

            values.add(new PhaseMinute(setting.phase(), Math.round(value)));
        }

        return values;
    }

    private static void validateValues(List<PhaseMinute> values) {
        for (int index = 1; index < values.size(); index++) {
            PhaseMinute previous = values.get(index - 1);
            PhaseMinute current = values.get(index);

            if (current.minute() <= previous.minute()) {
                throw new IllegalArgumentException(
                        "%s debe iniciar después de %s."
                                .formatted(current.phase().getLabel(), previous.phase().getLabel())
                );
            }
        }
    }

    private static void applyValues(List<PhaseSetting> settings, List<PhaseMinute> values) {
        for (int index = 0; index < settings.size(); index++) {
            settings.get(index).option().setValue(values.get(index).minute());
        }
    }

    private static String inputKey(Phase phase) {
        return "phase_" + phase.name().toLowerCase(Locale.ROOT);
    }

    private record PhaseSetting(Phase phase, Option<Integer> option, String inputKey) {}

    private record PhaseMinute(Phase phase, int minute) {}
}