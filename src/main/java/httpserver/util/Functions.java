package httpserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

public enum Functions {;

    public static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    public static String orDefault(final String value, final String defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        return value;
    }

    public static <T> T requireNotNull(final T input, final String message) {
        if (input == null) throw new IllegalArgumentException(message);
        return input;
    }

    public static <T> T requireTrue(final boolean valid, final T value, final String message) {
        if (!valid) throw new IllegalArgumentException(message);
        return value;
    }

    public static int rangeBound(final int minimum, final int maximum, final int value) {
        if (value < minimum) return minimum;
        if (value > maximum) return maximum;
        return value;
    }
    public static long rangeBound(final long minimum, final long maximum, final long value) {
        if (value < minimum) return minimum;
        if (value > maximum) return maximum;
        return value;
    }

}
