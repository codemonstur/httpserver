package httpserver.util;

import httpserver.error.InvalidInput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public enum Functions {;

    public static boolean isNullOrEmpty(final String value) {
        return value == null || value.isEmpty();
    }

    public static String orDefault(final String value, final String defaultValue) {
        if (value == null || value.isEmpty()) return defaultValue;
        return value;
    }

    public static <T> T requireNotNull(final T input, final String message) throws InvalidInput {
        if (input == null) throw new InvalidInput(message);
        return input;
    }

    public static <T> T requireTrue(final boolean valid, final T value, final String message) throws InvalidInput {
        if (!valid) throw new InvalidInput(message);
        return value;
    }

    public static long rangeBound(final long minimum, final long maximum, final long value) {
        if (value < minimum) return minimum;
        if (value > maximum) return maximum;
        return value;
    }

    public static void copy(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[8192];
        for (int read; (read = in.read(buffer)) != -1;) {
            out.write(buffer, 0, read);
        }
    }

}
