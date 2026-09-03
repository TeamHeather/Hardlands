package team.heather.hardlands.internal.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Option<T> {

    private final String key;
    private final Type dataType;
    private final Predicate<T> predicate;

    @NotNull private BiConsumer<T, T> changeListener;
    private T value;

    public Option(String key, Type dataType) {
        this(key, dataType, _ -> true);
    }

    public Option(String key, Type dataType, Predicate<T> predicate) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Option key cannot be null or blank");
        if (dataType == null) throw new IllegalArgumentException("Option data type cannot be null");
        if (predicate == null) throw new IllegalArgumentException("Option predicate cannot be null");

        this.key = key;
        this.dataType = dataType;
        this.predicate = predicate;

        this.changeListener = (_, _) -> {};
    }

    public void setChangeListener(@NonNull BiConsumer<T, T> changeListener) {
        this.changeListener = changeListener;
    }

    public void changeValue(T value) {
        if (this.value == value) return;

        T previousValue = this.value;
        this.value = value;
        this.changeListener.accept(previousValue, value);
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

    public T getValue() {
        return this.value;
    }

    public boolean isValid() {
        return this.value != null && this.predicate.test(this.value);
    }

    public boolean hasValue() {
        return this.value != null;
    }
}