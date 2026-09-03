package team.heather.hardlands.internal.config;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import team.heather.hardlands.Hardlands;
import team.heather.hardlands.internal.json.JsonConvertible;
import team.heather.hardlands.internal.event.ConfigChangeEvent;

public abstract class Configuration implements JsonConvertible {

    private final Map<String, Option<?>> options;

    @Nullable
    private String name;

    protected Configuration(@Nullable String name) {
        this.options = new LinkedHashMap<>();
        this.name = name;
    }

    public final void setConfigName(@NotNull String identifier) {
        this.name = identifier;
    }

    public final @Nullable String getConfigName() {
        return this.name;
    }

    public final Map<String, Option<?>> getConfigOptions() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.options));
    }

    public final boolean isConfigValid() {
        return this.options.values()
                .stream()
                .allMatch(Option::isValid)
                && this.onConfigValidation();
    }

    // Abstraction

    protected boolean onConfigValidation() {
        return true;
    }

    // Registry

    protected final <T> Option<T> registerOption(Option<T> option) {
        if (this.options.putIfAbsent(option.getKey(), option) != null) {
            throw new IllegalArgumentException("Option already registered: " + option.getKey());
        }

        option.setChangeListener((previousValue, newValue) ->
                Bukkit.getPluginManager().callEvent(new ConfigChangeEvent(
                        this,
                        option.getKey(),
                        previousValue,
                        newValue
                )));

        return option;
    }

    protected final <T> Option<T> registerOption(String key, Type type) {
        return this.registerOption(new Option<>(key, type));
    }

    protected final <T> Option<T> registerOption(String key, Type type, Predicate<T> validator) {
        return this.registerOption(new Option<>(key, type, validator));
    }

    protected final <T> Option<T> registerOption(String key, Type type, T defaultValue) {
        return this.registerOption(new Option<>(key, type, defaultValue, _ -> true));
    }

    protected final <T> Option<T> registerOption(String key, Type type, T defaultValue, Predicate<T> validator) {
        return this.registerOption(new Option<>(key, type, defaultValue, validator));
    }

    protected final <T> Option<T> registerOption(String key, Class<T> type) {
        return this.registerOption(new Option<>(key, type));
    }

    protected final <T> Option<T> registerOption(String key, Class<T> type, Predicate<T> validator) {
        return this.registerOption(new Option<>(key, type, validator));
    }

    protected final <T> Option<T> registerOption(String key, Class<T> type, T defaultValue) {
        return this.registerOption(new Option<>(key, type, defaultValue, _ -> true));
    }

    protected final <T> Option<T> registerOption(
            String key,
            Class<T> type,
            T defaultValue,
            Predicate<T> validator
    ) {
        return this.registerOption(new Option<>(key, type, defaultValue, validator));
    }

    // Sets

    protected final <T> Option<Set<T>> registerSet(String key, Class<T> elementType) {
        return this.registerOption(key, parameterizedType(Set.class, elementType));
    }

    protected final <T> Option<Set<T>> registerSet(
            String key,
            Class<T> elementType,
            Predicate<Set<T>> validator
    ) {
        return this.registerOption(key, parameterizedType(Set.class, elementType), validator);
    }

    // Lists

    protected final <T> Option<List<T>> registerList(
            String key,
            Class<T> elementType,
            Predicate<List<T>> validator
    ) {
        return this.registerOption(
                key,
                parameterizedType(List.class, elementType),
                validator
        );
    }

    protected final <T> Option<List<T>> registerList(String key, Class<T> elementType) {
        return this.registerList(key, elementType, _ -> true);
    }

    // Maps

    protected final <K, V> Option<Map<K, V>> registerMap(
            String key,
            Class<K> keyType,
            Class<V> valueType,
            @NotNull Predicate<Map<K, V>> validator
    ) {
        Option<Map<K, V>> option = new Option<>(
                key,
                parameterizedType(Map.class, keyType, valueType),
                validator
        );

        option.changeValue(new LinkedHashMap<>());

        return this.registerOption(option);
    }

    protected final <K, V> Option<Map<K, V>> registerMap(
            String key,
            Class<K> keyType,
            Class<V> valueType
    ) {
        return this.registerMap(key, keyType, valueType, _ -> true);
    }

    @Override
    public final void fromJson(JsonElement json) {
        json.getAsJsonObject().entrySet().forEach(entry -> {
            Option<?> option = this.options.get(entry.getKey());

            if (option != null) {
                deserializeOption(option, entry.getValue());
            }
        });
    }

    @Override
    public final JsonElement toJson() {
        JsonObject json = new JsonObject();

        this.options.forEach((key, option) ->
                json.add(option.getKey(), Hardlands.GSON.toJsonTree(option.getValue())));

        return json;
    }

    // Internals

    private static <T> void deserializeOption(Option<T> option, JsonElement json) {
        option.changeValue(Hardlands.GSON.fromJson(json, option.getDataType()));
    }

    private static Type parameterizedType(Class<?> rawType, Type... typeArguments) {
        return TypeToken.getParameterized(rawType, typeArguments).getType();
    }
}