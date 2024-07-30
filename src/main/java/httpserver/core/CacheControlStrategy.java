package httpserver.core;

import java.util.function.Consumer;

import static httpserver.core.Headers.*;

public enum CacheControlStrategy {
    SET_NOTHING(exchange -> {}),
    NEVER_CACHE(exchange -> {
        exchange.setResponseHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0, s-maxage=0");
        exchange.setResponseHeader(EXPIRES, "0");
        exchange.setResponseHeader(PRAGMA, "no-cache");
    }),
    STORE_BUT_CHECK_SERVER(exchange -> exchange.setResponseHeader(CACHE_CONTROL, "public, no-cache, max-age=0, must-revalidate")),
    IMMUTABLE(exchange -> {
        exchange.setResponseHeader(CACHE_CONTROL, "public, max-age=315569260, immutable");
        exchange.setResponseHeader(EXPIRES, "Fri, 1 Jan 2100 00:00:00 GMT");
    });

    private final Consumer<HttpServerExchange> impl;
    CacheControlStrategy(final Consumer<HttpServerExchange> impl) {
        this.impl = impl;
    }
    public void apply(final HttpServerExchange exchange) {
        impl.accept(exchange);
    }
}