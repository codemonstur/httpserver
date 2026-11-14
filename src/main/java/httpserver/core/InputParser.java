package httpserver.core;

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

/**
 * The {@code InputParser} enum.
 */
public enum InputParser {;

    private static final Pattern PATTERN_VALID_EMAIL = Pattern.compile("[a-z0-9\\-_+\\.]+@[a-z0-9-\\.]+");

    /**
     * Returns the result of {@code getMandatoryString}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getMandatoryString(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return value;
    }
    /**
     * Returns the result of {@code getMandatoryInteger}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static int getMandatoryInteger(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Integer.parseInt(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryLong}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static long getMandatoryLong(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Long.parseLong(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryBoolean}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return whether the condition holds
     * @throws InvalidInput if the operation fails
     */
    public static boolean getMandatoryBoolean(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final String value = getMandatoryString(formData, parameter);
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    /**
     * Returns the result of {@code getMandatoryDouble}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static double getMandatoryDouble(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Double.parseDouble(getMandatoryString(formData, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a double", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryEmailAddress}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getMandatoryEmailAddress(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final var email = getMandatoryString(formData, parameter);
        if (!PATTERN_VALID_EMAIL.matcher(email).matches())
            throw new InvalidInput(format("Parameter '%s' does not contain a valid email address", parameter));
        return email;
    }
    /**
     * Returns the result of {@code getMandatoryEnum}.
     *
     * @param <T> the type parameter
     * @param formData the form data
     * @param enumClass the enum class
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static <T extends Enum<T>> T getMandatoryEnum(final Map<String, String> formData, final Class<T> enumClass, final String parameter) throws InvalidInput {
        final var param = getMandatoryString(formData, parameter);
        if (isNullOrEmpty(param)) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return toEnumValue(param, enumClass);
    }

    /**
     * Returns the result of {@code getOptionalString}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getOptionalString(final Map<String, String> formData, final String parameter, final String defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        return value == null ? defaultValue : value;
    }
    /**
     * Returns the result of {@code getOptionalInteger}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Integer getOptionalInteger(final Map<String, String> formData, final String parameter, final Integer defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getBoundedInteger}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param minimum the minimum
     * @param defaultValue the default value
     * @param maximum the maximum
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static int getBoundedInteger(final Map<String, String> formData, final String parameter, final int minimum, final int defaultValue, final int maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalInteger(formData, parameter, defaultValue));
    }
    /**
     * Returns the result of {@code getOptionalLong}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Long getOptionalLong(final Map<String, String> formData, final String parameter, final Long defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Long.parseLong(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getBoundedLong}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param minimum the minimum
     * @param defaultValue the default value
     * @param maximum the maximum
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static long getBoundedLong(final Map<String, String> formData, final String parameter, final long minimum, final long defaultValue, final long maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalLong(formData, parameter, defaultValue));
    }
    /**
     * Returns the result of {@code getOptionalDouble}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Double getOptionalDouble(final Map<String, String> formData, final String parameter, final Double defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getOptionalBoolean}.
     *
     * @param formData the form data
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return whether the condition holds
     * @throws InvalidInput if the operation fails
     */
    public static boolean getOptionalBoolean(final Map<String, String> formData, final String parameter, final boolean defaultValue) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    /**
     * Returns the result of {@code getOptionalEnum}.
     *
     * @param <T> the type parameter
     * @param formData the form data
     * @param enumClass the enum class
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static <T extends Enum<T>> T getOptionalEnum(final Map<String, String> formData, final Class<T> enumClass, final String parameter, final T defaultValue) throws InvalidInput {
        final var param = formData.get(parameter);
        return isNullOrEmpty(param) ? defaultValue : toEnumValue(param, enumClass);
    }


    /**
     * Returns the result of {@code getMandatoryString}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getMandatoryString(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null || value.isEmpty()) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return value;
    }
    /**
     * Returns the result of {@code getMandatoryInteger}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static int getMandatoryInteger(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Integer.parseInt(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryLong}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static long getMandatoryLong(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Long.parseLong(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryBoolean}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return whether the condition holds
     * @throws InvalidInput if the operation fails
     */
    public static boolean getMandatoryBoolean(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    /**
     * Returns the result of {@code getMandatoryDouble}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static double getMandatoryDouble(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        try { return Double.parseDouble(getMandatoryString(exchange, parameter)); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a double", parameter));
        }
    }
    /**
     * Returns the result of {@code getMandatoryEmailAddress}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getMandatoryEmailAddress(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final var email = getMandatoryString(exchange, parameter);
        if (!PATTERN_VALID_EMAIL.matcher(email).matches())
            throw new InvalidInput(format("Parameter '%s' does not contain a valid email address", parameter));
        return email;
    }
    /**
     * Returns the result of {@code getMandatoryEnum}.
     *
     * @param <T> the type parameter
     * @param exchange the exchange
     * @param enumClass the enum class
     * @param parameter the parameter
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static <T extends Enum<T>> T getMandatoryEnum(final HttpServerExchange exchange, final Class<T> enumClass, final String parameter) throws InvalidInput {
        final var param = getMandatoryString(exchange, parameter);
        if (isNullOrEmpty(param)) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return toEnumValue(param, enumClass);
    }


    /**
     * Returns the result of {@code getOptionalString}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static String getOptionalString(final HttpServerExchange exchange, final String parameter, final String defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        return value == null ? defaultValue : value;
    }
    /**
     * Returns the result of {@code getOptionalInteger}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Integer getOptionalInteger(final HttpServerExchange exchange, final String parameter, final Integer defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Integer.parseInt(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getBoundedInteger}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param minimum the minimum
     * @param defaultValue the default value
     * @param maximum the maximum
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static int getBoundedInteger(final HttpServerExchange exchange, final String parameter, final int minimum, final int defaultValue, final int maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalInteger(exchange, parameter, defaultValue));
    }
    /**
     * Returns the result of {@code getOptionalLong}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Long getOptionalLong(final HttpServerExchange exchange, final String parameter, final Long defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Long.parseLong(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getBoundedLong}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param minimum the minimum
     * @param defaultValue the default value
     * @param maximum the maximum
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static long getBoundedLong(final HttpServerExchange exchange, final String parameter, final long minimum, final long defaultValue, final long maximum) throws InvalidInput {
        return rangeBound(minimum, maximum, getOptionalLong(exchange, parameter, defaultValue));
    }
    /**
     * Returns the result of {@code getOptionalDouble}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
    public static Double getOptionalDouble(final HttpServerExchange exchange, final String parameter, final Double defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        try { return Double.parseDouble(value); }
        catch (final NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain an integer", parameter));
        }
    }
    /**
     * Returns the result of {@code getOptionalBoolean}.
     *
     * @param exchange the exchange
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return whether the condition holds
     * @throws InvalidInput if the operation fails
     */
    public static boolean getOptionalBoolean(final HttpServerExchange exchange, final String parameter, final boolean defaultValue) throws InvalidInput {
        final String value = exchange.getQueryParameter(parameter);
        if (value == null) return defaultValue;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new InvalidInput(format("Parameter '%s' must contain a boolean", parameter));
    }
    /**
     * Returns the result of {@code getOptionalEnum}.
     *
     * @param <T> the type parameter
     * @param exchange the exchange
     * @param enumClass the enum class
     * @param parameter the parameter
     * @param defaultValue the default value
     * @return the result
     * @throws InvalidInput if the operation fails
     */
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
