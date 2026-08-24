package org.heather.hardlands.common.item;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerTextures;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.inventory.HardlandsInventory;
import org.heather.hardlands.core.config.Option;
import org.heather.hardlands.module.world.PregenerationManager;
import org.heather.hardlands.module.world.WorldManager;
import org.heather.hardlands.util.text.TextFormatter;

public enum InventoryItem {

    PREVIOUS(() -> createHeadItem(HeadTexture.ARROW_LEFT, "Anterior", "Regresa al {menú anterior}."), open(() -> HardlandsInventory.MAIN)),
    NEXT(() -> createHeadItem(HeadTexture.ARROW_RIGHT, "Siguiente", "Avanza a la {página siguiente}.")),
    PREPARATION(InventoryItem::createPreparationItem, InventoryItem::onClickPreparation),

    SCENARIOS(
            Material.CHERRY_SAPLING,
            "Escenarios",
            "Administra los {escenarios} de la partida.",
            open(() -> HardlandsInventory.SCENARIOS)),

    PLAYERS(
            Material.PLAYER_HEAD,
            "Jugadores",
            "Administra los {jugadores} participantes.",
            open(() -> HardlandsInventory.PLAYERS)),

    GENERAL(
            Material.COMPARATOR,
            "General",
            "Define las {reglas generales} de la partida.",
            open(() -> HardlandsInventory.GENERAL)),

    PHASES(
            Material.CLOCK,
            "Fases",
            "Define la {progresión} de la partida.",
            open(() -> HardlandsInventory.PHASES)),

    WORLD(
            HeadTexture.WORLD,
            "Mundo",
            "Configura los {mundos} y sus límites.",
            open(() -> HardlandsInventory.WORLD)),

    PRESETS(
            Material.WRITABLE_BOOK,
            "Plantillas",
            "Administra las {plantillas} disponibles.",
            open(() -> HardlandsInventory.PRESETS)),

    WORLD_WORLDS(InventoryItem::createWorldsItem),
    WORLD_BORDERS(InventoryItem::createWorldBordersItem),
    WORLD_SHRINK(InventoryItem::createWorldShrinkItem),
    WORLD_CENTER(InventoryItem::createWorldCenterItem),
    WORLD_BEHAVIOR(InventoryItem::createWorldBehaviorItem);

    private final Supplier<ItemStack> stackSupplier;
    private final Map<ClickType, ClickHandler> clickHandlers;

    private InventoryItem(Supplier<ItemStack> stackSupplier) {
        this(stackSupplier, Map.of());
    }

    private InventoryItem(Supplier<ItemStack> stackSupplier, ClickHandler handler) {
        this(stackSupplier, Map.of(ClickType.LEFT, handler));
    }

    private InventoryItem(Supplier<ItemStack> stackSupplier, Map<ClickType, ClickHandler> clickHandlers) {
        this.stackSupplier = stackSupplier;
        this.clickHandlers = clickHandlers;
    }

    private InventoryItem(Material material, String name, String description, ClickHandler handler) {
        this(() -> createDisplayStack(material, name, description, "", "{Clic} para abrir."), handler);
    }

    private InventoryItem(String texture, String name, String description, ClickHandler handler) {
        this(() -> createHeadItem(texture, name, description, "", "{Clic} para abrir."), handler);
    }

    public boolean onClick(InventoryClickEvent event) {
        ClickHandler handler = this.clickHandlers.get(event.getClick());
        return handler != null && handler.handle(event);
    }

    public ItemStack build() {
        return new ItemBuilder(this.stackSupplier.get()).setId(this.name()).build();
    }

    public static ItemStack createDisplayStack(Material material, String name, String... lore) {
        return createDisplayStack(new ItemBuilder(material), name, lore);
    }

    public static ItemStack createDisplayStack(ItemBuilder builder, String name, String... lore) {
        return builder.name(TextFormatter.formatTinyCaps(name)).formattedLore(lore).build();
    }

    public static Optional<InventoryItem> findByStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return Optional.empty();
        return new ItemBuilder(stack).findId().flatMap(InventoryItem::findByIdentifier);
    }

    private static ItemStack createPreparationItem() {
        WorldManager worldManager = Hardlands.getInstance().getWorldManagerOrThrow();
        PregenerationManager pregenerationManager = worldManager.getPregenerationManager();
        Option<Integer> survivalSize = worldManager.getSurvivalSizeOption();

        Integer size = survivalSize.getValue();
        PregenerationManager.State state = pregenerationManager.getState();
        float progress = pregenerationManager.getProgress();

        String border = survivalSize.isValid() && size != null
                ? "{%1$d × %1$d}".formatted(size)
                : "<gray>Sin configurar";

        ItemBuilder builder = new ItemBuilder(state.getMaterial())
                .name(TextFormatter.formatTinyCaps("Preparación"))
                .formattedLore(
                        "Aplica los {World Borders} e inicia la {pregeneración}.",
                        "",
                        "Borde: %s".formatted(border),
                        "Progreso: {%.1f%%}".formatted(progress),
                        "Estado: {%s}".formatted(state.getName()));

        switch (state) {
            case IDLE -> builder.addFormattedLore(
                    "",
                    worldManager.isConfigurationValid() ? "{Clic} para iniciar." : "{Configuración pendiente}.");
            case RUNNING -> builder.addFormattedLore("", "{Clic} para pausar.");
            case PAUSED -> builder.addFormattedLore("", "{Clic} para reanudar.");
            case COMPLETED -> {
            }
        }

        return builder.build();
    }

    private static boolean onClickPreparation(InventoryClickEvent event) {
        WorldManager worldManager = Hardlands.getInstance().getWorldManagerOrThrow();
        PregenerationManager pregenerationManager = worldManager.getPregenerationManager();

        switch (pregenerationManager.getState()) {
            case IDLE -> {
                if (!worldManager.isConfigurationValid()) return false;

                worldManager.configure();
                worldManager.pregenerate();
            }

            case RUNNING -> pregenerationManager.pause();
            case PAUSED -> pregenerationManager.resume();

            case COMPLETED -> {
                return false;
            }
        }

        event.setCurrentItem(PREPARATION.build());
        return true;
    }

    private static ItemStack createWorldsItem() {
        Set<String> worlds = Hardlands.getInstance().getWorldManagerOrThrow().getEnabledWorldsOption().getValue();
        String value = worlds == null ? "<gray>Sin configurar" : worlds.isEmpty() ? "<gray>Ninguno" : "{%s}".formatted(
                String.join(", ", worlds));

        return createDisplayStack(
                Material.GRASS_BLOCK,
                "Mundos",
                "Selecciona los {mundos} utilizados por la partida.",
                "",
                "Habilitados: %s".formatted(value),
                "",
                "{Clic} para configurar.");
    }

    private static ItemStack createWorldBordersItem() {
        WorldManager manager = Hardlands.getInstance().getWorldManagerOrThrow();

        return createDisplayStack(
                Material.STRUCTURE_VOID,
                "Límites",
                "Define los {World Borders} de cada fase.",
                "",
                "Supervivencia: %s".formatted(formatSize(manager.getSurvivalSizeOption().getValue())),
                "Meetup: %s".formatted(formatSize(manager.getMeetupSizeOption().getValue())),
                "Deathmatch: %s".formatted(formatSize(manager.getDeathmatchSizeOption().getValue())),
                "",
                "{Clic} para configurar.");
    }

    private static ItemStack createWorldShrinkItem() {
        WorldManager manager = Hardlands.getInstance().getWorldManagerOrThrow();

        return createDisplayStack(
                Material.CLOCK,
                "Reducción",
                "Define el tiempo de {reducción} de los límites.",
                "",
                "Meetup: %s".formatted(formatMinutes(manager.getMeetupShrinkTimeOption().getValue())),
                "Deathmatch: %s".formatted(formatMinutes(manager.getDeathmatchShrinkTimeOption().getValue())),
                "",
                "{Clic} para configurar.");
    }

    private static ItemStack createWorldCenterItem() {
        WorldManager manager = Hardlands.getInstance().getWorldManagerOrThrow();

        return createDisplayStack(
                Material.COMPASS,
                "Centro",
                "Establece el {centro} de los World Borders.",
                "",
                "X: %s".formatted(formatNumber(manager.getCenterXOption().getValue())),
                "Z: %s".formatted(formatNumber(manager.getCenterZOption().getValue())),
                "",
                "{Clic} para configurar.");
    }

    private static ItemStack createWorldBehaviorItem() {
        WorldManager manager = Hardlands.getInstance().getWorldManagerOrThrow();

        return createDisplayStack(
                Material.COMPARATOR,
                "Comportamiento",
                "Configura el {comportamiento} de los mundos.",
                "",
                "Superficie: %s".formatted(formatBoolean(manager.getSurfaceTeleportOption().getValue())),
                "Daño exterior: %s".formatted(formatBoolean(manager.getBorderDamageOption().getValue())),
                "",
                "{Clic} para configurar.");
    }

    private static ItemStack createHeadItem(String texture, String name, String... lore) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(URI.create(texture).toURL());
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("Invalid head texture URL: " + texture, exception);
        }

        profile.setTextures(textures);

        return createDisplayStack(new ItemBuilder(Material.PLAYER_HEAD).profile(profile), name, lore);
    }

    private static String formatSize(Integer value) {
        return value == null ? "<gray>Sin configurar" : "{%1$d × %1$d}".formatted(value);
    }

    private static String formatMinutes(Integer value) {
        return value == null ? "<gray>Sin configurar" : "{%d min}".formatted(value);
    }

    private static String formatNumber(Double value) {
        if (value == null) return "<gray>Sin configurar";
        return "{%s}".formatted(BigDecimal.valueOf(value).stripTrailingZeros().toPlainString());
    }

    private static String formatBoolean(Boolean value) {
        if (value == null) return "<gray>Sin configurar";
        return value ? "{Activado}" : "{Desactivado}";
    }

    private static ClickHandler open(Supplier<HardlandsInventory> definition) {
        return event -> {
            if (!(event.getWhoClicked() instanceof Player player)) return false;

            definition.get().openInventory(player);
            return true;
        };
    }

    private static Optional<InventoryItem> findByIdentifier(String id) {
        try {
            return Optional.of(valueOf(id));
        } catch (IllegalArgumentException _) {
            return Optional.empty();
        }
    }

    private static final class HeadTexture {

        private static final String ARROW_LEFT =
                "https://textures.minecraft.net/texture/f7aacad193e2226971ed95302dba433438be4644fbab5ebf818054061667fbe2";
        private static final String ARROW_RIGHT =
                "https://textures.minecraft.net/texture/d34ef0638537222b20f480694dadc0f85fbe0759d581aa7fcdf2e43139377158";
        private static final String WORLD =
                "https://textures.minecraft.net/texture/7dbb333846de0cd29732d64da58324f043a1f81c985ce1473ea6052d6dc03278";

        private HeadTexture() {}
    }

    @FunctionalInterface
    public interface ClickHandler {

        boolean handle(InventoryClickEvent event);
    }
}