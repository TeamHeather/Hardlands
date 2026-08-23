package org.heather.hardlands.core.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.heather.hardlands.Hardlands;
import org.heather.hardlands.core.data.json.JsonConvertible;

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

        this.options.values().forEach(option -> {
            if (!option.isValid()) {
                return;
            }

            json.add(
                    option.getKey(),
                    Hardlands.GSON.toJsonTree(option.getValue()));
        });

        return json;
    }

    @Override
    public void fromJson(JsonElement json) {
        json.getAsJsonObject().entrySet().forEach(entry -> {
            Option<?> option = this.options.get(entry.getKey());

            if (option == null) {
                return;
            }

            option.setValue(Hardlands.GSON.fromJson(
                    entry.getValue(),
                    option.getDataType()));
        });
    }

    public final Map<String, Option<?>> getConfigurationOptions() {
        return Collections.unmodifiableMap(
                new LinkedHashMap<>(this.options));
    }

    public final String getConfigurationIdentifier() {
        if (this.identifier == null) {
            throw new IllegalStateException(
                    "Configuration identifier has not been set");
        }

        return this.identifier;
    }

    protected final void setConfigurationIdentifier(String identifier) {
        if (this.identifier != null) {
            throw new IllegalStateException(
                    "Configuration identifier is already set");
        }

        this.identifier = identifier;
    }

    public boolean isConfigurationValid() {
        return this.options.values().stream()
                .allMatch(Option::isValid);
    }

    protected final <T> Option<T> registerOption(Option<T> option) {
        if (this.options.putIfAbsent(option.getKey(), option) != null) {
            throw new IllegalArgumentException(
                    "Option already registered: " + option.getKey());
        }

        return option;
    }

    protected final <T> Option<T> registerOption(
            String key,
            Class<T> type
    ) {
        return this.registerOption(new Option<>(key, type));
    }

    protected final <T> Option<T> registerOption(
            String key,
            Class<T> type,
            Predicate<T> validator
    ) {
        return this.registerOption(
                new Option<>(key, type, validator));
    }

    protected final <T> Option<List<T>> registerList(
            String key,
            Class<T> elementType
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        List.class,
                        elementType).getType());
    }

    protected final <T> Option<List<T>> registerList(
            String key,
            Class<T> elementType,
            Predicate<List<T>> validator
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        List.class,
                        elementType).getType(),
                validator);
    }

    protected final <T> Option<Set<T>> registerSet(
            String key,
            Class<T> elementType
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        Set.class,
                        elementType).getType());
    }

    protected final <T> Option<Set<T>> registerSet(
            String key,
            Class<T> elementType,
            Predicate<Set<T>> validator
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        Set.class,
                        elementType).getType(),
                validator);
    }

    protected final <K, V> Option<Map<K, V>> registerMap(
            String key,
            Class<K> keyType,
            Class<V> valueType
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        Map.class,
                        keyType,
                        valueType).getType());
    }

    protected final <K, V> Option<Map<K, V>> registerMap(
            String key,
            Class<K> keyType,
            Class<V> valueType,
            Predicate<Map<K, V>> validator
    ) {
        return this.registerOption(
                key,
                TypeToken.getParameterized(
                        Map.class,
                        keyType,
                        valueType).getType(),
                validator);
    }

    private <T> Option<T> registerOption(
            String key,
            Type type
    ) {
        return this.registerOption(
                new Option<>(key, type));
    }

    private <T> Option<T> registerOption(
            String key,
            Type type,
            Predicate<T> validator
    ) {
        return this.registerOption(
                new Option<>(key, type, validator));
    }
}