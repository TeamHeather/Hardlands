package team.heather.hardlands.common.ui.inventory.handler;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;

import java.util.*;
import java.util.function.Consumer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
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
		private final Consumer<Player> backHandler;

		private HardlandsPlayer profile;
		private int currentIndex;

		public PlayerProfileInventoryHandler(
						HardlandsPlayer profile,
						List<PlayerRepository.PlayerInfo> navigation,
						int currentIndex,
						Consumer<Player> backHandler
		) {
				this.profile = profile;
				this.navigation = navigation;
				this.currentIndex = currentIndex;
				this.originIndex = currentIndex;
				this.backHandler = backHandler;
		}

		@Override
		public void render(Inventory inventory) {
				HardlandsInventory.renderOutline(
								inventory,
								new ItemBuilder(Material.valueOf(this.getProfileColor().name() + "_STAINED_GLASS_PANE")).name("").build()
				);

				inventory.setItem(PROFILE_SLOT, this.createProfileItem());
				inventory.setItem(STATISTICS_SLOT, this.createStatisticGroup(
								Material.NETHER_STAR,
								"Estadísticas",
								PlayerStatistic.Group.GENERAL,
								PlayerStatistic.Group.COMBAT,
								PlayerStatistic.Group.AWARDS
				));
				inventory.setItem(CAREER_SLOT, this.createStatisticGroup(
								Material.CLOCK,
								"Trayectoria",
								PlayerStatistic.Group.CAREER
				));
				inventory.setItem(PREFERENCES_SLOT, this.createPreferencesItem());
				inventory.setItem(previousSlot(inventory), InventoryItem.PREVIOUS.build());
				inventory.setItem(nextSlot(inventory), InventoryItem.NEXT.build());
		}

		@Override
		public void onOpen(Inventory inventory, Player viewer) {
				if (this.isOwner(viewer)) {
						inventory.setItem(personalizationSlot(inventory), this.createPersonalizationItem());
				}
		}

		@Override
		public Optional<Boolean> onClick(InventoryClickEvent event, Player viewer) {
				Inventory inventory = event.getView().getTopInventory();
				int slot = event.getRawSlot();

				if (slot == previousSlot(inventory)) {
						if (this.currentIndex == this.originIndex) {
								this.backHandler.accept(viewer);
								return Optional.of(true);
						}

						return Optional.of(this.navigate(viewer, inventory, -1));
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

		private boolean navigate(Player viewer, Inventory inventory, int offset) {
				int targetIndex = this.currentIndex + offset;

				if (targetIndex < this.originIndex || targetIndex >= this.navigation.size()) {
						return false;
				}

				HardlandsPlayer target = Hardlands.getInstance().getPlayerManager().get(this.navigation.get(targetIndex).uuid());

				if (target == null) {
						return false;
				}

				this.profile = target;
				this.currentIndex = targetIndex;

				this.render(inventory);
				this.onOpen(inventory, viewer);

				return true;
		}

		private ItemStack createProfileItem() {
				TextColor color = this.getTextColor();
				String team = Hardlands.getInstance().getGameManager().getTeamManager().get(this.profile.getUniqueId());

				return new ItemBuilder(Material.PLAYER_HEAD)
								.skullOwner(this.profile.getName())
								.name(Component.text(TextFormatters.TINY_CAPS.format(this.profile.getName()), color))
								.addLore(
												property("Estado", this.profile.getPlayer() == null ? "Desconectado" : "Conectado", color),
												property("Equipo", team == null ? "Sin equipo" : team, color)
								)
								.build();
		}

		private ItemStack createStatisticGroup(
						Material material,
						String name,
						PlayerStatistic.Group... groups
		) {
				TextColor color = this.getTextColor();
				Set<String> pinned = this.getPinnedStatistics();
				List<Component> lore = new ArrayList<>();

				for (int groupIndex = 0; groupIndex < groups.length; groupIndex++) {
						PlayerStatistic.Group group = groups[groupIndex];

						if (groups.length > 1) {
								if (!lore.isEmpty()) {
										lore.add(Component.empty());
								}

								lore.add(Component.text(TextFormatters.TINY_CAPS.format(group.label()), color));
						}

						for (PlayerStatistic statistic : PlayerStatistic.all()) {
								if (statistic.group() == group) {
										lore.add(statistic.line(this.profile, color, pinned.contains(statistic.key())));
								}
						}
				}

				return new ItemBuilder(material)
								.name(Component.text(TextFormatters.TINY_CAPS.format(name), color))
								.addLore(lore.toArray(Component[]::new))
								.build();
		}

		private ItemStack createPreferencesItem() {
				DyeColor profileColor = this.getProfileColor();
				TextColor color = this.getTextColor();

				return new ItemBuilder(Material.COMPARATOR)
								.name(Component.text(TextFormatters.TINY_CAPS.format("Preferencias"), color))
								.addLore(
												statistic(
																sprite("item/%s_dye".formatted(profileColor.name().toLowerCase(Locale.ROOT))),
																"Color del perfil",
																ColorSelectionInventoryHandler.colorName(profileColor),
																color
												),
												property("HEX", HardlandsColor.profileHex(profileColor), color),
												property("Estadísticas fijadas", this.getPinnedStatistics().size(), color)
								)
								.build();
		}

		private ItemStack createPersonalizationItem() {
				DyeColor profileColor = this.getProfileColor();
				TextColor color = this.getTextColor();

				return new ItemBuilder(Material.valueOf(profileColor.name() + "_DYE"))
								.name(Component.text(TextFormatters.TINY_CAPS.format("Personalización"), color))
								.addLore(
												property("Color", ColorSelectionInventoryHandler.colorName(profileColor), color),
												property("Estadísticas fijadas", this.getPinnedStatistics().size(), color),
												Component.empty(),
												TextFormatters.HIGHLIGHT.format("{Clic} para personalizar tu perfil.", color)
								)
								.build();
		}

		private void showPersonalization(Player viewer) {
				TextColor color = this.getTextColor();

				ActionButton colorButton = ActionButton.create(
								Component.text("Color del perfil", color),
								Component.text(
												"%s · %s".formatted(
																ColorSelectionInventoryHandler.colorName(this.getProfileColor()),
																HardlandsColor.profileHex(this.getProfileColor())
												)
								),
								150,
								DialogAction.customClick(
												(_, audience) -> {
														if (audience instanceof Player player) {
																this.openColorSelection(player);
														}
												},
												ClickCallback.Options.builder().uses(1).build()
								)
				);

				ActionButton statisticsButton = ActionButton.create(
								Component.text("Estadísticas fijadas", color),
								Component.text("%d seleccionadas".formatted(this.getPinnedStatistics().size())),
								150,
								DialogAction.customClick(
												(_, audience) -> {
														if (audience instanceof Player player) {
																this.openStatisticSelection(player);
														}
												},
												ClickCallback.Options.builder().uses(1).build()
								)
				);

				viewer.showDialog(Dialog.create(builder -> builder
								.empty()
								.base(DialogBase.builder(Component.text("Personalización", color))
												.pause(false)
												.afterAction(DialogBase.DialogAfterAction.NONE)
												.body(List.of(
																DialogBody.plainMessage(
																				Component.text("Personaliza la apariencia y presentación pública de tu perfil.")
																)
												))
												.build())
								.type(DialogType.multiAction(
												List.of(colorButton, statisticsButton),
												createCloseButton(),
												2
								))));
		}

		private void openColorSelection(Player viewer) {
				viewer.closeDialog();

				HardlandsInventory.COLORS.openInventory(
								viewer,
								new ColorSelectionInventoryHandler(
												this.getProfileColor(),
												this::selectColor,
												this::reopenPersonalization
								)
				);
		}

		private void openStatisticSelection(Player viewer) {
				viewer.closeDialog();

				HardlandsInventory.PLAYER_STATISTICS.openInventory(
								viewer,
								new PlayerStatisticSelectionInventoryHandler(
												this.profile,
												this::selectStatistics,
												this::reopenPersonalization
								)
				);
		}

		private void selectColor(Player viewer, DyeColor color) {
				this.profile.getProfileColorOption().changeValue(color);
				repository().save(this.profile);

				this.reopenPersonalization(viewer);

				ChatMessenger.send(
								viewer,
								"El color de tu perfil ahora es [%s].".formatted(ColorSelectionInventoryHandler.colorName(color))
				);
		}

		private void selectStatistics(Player viewer, Set<String> statistics) {
				this.profile.getPinnedStatisticsOption().changeValue(new LinkedHashSet<>(statistics));
				repository().save(this.profile);
		}

		private void reopenPersonalization(Player viewer) {
				HardlandsInventory.PLAYER_PROFILE.openInventory(viewer, this);
				this.showPersonalization(viewer);
		}

		private DyeColor getProfileColor() {
				DyeColor color = this.profile.getProfileColorOption().getValue();
				return color == null ? DyeColor.RED : color;
		}

		private TextColor getTextColor() {
				return HardlandsColor.profile(this.getProfileColor());
		}

		private Set<String> getPinnedStatistics() {
				Set<String> statistics = this.profile.getPinnedStatisticsOption().getValue();
				return statistics == null ? Set.of() : statistics;
		}

		private boolean isOwner(Player viewer) {
				return viewer.getUniqueId().equals(this.profile.getUniqueId());
		}

		private static Component property(String name, Object value, TextColor color) {
				return Component.text(name + ": ", color).append(Component.text(String.valueOf(value), NamedTextColor.WHITE));
		}

		private static Component statistic(Component icon, String name, Object value, TextColor color) {
				return icon
								.append(Component.space())
								.append(property(name, value, color));
		}

		private static Component sprite(String path) {
				return TextFormatters.MINI_MESSAGE.format(
								"<white><sprite:\"minecraft:items\":\"minecraft:%s\"></white>".formatted(path)
				);
		}

		private static ActionButton createCloseButton() {
				return ActionButton.create(
								Component.text("Cerrar"),
								Component.text("Cerrar la personalización."),
								100,
								DialogAction.customClick(
												(_, audience) -> audience.closeDialog(),
												ClickCallback.Options.builder().uses(1).build()
								)
				);
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
}