package httpserver.core;

import java.util.function.Consumer;

import static httpserver.core.Headers.*;

/**
 * The {@code CacheControlStrategy} enum.
 */
public enum CacheControlStrategy {
    /**
     * The {@code SET_NOTHING} constant.
     */
    SET_NOTHING(exchange -> {}),
    /**
     * The {@code NEVER_CACHE} constant.
     */
    NEVER_CACHE(exchange -> {
        exchange.setResponseHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0, s-maxage=0");
        exchange.setResponseHeader(EXPIRES, "0");
        exchange.setResponseHeader(PRAGMA, "no-cache");
    }),
    /**
     * The {@code STORE_BUT_CHECK_SERVER} constant.
     */
    STORE_BUT_CHECK_SERVER(exchange -> exchange.setResponseHeader(CACHE_CONTROL, "public, no-cache, max-age=0, must-revalidate")),
    /**
     * The {@code IMMUTABLE} constant.
     */
    IMMUTABLE(exchange -> {
        exchange.setResponseHeader(CACHE_CONTROL, "public, max-age=315569260, immutable");
        // The day has to be zero padded (IMF-fixdate); "1 Jan" is not a valid HTTP date and
        // strict caches and proxies reject it.
        exchange.setResponseHeader(EXPIRES, "Fri, 01 Jan 2100 00:00:00 GMT");
    });

    private final Consumer<HttpServerExchange> impl;
    CacheControlStrategy(final Consumer<HttpServerExchange> impl) {
        this.impl = impl;
    }
    /**
     * Performs the {@code apply} operation.
     *
     * @param exchange the exchange
     */
    public void apply(final HttpServerExchange exchange) {
        impl.accept(exchange);
    }
}