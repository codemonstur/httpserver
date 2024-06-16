package httpserver.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.StringCharacterIterator;
import java.util.HexFormat;

import static httpserver.util.Strings.EMPTY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.CharacterIterator.DONE;

public enum Encoding {;

    public static byte[] sha256(final String data, final Charset charset) {
        return sha256(data.getBytes(charset));
    }
    public static byte[] sha256(final byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (final NoSuchAlgorithmException e) {
            // impossible, SHA256 has been included since like forever
            throw new RuntimeException(e);
        }
    }

    public static String encodeHex(final byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

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
