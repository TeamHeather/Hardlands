package team.heather.hardlands.internal.config;

import java.util.function.Predicate;

public class DefaultedOption<T> extends Option<T> {

    public DefaultedOption(String key, Class<T> dataType) {
        this(key, dataType, _ -> true);
    }

    public DefaultedOption(String key, Class<T> dataType, Predicate<T> predicate) {
        super(key, dataType, defaultValue(dataType), predicate);
    }

    private static <T> T defaultValue(Class<T> dataType) {
        if (dataType == Integer.class) {
            return dataType.cast(0);
        }

        if (dataType == String.class) {
            return dataType.cast("");
        }

        throw new IllegalArgumentException(
                "No default value is defined for data type: " + dataType.getTypeName()
        );
    }
}