package httpserver.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.StringCharacterIterator;

import static httpserver.util.Strings.EMPTY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.CharacterIterator.DONE;

public enum Encoding {;

    public static String encodeUrl(final String value) {
        return value != null ? URLEncoder.encode(value, UTF_8) : EMPTY;
    }

    public static String decodeUrl(final String value) {
        return value != null ? URLDecoder.decode(value, UTF_8) : EMPTY;
    }

    public static String escapeJson(final String input) {
        if (input == null) return "";

        final var ret = new StringBuilder();
        final var iterator = new StringCharacterIterator(input);
        for (char c = iterator.current(); c != DONE; c = iterator.next()) {
            switch (c) {
                case '\"': ret.append("\\\""); break;
                case '\t': ret.append("\\t"); break;
                case '\f': ret.append("\\f"); break;
                case '\n': ret.append("\\n"); break;
                case '\r': ret.append("\\r"); break;
                case '\\': ret.append("\\\\"); break;
                case '/' : ret.append("\\/"); break;
                case '\b': ret.append("\\b"); break;
                default  : ret.append(c); break;
            };
        }

        return ret.toString();
    }

}
