package org.heather.hardlands.core.configuration;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public record Validator<T>(String key, Predicate<T> predicate) implements Predicate<T> {

    public Validator {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Key cannot be null or blank");
        }

        if (predicate == null) {
            throw new IllegalArgumentException("Predicate cannot be null");
        }
    }

    @Override
    public boolean test(T value) {
        return this.predicate.test(value);
    }

    public boolean validate(T value) {
        return this.test(value);
    }

    public static <T> Validator<T> oneOf(Collection<? extends T> values) {
        if (values == null) {
            throw new IllegalArgumentException("Values cannot be null");
        }

        Set<? extends T> accepted = Set.copyOf(values);
        return new Validator<>(Keys.ONE_OF, accepted::contains);
    }

    public static final class Keys {

        public static final String AT_LEAST = "at-least";
        public static final String AT_MOST = "at-most";
        public static final String BETWEEN = "between";
        public static final String EVEN = "even";
        public static final String LENGTH_BETWEEN = "length-between";
        public static final String MATCHES = "matches";
        public static final String MAX_LENGTH = "max-length";
        public static final String MAX_SIZE = "max-size";
        public static final String MIN_LENGTH = "min-length";
        public static final String MIN_SIZE = "min-size";
        public static final String NEGATIVE = "negative";
        public static final String NON_BLANK = "non-blank";
        public static final String NON_EMPTY = "non-empty";
        public static final String NON_NEGATIVE = "non-negative";
        public static final String NON_POSITIVE = "non-positive";
        public static final String ODD = "odd";
        public static final String ONE_OF = "one-of";
        public static final String POSITIVE = "positive";
        public static final String SIZE_BETWEEN = "size-between";
        public static final String UNIT_INTERVAL = "unit-interval";

        private Keys() {}
    }

    public static final class Collections {

        public static final Validator<Collection<?>> NON_EMPTY = new Validator<>(Keys.NON_EMPTY, value -> !value.isEmpty());

        private Collections() {}

        public static Validator<Collection<?>> minSize(int minimum) {
            requireNonNegative(minimum, "Minimum size");
            return new Validator<>(parameterizedKey(Keys.MIN_SIZE, minimum), value -> value.size() >= minimum);
        }

        public static Validator<Collection<?>> maxSize(int maximum) {
            requireNonNegative(maximum, "Maximum size");
            return new Validator<>(parameterizedKey(Keys.MAX_SIZE, maximum), value -> value.size() <= maximum);
        }

        public static Validator<Collection<?>> sizeBetween(int minimum, int maximum) {
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.SIZE_BETWEEN, minimum, maximum), value ->
                    value.size() >= minimum && value.size() <= maximum);
        }
    }

    public static final class Doubles {

        public static final Validator<Double> NEGATIVE = new Validator<>(Keys.NEGATIVE, value -> Double.isFinite(value) && value < 0.0);
        public static final Validator<Double> NON_NEGATIVE = new Validator<>(Keys.NON_NEGATIVE, value -> Double.isFinite(value) && value >= 0.0);
        public static final Validator<Double> NON_POSITIVE = new Validator<>(Keys.NON_POSITIVE, value -> Double.isFinite(value) && value <= 0.0);
        public static final Validator<Double> POSITIVE = new Validator<>(Keys.POSITIVE, value -> Double.isFinite(value) && value > 0.0);
        public static final Validator<Double> UNIT_INTERVAL = new Validator<>(Keys.UNIT_INTERVAL, value -> Double.isFinite(value) && value >= 0.0 && value <= 1.0);

        private Doubles() {}

        public static Validator<Double> atLeast(double minimum) {
            requireFinite(minimum);

            return new Validator<>(parameterizedKey(Keys.AT_LEAST, minimum), value ->
                    Double.isFinite(value) && value >= minimum);
        }

        public static Validator<Double> atMost(double maximum) {
            requireFinite(maximum);

            return new Validator<>(parameterizedKey(Keys.AT_MOST, maximum), value ->
                    Double.isFinite(value) && value <= maximum);
        }

        public static Validator<Double> between(double minimum, double maximum) {
            requireFinite(minimum);
            requireFinite(maximum);
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.BETWEEN, minimum, maximum), value ->
                    Double.isFinite(value) && value >= minimum && value <= maximum);
        }
    }

    public static final class Floats {
        public static final Validator<Float> NEGATIVE =
                new Validator<>(Keys.NEGATIVE, value -> Float.isFinite(value) && value < 0.0F);
        public static final Validator<Float> NON_NEGATIVE =
                new Validator<>(Keys.NON_NEGATIVE, value -> Float.isFinite(value) && value >= 0.0F);
        public static final Validator<Float> NON_POSITIVE =
                new Validator<>(Keys.NON_POSITIVE, value -> Float.isFinite(value) && value <= 0.0F);
        public static final Validator<Float> POSITIVE =
                new Validator<>(Keys.POSITIVE, value -> Float.isFinite(value) && value > 0.0F);
        public static final Validator<Float> UNIT_INTERVAL =
                new Validator<>(Keys.UNIT_INTERVAL, value -> Float.isFinite(value) && value >= 0.0F && value <= 1.0F);

        private Floats() {
        }

        public static Validator<Float> atLeast(float minimum) {
            requireFinite(minimum);

            return new Validator<>(parameterizedKey(Keys.AT_LEAST, minimum), value -> Float.isFinite(value)
                    && value >= minimum);
        }

        public static Validator<Float> atMost(float maximum) {
            requireFinite(maximum);

            return new Validator<>(parameterizedKey(Keys.AT_MOST, maximum), value -> Float.isFinite(value)
                    && value <= maximum);
        }

        public static Validator<Float> between(float minimum, float maximum) {
            requireFinite(minimum);
            requireFinite(maximum);
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.BETWEEN, minimum, maximum), value -> Float.isFinite(value)
                    && value >= minimum
                    && value <= maximum);
        }
    }

    public static final class Integers {
        public static final Validator<Integer> EVEN = new Validator<>(Keys.EVEN, value -> value % 2 == 0);
        public static final Validator<Integer> NEGATIVE = new Validator<>(Keys.NEGATIVE, value -> value < 0);
        public static final Validator<Integer> NON_NEGATIVE = new Validator<>(Keys.NON_NEGATIVE, value -> value >= 0);
        public static final Validator<Integer> NON_POSITIVE = new Validator<>(Keys.NON_POSITIVE, value -> value <= 0);
        public static final Validator<Integer> ODD = new Validator<>(Keys.ODD, value -> value % 2 != 0);
        public static final Validator<Integer> POSITIVE = new Validator<>(Keys.POSITIVE, value -> value > 0);

        private Integers() {
        }

        public static Validator<Integer> atLeast(int minimum) {
            return new Validator<>(parameterizedKey(Keys.AT_LEAST, minimum), value -> value >= minimum);
        }

        public static Validator<Integer> atMost(int maximum) {
            return new Validator<>(parameterizedKey(Keys.AT_MOST, maximum), value -> value <= maximum);
        }

        public static Validator<Integer> between(int minimum, int maximum) {
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.BETWEEN, minimum, maximum), value -> value >= minimum
                    && value <= maximum);
        }
    }

    public static final class Longs {
        public static final Validator<Long> NEGATIVE = new Validator<>(Keys.NEGATIVE, value -> value < 0L);
        public static final Validator<Long> NON_NEGATIVE = new Validator<>(Keys.NON_NEGATIVE, value -> value >= 0L);
        public static final Validator<Long> NON_POSITIVE = new Validator<>(Keys.NON_POSITIVE, value -> value <= 0L);
        public static final Validator<Long> POSITIVE = new Validator<>(Keys.POSITIVE, value -> value > 0L);

        private Longs() {
        }

        public static Validator<Long> atLeast(long minimum) {
            return new Validator<>(parameterizedKey(Keys.AT_LEAST, minimum), value -> value >= minimum);
        }

        public static Validator<Long> atMost(long maximum) {
            return new Validator<>(parameterizedKey(Keys.AT_MOST, maximum), value -> value <= maximum);
        }

        public static Validator<Long> between(long minimum, long maximum) {
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.BETWEEN, minimum, maximum), value -> value >= minimum
                    && value <= maximum);
        }
    }

    public static final class Maps {
        public static final Validator<Map<?, ?>> NON_EMPTY = new Validator<>(Keys.NON_EMPTY, value -> !value.isEmpty());

        private Maps() {
        }

        public static Validator<Map<?, ?>> minSize(int minimum) {
            requireNonNegative(minimum, "Minimum size");
            return new Validator<>(parameterizedKey(Keys.MIN_SIZE, minimum), value -> value.size() >= minimum);
        }

        public static Validator<Map<?, ?>> maxSize(int maximum) {
            requireNonNegative(maximum, "Maximum size");
            return new Validator<>(parameterizedKey(Keys.MAX_SIZE, maximum), value -> value.size() <= maximum);
        }

        public static Validator<Map<?, ?>> sizeBetween(int minimum, int maximum) {
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.SIZE_BETWEEN, minimum, maximum), value -> value.size() >= minimum
                    && value.size() <= maximum);
        }
    }

    public static final class Strings {
        public static final Validator<String> NON_BLANK = new Validator<>(Keys.NON_BLANK, value -> !value.isBlank());
        public static final Validator<String> NON_EMPTY = new Validator<>(Keys.NON_EMPTY, value -> !value.isEmpty());

        private Strings() {
        }

        public static Validator<String> minLength(int minimum) {
            requireNonNegative(minimum, "Minimum length");
            return new Validator<>(parameterizedKey(Keys.MIN_LENGTH, minimum), value -> value.length() >= minimum);
        }

        public static Validator<String> maxLength(int maximum) {
            requireNonNegative(maximum, "Maximum length");
            return new Validator<>(parameterizedKey(Keys.MAX_LENGTH, maximum), value -> value.length() <= maximum);
        }

        public static Validator<String> lengthBetween(int minimum, int maximum) {
            requireRange(minimum, maximum);

            return new Validator<>(parameterizedKey(Keys.LENGTH_BETWEEN, minimum, maximum), value -> value.length() >= minimum
                    && value.length() <= maximum);
        }

        public static Validator<String> matches(Pattern pattern) {
            Objects.requireNonNull(pattern, "pattern");
            return new Validator<>(parameterizedKey(Keys.MATCHES, pattern.pattern()), pattern.asMatchPredicate());
        }
    }

    private static String parameterizedKey(String key, Object... arguments) {
        StringBuilder builder = new StringBuilder(key);

        for (Object argument : arguments) {
            builder.append(':').append(argument);
        }

        return builder.toString();
    }

    private static void requireFinite(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Value must be finite");
        }
    }

    private static void requireRange(double minimum, double maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum cannot be greater than maximum");
        }
    }

    private static void requireRange(long minimum, long maximum) {
        if (minimum > maximum) {
            throw new IllegalArgumentException("Minimum cannot be greater than maximum");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " cannot be negative");
        }
    }
}
