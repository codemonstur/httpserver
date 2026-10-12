package httpserver.util;

import httpserver.error.MalformedInput;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.StringCharacterIterator;
import java.util.Base64;
import java.util.HexFormat;

import static httpserver.util.Strings.EMPTY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.text.CharacterIterator.DONE;

public enum Encoding {;

    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

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

    public static MessageDigest newSha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    // For HMAC see RFC 2104.
    // SHA-256 uses a block length of 64. The Java MessageDigest class has no way to get the block length from an
    // instance. So it is not possible to write a generic HMAC implementation at this time.
    public static byte[] hmacSha256(final byte[] data, final byte[] key) {
        final var digest = newSha256();
        final var blockLength = 64;

        // RFC 2104: a key longer than the block size is replaced by the hash of that key first.
        // Skipping this makes the MAC wrong for every key over 64 bytes.
        final byte[] effectiveKey = key.length > blockLength ? digest.digest(key) : key;

        final var opad = new byte[blockLength];
        final var ipad = new byte[blockLength];
        for (int i = 0; i < blockLength; i++) {
            final int k = i < effectiveKey.length ? effectiveKey[i] : 0;
            opad[i] = (byte)(k ^ 0x5c);
            ipad[i] = (byte)(k ^ 0x36);
        }

        digest.update(ipad);
        final byte[] inner = digest.digest(data);
        digest.update(opad);
        return digest.digest(inner);
    }

    public static String encodeHex(final byte[] bytes) {
        return HexFormat.of().formatHex(bytes);
    }

    public static String encodeUrl(final String value) {
        return value != null ? URLEncoder.encode(value, UTF_8) : EMPTY;
    }
    /**
     * @throws httpserver.error.MalformedInput when the value contains a broken percent escape.
     *         Throwing an HttpError means an invalid query string or form body is answered with
     *         a 400 rather than bubbling up as a 500 Internal Server Error.
     */
    public static String decodeUrl(final String value) {
        if (value == null) return EMPTY;
        try {
            return URLDecoder.decode(value, UTF_8);
        } catch (final IllegalArgumentException e) {
            throw new MalformedInput("Malformed URL encoding in request", e);
        }
    }

    public static String encodeBase64Url(final byte[] value) {
        return Base64.getUrlEncoder().encodeToString(value);
    }
    public static byte[] decodeBase64Url(final String text) {
        return Base64.getUrlDecoder().decode(text);
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
                default  :
                    // Every character below 0x20 must be escaped in JSON and some of them (0x00
                    // to 0x08, 0x0b, 0x0e to 0x1f) are not among the named escapes above.
                    // Emitting those raw produces invalid JSON that most parsers reject.
                    if (c < 0x20) {
                        ret.append("\\u00");
                        ret.append(HEX_DIGITS[(c >> 4) & 0xF]);
                        ret.append(HEX_DIGITS[c & 0xF]);
                    } else {
                        ret.append(c);
                    }
                    break;
            };
        }

        return ret.toString();
    }

}
