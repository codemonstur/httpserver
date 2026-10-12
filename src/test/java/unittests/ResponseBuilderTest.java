package unittests;

import httpserver.core.HttpServerExchange;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static httpserver.core.CacheControlStrategy.NEVER_CACHE;
import static httpserver.core.ContentType.application_json;
import static httpserver.core.Headers.CACHE_CONTROL;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.Headers.LOCATION;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.CREATED;
import static httpserver.core.StatusCode.FOUND;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static unittests.Exchanges.newExchange;

public class ResponseBuilderTest {

    private static HttpServerExchange exchange() throws IOException {
        return newExchange("GET / HTTP/1.1\r\n\r\n");
    }

    private static String bodyOf(final HttpServerExchange exchange) throws IOException {
        final var out = new ByteArrayOutputStream();
        exchange.getResponseBody().writeTo(out);
        return out.toString(UTF_8);
    }

    @Test
    public void testSendSetsStatusHeadersAndBody() throws IOException {
        final var exchange = exchange();

        respond(exchange)
            .status(CREATED)
            .header("X-Test", "value")
            .contentType("text/plain; charset=UTF-8")
            .send("hello");

        assertEquals(CREATED, exchange.getStatusCode());
        assertEquals("value", exchange.getResponseHeader("X-Test"));
        assertEquals("text/plain; charset=UTF-8", exchange.getResponseHeader(CONTENT_TYPE));
        assertEquals("hello", bodyOf(exchange));
    }

    @Test
    public void testDefaultStatusIsInternalError() throws IOException {
        final var exchange = exchange();

        respond(exchange).send();

        assertEquals(INTERNAL_SERVER_ERROR, exchange.getStatusCode());
    }

    @Test
    public void testNumericHeaderOverloads() throws IOException {
        final var exchange = exchange();

        respond(exchange).header("X-Int", 42).header("X-Long", 43L).send();

        assertEquals("42", exchange.getResponseHeader("X-Int"));
        assertEquals("43", exchange.getResponseHeader("X-Long"));
    }

    @Test
    public void testContentTypeConstant() throws IOException {
        final var exchange = exchange();

        respond(exchange).contentType(application_json).send("{}");

        assertEquals("application/json", exchange.getResponseHeader(CONTENT_TYPE));
    }

    @Test
    public void testCacheStrategyIsApplied() throws IOException {
        final var exchange = exchange();

        respond(exchange).cache(NEVER_CACHE).send();

        assertEquals("no-cache, no-store, must-revalidate, pre-check=0, post-check=0, max-age=0, s-maxage=0",
                exchange.getResponseHeader(CACHE_CONTROL));
    }

    @Test
    public void testRedirectSetsFoundStatusAndLocation() throws IOException {
        final var exchange = exchange();

        respond(exchange).redirect("http://example.com/new-location").send();

        assertEquals(FOUND, exchange.getStatusCode());
        assertEquals("http://example.com/new-location", exchange.getResponseHeader(LOCATION));
    }

    @Test
    public void testValidBoundaryStatusCodes() throws IOException {
        final var lower = exchange();
        respond(lower).status(100).send();
        assertEquals(100, lower.getStatusCode());

        final var upper = exchange();
        respond(upper).status(599).send();
        assertEquals(599, upper.getStatusCode());
    }

    @Test
    public void testInvalidStatusCodesThrow() throws IOException {
        for (final int status : new int[]{-1, 0, 99, 999, 10000}) {
            try {
                respond(exchange()).status(status);
                fail("Expected IllegalArgumentException for status " + status);
            } catch (final IllegalArgumentException e) {
                assertEquals("Status must be a valid HTTP status code", e.getMessage());
            }
        }
    }

    @Test
    public void testSendObjectWithoutSerializerThrows() throws IOException {
        try {
            respond(exchange()).send(new Object());
            fail("Expected IllegalStateException when no serializer is configured");
        } catch (final IllegalStateException e) {
            assertEquals("Missing serializer", e.getMessage());
        }
    }

    @Test
    public void testSendObjectWithSerializer() throws IOException {
        final var exchange = exchange();

        respond(exchange, object -> "{\"serialized\":true}").send(new Object());

        assertEquals("{\"serialized\":true}", bodyOf(exchange));
    }

    @Test
    public void testSendByteArray() throws IOException {
        final var exchange = exchange();

        respond(exchange).send("bytes".getBytes(UTF_8));

        assertEquals("bytes", bodyOf(exchange));
    }

}
