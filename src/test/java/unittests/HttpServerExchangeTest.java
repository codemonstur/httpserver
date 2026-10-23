package unittests;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static httpserver.core.Headers.CONNECTION;
import static httpserver.core.Headers.CONTENT_LENGTH;
import static httpserver.core.Headers.DATE;
import static httpserver.core.Headers.EXPECT;
import static httpserver.core.Headers.HOST;
import static httpserver.core.Headers.TRANSFER_ENCODING;
import static httpserver.core.Headers.USER_AGENT;
import static httpserver.core.RequestParsing.discardRemainingRequestBody;
import static httpserver.core.StatusCode.CREATED;
import static httpserver.core.StatusCode.NO_CONTENT;
import static httpserver.util.Strings.KEEP_ALIVE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static unittests.Exchanges.newExchange;

public class HttpServerExchangeTest {

    private static final String GET_REQUEST = """
        GET /path/to?x=1&y=hello+world HTTP/1.1\r
        Host: example.com\r
        User-Agent: test-agent\r
        X-Custom: one\r
        X-Custom: two\r
        \r
        """;

    @Test
    public void testParsesRequestLine() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("GET", exchange.getRequestMethod());
        assertEquals("/path/to?x=1&y=hello+world", exchange.getRequestURI());
        assertEquals("HTTP/1.1", exchange.getRequestProtocol());
    }

    @Test
    public void testParsesHttp09RequestLine() throws IOException {
        final var exchange = newExchange("GET /\r\n");

        assertEquals("GET", exchange.getRequestMethod());
        assertEquals("/", exchange.getRequestURI());
        assertEquals("HTTP/0.9", exchange.getRequestProtocol());
    }

    @Test
    public void testGetRequestHeaderIsCaseInsensitive() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("example.com", exchange.getRequestHeader(HOST));
        assertEquals("example.com", exchange.getRequestHeader("host"));
        assertEquals("example.com", exchange.getRequestHeader("HOST"));
        assertEquals("test-agent", exchange.getRequestHeader(USER_AGENT));
        assertNull(exchange.getRequestHeader("Missing"));
    }

    @Test
    public void testGetRequestHeadersReturnsRepeatedHeaders() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        final List<String> headers = exchange.getRequestHeaders("X-Custom");

        assertEquals(List.of("one", "two"), headers);
        assertEquals(List.of("example.com"), exchange.getRequestHeaders(HOST));
        assertTrue(exchange.getRequestHeaders("Missing").isEmpty());
    }

    @Test
    public void testGetRequestPathStripsQueryString() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("/path/to", exchange.getRequestPath());
        assertEquals("/plain", newExchange("GET /plain HTTP/1.1\r\n\r\n").getRequestPath());
    }

    @Test
    public void testGetQueryString() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("x=1&y=hello+world", exchange.getQueryString());
        assertNull(newExchange("GET /plain HTTP/1.1\r\n\r\n").getQueryString());
    }

    @Test
    public void testGetQueryParameter() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("1", exchange.getQueryParameter("x"));
        assertEquals("hello world", exchange.getQueryParameter("y"));
        assertNull(exchange.getQueryParameter("missing"));
    }

    @Test
    public void testGetQueryParameterWithoutValue() throws IOException {
        final var exchange = newExchange("GET /path?flag&other=1 HTTP/1.1\r\n\r\n");

        assertEquals("", exchange.getQueryParameter("flag"));
        assertEquals("1", exchange.getQueryParameter("other"));
    }

    @Test
    public void testHttp10RequestSetsKeepAliveResponseHeader() throws IOException {
        final var exchange = newExchange("GET / HTTP/1.0\r\n\r\n");

        assertEquals(KEEP_ALIVE, exchange.getResponseHeader(CONNECTION));
    }

    @Test
    public void testResponseHeadersCanBeSetAndRemoved() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        exchange.setStatusCode(CREATED);
        exchange.setResponseHeader("X-Test", "value");

        assertEquals(CREATED, exchange.getStatusCode());
        assertEquals("value", exchange.getResponseHeader("X-Test"));

        exchange.removeResponseHeader("X-Test");

        assertNull(exchange.getResponseHeader("X-Test"));
    }

    @Test
    public void testGetInputStreamIsRestrictedByContentLength() throws IOException {
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n", new ByteArrayInputStream("hello world".getBytes(UTF_8)),
            new ByteArrayOutputStream());

        assertArrayEquals("hello".getBytes(UTF_8), exchange.getInputStream().readAllBytes());
    }

    @Test
    public void testGetInputStreamWithoutContentLengthIsEmpty() throws IOException {
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\n\r\n", new ByteArrayInputStream("hello".getBytes(UTF_8)), new ByteArrayOutputStream());

        assertEquals(-1, exchange.getInputStream().read());
    }

    @Test
    public void testGetInputStreamDoesNotReplayTheBody() throws IOException {
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n", new ByteArrayInputStream("hello".getBytes(UTF_8)),
            new ByteArrayOutputStream());

        // A handler consuming the body must leave the stream exhausted, so that the servers
        // discardRemainingRequestBody() does not try to read the body a second time off the socket.
        assertArrayEquals("hello".getBytes(UTF_8), exchange.getInputStream().readAllBytes());
        assertEquals(-1, exchange.getInputStream().read());

        discardRemainingRequestBody(exchange);

        assertEquals(-1, exchange.getInputStream().read());
    }

    @Test
    public void testRequestBodyAcceptedSendsContinueForExpectHeader() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\n" + EXPECT + ": 100-continue\r\n\r\n", new ByteArrayInputStream(new byte[0]), out);

        exchange.requestBodyAccepted();

        assertEquals("HTTP/1.1 100 Continue\r\n\r\n", out.toString(UTF_8));
    }

    @Test
    public void testRequestBodyAcceptedSendsNothingWithoutExpectHeader() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\n\r\n", new ByteArrayInputStream(new byte[0]), out);

        exchange.requestBodyAccepted();

        assertEquals(0, out.size());
    }

    @Test
    public void testRequestBodyAcceptedIgnoresExpectHeaderOnHttp10() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(
            "POST / HTTP/1.0\r\n" + EXPECT + ": 100-continue\r\n\r\n", new ByteArrayInputStream(new byte[0]), out);

        exchange.requestBodyAccepted();

        assertEquals(0, out.size());
    }

    @Test
    public void testSendResponseWritesHeadAndBody() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.startsWith("HTTP/1.1 200 Ok"));
        assertTrue(response.contains(CONTENT_LENGTH + ": 5"));
        assertTrue(response.contains(DATE + ": "));
        assertTrue(response.endsWith("hello"));
        assertTrue(exchange.isResponseSent());
    }

    @Test
    public void testSendResponseUsesCustomStatusMessage() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.setStatusMessage("All Good");

        exchange.sendResponse();

        assertTrue(out.toString(UTF_8).startsWith("HTTP/1.1 200 All Good"));
    }

    @Test
    public void testSendResponseForHeadRequestHasNoBody() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(
            "HEAD / HTTP/1.1\r\n\r\n", new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.startsWith("HTTP/1.1 200 Ok"));
        assertFalse(response.contains("hello"));
        // RFC 9110 9.3.2: a HEAD response carries the Content-Length the equivalent GET would
        // have sent, it just does not send the body itself.
        assertTrue(response.contains(CONTENT_LENGTH + ": 5"));
    }

    @Test
    public void testSendResponseForHttp09WritesOnlyTheBody() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange("GET /\r\n", new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);

        exchange.sendResponse();

        assertEquals("hello", out.toString(UTF_8));
    }

    @Test
    public void testSendResponseForHttp10IncludesConnectionHeader() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(
            "GET / HTTP/1.0\r\n\r\n", new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.startsWith("HTTP/1.0 200 Ok"));
        assertTrue(response.contains(CONNECTION + ": " + KEEP_ALIVE));
    }

    @Test
    public void testSendResponseThrowsForUnknownProtocol() throws IOException {
        final var exchange = newExchange(
            "GET / HTTP/2.0\r\n\r\n", new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
        exchange.send("hello", UTF_8);

        try {
            exchange.sendResponse();
            fail("Expected IOException for an unrecognised protocol");
        } catch (final IOException e) {
            assertEquals("HTTP protocol not recognised", e.getMessage());
        }
    }

    @Test
    public void testSendResponseOnlyWritesOnce() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);

        exchange.sendResponse();
        final int sizeAfterFirstSend = out.size();

        exchange.sendResponse();

        assertEquals(sizeAfterFirstSend, out.size());
    }

    @Test
    public void testSendResponseUsesSliceLengthForContentLength() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.send("xhellox".getBytes(UTF_8), 1, 5);

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.contains(CONTENT_LENGTH + ": 5"));
        assertTrue(response.endsWith("hello"));
    }

    @Test
    public void testSetNoContentLengthSkipsTheHeader() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello", UTF_8);
        exchange.setNoContentLength();

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertFalse(response.contains(CONTENT_LENGTH));
        assertTrue(response.endsWith("hello"));
    }

    @Test
    public void testSendChunkedFramesTheBody() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.contains(TRANSFER_ENCODING + ": chunked"));
        assertFalse(response.contains(CONTENT_LENGTH));
        assertTrue(response.endsWith("\r\n0\r\n\r\n"));
        assertEquals("hello", decodeChunks(response));
    }

    @Test
    public void testSendChunkedClosesOlderConnections() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange("GET /path HTTP/1.0\r\n\r\n",
            new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));

        exchange.sendResponse();

        // No chunked framing exists in HTTP/1.0: no framing header at all, and the close of
        // the connection is what tells the client that the body is complete.
        final String response = out.toString(UTF_8);
        assertTrue(response.contains(CONNECTION + ": close"));
        assertFalse(response.contains(TRANSFER_ENCODING));
        assertFalse(response.contains(CONTENT_LENGTH));
        assertTrue(response.endsWith("hello"));
    }

    @Test
    public void testSendChunkedOverridesAnEarlierSetNoContentLength() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.setNoContentLength();
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.contains(TRANSFER_ENCODING + ": chunked"));
        assertEquals("hello", decodeChunks(response));
    }

    @Test
    public void testSetNoContentLengthAfterSendChunkedStaysCloseDelimited() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));
        exchange.setNoContentLength();

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertFalse(response.contains(TRANSFER_ENCODING));
        assertFalse(response.contains(CONTENT_LENGTH));
        assertTrue(response.endsWith("hello"));
    }

    @Test
    public void testChunkedNotUsedForNoContent() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));
        exchange.setStatusCode(NO_CONTENT);

        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertFalse(response.contains(TRANSFER_ENCODING));
        assertFalse(response.contains(CONTENT_LENGTH));
        assertFalse(response.contains("hello"));
    }

    @Test
    public void testHeadDescribesAChunkedGetWithoutSendingIt() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange("HEAD /path HTTP/1.1\r\nHost: example.com\r\n\r\n",
            new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));

        exchange.sendResponse();

        // The headers describe the representation a GET would return; the body itself stays
        // unsent, exactly as it does for a Content-Length response to a HEAD.
        final String response = out.toString(UTF_8);
        assertTrue(response.contains(TRANSFER_ENCODING + ": chunked"));
        assertFalse(response.contains("hello"));
    }

    /**
     * Reads chunked framing back the way a client would: hex sized chunks until the zero
     * terminator, returning the decoded payload.
     */
    private static String decodeChunks(final String response) {
        String body = response.substring(response.indexOf("\r\n\r\n") + 4);
        final var decoded = new StringBuilder();
        while (true) {
            final int lineEnd = body.indexOf("\r\n");
            final int size = Integer.parseInt(body.substring(0, lineEnd), 16);
            if (size == 0) return decoded.toString();
            decoded.append(body, lineEnd + 2, lineEnd + 2 + size);
            body = body.substring(lineEnd + 2 + size + 2);
        }
    }

    @Test
    public void testMethodAllowsResponseBody() throws IOException {
        assertTrue(newExchange(GET_REQUEST).methodAllowsResponseBody());
        assertFalse(newExchange("HEAD / HTTP/1.1\r\n\r\n").methodAllowsResponseBody());
    }

    @Test
    public void testDiscardRemainingRequestBody() throws IOException {
        final var exchange = newExchange(
            "POST / HTTP/1.1\r\nContent-Length: 5\r\n\r\n", new ByteArrayInputStream("hello".getBytes(UTF_8)),
            new ByteArrayOutputStream());

        discardRemainingRequestBody(exchange);

        assertEquals(-1, exchange.getInputStream().read());
    }

    @Test
    public void testNotifyCompleteListeners() throws IOException {
        final var exchange = newExchange(GET_REQUEST);
        final List<String> called = new ArrayList<>();
        exchange.addExchangeCompleteListener((exception, e) -> called.add("first:" + (exception == null)));
        exchange.addExchangeCompleteListener((exception, e) -> called.add("second:" + (exception == null)));

        final var failure = new IOException("boom");
        exchange.notifyCompleteListeners(failure);
        exchange.notifyCompleteListeners(null);

        assertEquals(List.of("first:false", "second:false", "first:true", "second:true"), called);
    }

    @Test
    public void testNotifyCompleteListenersWithoutListeners() throws IOException {
        newExchange(GET_REQUEST).notifyCompleteListeners(null);
    }

    @Test
    public void testGetRawRequest() throws IOException {
        final var exchange = newExchange(GET_REQUEST);

        assertEquals("GET", new String(exchange.getRawRequest(), 0, 3, UTF_8));
    }

    @Test
    public void testGetOutputStreamIsTheProvidedStream() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(GET_REQUEST, new ByteArrayInputStream(new byte[0]), out);

        exchange.getOutputStream().write("data".getBytes(UTF_8));

        assertEquals("data", out.toString(UTF_8));
    }

}
