package org.heather.hardlands.core.configuration;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class ConfigChangeEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Configuration configuration;
    private final String optionKey;
    private final Object previousValue;
    private final Object newValue;

    public ConfigChangeEvent(Configuration configuration, String optionKey, Object previousValue, Object newValue) {
        super(!Bukkit.isPrimaryThread());
        this.configuration = configuration;
        this.optionKey = optionKey;
        this.previousValue = previousValue;
        this.newValue = newValue;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public String getOptionKey() {
        return optionKey;
    }

    public Object getPreviousValue() {
        return previousValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}