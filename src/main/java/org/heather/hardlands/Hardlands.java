package org.heather.hardlands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.heather.hardlands.common.command.HardlandsCommand;
import org.heather.hardlands.module.enchantment.EnchantmentListener;
import org.heather.hardlands.common.ui.inventory.InventoryListener;
import org.heather.hardlands.module.player.PlayerListener;
import org.heather.hardlands.core.SingleThreadScheduler;
import org.heather.hardlands.module.preset.PresetRepository;
import org.heather.hardlands.game.GameFlow;
import org.heather.hardlands.game.general.GeneralConfiguration;
import org.heather.hardlands.module.scenario.ScenarioManager;
import org.heather.hardlands.module.world.WorldManager;
import org.jetbrains.annotations.Nullable;

public final class Hardlands extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Nullable private static Hardlands instance;

    private final SingleThreadScheduler<Hardlands> singleThreadScheduler = new SingleThreadScheduler<>(this);
    private final GeneralConfiguration generalConfiguration = new GeneralConfiguration();
    private final PresetRepository presetRepository = new PresetRepository(this, "presets");
    private final ScenarioManager scenarioManager = new ScenarioManager(this);
    private final GameFlow gameFlow = new GameFlow(this);

    @Nullable private WorldManager worldManager;

    @Override
    public void onEnable() {
        super.getLogger().info("Initializing Hardlands...");

        super.getLogger().info("Initializing instance and WorldManager...");
        instance = this;
        this.worldManager = new WorldManager();

        super.getLogger().info("Loading default preset...");
        this.presetRepository.load("default");

        super.getLogger().info("Registering commands and listeners...");
        this.registerCommands(new HardlandsCommand());
        this.registerListeners(
                new PlayerListener(),
                new InventoryListener(),
                new EnchantmentListener()
        );

        super.getLogger().info(System.lineSeparator() + """
             _    _          _____  _____  _               _   _ _____   _____
            | |  | |   /\\   |  __ \\|  __ \\| |        /\\   | \\ | |  __ \\ / ____|
            | |__| |  /  \\  | |__) | |  | | |       /  \\  |  \\| | |  | | (___
            |  __  | / /\\ \\ |  _  /| |  | | |      / /\\ \\ | . ` | |  | |\\___ \\
            | |  | |/ ____ \\| | \\ \\| |__| | |____ / ____ \\| |\\  | |__| |____) |
            |_|  |_/_/    \\_\\_|  \\_\\_____/|______/_/    \\_\\_| \\_|_____/|_____/
            """);
        super.getLogger().info("Plugin successfully enabled.");
    }

    @Override
    public void onDisable() {
        super.getLogger().info("Disabling Hardlands...");

        super.getLogger().info("Closing SingleThreadScheduler...");
        this.singleThreadScheduler.close();

        super.getLogger().info("Plugin successfully disabled.");
    }

    public WorldManager getWorldManagerOrThrow() {
        if (this.worldManager == null) {
            throw new IllegalStateException("WorldManager has not been initialized.");
        }

        return this.worldManager;
    }

    //* Public API

    public static NamespacedKey createNamespacedKey(String key) {
        return NamespacedKey.fromString(key, instance);
    }

    public static Hardlands getInstance() {
        return instance;
    }

    public SingleThreadScheduler getSingleThreadScheduler() {
        return this.singleThreadScheduler;
    }

    public GeneralConfiguration getGeneralConfiguration() {
        return this.generalConfiguration;
    }

    public PresetRepository getPresetRepository() {
        return this.presetRepository;
    }

    public ScenarioManager getScenarioManager() {
        return this.scenarioManager;
    }

    public GameFlow getGameFlow() {
        return this.gameFlow;
    }

    //* Internal Class Utilities

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
}