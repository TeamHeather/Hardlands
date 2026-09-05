package team.heather.hardlands;

import java.time.LocalTime;
import java.util.function.Function;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.common.command.HardlandsCommand;
import team.heather.hardlands.common.command.PhaseCommand;
import team.heather.hardlands.common.command.StaffCommand;
import team.heather.hardlands.common.item.ItemListener;
import team.heather.hardlands.common.player.PlayerListener;
import team.heather.hardlands.common.player.PlayerManager;
import team.heather.hardlands.common.ui.tablist.TabListListener;
import team.heather.hardlands.common.ui.HardlandsColor;
import team.heather.hardlands.common.ui.inventory.InventoryListener;
import team.heather.hardlands.common.ui.tablist.TabListRenderer;
import team.heather.hardlands.game.GameListener;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.internal.InternalDefinitions;
import team.heather.hardlands.internal.ThreadScheduler;
import team.heather.hardlands.internal.json.LocalTimeAdapter;
import team.heather.hardlands.internal.repository.Repositories;
import team.heather.hardlands.module.enchantment.EnchantmentManager;
import team.heather.hardlands.module.scenario.ScenarioManager;
import team.heather.hardlands.util.TextFormatters;

/**
 * Main Hardlands plugin entry point.
 *
 * <p>Components are initialized by dependency priority. Independent components
 * are ordered alphabetically, while dependent components are created only after
 * their required dependencies are available.</p>
 */
public final class Hardlands extends JavaPlugin {

    public static final Component LABEL = TextFormatters.MINI_MESSAGE.format("ʜᴀʀᴅʟᴀɴᴅꜱ").color(HardlandsColor.HARDLANDS);
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter().nullSafe())
            .setPrettyPrinting()
            .create();

    /** Asynchronous scheduler available throughout the plugin lifecycle. */
    private final ThreadScheduler<Hardlands> threadScheduler = new ThreadScheduler<>(this);

    @Nullable private InternalDefinitions internalDefinitions;
    @Nullable private EnchantmentManager enchantmentManager;
    @Nullable private ScenarioManager scenarioManager;
    @Nullable private WorldManager worldManager;
    @Nullable private GameManager gameManager;
    @Nullable private Repositories repositories;
    @Nullable private PlayerManager playerManager;

    @Override
    public void onEnable() {
        // Core
        this.internalDefinitions = new InternalDefinitions(this, "internal");
        this.internalDefinitions.load();

        // Independent systems
        this.enchantmentManager = new EnchantmentManager(this);
        this.scenarioManager = new ScenarioManager(this);
        this.worldManager = new WorldManager(this);

        // Dependent systems
        this.gameManager = this.initialize(GameManager::new, this.worldManager);
        this.repositories = this.initialize(
                Repositories::new,
                this.gameManager,
                this.scenarioManager,
                this.worldManager
        );
        this.playerManager = this.initialize(PlayerManager::new, this.repositories);

        // Persistent data
        this.repositories.preset().load("default");

        // Commands
        this.registerCommands(
                new HardlandsCommand(),
                new PhaseCommand(this.gameManager),
                new StaffCommand()
        );

        // Listeners
        this.registerListeners(
                new GameListener(),
                new InventoryListener(),
                new ItemListener(),
                new PlayerListener(),
                new TabListListener(this.gameManager, new TabListRenderer("MrPepe3012", HardlandsColor.profile(DyeColor.YELLOW)))
        );

        // Runtime
        this.gameManager.run();

        super.getLogger().info(System.lineSeparator() + """
         _    _          _____  _____  _               _   _ _____   _____
        | |  | |   /\\   |  __ \\|  __ \\| |        /\\   | \\ | |  __ \\ / ____|
        | |__| |  /  \\  | |__) | |  | | |       /  \\  |  \\| | |  | | (___
        |  __  | / /\\ \\ |  _  /| |  | | |      / /\\ \\ | . ` | |  | |\\___ \\
        | |  | |/ ____ \\| | \\ \\| |__| | |____ / ____ \\| |\\  | |__| |____) |
        |_|  |_/_/    \\_\\_|  \\_\\_____/|______/_/    \\_\\_| \\_|_____/|_____/
        """);
        super.getLogger().info("Hardlands successfully enabled.");
    }

    @Override
    public void onDisable() {
        if (this.gameManager != null) {
            this.gameManager.close();
        }

        this.threadScheduler.close();

        super.getLogger().info("Hardlands successfully disabled.");
    }

    public ThreadScheduler<Hardlands> getThreadScheduler() {
        return this.threadScheduler;
    }

    public @Nullable InternalDefinitions getInternalDefinitions() {
        return this.internalDefinitions;
    }

    public @Nullable EnchantmentManager getEnchantmentManager() {
        return this.enchantmentManager;
    }

    public @Nullable ScenarioManager getScenarioManager() {
        return this.scenarioManager;
    }

    public @Nullable WorldManager getWorldManager() {
        return this.worldManager;
    }

    public @Nullable GameManager getGameManager() {
        return this.gameManager;
    }

    public @Nullable Repositories getRepositories() {
        return this.repositories;
    }

    public @Nullable PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    /**
     * Creates a component only when all of its dependencies are initialized.
     */
    private <T> T initialize(Function<Hardlands, T> initializer, Object... dependencies) {
        for (Object dependency : dependencies) {
            if (dependency == null) {
                throw new IllegalStateException("Required dependency has not been initialized");
            }
        }

        return initializer.apply(this);
    }

    private void registerCommands(BaseCommand... commands) {
        PaperCommandManager commandManager = new PaperCommandManager(this);

        for (BaseCommand command : commands) {
            commandManager.registerCommand(command);
        }
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }


    public static Hardlands getInstance() {
        return JavaPlugin.getPlugin(Hardlands.class);
    }

    public static NamespacedKey createKey(String path) {
        return NamespacedKey.fromString(path, getInstance());
    }
}