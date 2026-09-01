package team.heather.hardlands.module.scenario;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.config.Configuration;

import java.util.UUID;

public abstract class ScenarioProcessor extends Configuration implements Listener {

    @Nullable private Hardlands plugin;

    protected ScenarioProcessor() {
        super(null);
    }

    protected boolean onEnableValidation() {
        return true;
    }

    public final boolean canEnable() {
        return super.isConfigValid() && this.onEnableValidation();
    }

    final void initializeScenario(Hardlands plugin, String identifier) {
        if (this.plugin != null) {
            throw new IllegalStateException("Scenario is already initialized");
        }

        setConfigName(identifier);
        this.plugin = plugin;
    }

    final void enableScenario() {
        if (!canEnable()) {
            throw new IllegalStateException("Scenario configuration is invalid: " + this.getConfigName());
        }

        Bukkit.getPluginManager().registerEvents(this, getPluginOrThrow());
    }

    final void disableScenario() {
        HandlerList.unregisterAll(this);
    }

    protected final Hardlands getPluginOrThrow() {
        if (plugin == null) {
            throw new IllegalStateException("Scenario has not been initialized");
        }

        return plugin;
    }
}