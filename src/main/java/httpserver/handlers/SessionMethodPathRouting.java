package httpserver.handlers;

import httpserver.core.HttpServerExchange;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.StatusCode.NOT_FOUND;

/**
 * The {@code SessionMethodPathRouting} class.
 *
 * @param <T> the type parameter
 */
public class SessionMethodPathRouting<T> implements SessionHttpHandler<T> {

    /**
     * Returns the result of {@code sessionRouting}.
     *
     * @return the result
     */
    public static SessionMethodPathRouting sessionRouting() {
        return new SessionMethodPathRouting();
    }

    private final Map<String, Map<String, SessionHttpHandler>> routes = new HashMap<>();
    private SessionHttpHandler fallback = (session, exchange) -> exchange.setStatusCode(NOT_FOUND);

    /**
     * Returns the result of {@code fallback}.
     *
     * @param fallback the fallback
     * @return the result
     */
    public SessionMethodPathRouting fallback(final SessionHttpHandler fallback) {
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
    public SessionMethodPathRouting head(final String path, final SessionHttpHandler handler) {
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
    public SessionMethodPathRouting get(final String path, final SessionHttpHandler handler) {
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
    public SessionMethodPathRouting put(final String path, final SessionHttpHandler handler) {
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
    public SessionMethodPathRouting post(final String path, final SessionHttpHandler handler) {
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
    public SessionMethodPathRouting patch(final String path, final SessionHttpHandler handler) {
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
    public SessionMethodPathRouting delete(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("DELETE", s -> new HashMap<>()).put(path, handler);
        return this;
    }

    /**
     * Performs the {@code handleRequest} operation.
     *
     * @param session the session
     * @param exchange the exchange
     * @throws Exception if the operation fails
     */
    @Override
    public void handleRequest(final T session, final HttpServerExchange exchange) throws Exception {
        final var map = routes.get(exchange.getRequestMethod());
        final var handler = map == null ? fallback : map.getOrDefault(exchange.getRequestPath(), fallback);
        handler.handleRequest(session, exchange);
    }


    /**
     * Creates a new instance.
     */
    public SessionMethodPathRouting() {
    }
}
