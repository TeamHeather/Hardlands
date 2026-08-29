package team.heather.hardlands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import team.heather.hardlands.common.command.HardlandsCommand;
import team.heather.hardlands.common.player.PlayerListener;
import team.heather.hardlands.common.ui.inventory.InventoryListener;
import team.heather.hardlands.core.SingleThreadScheduler;
import team.heather.hardlands.module.preset.PresetRepository;
import team.heather.hardlands.game.GameFlow;
import team.heather.hardlands.module.scenario.ScenarioManager;
import team.heather.hardlands.module.world.WorldManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class Hardlands extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Nullable private static Hardlands instance;

    private final SingleThreadScheduler<Hardlands> singleThreadScheduler = new SingleThreadScheduler<>(this);
    private final PresetRepository presetRepository = new PresetRepository(this, "presets");
    private final ScenarioManager scenarioManager = new ScenarioManager(this);
    private final GameFlow gameFlow = new GameFlow(this);

    @Nullable private WorldManager worldManager;

    @Override
    public void onEnable() {
        getLogger().info("Initializing Hardlands...");

        getLogger().info("Initializing instance and WorldManager...");
        setInstance(this);
        worldManager = new WorldManager();

        getLogger().info("Loading default preset...");
        presetRepository.load("default");

        getLogger().info("Registering commands and listeners...");
        registerCommands(new HardlandsCommand());
        registerListeners(
                new PlayerListener(),
                new InventoryListener()
        );

        getLogger().info(System.lineSeparator() + """
             _    _          _____  _____  _               _   _ _____   _____
            | |  | |   /\\   |  __ \\|  __ \\| |        /\\   | \\ | |  __ \\ / ____|
            | |__| |  /  \\  | |__) | |  | | |       /  \\  |  \\| | |  | | (___
            |  __  | / /\\ \\ |  _  /| |  | | |      / /\\ \\ | . ` | |  | |\\___ \\
            | |  | |/ ____ \\| | \\ \\| |__| | |____ / ____ \\| |\\  | |__| |____) |
            |_|  |_/_/    \\_\\_|  \\_\\_____/|______/_/    \\_\\_| \\_|_____/|_____/
            """);
        getLogger().info("Plugin successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling Hardlands...");

        getLogger().info("Closing SingleThreadScheduler...");
        singleThreadScheduler.close();

        getLogger().info("Plugin successfully disabled.");
    }

    public WorldManager getWorldManagerOrThrow() {
        if (worldManager == null) {
            throw new IllegalStateException("WorldManager has not been initialized.");
        }

        return worldManager;
    }

    //! Public API

    public static NamespacedKey createKey(String path) {
        return NamespacedKey.fromString(path, instance);
    }

    @Nullable
    public static Hardlands getInstance() {
        return instance;
    }

    public SingleThreadScheduler<Hardlands> getSingleThreadScheduler() {
        return singleThreadScheduler;
    }

    public PresetRepository getPresetRepository() {
        return presetRepository;
    }

    public ScenarioManager getScenarioManager() {
        return scenarioManager;
    }

    public GameFlow getGameFlow() {
        return gameFlow;
    }

    //! Internal Class Utilities

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) {
            Bukkit.getPluginManager().registerEvents(listener, this);
        }
    }

    private void registerCommands(BaseCommand... commands) {
        PaperCommandManager commandManager = new PaperCommandManager(this);

        for (BaseCommand command : commands) {
            commandManager.registerCommand(command);
        }
    }

    private static void setInstance(@NotNull Hardlands instance) {
        Hardlands.instance = instance;
    }
}