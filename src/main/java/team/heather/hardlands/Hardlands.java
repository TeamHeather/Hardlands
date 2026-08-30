package team.heather.hardlands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.feature.command.HardlandsCommand;
import team.heather.hardlands.core.SingleThreadScheduler;
import team.heather.hardlands.feature.item.ItemListener;
import team.heather.hardlands.feature.player.PlayerListener;
import team.heather.hardlands.feature.ui.inventory.InventoryListener;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.game.PresetRepository;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.module.enchantment.EnchantmentManager;
import team.heather.hardlands.module.scenario.ScenarioManager;

public final class Hardlands extends JavaPlugin {

    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Nullable private static Hardlands instance;

    private final SingleThreadScheduler<Hardlands> singleThreadScheduler = new SingleThreadScheduler<>(this);

    @Nullable private PresetRepository presetRepository;
    @Nullable private EnchantmentManager enchantmentManager;
    @Nullable private ScenarioManager scenarioManager;
    @Nullable private GameManager gameManager;
    @Nullable private WorldManager worldManager;

    @Override
    public void onEnable() {
        instance = this;

        this.presetRepository = new PresetRepository(this, "presets");
        this.enchantmentManager = new EnchantmentManager(this);
        this.scenarioManager = new ScenarioManager(this);
        this.gameManager = new GameManager(this);
        this.worldManager = new WorldManager();

        this.presetRepository.load("default");

        this.registerCommands(new HardlandsCommand());
        this.registerListeners(
                new PlayerListener(),
                new InventoryListener(),
                new ItemListener()
        );

        this.gameManager.initialize();

        getLogger().info(System.lineSeparator() + """
             _    _          _____  _____  _               _   _ _____   _____
            | |  | |   /\\   |  __ \\|  __ \\| |        /\\   | \\ | |  __ \\ / ____|
            | |__| |  /  \\  | |__) | |  | | |       /  \\  |  \\| | |  | | (___
            |  __  | / /\\ \\ |  _  /| |  | | |      / /\\ \\ | . ` | |  | |\\___ \\
            | |  | |/ ____ \\| | \\ \\| |__| | |____ / ____ \\| |\\  | |__| |____) |
            |_|  |_/_/    \\_\\_|  \\_\\_____/|______/_/    \\_\\_| \\_|_____/|_____/
            """);

        getLogger().info("Hardlands successfully enabled.");
    }

    @Override
    public void onDisable() {
        this.singleThreadScheduler.close();
        instance = null;

        getLogger().info("Hardlands successfully disabled.");
    }

    //* Public API

    public static NamespacedKey createKey(String path) {
        return NamespacedKey.fromString(path, getInstance());
    }

    public static Hardlands getInstance() {
        return requireInitialized(instance, "Hardlands");
    }

    public SingleThreadScheduler<Hardlands> getSingleThreadScheduler() {
        return this.singleThreadScheduler;
    }

    public PresetRepository getPresetRepository() {
        return requireInitialized(this.presetRepository, "PresetRepository");
    }

    public EnchantmentManager getEnchantmentManager() {
        return requireInitialized(this.enchantmentManager, "EnchantmentManager");
    }

    public ScenarioManager getScenarioManager() {
        return requireInitialized(this.scenarioManager, "ScenarioManager");
    }

    public GameManager getGameManager() {
        return requireInitialized(this.gameManager, "GameManager");
    }

    public WorldManager getWorldManager() {
        return requireInitialized(this.worldManager, "WorldManager");
    }

    //* Registration

    private void registerCommands(BaseCommand... commands) {
        PaperCommandManager commandManager = new PaperCommandManager(this);
        for (BaseCommand command : commands) commandManager.registerCommand(command);
    }

    private void registerListeners(Listener... listeners) {
        for (Listener listener : listeners) Bukkit.getPluginManager().registerEvents(listener, this);
    }

    private static <T> T requireInitialized(@Nullable T value, String name) {
        if (value == null) throw new IllegalStateException(name + " has not been initialized.");
        return value;
    }
}