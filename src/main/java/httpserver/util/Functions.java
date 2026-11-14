package httpserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * The {@code Functions} enum.
 */
public enum Functions {;

    /**
     * Returns the result of {@code isNullOrEmpty}.
     *
     * @param value the value
     * @return whether the condition holds
     */
    public static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    /**
     * Returns the result of {@code orDefault}.
     *
     * @param value the value
     * @param defaultValue the default value
     * @return the result
     */
    public static String orDefault(final String value, final String defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        return value;
    }

    /**
     * Returns the result of {@code requireNotNull}.
     *
     * @param <T> the type parameter
     * @param input the input
     * @param message the message
     * @return the result
     */
    public static <T> T requireNotNull(final T input, final String message) {
        if (input == null) throw new IllegalArgumentException(message);
        return input;
    }

    /**
     * Returns the result of {@code requireTrue}.
     *
     * @param <T> the type parameter
     * @param valid the valid
     * @param value the value
     * @param message the message
     * @return the result
     */
    public static <T> T requireTrue(final boolean valid, final T value, final String message) {
        if (!valid) throw new IllegalArgumentException(message);
        return value;
    }

    /**
     * Returns the result of {@code rangeBound}.
     *
     * @param minimum the minimum
     * @param maximum the maximum
     * @param value the value
     * @return the result
     */
    public static int rangeBound(final int minimum, final int maximum, final int value) {
        if (value < minimum) return minimum;
        if (value > maximum) return maximum;
        return value;
    }
    /**
     * Returns the result of {@code rangeBound}.
     *
     * @param minimum the minimum
     * @param maximum the maximum
     * @param value the value
     * @return the result
     */
    public static long rangeBound(final long minimum, final long maximum, final long value) {
        if (value < minimum) return minimum;
        if (value > maximum) return maximum;
        return value;
    }

}
