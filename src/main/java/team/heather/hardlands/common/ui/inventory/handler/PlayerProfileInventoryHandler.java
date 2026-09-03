package team.heather.hardlands.common.ui.inventory.handler;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
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
import team.heather.hardlands.common.player.HardlandsPlayer;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.common.ui.chat.ChatMessenger;
import team.heather.hardlands.common.ui.inventory.HardlandsInventory;
import team.heather.hardlands.internal.repository.PlayerRepository;
import team.heather.hardlands.util.TextFormatters;

public final class PlayerProfileInventoryHandler implements InventoryHandler {

		private static final int PROFILE_SLOT = 4;
		private static final int STATISTICS_SLOT = 20;
		private static final int CAREER_SLOT = 22;
		private static final int PREFERENCES_SLOT = 24;

		private final List<PlayerRepository.PlayerInfo> navigation;
		private final int originIndex;

		private HardlandsPlayer profile;
		private int currentIndex;

		public PlayerProfileInventoryHandler(
						HardlandsPlayer profile,
						List<PlayerRepository.PlayerInfo> navigation,
						int currentIndex
		) {
				this.profile = profile;
				this.navigation = navigation;
				this.originIndex = currentIndex;
				this.currentIndex = currentIndex;
		}

		@Override
		public void render(Inventory inventory) {
				List<PlayerRepository.PlayerInfo> players = repository().players();

				this.renderOutline(inventory);

				inventory.setItem(PROFILE_SLOT, this.createProfileItem());
				inventory.setItem(STATISTICS_SLOT, this.createStatisticsItem());
				inventory.setItem(CAREER_SLOT, this.createCareerItem(players));
				inventory.setItem(PREFERENCES_SLOT, this.createPreferencesItem());

				inventory.setItem(previousSlot(inventory), InventoryItem.PREVIOUS.build());
				inventory.setItem(nextSlot(inventory), InventoryItem.NEXT.build());
		}

		@Override
		public void onOpen(Inventory inventory, Player viewer) {
				this.renderPersonalization(inventory, viewer);
		}

		@Override
		public Optional<Boolean> onClick(InventoryClickEvent event, Player viewer) {
				Inventory inventory = event.getView().getTopInventory();
				int slot = event.getRawSlot();

				if (slot == previousSlot(inventory)) {
						return Optional.of(this.previous(viewer, inventory));
				}

				if (slot == nextSlot(inventory)) {
						return Optional.of(this.navigate(viewer, inventory, 1));
				}

				if (slot == personalizationSlot(inventory) && this.isOwner(viewer)) {
						this.showPersonalization(viewer);
						return Optional.of(true);
				}

				return Optional.empty();
		}

		private boolean previous(Player viewer, Inventory inventory) {
				if (this.currentIndex == this.originIndex) {
						HardlandsInventory.PLAYERS.openInventory(viewer);
						return true;
				}

				return this.navigate(viewer, inventory, -1);
		}

		private boolean navigate(Player viewer, Inventory inventory, int offset) {
				int targetIndex = this.currentIndex + offset;

				if (targetIndex < this.originIndex || targetIndex >= this.navigation.size()) {
						return false;
				}

				HardlandsPlayer target = getPlayer(this.navigation.get(targetIndex).uuid());

				if (target == null) {
						return false;
				}

				this.profile = target;
				this.currentIndex = targetIndex;

				this.render(inventory);
				this.renderPersonalization(inventory, viewer);

				return true;
		}

		private ItemStack createProfileItem() {
				String team = Hardlands.getInstance().getGameManager().getTeamManager().get(this.profile.getUniqueId());

				return group(
								new ItemBuilder(Material.PLAYER_HEAD).skullOwner(this.profile.getName()),
								this.profile.getName(),
								statistic(
												Icon.STATUS.component,
												"Estado",
												this.profile.getPlayer() == null ? "Desconectado" : "Conectado"
								),
								statistic(
												Icon.TEAM.component,
												"Equipo",
												team == null ? "Sin equipo" : team
								)
				);
		}

		private ItemStack createStatisticsItem() {
				return group(
								new ItemBuilder(Material.NETHER_STAR),
								"Estadísticas",
								statistic(
												Icon.GAMES.component,
												"Partidas jugadas",
												value(this.profile.getGamesPlayedOption().getValue())
								),
								statistic(
												Icon.WINS.component,
												"Victorias",
												value(this.profile.getTotalWinsOption().getValue())
								),
								statistic(
												Icon.SURVIVAL.component,
												"Supervivencia media",
												formatNumber(this.profile.getAverageSurvivalTimeOption().getValue())
								),
								Component.empty(),
								statistic(
												Icon.KILLS.component,
												"Eliminaciones",
												value(this.profile.getTotalKillsOption().getValue())
								),
								statistic(
												Icon.DEATHS.component,
												"Muertes",
												value(this.profile.getTotalDeathsOption().getValue())
								),
								Component.empty(),
								statistic(
												Icon.IRON_MAN.component,
												"Iron Man",
												value(this.profile.getIronManAwardsOption().getValue())
								),
								statistic(
												Icon.KILL_TOP.component,
												"Kill Top",
												value(this.profile.getKillTopAwardsOption().getValue())
								),
								statistic(
												Icon.PAPER_MAN.component,
												"Paper Man",
												value(this.profile.getPaperManAwardsOption().getValue())
								)
				);
		}

		private ItemStack createCareerItem(List<PlayerRepository.PlayerInfo> players) {
				return group(
								new ItemBuilder(Material.CLOCK),
								"Trayectoria",
								playerStatistic(
												"Mejor amigo",
												this.profile.getBestFriendOption().getValue(),
												players
								),
								playerStatistic(
												"Asesino principal",
												this.profile.getPrimaryKillerOption().getValue(),
												players
								),
								playerStatistic(
												"Víctima principal",
												this.profile.getPrimaryVictimOption().getValue(),
												players
								),
								Component.empty(),
								playerStatistic(
												"Hoster con más partidas",
												this.profile.getMostPlayedHostOption().getValue(),
												players
								),
								statistic(
												Icon.PRESET.component,
												"Plantilla más jugada",
												Optional.ofNullable(this.profile.getMostPlayedPresetOption().getValue()).orElse("Ninguna")
								)
				);
		}

		private ItemStack createPreferencesItem() {
				DyeColor color = this.getProfileColor();

				return group(
								new ItemBuilder(Material.COMPARATOR),
								"Preferencias",
								statistic(
												sprite("item/%s_dye".formatted(color.name().toLowerCase(Locale.ROOT))),
												"Color del perfil",
												ColorSelectionInventoryHandler.colorName(color)
								)
				);
		}

		private ItemStack createPersonalizationItem() {
				DyeColor color = this.getProfileColor();

				return group(
								new ItemBuilder(Material.valueOf(color.name() + "_DYE")),
								"Personalización",
								statistic(
												sprite("item/%s_dye".formatted(color.name().toLowerCase(Locale.ROOT))),
												"Color",
												ColorSelectionInventoryHandler.colorName(color)
								),
								Component.empty(),
								Component.text("Clic para personalizar tu perfil.", NamedTextColor.GRAY)
				);
		}

		private void renderOutline(Inventory inventory) {
				ItemStack outline = new ItemBuilder(
								Material.valueOf(this.getProfileColor().name() + "_STAINED_GLASS_PANE")
				).name("").build();

				int bottomRowStart = inventory.getSize() - 9;

				for (int column = 0; column < 9; column++) {
						inventory.setItem(column, outline);
						inventory.setItem(bottomRowStart + column, outline);
				}

				for (int slot = 9; slot < bottomRowStart; slot += 9) {
						inventory.setItem(slot, outline);
						inventory.setItem(slot + 8, outline);
				}
		}

		private void renderPersonalization(Inventory inventory, Player viewer) {
				inventory.setItem(
								personalizationSlot(inventory),
								this.isOwner(viewer) ? this.createPersonalizationItem() : outlineItem()
				);
		}

		private void showPersonalization(Player viewer) {
				DyeColor color = this.getProfileColor();

				viewer.showDialog(Dialog.create(builder -> builder
								.empty()
								.base(DialogBase.builder(
																Component.text(
																				TextFormatters.TINY_CAPS.format("Personalización"),
																				HardlandsColor.HARDLANDS
																)
												)
												.pause(false)
												.afterAction(DialogBase.DialogAfterAction.NONE)
												.body(List.of(
																DialogBody.plainMessage(
																				Component.text("Administra la apariencia y preferencias de tu perfil.")
																)
												))
												.build())
								.type(DialogType.confirmation(
												ActionButton.create(
																Component.text("Color del perfil"),
																Component.text(
																				"Actual: " + ColorSelectionInventoryHandler.colorName(color)
																),
																150,
																DialogAction.customClick(
																				(_, audience) -> {
																						if (!(audience instanceof Player player)) {
																								return;
																						}

																						player.closeDialog();

																						HardlandsInventory.COLORS.openInventory(
																										player,
																										new ColorSelectionInventoryHandler(
																														color,
																														this::selectColor,
																														this::reopenProfile
																										)
																						);
																				},
																				ClickCallback.Options.builder().uses(1).build()
																)
												),
												ActionButton.create(
																Component.text("Cerrar"),
																Component.text("Cerrar la personalización."),
																100,
																DialogAction.customClick(
																				(_, audience) -> audience.closeDialog(),
																				ClickCallback.Options.builder().uses(1).build()
																)
												)
								))));
		}

		private void selectColor(Player viewer, DyeColor color) {
				this.profile.getProfileColorOption().changeValue(color);
				repository().save(this.profile);

				this.reopenProfile(viewer);

				ChatMessenger.send(
								viewer,
								"El color de tu perfil ahora es <white>%s<white>."
												.formatted(ColorSelectionInventoryHandler.colorName(color))
				);
		}

		private void reopenProfile(Player viewer) {
				HardlandsInventory.PLAYER_PROFILE.openInventory(viewer, this);
		}

		private static ItemStack group(ItemBuilder builder, String name, Component... lore) {
				return builder
								.name(Component.text(TextFormatters.TINY_CAPS.format(name), HardlandsColor.HARDLANDS))
								.addLore(lore)
								.build();
		}

		private static Component statistic(Component icon, String name, Object value) {
				return icon
								.append(Component.space())
								.append(Component.text(name + ": ", NamedTextColor.GRAY))
								.append(Component.text(String.valueOf(value), NamedTextColor.WHITE));
		}

		private static Component playerStatistic(
						String name,
						@Nullable UUID playerId,
						List<PlayerRepository.PlayerInfo> players
		) {
				Component icon = playerId == null
								? Icon.PLAYER.component
								: TextFormatters.MINI_MESSAGE.format(
								"<white><head:%s></white>".formatted(playerId)
				);

				return statistic(icon, name, playerName(playerId, players));
		}

		private static Component sprite(String path) {
				return TextFormatters.MINI_MESSAGE.format(
								"<white><sprite:\"minecraft:items\":\"minecraft:%s\"></white>"
												.formatted(path)
				);
		}

		private DyeColor getProfileColor() {
				DyeColor color = this.profile.getProfileColorOption().getValue();

				if (color == null) {
						throw new IllegalStateException("Player profile color has not been initialized: " + this.profile.getUniqueId());
				}

				return color;
		}

		private boolean isOwner(Player viewer) {
				return viewer.getUniqueId().equals(this.profile.getUniqueId());
		}

		private static @Nullable HardlandsPlayer getPlayer(UUID uuid) {
				HardlandsPlayer player = Hardlands.getInstance().getPlayerManager().get(uuid);

				return player != null ? player : repository().load(uuid).orElse(null);
		}

		private static String playerName(
						@Nullable UUID playerId,
						List<PlayerRepository.PlayerInfo> players
		) {
				if (playerId == null) {
						return "Ninguno";
				}

				for (PlayerRepository.PlayerInfo player : players) {
						if (player.uuid().equals(playerId)) {
								return player.name();
						}
				}

				return "Desconocido";
		}

		private static int value(@Nullable Integer value) {
				return value == null ? 0 : value;
		}

		private static String formatNumber(@Nullable Double value) {
				return value == null
								? "0"
								: BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
		}

		private static ItemStack outlineItem() {
				return new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).name("").build();
		}

		private static PlayerRepository repository() {
				return Hardlands.getInstance().getRepositories().player();
		}

		private static int previousSlot(Inventory inventory) {
				return inventory.getSize() - 6;
		}

		private static int personalizationSlot(Inventory inventory) {
				return inventory.getSize() - 5;
		}

		private static int nextSlot(Inventory inventory) {
				return inventory.getSize() - 4;
		}

		private enum Icon {

				STATUS("item/ender_eye"),
				TEAM("item/lead"),
				GAMES("item/book"),
				WINS("item/nether_star"),
				SURVIVAL("item/clock_00"),
				KILLS("item/iron_sword"),
				DEATHS("item/bone"),
				IRON_MAN("item/iron_chestplate"),
				KILL_TOP("item/diamond_sword"),
				PAPER_MAN("item/paper"),
				PRESET("item/written_book"),
				PLAYER("item/player_head");

				private final Component component;

				Icon(String path) {
						this.component = sprite(path);
				}
		}
}