package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.StatusCode.NOT_FOUND;

/**
 * The {@code MethodPathRouting} class.
 */
public class MethodPathRouting implements HttpHandler {

    /**
     * Returns the result of {@code routing}.
     *
     * @return the result
     */
    public static MethodPathRouting routing() {
        return new MethodPathRouting();
    }
    /**
     * Returns the result of {@code methodPathRouting}.
     *
     * @return the result
     */
    public static MethodPathRouting methodPathRouting() {
        return new MethodPathRouting();
    }

    private final Map<String, Map<String, HttpHandler>> routes = new HashMap<>();
    private HttpHandler fallback = exchange -> exchange.setStatusCode(NOT_FOUND);

    /**
     * Returns the result of {@code fallback}.
     *
     * @param fallback the fallback
     * @return the result
     */
    public MethodPathRouting fallback(final HttpHandler fallback) {
        this.fallback = fallback;
        return this;
    }
    /**
     * Returns the result of {@code head}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting head(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("HEAD", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    /**
     * Returns the result of {@code get}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting get(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("GET", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    /**
     * Returns the result of {@code put}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting put(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("PUT", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    /**
     * Returns the result of {@code post}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting post(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("POST", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    /**
     * Returns the result of {@code patch}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting patch(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("PATCH", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    /**
     * Returns the result of {@code delete}.
     *
     * @param path the path
     * @param handler the handler
     * @return the result
     */
    public MethodPathRouting delete(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("DELETE", s -> new HashMap<>()).put(path, handler);
        return this;
    }

    /**
     * Performs the {@code handleRequest} operation.
     *
     * @param exchange the exchange
     * @throws Exception if the operation fails
     */
    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final var map = routes.get(exchange.getRequestMethod());
        final var handler = map == null ? fallback : map.getOrDefault(exchange.getRequestPath(), fallback);
        handler.handleRequest(exchange);
    }


    /**
     * Creates a new instance.
     */
    public MethodPathRouting() {
    }
}
