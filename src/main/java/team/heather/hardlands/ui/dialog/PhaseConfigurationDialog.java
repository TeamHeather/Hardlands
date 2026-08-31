package team.heather.hardlands.ui.dialog;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.config.Option;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.util.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

@SuppressWarnings("UnstableApiUsage")
public final class PhaseConfigurationDialog {

    private static final int INPUT_WIDTH = 320;
    private static final int ACTION_WIDTH = 120;

    private static final float MIN_MINUTE = 0.0F;
    private static final float MAX_MINUTE = 180.0F;
    private static final float MINUTE_STEP = 1.0F;

    private static final String START_TIME = "startTime";

    private static final String GRACE_PERIOD = "gracePeriodMinute";
    private static final String PVP = "pvpMinute";
    private static final String BORDER_SHRINK = "borderShrinkMinute";
    private static final String MEETUP = "meetupMinute";
    private static final String FINAL_SHRINK = "finalShrinkMinute";
    private static final String DEATHMATCH = "deathmatchMinute";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter CURRENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);


    private PhaseConfigurationDialog() {}

    public static void show(Player player) {
        GameManager manager = Hardlands.getInstance().getGameManager();
        List<MinuteSetting> minuteSettings = createMinuteSettings(manager);

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(TextFormatter.tinyCaps("Configuración de fases"))
                        .externalTitle(Component.text("Fases"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .inputs(createInputs(manager, minuteSettings))
                        .build())
                .type(DialogType.confirmation(
                        createSaveButton(manager, minuteSettings),
                        createCancelButton()
                ))));
    }

    // Inputs

    private static List<DialogInput> createInputs(
            GameManager manager,
            List<MinuteSetting> minuteSettings
    ) {
        List<DialogInput> inputs = new ArrayList<>(minuteSettings.size() + 1);

        inputs.add(createStartTimeInput(manager));

        for (MinuteSetting setting : minuteSettings) {
            inputs.add(createMinuteInput(setting));
        }

        return inputs;
    }

    private static DialogInput createStartTimeInput(GameManager manager) {
        LocalTime startTime = manager.getStartTimeOption().getValue();
        LocalTime currentTime = LocalTime.now();

        Component label = TextFormatter.tinyCaps("Hora de inicio")
                .color(NamedTextColor.WHITE)
                .append(Component.newline())
                .append(Component.text(
                        "Hora actual: " + CURRENT_TIME_FORMATTER.format(currentTime),
                        HardlandsColor.LIGHT_GRAY
                ));

        return DialogInput.text(START_TIME, label)
                .width(INPUT_WIDTH)
                .initial(startTime == null ? "" : TIME_FORMATTER.format(startTime))
                .maxLength(5)
                .build();
    }

    private static DialogInput createMinuteInput(MinuteSetting setting) {
        Integer value = setting.option().getValue();

        return DialogInput.numberRange(
                setting.key(),
                INPUT_WIDTH,
                TextFormatter.tinyCaps(setting.label())
                        .color(NamedTextColor.WHITE),
                "%1$s: %2$s min",
                MIN_MINUTE,
                MAX_MINUTE,
                value == null ? null : value.floatValue(),
                MINUTE_STEP
        );
    }

    // Actions

    private static ActionButton createSaveButton(
            GameManager manager,
            List<MinuteSetting> minuteSettings
    ) {
        return ActionButton.create(
                TextFormatter.tinyCaps("Guardar")
                        .color(HardlandsColor.PRIMARY),
                Component.text(
                        "Aplicar la hora de inicio y los tiempos de la partida.",
                        HardlandsColor.LIGHT_GRAY
                ),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (!(audience instanceof Player player)) return;

                            try {
                                PhaseConfiguration configuration =
                                        readConfiguration(minuteSettings, response);

                                validateConfiguration(configuration);
                                applyConfiguration(manager, minuteSettings, configuration);
                            } catch (IllegalArgumentException exception) {
                                player.sendRichMessage("<red>" + exception.getMessage());
                                return;
                            }

                            player.closeDialog();
                            player.sendRichMessage(
                                    "<green>Configuración de fases guardada."
                            );
                        },
                        ClickCallback.Options.builder()
                                .uses(ClickCallback.UNLIMITED_USES)
                                .build()
                )
        );
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                TextFormatter.tinyCaps("Cancelar")
                        .color(NamedTextColor.GRAY),
                Component.text(
                        "Cerrar sin guardar.",
                        HardlandsColor.LIGHT_GRAY
                ),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (_, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()
                )
        );
    }

    // Reading

    private static PhaseConfiguration readConfiguration(
            List<MinuteSetting> minuteSettings,
            DialogResponseView response
    ) {
        return new PhaseConfiguration(
                readStartTime(response),
                readMinutes(minuteSettings, response)
        );
    }

    private static LocalTime readStartTime(DialogResponseView response) {
        String input = response.getText(START_TIME);

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Debe configurar una hora de inicio."
            );
        }

        try {
            return LocalTime.parse(input.strip(), TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "La hora de inicio debe usar el formato HH:mm."
            );
        }
    }

    private static List<MinuteValue> readMinutes(
            List<MinuteSetting> settings,
            DialogResponseView response
    ) {
        List<MinuteValue> values = new ArrayList<>(settings.size());

        for (MinuteSetting setting : settings) {
            Float value = response.getFloat(setting.key());

            if (value == null) {
                throw new IllegalArgumentException(
                        "Debe configurar el minuto de %s."
                                .formatted(setting.label())
                );
            }

            values.add(new MinuteValue(
                    setting.label(),
                    Math.round(value)
            ));
        }

        return values;
    }

    // Validation

    private static void validateConfiguration(
            PhaseConfiguration configuration
    ) {
        List<MinuteValue> values = configuration.minuteValues();

        for (int index = 1; index < values.size(); index++) {
            MinuteValue previous = values.get(index - 1);
            MinuteValue current = values.get(index);

            if (current.minute() <= previous.minute()) {
                throw new IllegalArgumentException(
                        "%s debe ocurrir después de %s."
                                .formatted(
                                        current.label(),
                                        previous.label()
                                )
                );
            }
        }
    }

    // Applying

    private static void applyConfiguration(
            GameManager manager,
            List<MinuteSetting> settings,
            PhaseConfiguration configuration
    ) {
        manager.getStartTimeOption()
                .setValue(configuration.startTime());

        List<MinuteValue> values = configuration.minuteValues();

        for (int index = 0; index < settings.size(); index++) {
            settings.get(index)
                    .option()
                    .setValue(values.get(index).minute());
        }
    }

    // Settings

    private static List<MinuteSetting> createMinuteSettings(
            GameManager manager
    ) {
        return List.of(
                new MinuteSetting(
                        GRACE_PERIOD,
                        "Período de gracia",
                        manager.getGracePeriodMinuteOption()
                ),
                new MinuteSetting(
                        PVP,
                        "PvP",
                        manager.getPvpMinuteOption()
                ),
                new MinuteSetting(
                        BORDER_SHRINK,
                        "Reducción del borde",
                        manager.getBorderShrinkMinuteOption()
                ),
                new MinuteSetting(
                        MEETUP,
                        "Encuentro",
                        manager.getMeetupMinuteOption()
                ),
                new MinuteSetting(
                        FINAL_SHRINK,
                        "Reducción final",
                        manager.getFinalShrinkMinuteOption()
                ),
                new MinuteSetting(
                        DEATHMATCH,
                        "Combate a muerte",
                        manager.getDeathmatchMinuteOption()
                )
        );
    }

    private record PhaseConfiguration(
            LocalTime startTime,
            List<MinuteValue> minuteValues
    ) {}

    private record MinuteSetting(
            String key,
            String label,
            Option<Integer> option
    ) {}

    private record MinuteValue(
            String label,
            int minute
    ) {}
}