package org.heather.hardlands.configuration;

import java.util.Locale;
import java.util.regex.Pattern;

final class ValidatorFormatter {

    private static final String LIST_TYPE = "java.util.List";
    private static final String SET_TYPE = "java.util.Set";
    private static final String MAP_TYPE = "java.util.Map";

    String format(String typeName, String[] validators) {
        if (validators.length == 0) return "";

        StringBuilder expression = new StringBuilder(this.formatValidator(typeName, validators[0]));

        for (int index = 1; index < validators.length; index++) {
            expression.append(".and(").append(this.formatValidator(typeName, validators[index])).append(')');
        }

        if (this.requiresGenericAdapter(typeName)) {
            return ", value -> " + expression + ".test(value)";
        }

        return ", " + expression;
    }

    private boolean requiresGenericAdapter(String typeName) {
        return typeName.equals(LIST_TYPE) || typeName.equals(SET_TYPE) || typeName.equals(MAP_TYPE);
    }

    private String formatValidator(String typeName, String specification) {
        int separator = specification.indexOf(':');
        String key = separator < 0 ? specification : specification.substring(0, separator);
        String arguments = separator < 0 ? null : specification.substring(separator + 1);

        return switch (this.validatorGroup(typeName)) {
            case "Integers" -> this.formatIntegerValidator(key, arguments);
            case "Longs" -> this.formatLongValidator(key, arguments);
            case "Floats" -> this.formatFloatValidator(key, arguments);
            case "Doubles" -> this.formatDoubleValidator(key, arguments);
            case "Strings" -> this.formatStringValidator(key, arguments);
            case "Collections" -> this.formatCollectionValidator(key, arguments);
            case "Maps" -> this.formatMapValidator(key, arguments);
            default -> throw new IllegalArgumentException("Unsupported validator group for type: " + typeName);
        };
    }

    private String formatIntegerValidator(String key, String arguments) {
        return switch (key) {
            case "even", "negative", "non-negative", "non-positive", "odd", "positive" ->
                    this.constant("Integers", key, arguments);
            case "at-least", "at-most" ->
                    this.factory("Integers", key, this.numericArguments(key, arguments, "Integer", 1));
            case "between" ->
                    this.factory("Integers", key, this.numericArguments(key, arguments, "Integer", 2));
            default -> throw this.unsupportedValidator(key, "Integer");
        };
    }

    private String formatLongValidator(String key, String arguments) {
        return switch (key) {
            case "negative", "non-negative", "non-positive", "positive" ->
                    this.constant("Longs", key, arguments);
            case "at-least", "at-most" ->
                    this.factory("Longs", key, this.numericArguments(key, arguments, "Long", 1));
            case "between" ->
                    this.factory("Longs", key, this.numericArguments(key, arguments, "Long", 2));
            default -> throw this.unsupportedValidator(key, "Long");
        };
    }

    private String formatFloatValidator(String key, String arguments) {
        return switch (key) {
            case "negative", "non-negative", "non-positive", "positive", "unit-interval" ->
                    this.constant("Floats", key, arguments);
            case "at-least", "at-most" ->
                    this.factory("Floats", key, this.numericArguments(key, arguments, "Float", 1));
            case "between" ->
                    this.factory("Floats", key, this.numericArguments(key, arguments, "Float", 2));
            default -> throw this.unsupportedValidator(key, "Float");
        };
    }

    private String formatDoubleValidator(String key, String arguments) {
        return switch (key) {
            case "negative", "non-negative", "non-positive", "positive", "unit-interval" ->
                    this.constant("Doubles", key, arguments);
            case "at-least", "at-most" ->
                    this.factory("Doubles", key, this.numericArguments(key, arguments, "Double", 1));
            case "between" ->
                    this.factory("Doubles", key, this.numericArguments(key, arguments, "Double", 2));
            default -> throw this.unsupportedValidator(key, "Double");
        };
    }

    private String formatStringValidator(String key, String arguments) {
        return switch (key) {
            case "non-blank", "non-empty" ->
                    this.constant("Strings", key, arguments);
            case "min-length", "max-length" ->
                    this.factory("Strings", key, this.numericArguments(key, arguments, "Integer", 1));
            case "length-between" ->
                    this.factory("Strings", key, this.numericArguments(key, arguments, "Integer", 2));
            case "matches" ->
                    this.patternValidator(arguments);
            default -> throw this.unsupportedValidator(key, "String");
        };
    }

    private String formatCollectionValidator(String key, String arguments) {
        return switch (key) {
            case "non-empty" ->
                    this.constant("Collections", key, arguments);
            case "min-size", "max-size" ->
                    this.factory("Collections", key, this.numericArguments(key, arguments, "Integer", 1));
            case "size-between" ->
                    this.factory("Collections", key, this.numericArguments(key, arguments, "Integer", 2));
            default -> throw this.unsupportedValidator(key, "Collection");
        };
    }

    private String formatMapValidator(String key, String arguments) {
        return switch (key) {
            case "non-empty" ->
                    this.constant("Maps", key, arguments);
            case "min-size", "max-size" ->
                    this.factory("Maps", key, this.numericArguments(key, arguments, "Integer", 1));
            case "size-between" ->
                    this.factory("Maps", key, this.numericArguments(key, arguments, "Integer", 2));
            default -> throw this.unsupportedValidator(key, "Map");
        };
    }

    private String constant(String group, String key, String arguments) {
        if (arguments != null) {
            throw new IllegalArgumentException("Validator '" + key + "' does not accept arguments");
        }

        return "Validator.%s.%s".formatted(group, this.constantName(key));
    }

    private String factory(String group, String key, String arguments) {
        return "Validator.%s.%s(%s)".formatted(group, toCamelCase(key), arguments);
    }

    private String patternValidator(String pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("Validator 'matches' requires a pattern");
        }

        Pattern.compile(pattern);
        return "Validator.Strings.matches(java.util.regex.Pattern.compile(%s))".formatted(quote(pattern));
    }

    private String numericArguments(String key, String arguments, String typeName, int count) {
        if (arguments == null) {
            throw new IllegalArgumentException("Validator '" + key + "' requires " + count + " argument(s)");
        }

        String[] values = arguments.split(":", -1);

        if (values.length != count) {
            throw new IllegalArgumentException("Validator '" + key + "' requires " + count + " argument(s)");
        }

        for (int index = 0; index < values.length; index++) {
            values[index] = this.numericLiteral(values[index], typeName);
        }

        return String.join(", ", values);
    }

    private String numericLiteral(String value, String typeName) {
        String normalized = value.trim();

        return switch (typeName) {
            case "Integer" -> Integer.toString(Integer.parseInt(normalized));
            case "Long" -> Long.parseLong(normalized) + "L";
            case "Float" -> {
                float number = Float.parseFloat(normalized);
                if (!Float.isFinite(number)) throw new IllegalArgumentException("Float validator argument must be finite");
                yield Float.toString(number) + "F";
            }
            case "Double" -> {
                double number = Double.parseDouble(normalized);
                if (!Double.isFinite(number)) throw new IllegalArgumentException("Double validator argument must be finite");
                yield Double.toString(number);
            }
            default -> throw new IllegalArgumentException("Unsupported numeric type: " + typeName);
        };
    }

    private String validatorGroup(String typeName) {
        return switch (typeName) {
            case "String" -> "Strings";
            case "Integer" -> "Integers";
            case "Long" -> "Longs";
            case "Float" -> "Floats";
            case "Double" -> "Doubles";
            case "java.util.Collection", LIST_TYPE, SET_TYPE -> "Collections";
            case MAP_TYPE -> "Maps";
            default -> throw new IllegalArgumentException("Validators are not supported for option type: " + typeName);
        };
    }

    private IllegalArgumentException unsupportedValidator(String key, String type) {
        return new IllegalArgumentException("Validator '" + key + "' is not supported for " + type);
    }

    private String constantName(String key) {
        return key.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static String toCamelCase(String value) {
        StringBuilder result = new StringBuilder();
        boolean uppercase = false;

        for (char character : value.toCharArray()) {
            if (character == '-') {
                uppercase = true;
                continue;
            }

            result.append(uppercase ? Character.toUpperCase(character) : character);
            uppercase = false;
        }

        return result.toString();
    }

    private static String quote(String value) {
        return '"' + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t") + '"';
    }
}