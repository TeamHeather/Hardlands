package team.heather.hardlands.common.ui.inventory.handler;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.*;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.common.item.InventoryItem;
import team.heather.hardlands.common.item.ItemBuilder;
import team.heather.hardlands.common.player.PlayerData;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.common.ui.chat.ChatMessenger;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;
import team.heather.hardlands.game.team.TeamManager;
import team.heather.hardlands.internal.config.Option;
import team.heather.hardlands.internal.repository.PlayerRepository;
import team.heather.hardlands.util.TextFormatters;

public final class PlayerInventoryHandler implements InventoryHandler {

    private static final String TEAM_INPUT = "team";
    private static final String OPTION_PREFIX = "option";
    private static final int INPUT_WIDTH = 320;

    private List<PlayerRepository.PlayerInfo> players = List.of();
    private Filter filter = Filter.ALL;
    private Sort sort = Sort.ONLINE;
    private int page;

    @Override
    public void render(Inventory inventory) {
        this.refreshPlayers();

        int capacity = HardlandsInventory.getContentCapacity(inventory);
        int maxPage = this.players.isEmpty() ? 0 : (this.players.size() - 1) / capacity;

        this.page = Math.min(this.page, maxPage);

        for (int index = 0; index < capacity; index++) {
            inventory.setItem(HardlandsInventory.contentSlot(index), null);
        }

        int start = this.page * capacity;
        int end = Math.min(start + capacity, this.players.size());

        for (int index = start; index < end; index++) {
            PlayerData player = getPlayer(this.players.get(index).uuid());

            if (player != null) {
                inventory.setItem(
                        HardlandsInventory.contentSlot(index - start),
                        this.createPlayerItem(player)
                );
            }
        }

        inventory.setItem(controlSlot(inventory), this.createControlItem(maxPage + 1));
    }

    @Override
    public Optional<Boolean> onClick(InventoryClickEvent event, Player viewer) {
        Inventory inventory = event.getView().getTopInventory();
        int slot = event.getRawSlot();

        if (slot == previousSlot(inventory) && this.page > 0) {
            this.page--;
            this.render(inventory);
            return Optional.of(true);
        }

        if (slot == nextSlot(inventory)) {
            return Optional.of(this.nextPage(inventory));
        }

        if (slot == controlSlot(inventory)) {
            if (event.isLeftClick()) {
                this.filter = this.filter.next();
            } else if (event.isRightClick()) {
                this.sort = this.sort.next();
            } else {
                return Optional.of(false);
            }

            this.page = 0;
            this.render(inventory);

            return Optional.of(true);
        }

        int contentIndex = HardlandsInventory.getContentIndex(inventory, slot);

        if (contentIndex < 0) {
            return Optional.empty();
        }

        int playerIndex = this.page * HardlandsInventory.getContentCapacity(inventory) + contentIndex;

        if (playerIndex >= this.players.size()) {
            return Optional.of(false);
        }

        PlayerData player = getPlayer(this.players.get(playerIndex).uuid());

        if (player == null) {
            return Optional.of(false);
        }

        if (event.isLeftClick()) {
            HardlandsInventory.PLAYER_PROFILE.openInventory(
                    viewer,
                    new PlayerProfileInventoryHandler(
                            player,
                            this.players,
                            playerIndex,
                            target -> HardlandsInventory.PLAYERS.openInventory(target, this)
                    )
            );

            return Optional.of(true);
        }

        if (event.isRightClick()) {
            this.showEditor(viewer, inventory, player);
            return Optional.of(true);
        }

        return Optional.of(false);
    }

    private void refreshPlayers() {
        List<PlayerRepository.PlayerInfo> filtered = new ArrayList<>();

        for (PlayerRepository.PlayerInfo player : repository().players()) {
            if (this.filter.includes(isOnline(player))) {
                filtered.add(player);
            }
        }

        filtered.sort(this.sort.comparator());
        this.players = filtered;
    }

    private boolean nextPage(Inventory inventory) {
        if ((this.page + 1) * HardlandsInventory.getContentCapacity(inventory) >= this.players.size()) {
            return false;
        }

        this.page++;
        this.render(inventory);

        return true;
    }

    private ItemStack createPlayerItem(PlayerData player) {
        DyeColor profileColor = player.getProfileColorOption().getValue();
        TextColor color = HardlandsColor.profile(profileColor == null ? DyeColor.RED : profileColor);
        String team = teamManager().get(player.getUniqueId());

        List<Component> lore = new ArrayList<>();

        lore.add(property(
                "Estado",
                player.getPlayer() == null ? "Desconectado" : "Conectado",
                color
        ));

        lore.add(property(
                "Equipo",
                team == null ? "Sin equipo" : team,
                color
        ));

        Set<String> pinnedStatistics = player.getPinnedStatisticsOption().getValue();

        if (pinnedStatistics != null && !pinnedStatistics.isEmpty()) {
            lore.add(Component.empty());

            for (PlayerStatistic statistic : PlayerStatistic.all()) {
                if (pinnedStatistics.contains(statistic.key())) {
                    lore.add(statistic.line(player, color, true));
                }
            }
        }

        lore.add(Component.empty());
        lore.add(TextFormatters.HIGHLIGHT.format("{Clic izquierdo} para abrir el perfil.", color));
        lore.add(TextFormatters.HIGHLIGHT.format("{Clic derecho} para administrar.", color));

        return new ItemBuilder(Material.PLAYER_HEAD)
                .skullOwner(player.getName())
                .name(Component.text(TextFormatters.TINY_CAPS.format(player.getName()), color))
                .addLore(lore.toArray(Component[]::new))
                .build();
    }

    private static Component property(String name, Object value, TextColor color) {
        return Component.text(name + ": ", color)
                .append(Component.text(String.valueOf(value), NamedTextColor.WHITE));
    }

    private ItemStack createControlItem(int pages) {
        return InventoryItem.createDisplayStack(
                Material.COMPARATOR,
                "Organización",
                "Filtro: [%s]".formatted(this.filter.label),
                "Orden: [%s]".formatted(this.sort.label),
                "",
                "Perfiles: [%d]".formatted(this.players.size()),
                "Página: [%d/%d]".formatted(this.page + 1, pages),
                "",
                "{Clic izquierdo} para cambiar el filtro.",
                "{Clic derecho} para cambiar el orden."
        );
    }

    private void showEditor(Player viewer, Inventory inventory, PlayerData player) {
        List<PlayerRepository.PlayerInfo> knownPlayers = repository().players();
        List<OptionBinding> bindings = createBindings(player);
        List<DialogInput> inputs = createInputs(player, bindings, knownPlayers);

        viewer.showDialog(Dialog.create(builder -> builder
                .empty()
                .base(DialogBase.builder(
                                Component.text(
                                        TextFormatters.TINY_CAPS.format("Administrar · " + player.getName()),
                                        HardlandsColor.HARDLANDS
                                )
                        )
                        .pause(false)
                        .afterAction(DialogBase.DialogAfterAction.NONE)
                        .body(List.of(
                                DialogBody.plainMessage(
                                        Component.text("Administre los datos persistentes y el equipo temporal del jugador.")
                                ),
                                DialogBody.plainMessage(
                                        Component.text("Las referencias a jugadores aceptan username o UUID.")
                                )
                        ))
                        .inputs(inputs)
                        .build())
                .type(DialogType.confirmation(
                        this.createSaveButton(inventory, player, bindings, knownPlayers),
                        createCancelButton()
                ))));
    }

    private static List<OptionBinding> createBindings(PlayerData player) {
        List<OptionBinding> bindings = new ArrayList<>(player.getConfigOptions().size());
        int index = 0;

        for (Option<?> option : player.getConfigOptions().values()) {
            bindings.add(new OptionBinding(option, OPTION_PREFIX + index++));
        }

        return bindings;
    }

    private static List<DialogInput> createInputs(
            PlayerData player,
            List<OptionBinding> bindings,
            List<PlayerRepository.PlayerInfo> knownPlayers
    ) {
        List<DialogInput> inputs = new ArrayList<>(bindings.size() + 1);

        inputs.add(DialogInput.text(
                TEAM_INPUT,
                INPUT_WIDTH,
                Component.text(TextFormatters.TINY_CAPS.format("Equipo temporal"), HardlandsColor.HARDLANDS),
                true,
                Optional.ofNullable(teamManager().get(player.getUniqueId())).orElse(""),
                32,
                null
        ));

        for (OptionBinding binding : bindings) {
            inputs.add(DialogInput.text(
                    binding.input(),
                    INPUT_WIDTH,
                    Component.text(
                            TextFormatters.TINY_CAPS.format(formatKey(binding.option().getKey())),
                            HardlandsColor.LIGHT_GRAY
                    ),
                    true,
                    formatValue(binding.option(), knownPlayers),
                    128,
                    null
            ));
        }

        return inputs;
    }

    private ActionButton createSaveButton(
            Inventory inventory,
            PlayerData player,
            List<OptionBinding> bindings,
            List<PlayerRepository.PlayerInfo> knownPlayers
    ) {
        return ActionButton.create(
                Component.text("Guardar"),
                Component.text("Guarde los cambios realizados."),
                100,
                DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player viewer) {
                                this.save(viewer, inventory, player, bindings, knownPlayers, response);
                            }
                        },
                        ClickCallback.Options.builder().uses(ClickCallback.UNLIMITED_USES).build()
                )
        );
    }

    private static ActionButton createCancelButton() {
        return ActionButton.create(
                Component.text("Cancelar"),
                Component.text("Cierre sin guardar los cambios."),
                100,
                DialogAction.customClick(
                        (_, audience) -> audience.closeDialog(),
                        ClickCallback.Options.builder().uses(1).build()
                )
        );
    }

    private void save(
            Player viewer,
            Inventory inventory,
            PlayerData player,
            List<OptionBinding> bindings,
            List<PlayerRepository.PlayerInfo> knownPlayers,
            DialogResponseView response
    ) {
        Map<Option<?>, Object> values = new LinkedHashMap<>(bindings.size());

        try {
            for (OptionBinding binding : bindings) {
                Object value = parseValue(binding.option(), response.getText(binding.input()), knownPlayers);

                if (value != null && !isValid(binding.option(), value)) {
                    throw invalidValue(binding.option());
                }

                values.put(binding.option(), value);
            }
        } catch (IllegalArgumentException exception) {
            ChatMessenger.send(viewer, "<red>" + exception.getMessage());
            return;
        }

        teamManager().set(player.getUniqueId(), normalize(response.getText(TEAM_INPUT)));

        for (Map.Entry<Option<?>, Object> entry : values.entrySet()) {
            setValue(entry.getKey(), entry.getValue());
        }

        repository().save(player);

        this.render(inventory);
        viewer.closeDialog();

        ChatMessenger.send(viewer, "Se han guardado los datos de <green>%s<white>.".formatted(player.getName()));
    }

    private static Object parseValue(
            Option<?> option,
            @Nullable String input,
            List<PlayerRepository.PlayerInfo> knownPlayers
    ) {
        Type type = option.getDataType();

        if (input == null || input.isBlank()) {
            if (type == String.class || type == UUID.class) {
                return null;
            }

            throw invalidValue(option);
        }

        String value = input.strip();

        if (type == String.class) {
            return value;
        }

        if (type == UUID.class) {
            return resolvePlayerId(value, knownPlayers);
        }

        if (type == DyeColor.class) {
            try {
                return DyeColor.valueOf(value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw invalidValue(option);
            }
        }

        try {
            if (isStringSet(type)) {
                Set<String> values = new LinkedHashSet<>();

                if (input == null || input.isBlank()) {
                    return values;
                }

                for (String entry : input.split(",")) {
                    if (!entry.isBlank()) {
                        values.add(entry.strip());
                    }
                }

                return values;
            }

            if (type == Integer.class) {
                return Integer.valueOf(value);
            }

            if (type == Double.class) {
                double number = Double.parseDouble(value);

                if (!Double.isFinite(number)) {
                    throw invalidValue(option);
                }

                return number;
            }
        } catch (NumberFormatException exception) {
            throw invalidValue(option);
        }

        throw new IllegalStateException("Unsupported player option type: " + type.getTypeName());
    }

    private static boolean isStringSet(Type type) {
        if (!(type instanceof ParameterizedType parameterizedType) || parameterizedType.getRawType() != Set.class) {
            return false;
        }

        Type[] arguments = parameterizedType.getActualTypeArguments();

        return arguments.length == 1 && arguments[0] == String.class;
    }

    private static UUID resolvePlayerId(String value, List<PlayerRepository.PlayerInfo> players) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException _) {
            // Resolve as username.
        }

        UUID result = null;

        for (PlayerRepository.PlayerInfo player : players) {
            if (!player.name().equalsIgnoreCase(value)) {
                continue;
            }

            if (result != null) {
                throw new IllegalArgumentException("El username \"%s\" es ambiguo. Use el UUID.".formatted(value));
            }

            result = player.uuid();
        }

        if (result == null) {
            throw new IllegalArgumentException("No existe ningún perfil para \"%s\".".formatted(value));
        }

        return result;
    }

    private static String formatValue(Option<?> option, List<PlayerRepository.PlayerInfo> players) {
        Object value = option.getValue();

        if (value == null) {
            return "";
        }

        if (value instanceof UUID uuid) {
            for (PlayerRepository.PlayerInfo player : players) {
                if (player.uuid().equals(uuid)) {
                    return player.name();
                }
            }

            return uuid.toString();
        }

        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }

        return String.valueOf(value);
    }

    private static String formatKey(String key) {
        StringBuilder result = new StringBuilder(key.length() + 8);

        for (char character : key.toCharArray()) {
            if (Character.isUpperCase(character)) {
                result.append(' ').append(Character.toLowerCase(character));
            } else {
                result.append(character);
            }
        }

        if (!result.isEmpty()) {
            result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
        }

        return result.toString();
    }

    private static boolean isOnline(PlayerRepository.PlayerInfo player) {
        return Bukkit.getPlayer(player.uuid()) != null;
    }

    private static @Nullable PlayerData getPlayer(UUID playerId) {
        PlayerData player = Hardlands.getInstance().getPlayerManager().get(playerId);

        return player != null ? player : repository().load(playerId).orElse(null);
    }

    private static @Nullable String normalize(@Nullable String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static IllegalArgumentException invalidValue(Option<?> option) {
        return new IllegalArgumentException("Valor inválido para \"%s\".".formatted(formatKey(option.getKey())));
    }

    private static TeamManager teamManager() {
        return Hardlands.getInstance().getGameManager().getTeamManager();
    }

    private static PlayerRepository repository() {
        return Hardlands.getInstance().getRepositories().player();
    }

    private static int previousSlot(Inventory inventory) {
        return inventory.getSize() - 6;
    }

    private static int controlSlot(Inventory inventory) {
        return inventory.getSize() - 5;
    }

    private static int nextSlot(Inventory inventory) {
        return inventory.getSize() - 4;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean isValid(Option<T> option, Object value) {
        return option.getPredicate().test((T) value);
    }

    @SuppressWarnings("unchecked")
    private static <T> void setValue(Option<T> option, Object value) {
        option.changeValue((T) value);
    }

    private record OptionBinding(Option<?> option, String input) {}

    private enum Filter {

        ALL("Todos"),
        ONLINE("Conectados"),
        OFFLINE("Desconectados");

        private static final Filter[] VALUES = values();

        private final String label;

        Filter(String label) {
            this.label = label;
        }

        private boolean includes(boolean online) {
            return switch (this) {
                case ALL -> true;
                case ONLINE -> online;
                case OFFLINE -> !online;
            };
        }

        private Filter next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }

    private enum Sort {

        ONLINE("Conectados primero"),
        NAME_ASCENDING("Nombre A → Z"),
        NAME_DESCENDING("Nombre Z → A"),
        TEAM("Equipo");

        private static final Sort[] VALUES = values();

        private final String label;

        Sort(String label) {
            this.label = label;
        }

        private Comparator<PlayerRepository.PlayerInfo> comparator() {
            return switch (this) {
                case ONLINE -> Comparator
                        .comparing((PlayerRepository.PlayerInfo player) -> !isOnline(player))
                        .thenComparing(PlayerRepository.PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);

                case NAME_ASCENDING -> Comparator.comparing(
                        PlayerRepository.PlayerInfo::name,
                        String.CASE_INSENSITIVE_ORDER
                );

                case NAME_DESCENDING -> Comparator.comparing(
                        PlayerRepository.PlayerInfo::name,
                        String.CASE_INSENSITIVE_ORDER.reversed()
                );

                case TEAM -> Comparator
                        .comparing(
                                (PlayerRepository.PlayerInfo player) -> teamManager().get(player.uuid()),
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                        )
                        .thenComparing(PlayerRepository.PlayerInfo::name, String.CASE_INSENSITIVE_ORDER);
            };
        }

        private Sort next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }
}