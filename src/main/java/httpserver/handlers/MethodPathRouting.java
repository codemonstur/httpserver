package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.StatusCode.NOT_FOUND;

public class MethodPathRouting implements HttpHandler {

    public static MethodPathRouting routing() {
        return new MethodPathRouting();
    }
    public static MethodPathRouting methodPathRouting() {
        return new MethodPathRouting();
    }

    private final Map<String, Map<String, HttpHandler>> routes = new HashMap<>();
    private HttpHandler fallback = exchange -> exchange.setStatusCode(NOT_FOUND);

    public MethodPathRouting fallback(final HttpHandler fallback) {
        this.fallback = fallback;
        return this;
    }
    public MethodPathRouting head(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("HEAD", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public MethodPathRouting get(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("GET", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public MethodPathRouting put(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("PUT", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public MethodPathRouting post(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("POST", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public MethodPathRouting patch(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("PATCH", s -> new HashMap<>()).put(path, handler);
        return this;
    }
    public MethodPathRouting delete(final String path, final HttpHandler handler) {
        routes.computeIfAbsent("DELETE", s -> new HashMap<>()).put(path, handler);
        return this;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        final var map = routes.get(exchange.getRequestMethod());
        final var handler = map == null ? fallback : map.getOrDefault(exchange.getRequestPath(), fallback);
        handler.handleRequest(exchange);
    }

}
