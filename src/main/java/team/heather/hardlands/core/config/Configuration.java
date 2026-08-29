package team.heather.hardlands.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.core.data.json.JsonConvertible;
import team.heather.hardlands.core.event.ConfigChangeEvent;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public abstract class Configuration implements JsonConvertible {

    private final Map<String, Option<?>> options = new LinkedHashMap<>();

    private String identifier;

    protected Configuration() {}

    protected Configuration(String identifier) {
        this.setConfigurationIdentifier(identifier);
    }

    @Override
    public final JsonElement toJson() {
        JsonObject json = new JsonObject();

        for (Option<?> option : this.options.values()) {
            if (!option.isValid()) continue;
            json.add(option.getKey(), Hardlands.GSON.toJsonTree(option.getValue()));
        }

        return json;
    }

    @Override
    public final void fromJson(JsonElement json) {
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
            Option<?> option = this.options.get(entry.getKey());
            if (option != null) deserializeOption(option, entry.getValue());
        }
    }

    public final String getConfigurationIdentifier() {
        if (this.identifier == null) throw new IllegalStateException("Configuration identifier has not been set");
        return this.identifier;
    }

    public final Map<String, Option<?>> getConfigurationOptions() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.options));
    }

    public boolean isConfigurationValid() {
        return this.options.values().stream().allMatch(Option::isValid);
    }

    protected final void setConfigurationIdentifier(String identifier) {
        if (this.identifier != null) throw new IllegalStateException("Configuration identifier is already set");
        this.identifier = identifier;
    }

    protected final <T> Option<T> registerOption(Option<T> option) {
        if (this.options.putIfAbsent(option.getKey(), option) != null) {
            throw new IllegalArgumentException("Option already registered: " + option.getKey());
        }

        option.setChangeListener((previousValue, newValue) ->
                this.callConfigurationChangeEvent(option.getKey(), previousValue, newValue));

        return option;
    }

    protected final <T> Option<T> registerOption(String key, Class<T> type) {
        return this.registerOption(new Option<>(key, type));
    }

    protected final <T> Option<T> registerOption(String key, Class<T> type, Predicate<T> validator) {
        return this.registerOption(new Option<>(key, type, validator));
    }

    protected final <T> Option<List<T>> registerList(String key, Class<T> elementType) {
        return this.registerOption(key, parameterizedType(List.class, elementType));
    }

    protected final <T> Option<List<T>> registerList(String key, Class<T> elementType, Predicate<List<T>> validator) {
        return this.registerOption(key, parameterizedType(List.class, elementType), validator);
    }

    protected final <T> Option<Set<T>> registerSet(String key, Class<T> elementType) {
        return this.registerOption(key, parameterizedType(Set.class, elementType));
    }

    protected final <T> Option<Set<T>> registerSet(String key, Class<T> elementType, Predicate<Set<T>> validator) {
        return this.registerOption(key, parameterizedType(Set.class, elementType), validator);
    }

    protected final <K, V> Option<Map<K, V>> registerMap(String key, Class<K> keyType, Class<V> valueType) {
        Option<Map<K, V>> option = new Option<>(
                key,
                parameterizedType(Map.class, keyType, valueType)
        );

        option.setValue(new LinkedHashMap<>());

        return registerOption(option);
    }

    protected final <K, V> Option<Map<K, V>> registerMap(
            String key,
            Class<K> keyType,
            Class<V> valueType,
            Predicate<Map<K, V>> validator
    ) {
        Option<Map<K, V>> option = new Option<>(
                key,
                parameterizedType(Map.class, keyType, valueType),
                validator
        );

        option.setValue(new LinkedHashMap<>());

        return registerOption(option);
    }

    private <T> Option<T> registerOption(String key, Type type) {
        return this.registerOption(new Option<>(key, type));
    }

    private <T> Option<T> registerOption(String key, Type type, Predicate<T> validator) {
        return this.registerOption(new Option<>(key, type, validator));
    }

    private void callConfigurationChangeEvent(String optionKey, Object previousValue, Object newValue) {
        Bukkit.getPluginManager().callEvent(new ConfigChangeEvent(this, optionKey, previousValue, newValue));
    }

    private static Type parameterizedType(Class<?> rawType, Type... typeArguments) {
        return TypeToken.getParameterized(rawType, typeArguments).getType();
    }

    private static <T> void deserializeOption(Option<T> option, JsonElement json) {
        option.setValue(Hardlands.GSON.fromJson(json, option.getDataType()));
    }
}