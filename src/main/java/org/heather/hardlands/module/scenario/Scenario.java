package org.heather.hardlands.module.scenario;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.config.Configuration;
import org.jetbrains.annotations.Nullable;

public abstract class Scenario extends Configuration implements Listener {

    @Nullable private Hardlands plugin;

    final void initializeScenario(final Hardlands plugin, String identifier) {
        if (this.plugin != null) {
            throw new IllegalStateException("Scenario is already initialized");
        }

        super.setConfigurationIdentifier(identifier);
        this.plugin = plugin;
    }

    final void enableScenario() {
        if (!this.canEnable()) {
            throw new IllegalStateException("Scenario configuration is invalid: " + this.getConfigurationIdentifier());
        }

        if (this.plugin == null) {
            throw new IllegalArgumentException("Scenario has not been initialized");
        }

        Bukkit.getPluginManager().registerEvents(this, this.plugin);
    }

    final void disableScenario() {
        HandlerList.unregisterAll(this);
    }

    public boolean canEnable() {
        return super.isConfigurationValid();
    }

    protected final Hardlands getPluginOrThrow() {
        if (this.plugin == null) {
            throw new IllegalStateException("Scenario has not been initialized");
        }

        return this.plugin;
    }
}