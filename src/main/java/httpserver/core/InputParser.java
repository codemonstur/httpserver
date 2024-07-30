package httpserver.core;

import com.sun.source.tree.ReturnTree;
import httpserver.error.InvalidInput;

import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Pattern;

import static httpserver.util.Functions.isNullOrEmpty;
import static httpserver.util.Functions.rangeBound;
import static java.lang.String.copyValueOf;
import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public enum InputParser {;

    private static final Pattern PATTERN_VALID_EMAIL = Pattern.compile("[a-z0-9\\-_+\\.]+@[a-z0-9-\\.]+");

    public static String getMandatoryString(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return value;
    }
    public static int getMandatoryInteger(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Integer.parseInt(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static long getMandatoryLong(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Long.parseLong(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }
    public static boolean getMandatoryBoolean(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final String value = getMandatoryString(formData, parameter);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    public static double getMandatoryDouble(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Double.parseDouble(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a double", parameter));
        }
    }
    public static String getMandatoryEmailAddress(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final var email = getMandatoryString(formData, parameter);
        if (!PATTERN_VALID_EMAIL.matcher(email).matches())
            throw new InvalidInput("Parameter '" + parameter + "' does not contain a valid email address");
        return email;
    }
    public static <T extends Enum<T>> T getMandatoryEnum(final Map<String, String> formData, final Class<T> enumClass, final String parameter) throws InvalidInput {
        final var param = getMandatoryString(formData, parameter);
        if (isNullOrEmpty(param)) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return toEnumValue(param, enumClass);
    }

    public static String getOptionalString(final Map<String, String> formData, final String parameter, final String defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        return value == null ? defaultValue : value;
    }
    public static Integer getOptionalInteger(final Map<String, String> formData, final String parameter, final Integer defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static int getBoundedInteger(final Map<String, String> formData, final String parameter, final int minimum, final int defaultValue, final int maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalInteger(formData, parameter, defaultValue));
    }
    public static Long getOptionalLong(final Map<String, String> formData, final String parameter, final Long defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Long.parseLong(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static long getBoundedLong(final Map<String, String> formData, final String parameter, final long minimum, final long defaultValue, final long maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalLong(formData, parameter, defaultValue));
    }
    public static Double getOptionalDouble(final Map<String, String> formData, final String parameter, final Double defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static boolean getOptionalBoolean(final Map<String, String> formData, final String parameter, final boolean defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput("Parameter '" + parameter + "' must contain a boolean");
    }
    public static <T extends Enum<T>> T getOptionalEnum(final Map<String, String> formData, final Class<T> enumClass, final String parameter, final T defaultValue) throws InvalidInput {
        final var param = formData.get(parameter);
        return isNullOrEmpty(param) ? defaultValue : toEnumValue(param, enumClass);
    }


    public static String getMandatoryString(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null || value.isEmpty()) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return value;
    }
    public static int getMandatoryInteger(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Integer.parseInt(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static long getMandatoryLong(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Long.parseLong(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }
    public static boolean getMandatoryBoolean(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    public static double getMandatoryDouble(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Double.parseDouble(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a double", parameter));
        }
    }
    public static String getMandatoryEmailAddress(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final var email = getMandatoryString(exchange, parameter);
        if (!PATTERN_VALID_EMAIL.matcher(email).matches())
            throw new InvalidInput("Parameter '" + parameter + "' does not contain a valid email address");
        return email;
    }
    public static <T extends Enum<T>> T getMandatoryEnum(final HttpServerExchange exchange, final Class<T> enumClass, final String parameter) throws InvalidInput {
        final var param = getMandatoryString(exchange, parameter);
        if (isNullOrEmpty(param)) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return toEnumValue(param, enumClass);
    }


    public static String getOptionalString(final HttpServerExchange exchange, final String parameter, final String defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        return value == null ? defaultValue : value;
    }
    public static Integer getOptionalInteger(final HttpServerExchange exchange, final String parameter, final Integer defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static int getBoundedInteger(final HttpServerExchange exchange, final String parameter, final int minimum, final int defaultValue, final int maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalInteger(exchange, parameter, defaultValue));
    }
    public static Long getOptionalLong(final HttpServerExchange exchange, final String parameter, final Long defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Long.parseLong(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static long getBoundedLong(final HttpServerExchange exchange, final String parameter, final long minimum, final long defaultValue, final long maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalLong(exchange, parameter, defaultValue));
    }
    public static Double getOptionalDouble(final HttpServerExchange exchange, final String parameter, final Double defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    public static boolean getOptionalBoolean(final HttpServerExchange exchange, final String parameter, final boolean defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput("Parameter '" + parameter + "' must contain a boolean");
    }
    public static <T extends Enum<T>> T getOptionalEnum(final HttpServerExchange exchange, final Class<T> enumClass, final String parameter, final T defaultValue) throws InvalidInput {
        final var param = exchange.getQueryParameter(parameter);
        return isNullOrEmpty(param) ? defaultValue : toEnumValue(param, enumClass);
    }

    private static <T extends Enum<T>> T toEnumValue(final String value, final Class<T> enumClass) throws InvalidInput {
        try {
            return Enum.valueOf(enumClass, value);
        } catch (final Exception e) {
            final String names = Arrays
                    .stream(enumClass.getEnumConstants())
                    .map(Object::toString)
                    .collect(joining(", "));
            throw new InvalidInput(format("Invalid enum '%s', possible values are: %s", value, names));
        }
    }

}
