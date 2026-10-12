package httpserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static httpserver.core.CacheControlStrategy.SET_NOTHING;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.Headers.LOCATION;
import static httpserver.core.StatusCode.FOUND;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.util.Functions.requireTrue;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class ResponseBuilder {

    public static ResponseBuilder respond(final HttpServerExchange exchange) {
        return new ResponseBuilder(exchange);
    }
    public static ResponseBuilder respond(final HttpServerExchange exchange, final Function<Object, String> toJson) {
        return new ResponseBuilder(exchange, toJson);
    }

    private final Function<Object, String> toJson;
    private final HttpServerExchange exchange;
    private CacheControlStrategy cacheStrategy = SET_NOTHING;
    // A list per name, because a response may legitimately carry the same header more than once
    // (Set-Cookie above all) and every value has to survive.
    private final Map<String, List<String>> headers = new LinkedHashMap<>();
    private int status = INTERNAL_SERVER_ERROR;

    private ResponseBuilder(final HttpServerExchange exchange) {
        this(exchange, null);
    }
    private ResponseBuilder(final HttpServerExchange exchange, final Function<Object, String> toJson) {
        this.exchange = exchange;
        this.toJson = toJson;
    }

    public ResponseBuilder status(final int status) {
        this.status = requireTrue(status >= 100 && status < 999, status, "Status must be a valid HTTP status code");
        return this;
    }
    public ResponseBuilder header(final String name, final String value) {
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }
    public ResponseBuilder header(final String name, final int value) {
        return header(name, Integer.toString(value));
    }
    public ResponseBuilder header(final String name, final long value) {
        return header(name, Long.toString(value));
    }
    public ResponseBuilder cache(final CacheControlStrategy strategy) {
        this.cacheStrategy = strategy;
        return this;
    }
    public ResponseBuilder contentType(final String type) {
        return singleValuedHeader(CONTENT_TYPE, type);
    }
    public ResponseBuilder contentType(final ContentType type) {
        return contentType(type.toString());
    }
    public ResponseBuilder redirect(final String url) {
        this.status = FOUND;
        return singleValuedHeader(LOCATION, url);
    }
    /** Replaces the value of a header; a null value clears it, which is what a missing header means. */
    private ResponseBuilder singleValuedHeader(final String name, final String value) {
        final var values = new ArrayList<String>();
        if (value != null) values.add(value);
        this.headers.put(name, values);
        return this;
    }

    private void preSend() {
        exchange.setStatusCode(status);
        cacheStrategy.apply(exchange);
        for (final var entry : headers.entrySet()) {
            // Replaced rather than appended, so the builder always wins over an earlier value
            // for the same name, while several builder values for it all get written.
            exchange.removeResponseHeader(entry.getKey());
            for (final String value : entry.getValue()) {
                exchange.addResponseHeader(entry.getKey(), value);
            }
        }
    }
    public void send(final String data) {
        preSend();
        exchange.send(data, UTF_8);
    }

    public void send(final byte[] data) {
        preSend();
        exchange.send(data);
    }
    public void send(final InputStream in) throws IOException {
        preSend();
        exchange.send(in.readAllBytes());
    }
    public void send(final Object object) {
        if (toJson == null) throw new IllegalStateException("Missing serializer");
        preSend();
        exchange.send(toJson.apply(object), UTF_8);
    }

    public void send() {
        preSend();
    }

}
