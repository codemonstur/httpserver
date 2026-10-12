package unittests;

import httpserver.HttpHandler;
import httpserver.handlers.HostRouting;
import org.junit.Test;

import static httpserver.core.StatusCode.CONFLICT;
import static httpserver.core.StatusCode.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static unittests.Exchanges.newExchange;

public class HostRoutingTest {

    private static HttpHandler newHostHandler() {
        return HostRouting.hostRouting()
            .host("example.com", exchange -> exchange.setResponseHeader("X-Host", "example"))
            .host("other.org", exchange -> exchange.setResponseHeader("X-Host", "other"))
            .build();
    }

    private static String handleWithHost(final String hostHeader) throws Exception {
        final var request = hostHeader == null
            ? "GET / HTTP/1.1\r\n\r\n"
            : "GET / HTTP/1.1\r\n" + hostHeader + "\r\n\r\n";
        final var exchange = newExchange(request);
        newHostHandler().handleRequest(exchange);
        return exchange.getResponseHeader("X-Host");
    }

    @Test
    public void testRoutesByHostHeader() throws Exception {
        assertEquals("example", handleWithHost("Host: example.com"));
        assertEquals("other", handleWithHost("Host: other.org"));
    }

    @Test
    public void testHostMatchingIgnoresCase() throws Exception {
        assertEquals("example", handleWithHost("Host: EXAMPLE.COM"));
        assertEquals("example", handleWithHost("Host: Example.Com"));
    }

    @Test
    public void testPortSectionIsStrippedFromHost() throws Exception {
        assertEquals("example", handleWithHost("Host: example.com:8080"));
        assertEquals("other", handleWithHost("Host: other.org:443"));
    }

    @Test
    public void testUnknownHostUsesDefaultFallback() throws Exception {
        final var exchange = newExchange("GET / HTTP/1.1\r\nHost: unknown.com\r\n\r\n");

        newHostHandler().handleRequest(exchange);

        assertEquals(NOT_FOUND, exchange.getStatusCode());
        assertNull(exchange.getResponseHeader("X-Host"));
    }

    @Test
    public void testMissingHostHeaderUsesDefaultFallback() throws Exception {
        assertNull(handleWithHost(null));
    }

    @Test
    public void testCustomFallbackIsUsed() throws Exception {
        final var exchange = newExchange("GET / HTTP/1.1\r\nHost: unknown.com\r\n\r\n");

        HostRouting.hostRouting()
            .host("example.com", e -> e.setResponseHeader("X-Host", "example"))
            .fallback(e -> e.setStatusCode(CONFLICT))
            .build()
            .handleRequest(exchange);

        assertEquals(CONFLICT, exchange.getStatusCode());
    }

    @Test
    public void testMissingHostHeaderUsesCustomFallback() throws Exception {
        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");

        HostRouting.hostRouting()
            .host("example.com", e -> e.setResponseHeader("X-Host", "example"))
            .fallback(e -> e.setStatusCode(CONFLICT))
            .build()
            .handleRequest(exchange);

        assertEquals(CONFLICT, exchange.getStatusCode());
    }

}
