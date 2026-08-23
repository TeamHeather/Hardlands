package org.heather.hardlands.common.item;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.profile.PlayerTextures;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.common.inventory.InventoryDefinition;
import org.heather.hardlands.core.config.Option;
import org.heather.hardlands.module.world.PregenerationManager;
import org.heather.hardlands.module.world.WorldManager;
import org.heather.hardlands.util.text.TextFormatter;

public enum InventoryItem {

    PREVIOUS(
            HeadTexture.ARROW_LEFT,
            "Anterior",
            "Regresa al menú o página anterior.",
            open(() -> InventoryDefinition.MAIN)),

    NEXT(
            HeadTexture.ARROW_RIGHT,
            "Siguiente",
            "Avanza a la siguiente página."),

    PREPARATION(
            InventoryItem::createPreparationItem,
            Map.of(ClickType.LEFT, InventoryItem::onClickPreparation)),

    SCENARIOS(
            Material.CHERRY_SAPLING,
            "Escenarios",
            "Activa, desactiva y configura los escenarios de la partida.",
            open(() -> InventoryDefinition.SCENARIOS)),

    PLAYERS(
            Material.PLAYER_HEAD,
            "Jugadores",
            "Administra los jugadores de la partida.",
            open(() -> InventoryDefinition.PLAYERS)),

    GENERAL(
            Material.COMPARATOR,
            "General",
            "Configura las opciones generales de la partida.",
            open(() -> InventoryDefinition.GENERAL)),

    PHASES(
            Material.CLOCK,
            "Fases",
            "Configura la progresión y los tiempos de las fases de la partida.",
            open(() -> InventoryDefinition.PHASES)),

    WORLD(
            HeadTexture.WORLD,
            "Mundo",
            "Configura la generación y los límites del mundo.",
            open(() -> InventoryDefinition.WORLD)),

    PRESETS(
            Material.WRITABLE_BOOK,
            "Plantillas",
            "Administra las plantillas de configuración.",
            open(() -> InventoryDefinition.PRESETS)),

    SAVE_PRESET(
            Material.WRITABLE_BOOK,
            "Guardar preset",
            "Guarda la configuración actual del servidor como un preset."),

    ;

    private final Supplier<ItemStack> stackSupplier;
    private final Map<ClickType, ClickHandler> clickHandlers;

    InventoryItem(
            Supplier<ItemStack> stackSupplier,
            Map<ClickType, ClickHandler> clickHandlers
    ) {
        this.stackSupplier = stackSupplier;
        this.clickHandlers = clickHandlers;
    }

    InventoryItem(
            Material material,
            String name,
            String description,
            Map<ClickType, ClickHandler> clickHandlers
    ) {
        this(
                () -> createDisplayStack(material, name, description),
                clickHandlers);
    }

    InventoryItem(
            String textureUrl,
            String name,
            String description,
            Map<ClickType, ClickHandler> clickHandlers
    ) {
        this(
                () -> createHeadItem(textureUrl, name, description),
                clickHandlers);
    }

    InventoryItem(
            Material material,
            String name,
            String description,
            ClickHandler handler
    ) {
        this(
                material,
                name,
                description,
                Map.of(ClickType.LEFT, handler));
    }

    InventoryItem(
            Material material,
            String name,
            String description
    ) {
        this(
                material,
                name,
                description,
                Map.of());
    }

    InventoryItem(
            String textureUrl,
            String name,
            String description
    ) {
        this(
                textureUrl,
                name,
                description,
                Map.of());
    }

    InventoryItem(
            String textureUrl,
            String name,
            String description,
            ClickHandler handler
    ) {
        this(
                textureUrl,
                name,
                description,
                Map.of(ClickType.LEFT, handler));
    }

    public boolean onClick(InventoryClickEvent event) {
        ClickHandler handler = this.clickHandlers.get(event.getClick());

        return handler != null && handler.handle(event);
    }

    public ItemStack build() {
        return new ItemBuilder(this.stackSupplier.get())
                .setId(this.name())
                .build();
    }

    public static ItemStack createDisplayStack(
            ItemBuilder builder,
            String name,
            String description
    ) {
        return builder
                .name(name)
                .lore("<gray>" + description)
                .build();
    }

    public static ItemStack createDisplayStack(
            Material material,
            String name,
            String description
    ) {
        return createDisplayStack(
                new ItemBuilder(material),
                name,
                description);
    }

    public static Optional<InventoryItem> findByStack(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return Optional.empty();
        }

        return new ItemBuilder(stack)
                .findId()
                .flatMap(InventoryItem::findByIdentifier);
    }

    private static ItemStack createHeadItem(
            String textureUrl,
            String name,
            String description
    ) {
        PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
        PlayerTextures textures = profile.getTextures();

        try {
            textures.setSkin(URI.create(textureUrl).toURL());
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException(
                    "Invalid head texture URL: " + textureUrl,
                    exception);
        }

        profile.setTextures(textures);

        return createDisplayStack(
                new ItemBuilder(Material.PLAYER_HEAD)
                        .profile(profile),
                name,
                description);
    }

    // Preparation Item

    private static ItemStack createPreparationItem() {
        WorldManager worldManager =
                Hardlands.getInstance().getWorldManagerOrThrow();

        PregenerationManager pregenerationManager =
                worldManager.getPregenerationManager();

        Option<Integer> survivalSizeOption =
                worldManager.getSurvivalSizeOption();

        Integer borderSize = survivalSizeOption.getValue();

        PregenerationManager.State state =
                pregenerationManager.getState();

        float progress = pregenerationManager.getProgress();

        String borderSizeText =
                survivalSizeOption.isValid() && borderSize != null
                        ? "{%1$d × %1$d}".formatted(borderSize)
                        : "<gray>Inválido";

        String progressText =
                progress >= 100.0F
                        ? "{%.1f%%}".formatted(progress)
                        : "<gray>%.1f%%".formatted(progress);

        return new ItemBuilder(state.getMaterial())
                .name(TextFormatter.formatTinyCaps("Preparación"))
                .formattedLore(
                        "Establece los {World Borders} según lo configurado e inicia la {pregeneración} de los mundos abiertos.",
                        "",
                        "World Border: %s".formatted(borderSizeText),
                        "Progreso: %s".formatted(progressText))
                .addLore(
                        Component.text("Estado: ", NamedTextColor.WHITE)
                                .append(state.display()))
                .build();
    }

    private static boolean onClickPreparation(
            InventoryClickEvent event
    ) {
        WorldManager worldManager =
                Hardlands.getInstance().getWorldManagerOrThrow();

        PregenerationManager pregenerationManager =
                worldManager.getPregenerationManager();

        switch (pregenerationManager.getState()) {
            case IDLE -> {
                if (!worldManager.isConfigurationValid()) {
                    return false;
                }

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

    // Utilities

    private static ClickHandler open(
            Supplier<InventoryDefinition> definition
    ) {
        return event -> {
            if (!(event.getWhoClicked() instanceof Player player)) {
                return false;
            }

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