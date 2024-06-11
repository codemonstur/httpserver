package httpserver.core;

import httpserver.error.InvalidInput;

import java.util.Map;

import static java.lang.String.format;

public enum InputParser {;

    public static String getMandatoryString(final Map<String, String> formData, final String parameter) throws InvalidInput {
        final String value = formData.get(parameter);
        if (value == null) throw new InvalidInput(format("Missing parameter '%s'", parameter));
        return value;
    }
    public static long getMandatoryLong(final Map<String, String> formData, final String parameter) throws InvalidInput {
        try { return Long.parseLong(getMandatoryString(formData, parameter)); }
        catch (NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }
    public static long getMandatoryLong(final HttpServerExchange exchange, final String parameter) throws InvalidInput {
        final String param = exchange.getQueryParameter(parameter);
        if (param == null || param.isEmpty()) throw new InvalidInput(format("Missing parameter '%s'", parameter));

        try { return Long.parseLong(param); }
        catch (NumberFormatException e) {
            throw new InvalidInput(format("Parameter '%s' must contain a long", parameter));
        }
    }

}
