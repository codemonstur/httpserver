package httpserver.core;

import httpserver.caching.CacheControlStrategy;
import httpserver.error.InvalidInput;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static httpserver.caching.CacheControlStrategy.SET_NOTHING;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.Headers.LOCATION;
import static httpserver.core.StatusCode.FOUND;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.util.Functions.requireNotNull;
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
    private final Map<String, String> headers = new HashMap<>();
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
        this.headers.put(name, value);
        return this;
    }
    public ResponseBuilder header(final String name, final int value) {
        this.headers.put(name, Integer.toString(value));
        return this;
    }
    public ResponseBuilder header(final String name, final long value) {
        this.headers.put(name, Long.toString(value));
        return this;
    }
    public ResponseBuilder cache(final CacheControlStrategy strategy) {
        this.cacheStrategy = strategy;
        return this;
    }
    public ResponseBuilder contentType(final String type) {
        this.headers.put(CONTENT_TYPE, type);
        return this;
    }
    public ResponseBuilder contentType(final ContentType type) {
        this.headers.put(CONTENT_TYPE, type.toString());
        return this;
    }
    public ResponseBuilder redirect(final String url) {
        this.status = FOUND;
        this.headers.put(LOCATION, url);
        return this;
    }

    private void preSend() {
        exchange.setStatusCode(status);
        cacheStrategy.apply(exchange);
        for (final var entry : headers.entrySet()) {
            exchange.setResponseHeader(entry.getKey(), entry.getValue());
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
