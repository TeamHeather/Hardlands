package team.heather.hardlands;

import java.time.LocalTime;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.PaperCommandManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.common.command.HardlandsCommand;
import team.heather.hardlands.common.command.PhaseCommand;
import team.heather.hardlands.internal.data.InternalDefinitions;
import team.heather.hardlands.internal.ThreadScheduler;
import team.heather.hardlands.internal.data.json.LocalTimeAdapter;
import team.heather.hardlands.common.item.ItemListener;
import team.heather.hardlands.common.player.PlayerListener;
import team.heather.hardlands.game.GameListener;
import team.heather.hardlands.game.GameManager;
import team.heather.hardlands.internal.data.PresetRepository;
import team.heather.hardlands.game.world.WorldManager;
import team.heather.hardlands.module.enchantment.EnchantmentManager;
import team.heather.hardlands.module.scenario.ScenarioManager;
import team.heather.hardlands.common.ui.inventory.InventoryListener;
import team.heather.hardlands.common.ui.HardlandsColor;

public final class Hardlands extends JavaPlugin {

    public static final Component LABEL = MiniMessage.miniMessage().deserialize("ʜᴀʀᴅʟᴀɴᴅꜱ").color(HardlandsColor.HARDLANDS);
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter().nullSafe())
            .setPrettyPrinting()
            .create();


    private final ThreadScheduler<Hardlands> threadScheduler = new ThreadScheduler<>(this);

    @Nullable private InternalDefinitions internalDefinitions;
    @Nullable private EnchantmentManager enchantmentManager;
    @Nullable private ScenarioManager scenarioManager;
    @Nullable private WorldManager worldManager;
    @Nullable private GameManager gameManager;
    @Nullable private PresetRepository presetRepository;

    @Override
    public void onEnable() {

        this.internalDefinitions = new InternalDefinitions(this, "internal");
        this.enchantmentManager = new EnchantmentManager(this);
        this.scenarioManager = new ScenarioManager(this);
        this.worldManager = new WorldManager();
        this.gameManager = new GameManager(this);
        this.presetRepository = new PresetRepository(this, "presets");

        this.gameManager.start();
        this.internalDefinitions.load();
        this.presetRepository.load("default");

        this.registerCommands(
                new HardlandsCommand(),
                new PhaseCommand(this.getGameManager())
        );

        this.registerListeners(
                new PlayerListener(),
                new InventoryListener(),
                new ItemListener(),
                new GameListener()
        );

        super.getLogger().info("Hardlands successfully enabled.");
    }

    @Override
    public void onDisable() {
        if (this.gameManager != null) {
            this.gameManager.stop();
        }

        this.threadScheduler.close();

        super.getLogger().info("Hardlands successfully disabled.");
    }

    public static NamespacedKey createKey(String path) {
        return NamespacedKey.fromString(path, getInstance());
    }

    public static Hardlands getInstance() {
        return JavaPlugin.getPlugin(Hardlands.class);
    }

    public ThreadScheduler<Hardlands> getSingleThreadScheduler() {
        return this.threadScheduler;
    }

    public InternalDefinitions getInternalDefinitions() {
        return requireInitialized(this.internalDefinitions, "InternalDefinitions");
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

    public WorldManager getWorldManager() {
        return requireInitialized(this.worldManager, "WorldManager");
    }

    public GameManager getGameManager() {
        return requireInitialized(this.gameManager, "GameManager");
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

    private static <T> T requireInitialized(@Nullable T value, String name) {
        if (value == null) {
            throw new IllegalStateException(name + " has not been initialized yet.");
        }

        return value;
    }
}