package httpserver.handlers;

import httpserver.core.HttpServerExchange;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.StatusCode.NOT_FOUND;

public class SessionMethodPathRouting<T> implements SessionHttpHandler<T> {

    public static SessionMethodPathRouting sessionRouting() {
        return new SessionMethodPathRouting();
    }

    private final Map<String, Map<String, SessionHttpHandler>> routes = new HashMap<>();
    private SessionHttpHandler fallback = (session, exchange) -> exchange.setStatusCode(NOT_FOUND);

    public SessionMethodPathRouting fallback(final SessionHttpHandler fallback) {
        this.fallback = fallback;
        return this;
    }
    public SessionMethodPathRouting head(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("HEAD", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public SessionMethodPathRouting get(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("GET", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public SessionMethodPathRouting put(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("PUT", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public SessionMethodPathRouting post(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("POST", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public SessionMethodPathRouting patch(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("PATCH", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public SessionMethodPathRouting delete(final String path, final SessionHttpHandler handler) {
        routes.computeIfAbsent("DELETE", s -> new HashMap<>()).put(path, handler);
        return this;
    }

    @Override
    public void handleRequest(final T session, final HttpServerExchange exchange) throws Exception {
        final var map = routes.get(exchange.getRequestMethod());
        final var handler = map == null ? fallback : map.getOrDefault(exchange.getRequestPath(), fallback);
        handler.handleRequest(session, exchange);
    }

}
