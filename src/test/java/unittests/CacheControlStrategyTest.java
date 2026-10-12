package unittests;

import httpserver.core.HttpServerExchange;
import org.junit.Test;

import java.io.IOException;

import static httpserver.core.CacheControlStrategy.IMMUTABLE;
import static httpserver.core.CacheControlStrategy.NEVER_CACHE;
import static httpserver.core.CacheControlStrategy.SET_NOTHING;
import static httpserver.core.CacheControlStrategy.STORE_BUT_CHECK_SERVER;
import static httpserver.core.Headers.CACHE_CONTROL;
import static httpserver.core.Headers.EXPIRES;
import static httpserver.core.Headers.PRAGMA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static unittests.Exchanges.newExchange;

public class CacheControlStrategyTest {

    private static HttpServerExchange exchange() throws IOException {
        return newExchange("GET / HTTP/1.1\r\n\r\n");
    }

    @Test
    public void testSetNothingAddsNoHeaders() throws IOException {
        final var exchange = exchange();

        SET_NOTHING.apply(exchange);

        assertNull(exchange.getResponseHeader(CACHE_CONTROL));
        assertNull(exchange.getResponseHeader(EXPIRES));
        assertNull(exchange.getResponseHeader(PRAGMA));
    }

    @Test
    public void testNeverCache() throws IOException {
        final var exchange = exchange();

        NEVER_CACHE.apply(exchange);

        assertEquals("no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0, s-maxage=0",
                exchange.getResponseHeader(CACHE_CONTROL));
        assertEquals("0", exchange.getResponseHeader(EXPIRES));
        assertEquals("no-cache", exchange.getResponseHeader(PRAGMA));
    }

    @Test
    public void testStoreButCheckServer() throws IOException {
        final var exchange = exchange();

        STORE_BUT_CHECK_SERVER.apply(exchange);

        assertEquals("public, no-cache, max-age=0, must-revalidate", exchange.getResponseHeader(CACHE_CONTROL));
        assertNull(exchange.getResponseHeader(EXPIRES));
        assertNull(exchange.getResponseHeader(PRAGMA));
    }

    @Test
    public void testImmutable() throws IOException {
        final var exchange = exchange();

        IMMUTABLE.apply(exchange);

        assertEquals("public, max-age=315569260, immutable", exchange.getResponseHeader(CACHE_CONTROL));
        assertEquals("Fri, 01 Jan 2100 00:00:00 GMT", exchange.getResponseHeader(EXPIRES));
        assertNull(exchange.getResponseHeader(PRAGMA));
    }

}
