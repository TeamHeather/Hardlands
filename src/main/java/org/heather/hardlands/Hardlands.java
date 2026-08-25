package org.heather.hardlands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.heather.hardlands.common.command.HardlandsCommand;
import org.heather.hardlands.config.inventory.InventoryListener;
import org.heather.hardlands.common.player.PlayerListener;
import org.heather.hardlands.util.ThreadScheduler;
import org.heather.hardlands.module.PresetRepository;
import org.heather.hardlands.module.general.GeneralConfiguration;
import org.heather.hardlands.module.phase.PhaseController;
import org.heather.hardlands.module.scenario.ScenarioManager;
import org.heather.hardlands.module.world.WorldManager;
import org.jetbrains.annotations.Nullable;

public final class Hardlands extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final ThreadLocalRandom RANDOM = ThreadLocalRandom.current();

    private static Hardlands instance;

    private final GeneralConfiguration generalConfiguration = new GeneralConfiguration();
    private final PresetRepository presetRepository = new PresetRepository(this, "presets");
    private final ThreadScheduler threadScheduler = new ThreadScheduler(this);
    private final ScenarioManager scenarioManager = new ScenarioManager(this);
    private final PhaseController phaseController = new PhaseController(this);

    @Nullable private WorldManager worldManager;

    @Override
    public void onEnable() {
        super.getLogger().info("Initializing...");

        instance = this;
        this.worldManager = new WorldManager();

        this.presetRepository.load("default");

        this.registerListeners(new PlayerListener(), new InventoryListener());
        this.registerCommands(new HardlandsCommand());

        super.getLogger().info(System.lineSeparator() + """
             _    _          _____  _____  _               _   _ _____   _____
            | |  | |   /\\   |  __ \\|  __ \\| |        /\\   | \\ | |  __ \\ / ____|
            | |__| |  /  \\  | |__) | |  | | |       /  \\  |  \\| | |  | | (___
            |  __  | / /\\ \\ |  _  /| |  | | |      / /\\ \\ | . ` | |  | |\\___ \\
            | |  | |/ ____ \\| | \\ \\| |__| | |____ / ____ \\| |\\  | |__| |____) |
            |_|  |_/_/    \\_\\_|  \\_\\_____/|______/_/    \\_\\_| \\_|_____/|_____/
            """);

        super.getLogger().info("Initialized.");
    }

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

    @Override
    public void onDisable() {
        super.getLogger().info("Disabling...");

        this.threadScheduler.terminate();

        super.getLogger().info("Plugin successfully disabled.");
    }

    public static NamespacedKey namespacedKey(String key) {
        return NamespacedKey.fromString(key, instance);
    }

    public static Hardlands getInstance() {
        return instance;
    }

    public GeneralConfiguration getGeneralConfiguration() {
        return this.generalConfiguration;
    }

    public ThreadScheduler getThreadScheduler() {
        return this.threadScheduler;
    }

    public ScenarioManager getScenarioManager() {
        return this.scenarioManager;
    }

    public PhaseController getPhaseController() {
        return this.phaseController;
    }

    public PresetRepository getPresetRepository() {
        return this.presetRepository;
    }

    public WorldManager getWorldManagerOrThrow() {
        if (this.worldManager == null) {
            throw new IllegalStateException("WorldManager has not been initialized.");
        }

        return this.worldManager;
    }
}