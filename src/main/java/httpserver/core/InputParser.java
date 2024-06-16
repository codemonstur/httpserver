package httpserver.core;

import httpserver.error.InvalidInput;

import java.util.Map;
import java.util.regex.Pattern;

import static httpserver.util.Functions.rangeBound;
import static java.lang.String.copyValueOf;
import static java.lang.String.format;

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


    public static String getMandatoryString(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String param = exchange.getQueryParameter(parameter);
        if (param == null || param.isEmpty()) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return param;
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

}
