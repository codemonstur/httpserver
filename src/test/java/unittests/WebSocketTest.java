package unittests;

import httpserver.websocket.WebSocket;
import httpserver.websocket.WebSocketException;
import httpserver.websocket.WebSocketHandler;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static httpserver.core.Headers.CONNECTION;
import static httpserver.core.Headers.SEC_WEBSOCKET_ACCEPT;
import static httpserver.core.Headers.SEC_WEBSOCKET_VERSION;
import static httpserver.core.Headers.UPGRADE;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static unittests.ClientFrames.binary;
import static unittests.ClientFrames.close;
import static unittests.ClientFrames.frame;
import static unittests.ClientFrames.ping;
import static unittests.ClientFrames.text;
import static unittests.ClientFrames.unmasked;
import static unittests.Exchanges.newExchange;
import static unittests.Exchanges.request;

public class WebSocketTest {

    // The connection token is a list here on purpose: real clients and proxies send both.
    private static final String WS_REQUEST = """
        GET /chat HTTP/1.1\r
        Host: example.com\r
        Upgrade: websocket\r
        Connection: keep-alive, Upgrade\r
        Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r
        Sec-WebSocket-Version: 13\r
        \r
        """;

    @Test
    public void testCompletesTheOpeningHandshake() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(WS_REQUEST, new ByteArrayInputStream(close()), out);

        final var session = exchange.acceptWebSocket(new WebSocketHandler() {});

        // The sample handshake from RFC 6455 section 1.3, key and answer both.
        final String headers = headers(out);
        assertTrue(headers.startsWith("HTTP/1.1 101 Switching Protocols"));
        assertTrue(headers.contains(UPGRADE + ": websocket"));
        assertTrue(headers.contains(CONNECTION + ": Upgrade"));
        assertTrue(headers.contains(SEC_WEBSOCKET_ACCEPT + ": s3pPLMBiTxaQ9kYGzzhZRbK+xOo="));
        assertFalse(headers.contains("Content-Length"));
        assertNotNull(session);
        assertFalse(session.isOpen());
        assertTrue(exchange.isConnectionClosed());
    }

    @Test
    public void testIsWebSocketUpgradeChecksBothHeaders() throws IOException {
        assertTrue(newExchange(WS_REQUEST).isWebSocketUpgrade());
        assertFalse(newExchange(request("GET", "/")).isWebSocketUpgrade());
        assertFalse(newExchange(WS_REQUEST.replace("Upgrade: websocket\r\n", "")).isWebSocketUpgrade());
        assertFalse(newExchange(WS_REQUEST.replace("Connection: keep-alive, Upgrade\r\n", "")).isWebSocketUpgrade());
    }

    @Test
    public void testRejectsUnsupportedVersions() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(WS_REQUEST.replace("Version: 13", "Version: 8"),
            new ByteArrayInputStream(new byte[0]), out);

        final var session = exchange.acceptWebSocket(new WebSocketHandler() {});

        assertNull(session);
        exchange.sendResponse();
        final String headers = headers(out);
        assertTrue(headers.startsWith("HTTP/1.1 426 Upgrade Required"));
        assertTrue(headers.contains(SEC_WEBSOCKET_VERSION + ": 13"));
    }

    @Test
    public void testRejectsMissingOrMalformedKeys() throws IOException {
        final var noKeyOut = new ByteArrayOutputStream();
        final var noKey = newExchange(WS_REQUEST.replace("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n", ""),
            new ByteArrayInputStream(new byte[0]), noKeyOut);
        assertNull(noKey.acceptWebSocket(new WebSocketHandler() {}));
        noKey.sendResponse();
        assertTrue(headers(noKeyOut).startsWith("HTTP/1.1 400 Bad Request"));

        final var badKeyOut = new ByteArrayOutputStream();
        final var badKey = newExchange(WS_REQUEST.replace("dGhlIHNhbXBsZSBub25jZQ==", "not base64!!"),
            new ByteArrayInputStream(new byte[0]), badKeyOut);
        assertNull(badKey.acceptWebSocket(new WebSocketHandler() {}));
        badKey.sendResponse();
        assertTrue(headers(badKeyOut).startsWith("HTTP/1.1 400 Bad Request"));
    }

    @Test
    public void testRejectsPlainHttpRequests() throws IOException {
        final var exchange = newExchange(request("GET", "/"),
            new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());

        assertNull(exchange.acceptWebSocket(new WebSocketHandler() {}));
    }

    @Test
    public void testEchoesTextMessages() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var input = new ByteArrayInputStream(concat(text("hi"), close(1000, "")));
        final var exchange = newExchange(WS_REQUEST, input, out);

        exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onMessage(final WebSocket webSocket, final String message) throws IOException {
                webSocket.send("echo: " + message);
            }
        });

        assertArrayEquals(concat(
            serverFrame(0x1, "echo: hi".getBytes(UTF_8)),
            serverFrame(0x8, new byte[] { 0x03, (byte) 0xE8 })), framesAfterHeaders(out));
    }

    @Test
    public void testAnswersPingsWithPongs() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var input = new ByteArrayInputStream(concat(ping("ping!".getBytes(UTF_8)), close(1000, "")));
        final var exchange = newExchange(WS_REQUEST, input, out);

        exchange.acceptWebSocket(new WebSocketHandler() {});

        assertArrayEquals(concat(
            serverFrame(0xA, "ping!".getBytes(UTF_8)),
            serverFrame(0x8, new byte[] { 0x03, (byte) 0xE8 })), framesAfterHeaders(out));
    }

    @Test
    public void testReassemblesFragmentedMessages() throws IOException {
        final var received = new ArrayList<String>();
        final var input = new ByteArrayInputStream(concat(
            frame(false, 0x1, "hel".getBytes(UTF_8)),
            frame(true, 0x0, "lo".getBytes(UTF_8)),
            close(1000, "")));
        final var exchange = newExchange(WS_REQUEST, input, new ByteArrayOutputStream());

        exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onMessage(final WebSocket webSocket, final String message) {
                received.add(message);
            }
        });

        assertEquals(List.of("hello"), received);
    }

    @Test
    public void testDispatchesBinaryMessages() throws IOException {
        final var received = new ArrayList<byte[]>();
        final var input = new ByteArrayInputStream(concat(binary(new byte[] { 1, 2, 3 }), close(1000, "")));
        final var exchange = newExchange(WS_REQUEST, input, new ByteArrayOutputStream());

        exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onMessage(final WebSocket webSocket, final byte[] message) {
                received.add(message);
            }
        });

        assertEquals(1, received.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, received.get(0));
    }

    @Test
    public void testCloseFromAHandlerSendsTheCloseFrame() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(WS_REQUEST, new ByteArrayInputStream(text("hi")), out);
        final var closed = new boolean[1];

        final var session = exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onMessage(final WebSocket webSocket, final String message) throws IOException {
                webSocket.close(WebSocket.CLOSE_NORMAL, "bye");
            }
            @Override public void onClose(final WebSocket webSocket, final int code, final String reason) {
                closed[0] = true;
            }
        });

        assertArrayEquals(serverFrame(0x8, new byte[] { 0x03, (byte) 0xE8, 'b', 'y', 'e' }),
            framesAfterHeaders(out));
        assertFalse(session.isOpen());
        assertTrue(closed[0]);
    }

    @Test
    public void testOnCloseReportsThePeerStatus() throws IOException {
        final var codes = new int[1];
        final var reasons = new String[1];
        final var input = new ByteArrayInputStream(close(1001, "going away"));
        final var exchange = newExchange(WS_REQUEST, input, new ByteArrayOutputStream());

        exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onClose(final WebSocket webSocket, final int code, final String reason) {
                codes[0] = code;
                reasons[0] = reason;
            }
        });

        assertEquals(1001, codes[0]);
        assertEquals("going away", reasons[0]);
    }

    @Test
    public void testOnCloseReportsAClientThatHungUp() throws IOException {
        final var codes = new int[1];
        final var exchange = newExchange(WS_REQUEST, new ByteArrayInputStream(new byte[0]),
            new ByteArrayOutputStream());

        exchange.acceptWebSocket(new WebSocketHandler() {
            @Override public void onClose(final WebSocket webSocket, final int code, final String reason) {
                codes[0] = code;
            }
        });

        assertEquals(WebSocket.CLOSE_NO_STATUS, codes[0]);
    }

    @Test
    public void testProtocolViolationsFailWithACloseFrame() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(WS_REQUEST, new ByteArrayInputStream(unmasked(true, 0x1, "hi".getBytes(UTF_8))), out);

        try {
            exchange.acceptWebSocket(new WebSocketHandler() {});
            fail("Expected the unmasked frame to fail the connection");
        } catch (final WebSocketException e) {
            assertEquals(1002, e.closeCode);
        }

        final byte[] frames = framesAfterHeaders(out);
        assertEquals((byte) 0x88, frames[0]); // close
        assertEquals(0x03, frames[2]);
        assertEquals((byte) 0xEA, frames[3]); // 1002: protocol error
    }

    @Test
    public void testFailingHandlersCloseWithAnInternalError() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var input = new ByteArrayInputStream(concat(text("hi"), close(1000, "")));
        final var exchange = newExchange(WS_REQUEST, input, out);

        try {
            exchange.acceptWebSocket(new WebSocketHandler() {
                @Override public void onMessage(final WebSocket webSocket, final String message) {
                    throw new IllegalStateException("boom");
                }
            });
            fail("Expected the failing handler to end the session");
        } catch (final IOException e) {
            assertEquals("boom", e.getCause().getMessage());
        }

        final byte[] frames = framesAfterHeaders(out);
        assertEquals((byte) 0x88, frames[0]);
        assertEquals(0x03, frames[2]);
        assertEquals((byte) 0xF3, frames[3]); // 1011: internal error
    }

    private static byte[] serverFrame(final int opcode, final byte[] payload) {
        final var out = new ByteArrayOutputStream();
        out.write(0x80 | opcode);
        out.write(payload.length);
        out.writeBytes(payload);
        return out.toByteArray();
    }

    private static byte[] framesAfterHeaders(final ByteArrayOutputStream out) {
        final byte[] bytes = out.toByteArray();
        for (int i = 0; i + 3 < bytes.length; i++)
            if (bytes[i] == '\r' && bytes[i + 1] == '\n' && bytes[i + 2] == '\r' && bytes[i + 3] == '\n')
                return Arrays.copyOfRange(bytes, i + 4, bytes.length);
        fail("No header terminator in the response");
        return null;
    }

    private static String headers(final ByteArrayOutputStream out) {
        final byte[] bytes = out.toByteArray();
        for (int i = 0; i + 3 < bytes.length; i++)
            if (bytes[i] == '\r' && bytes[i + 1] == '\n' && bytes[i + 2] == '\r' && bytes[i + 3] == '\n')
                return new String(bytes, 0, i + 4, US_ASCII);
        fail("No header terminator in the response");
        return null;
    }

    private static byte[] concat(final byte[]... parts) {
        final var out = new ByteArrayOutputStream();
        for (final byte[] part : parts) out.writeBytes(part);
        return out.toByteArray();
    }

}
