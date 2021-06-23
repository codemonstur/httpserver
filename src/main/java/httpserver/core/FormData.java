package httpserver.core;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Encoding.decodeUrl;
import static httpserver.util.Strings.AMPERSAND;
import static httpserver.util.Strings.EMPTY;

public class FormData {

    public static FormData parseForm(final HttpServerExchange exchange, final Charset charset) throws IOException {
        final String type = exchange.getRequestHeader(CONTENT_TYPE);
        if ("application/x-www-form-urlencoded".equals(type)) {
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
            return new FormData(map);
        } else if ("multipart/form-data".equals(type)) {
            throw new IOException("Multipart form parsing not supported");
        }
        throw new IOException("Request body contains unknown form type: " + type);
    }

    private final Map<String, String> fields;
    private FormData(final Map<String, String> fields) {
        this.fields = fields;
    }

    public String get(final String parameter) {
        return fields.get(parameter);
    }

}
