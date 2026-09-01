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
import team.heather.hardlands.core.data.json.LocalTimeAdapter;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.ui.HardlandsColor;
import team.heather.hardlands.util.text.TextFormatter;

@SuppressWarnings("UnstableApiUsage")
public final class PhaseConfigurationDialog {

    private static final int INPUT_WIDTH = 240;
    private static final int ACTION_WIDTH = 100;

    private static final float MIN_MINUTE = 0.0F;
    private static final float MAX_MINUTE = 180.0F;
    private static final float MINUTE_STEP = 1.0F;

    private static final String START_TIME = "startTime";
    private static final String PVP = "pvpMinute";
    private static final String BORDER_SHRINK = "borderShrinkMinute";
    private static final String MEETUP = "meetupMinute";
    private static final String FINAL_SHRINK = "finalShrinkMinute";
    private static final String DEATHMATCH = "deathmatchMinute";

    private static final DateTimeFormatter CURRENT_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    private PhaseConfigurationDialog() {}

    public static void show(Player player) {
        GameManager manager = Hardlands.getInstance().getGameManager();
        List<MinuteSetting> settings = createMinuteSettings(manager);

        player.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(
                                TextFormatter.tinyCaps("Configuracion de fases")
                        )
                        .externalTitle(TextFormatter.tinyCaps("Fases"))
                        .canCloseWithEscape(true)
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .inputs(createInputs(manager, settings))
                        .build())
                .type(DialogType.confirmation(
                        createSaveButton(manager, settings),
                        createCancelButton()
                ))));
    }

    private static List<DialogInput> createInputs(
            GameManager manager,
            List<MinuteSetting> settings
    ) {
        List<DialogInput> inputs = new ArrayList<>(settings.size() + 1);

        inputs.add(createStartTimeInput(manager));

        for (MinuteSetting setting : settings) {
            inputs.add(createMinuteInput(setting));
        }

        return inputs;
    }

    private static DialogInput createStartTimeInput(GameManager manager) {
        LocalTime startTime = manager.getStartTimeOption().getValue();
        LocalTime currentTime = LocalTime.now();

        Component label = TextFormatter.tinyCaps("Hora de inicio (HH:mm)")
                .color(NamedTextColor.WHITE)
                .append(Component.newline())
                .append(TextFormatter.tinyCaps(
                                "Hora actual: "
                                        + LocalTimeAdapter.HHMMSS_FORMATTER.format(currentTime)
                        )
                        .color(HardlandsColor.LIGHT_GRAY));

        return DialogInput.text(START_TIME, label)
                .width(INPUT_WIDTH)
                .initial(startTime == null
                        ? ""
                        : LocalTimeAdapter.HHMM_FORMATTER.format(startTime))
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

    private static ActionButton createSaveButton(
            GameManager manager,
            List<MinuteSetting> settings
    ) {
        return ActionButton.create(
                TextFormatter.tinyCaps("Guardar")
                        .color(HardlandsColor.HARDLANDS),
                TextFormatter.tinyCaps("Guardar los cambios.")
                        .color(HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player player) {
                                saveConfiguration(
                                        player,
                                        manager,
                                        settings,
                                        response
                                );
                            }
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
                TextFormatter.tinyCaps("Descartar los cambios.")
                        .color(HardlandsColor.LIGHT_GRAY),
                ACTION_WIDTH,
                DialogAction.customClick(
                        (_, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder()
                                .uses(1)
                                .build()
                )
        );
    }

    private static void saveConfiguration(
            Player player,
            GameManager manager,
            List<MinuteSetting> settings,
            DialogResponseView response
    ) {
        try {
            PhaseConfiguration configuration =
                    readConfiguration(settings, response);

            validateConfiguration(configuration);
            applyConfiguration(manager, settings, configuration);
        } catch (IllegalArgumentException exception) {
            player.sendMessage(
                    TextFormatter.tinyCaps(exception.getMessage())
                            .color(NamedTextColor.RED)
            );
            return;
        }

        player.closeDialog();
        player.sendMessage(
                TextFormatter.tinyCaps("Configuracion guardada.")
                        .color(NamedTextColor.GREEN)
        );
    }

    private static PhaseConfiguration readConfiguration(
            List<MinuteSetting> settings,
            DialogResponseView response
    ) {
        return new PhaseConfiguration(
                readStartTime(response),
                readMinutes(settings, response)
        );
    }

    private static LocalTime readStartTime(DialogResponseView response) {
        String input = response.getText(START_TIME);

        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException(
                    "Configura una hora de inicio."
            );
        }

        try {
            return LocalTime.parse(input.strip(), LocalTimeAdapter.HHMM_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Usa el formato HH:mm."
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
                        "Configura el minuto de %s."
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

    private static void validateConfiguration(
            PhaseConfiguration configuration
    ) {
        List<MinuteValue> values = configuration.minuteValues();

        for (int index = 1; index < values.size(); index++) {
            MinuteValue previous = values.get(index - 1);
            MinuteValue current = values.get(index);

            if (current.minute() > previous.minute()) {
                continue;
            }

            throw new IllegalArgumentException(
                    "%s debe ir despues de %s."
                            .formatted(
                                    current.label(),
                                    previous.label()
                            )
            );
        }
    }

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

    private static List<MinuteSetting> createMinuteSettings(
            GameManager manager
    ) {
        return List.of(
                new MinuteSetting(
                        PVP,
                        "PvP",
                        manager.getPvpMinuteOption()
                ),
                new MinuteSetting(
                        BORDER_SHRINK,
                        "Borde",
                        manager.getBorderShrinkMinuteOption()
                ),
                new MinuteSetting(
                        MEETUP,
                        "Meetup",
                        manager.getMeetupMinuteOption()
                ),
                new MinuteSetting(
                        FINAL_SHRINK,
                        "Borde final",
                        manager.getFinalShrinkMinuteOption()
                ),
                new MinuteSetting(
                        DEATHMATCH,
                        "Deathmatch",
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