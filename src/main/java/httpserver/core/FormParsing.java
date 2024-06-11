package httpserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Encoding.decodeUrl;
import static httpserver.util.Strings.AMPERSAND;
import static httpserver.util.Strings.EMPTY;
import static java.nio.charset.StandardCharsets.US_ASCII;

public enum FormParsing {;

    public static Map<String, String> parseUrlEncodedForm(final HttpServerExchange exchange, final Charset charset) throws IOException {
        final String type = exchange.getRequestHeader(CONTENT_TYPE);
        if (!"application/x-www-form-urlencoded".equals(type))
            throw new IOException("Content type no url encoded form. Found '" + type + "' instead.");

        final var map = new HashMap<String, String>();
        final var fields = new String(exchange.getInputStream().readAllBytes(), charset).split(AMPERSAND);
        for (final String paramPair : fields) {
            final int equalsOffset = paramPair.indexOf(EQUALS);
            if (equalsOffset == -1) map.put(paramPair, EMPTY);
            else {
                final String paramName = decodeUrl(paramPair.substring(0, equalsOffset));
                final String paramValue = decodeUrl(paramPair.substring(equalsOffset + 1));
                map.put(paramName, paramValue);
            }
        }
        return map;
    }

    public static List<FormPart> parseMultipartForm(final String contentType, final int contentLength, final InputStream is) throws IOException {
        if (!contentType.startsWith("multipart/form-data")) throw new IOException("Not multipart form data");
        if (contentLength > 64 * 1024) throw new IOException("Form content too large");

        final byte[] boundary = ("--" + contentType.substring(contentType.indexOf('=') + 1)).getBytes(US_ASCII);
        final byte[] rawData = new byte[contentLength];
        final List<FormPart> parts = new ArrayList<>();

        int start = readUntil(rawData, 0, is, boundary); int end = -1;
        while (start != -1) {
            // read the \r\n directly after the boundary, and move the offset forward by two
            // so we can search for the next boundary. This is safe because the ending boundary
            // always has two dashes at the end.
            rawData[start  ] = (byte) is.read();
            rawData[start+1] = (byte) is.read();
            end = readUntil(rawData, start+2, is, boundary);
            if (end == -1) break; // there is no next boundary so we stop
            parts.add(new FormPart(rawData, start, end - boundary.length - 2));
            start = end;
        }

        return parts;
    }

    private static int readUntil(final byte[] data, int offset, final InputStream in, final byte[] boundary) throws IOException {
        int b; while ((b = in.read()) != -1) {
            data[offset++] = (byte) b;
            if (containsAt(data, boundary, offset)) return offset;
            if (offset > data.length) throw new IOException("Filled buffer but more data still available");
        }
        return -1;
    }

    public static int indexOf(final byte[] array, final byte[] target, final int start, final int end) {
        if (target.length == 0) return 0;

        int offset = start;
        while (offset < end) {
            if (containsAt(array, target, offset))
                return offset;
            offset++;
        }

        return -1;
    }

    private static boolean containsAt(final byte[] data, final byte[] match, final int offset) {
        if (offset > data.length) return false;
        if (match.length == 0) return true;
        if (offset - match.length < 0) return false;

        for (int i = 0; i < match.length; i++) {
            if (data[offset - match.length + i] != match[i])
                return false;
        }
        return true;
    }

}
