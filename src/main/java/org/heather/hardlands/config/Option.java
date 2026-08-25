package org.heather.hardlands.config;

import java.lang.reflect.Type;
import java.util.function.Predicate;

public final class Option<T> {

    private final String key;
    private final Type dataType;
    private final Predicate<T> predicate;

    private T value;

    public Option(String key, Type dataType) {
        this(key, dataType, _ -> true);
    }

    public Option(String key, Type dataType, Predicate<T> predicate) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Option key cannot be null or blank");
        }

        if (dataType == null) {
            throw new IllegalArgumentException("Option data enchantment cannot be null");
        }

        if (predicate == null) {
            throw new IllegalArgumentException("Option predicate cannot be null");
        }

        this.key = key;
        this.dataType = dataType;
        this.predicate = predicate;
    }

    public boolean isValid() {
        return this.value != null && this.predicate.test(this.value);
    }

    public String getKey() {
        return this.key;
    }

    public Type getDataType() {
        return this.dataType;
    }

    public Predicate<T> getPredicate() {
        return this.predicate;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public T getValue() {
        return this.value;
    }

    public boolean hasValue() {
        return this.value != null;
    }
}
