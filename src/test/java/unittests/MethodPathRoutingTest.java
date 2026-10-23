package unittests;

import httpserver.handlers.MethodPathRouting;
import org.junit.jupiter.api.Test;

import static httpserver.core.StatusCode.CONFLICT;
import static httpserver.core.StatusCode.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static unittests.Exchanges.newExchange;
import static unittests.Exchanges.request;

public class MethodPathRoutingTest {

    private static MethodPathRouting newRouting() {
        return MethodPathRouting.methodPathRouting()
            .head("/hello", exchange -> exchange.setResponseHeader("X-Route", "head"))
            .get("/hello", exchange -> exchange.setResponseHeader("X-Route", "get"))
            .put("/hello", exchange -> exchange.setResponseHeader("X-Route", "put"))
            .post("/hello", exchange -> exchange.setResponseHeader("X-Route", "post"))
            .patch("/hello", exchange -> exchange.setResponseHeader("X-Route", "patch"))
            .delete("/hello", exchange -> exchange.setResponseHeader("X-Route", "delete"));
    }

    private static String route(final String request) throws Exception {
        final var exchange = newExchange(request);
        newRouting().handleRequest(exchange);
        return exchange.getResponseHeader("X-Route");
    }

    @Test
    public void testRoutesEachMethodToItsHandler() throws Exception {
        assertEquals("head", route(request("HEAD", "/hello")));
        assertEquals("get", route(request("GET", "/hello")));
        assertEquals("put", route(request("PUT", "/hello")));
        assertEquals("post", route(request("POST", "/hello")));
        assertEquals("patch", route(request("PATCH", "/hello")));
        assertEquals("delete", route(request("DELETE", "/hello")));
    }

    @Test
    public void testRoutingIgnoresQueryString() throws Exception {
        assertEquals("get", route(request("GET", "/hello?page=2&size=10")));
    }

    @Test
    public void testUnroutedPathUsesDefaultFallback() throws Exception {
        final var exchange = newExchange(request("GET", "/missing"));

        newRouting().handleRequest(exchange);

        assertEquals(NOT_FOUND, exchange.getStatusCode());
        assertNull(exchange.getResponseHeader("X-Route"));
    }

    @Test
    public void testUnroutedMethodUsesDefaultFallback() throws Exception {
        final var exchange = newExchange(request("OPTIONS", "/hello"));

        newRouting().handleRequest(exchange);

        assertEquals(NOT_FOUND, exchange.getStatusCode());
    }

    @Test
    public void testCustomFallbackIsUsed() throws Exception {
        final var exchange = newExchange(request("GET", "/missing"));

        newRouting().fallback(exchange2 -> exchange2.setStatusCode(CONFLICT)).handleRequest(exchange);

        assertEquals(CONFLICT, exchange.getStatusCode());
    }

}
