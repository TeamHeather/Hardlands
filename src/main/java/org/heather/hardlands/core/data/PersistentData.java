package org.heather.hardlands.core.data;

import java.util.Optional;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public final class PersistentData {

    private PersistentData() {}

    public static <P, C> void set(
            PersistentDataHolder holder,
            NamespacedKey key,
            PersistentDataType<P, C> type,
            C value) {
        container(holder).set(key, type, value);
    }

    public static <P, C> Optional<C> find(PersistentDataHolder holder, NamespacedKey key, PersistentDataType<P, C> type) {
        return Optional.ofNullable(get(holder, key, type));
    }

    public static <P, C> C get(PersistentDataHolder holder, NamespacedKey key, PersistentDataType<P, C> type) {
        return container(holder).get(key, type);
    }

    public static <P, C> C getOrDefault(
            PersistentDataHolder holder,
            NamespacedKey key,
            PersistentDataType<P, C> type,
            C defaultValue
    ) {
        return container(holder).getOrDefault(key, type, defaultValue);
    }

    public static <P, C> boolean has(PersistentDataHolder holder, NamespacedKey key, PersistentDataType<P, C> type) {
        return container(holder).has(key, type);
    }

    public static boolean has(PersistentDataHolder holder, NamespacedKey key) {
        return container(holder).has(key);
    }

    public static void remove(PersistentDataHolder holder, NamespacedKey key) {
        container(holder).remove(key);
    }

    private static PersistentDataContainer container(PersistentDataHolder holder) {
        return holder.getPersistentDataContainer();
    }
}
