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

/**
 * The {@code ResponseBuilder} class.
 */
public final class ResponseBuilder {

    /**
     * Returns the result of {@code respond}.
     *
     * @param exchange the exchange
     * @return the result
     */
    public static ResponseBuilder respond(final HttpServerExchange exchange) {
        return new ResponseBuilder(exchange);
    }
    /**
     * Returns the result of {@code respond}.
     *
     * @param exchange the exchange
     * @param toJson the to json
     * @return the result
     */
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

    /**
     * Returns the result of {@code status}.
     *
     * @param status the status
     * @return the result
     */
    public ResponseBuilder status(final int status) {
        this.status = requireTrue(status >= 100 && status < 999, status, "Status must be a valid HTTP status code");
        return this;
    }
    /**
     * Returns the result of {@code header}.
     *
     * @param name the name
     * @param value the value
     * @return the result
     */
    public ResponseBuilder header(final String name, final String value) {
        this.headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        return this;
    }
    /**
     * Returns the result of {@code header}.
     *
     * @param name the name
     * @param value the value
     * @return the result
     */
    public ResponseBuilder header(final String name, final int value) {
        return header(name, Integer.toString(value));
    }
    /**
     * Returns the result of {@code header}.
     *
     * @param name the name
     * @param value the value
     * @return the result
     */
    public ResponseBuilder header(final String name, final long value) {
        return header(name, Long.toString(value));
    }
    /**
     * Returns the result of {@code cache}.
     *
     * @param strategy the strategy
     * @return the result
     */
    public ResponseBuilder cache(final CacheControlStrategy strategy) {
        this.cacheStrategy = strategy;
        return this;
    }
    /**
     * Returns the result of {@code contentType}.
     *
     * @param type the type
     * @return the result
     */
    public ResponseBuilder contentType(final String type) {
        return singleValuedHeader(CONTENT_TYPE, type);
    }
    /**
     * Returns the result of {@code contentType}.
     *
     * @param type the type
     * @return the result
     */
    public ResponseBuilder contentType(final ContentType type) {
        return contentType(type.toString());
    }
    /**
     * Returns the result of {@code redirect}.
     *
     * @param url the url
     * @return the result
     */
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
    /**
     * Performs the {@code send} operation.
     *
     * @param data the data
     */
    public void send(final String data) {
        preSend();
        exchange.send(data, UTF_8);
    }

    /**
     * Performs the {@code send} operation.
     *
     * @param data the data
     */
    public void send(final byte[] data) {
        preSend();
        exchange.send(data);
    }
    /**
     * Performs the {@code send} operation.
     *
     * @param in the in
     * @throws IOException if the operation fails
     */
    public void send(final InputStream in) throws IOException {
        preSend();
        exchange.send(in.readAllBytes());
    }
    /**
     * Performs the {@code send} operation.
     *
     * @param object the object
     */
    public void send(final Object object) {
        if (toJson == null) throw new IllegalStateException("Missing serializer");
        preSend();
        exchange.send(toJson.apply(object), UTF_8);
    }

    /**
     * Performs the {@code send} operation.
     */
    public void send() {
        preSend();
    }

}
