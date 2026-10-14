package httpserver.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * One websocket connection. The sending half stays usable from any thread, the receiving half
 * runs on the worker that owns the connection until the closing handshake completes. Frames
 * from concurrent senders never interleave: every frame is written under one lock.
 */
public final class WebSocket {

    /** Status codes from RFC 6455 section 7.4, for close(). */
    public static final int
        CLOSE_NORMAL = 1000,
        CLOSE_GOING_AWAY = 1001,
        CLOSE_PROTOCOL_ERROR = 1002,
        CLOSE_UNSUPPORTED_DATA = 1003,
        CLOSE_INVALID_PAYLOAD = 1007,
        CLOSE_POLICY_VIOLATION = 1008,
        CLOSE_MESSAGE_TOO_BIG = 1009,
        CLOSE_INTERNAL_ERROR = 1011,
        /**
         * Reported to onClose when a peer hangs up without sending a close status. It, and
         * its sibling for an absent reason, never goes on the wire.
         */
        CLOSE_NO_STATUS = 1005;

    private static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
    private static final byte[] EMPTY = new byte[0];

    private final InputStream in;
    private final OutputStream out;
    private final WebSocketHandler handler;
    private final Object writeLock = new Object();

    private volatile boolean open = true;
    private boolean closeSent = false;

    public WebSocket(final InputStream in, final OutputStream out, final WebSocketHandler handler) {
        this.in = in;
        this.out = out;
        this.handler = handler;
    }

    /**
     * The opening handshake's answer key: base64 of the SHA-1 of the client's nonce and the
     * protocol's magic GUID (RFC 6455 section 4.2.2).
     */
    public static String acceptKey(final String key) {
        try {
            final var sha1 = MessageDigest.getInstance("SHA-1");
            return Base64.getEncoder().encodeToString(sha1.digest((key + GUID).getBytes(US_ASCII)));
        } catch (final NoSuchAlgorithmException e) {
            throw new AssertionError("Every Java has SHA-1", e);
        }
    }

    /** Sends a text message. */
    public void send(final String text) throws IOException {
        sendFrame(true, Frames.OP_TEXT, text.getBytes(UTF_8));
    }

    /** Sends a binary message. */
    public void send(final byte[] binary) throws IOException {
        sendFrame(true, Frames.OP_BINARY, binary);
    }

    /** Sends an empty ping, the protocol's own keepalive. */
    public void sendPing() throws IOException {
        sendPing(EMPTY);
    }

    public void sendPing(final byte[] payload) throws IOException {
        if (payload.length > 125) throw new IllegalArgumentException("Control frames are limited to 125 bytes");
        sendFrame(true, Frames.OP_PING, payload);
    }

    /** Ends the session with the normal status: close frame out, read loop stopped. */
    public void close() throws IOException {
        close(CLOSE_NORMAL, "");
    }

    /**
     * Ends the session with a status code and reason. Called from a message callback the
     * loop stops as soon as the callback returns; called from another thread the session
     * ends once the client answers the close. Never sends a second close: a peer that got
     * here first already started the closing handshake.
     */
    public void close(final int code, final String reason) throws IOException {
        synchronized (writeLock) {
            if (!closeSent) sendCloseFrame(code, reason);
            closeSent = true;
            open = false;
        }
    }

    public boolean isOpen() {
        return open;
    }

    /**
     * Runs the receiving side until the connection ends: messages go to the handler one at a
     * time, pings are answered, and the closing handshake is completed in both directions.
     * Called by acceptWebSocket; blocks until the session is over.
     */
    public void run() throws IOException {
        int closeCode = CLOSE_NO_STATUS;
        String closeReason = "";
        int fragmentOpcode = -1;
        final var fragments = new ByteArrayOutputStream();

        try {
            try {
                handler.onOpen(this);
            } catch (final Exception e) {
                throw handlerFailed(e);
            }
            while (open) {
                final var frame = Frames.read(in);
                if (frame == null) break; // the client hung up without a close frame

                if (frame.opcode == Frames.OP_PING) {
                    sendFrame(true, Frames.OP_PONG, frame.payload);
                } else if (frame.opcode == Frames.OP_PONG) {
                    // nothing to do here: either we did not ask, or this answers a ping whose
                    // reply we do not need to act on
                } else if (frame.opcode == Frames.OP_CLOSE) {
                    closeCode = closeCodeOf(frame.payload);
                    closeReason = closeReasonOf(frame.payload);
                    synchronized (writeLock) {
                        if (!closeSent) {
                            // Answer with what the client sent, so an empty close stays empty:
                            // a status of 1005 only ever describes the absence of one.
                            if (frame.payload.length == 0) sendFrame(true, Frames.OP_CLOSE, EMPTY);
                            else sendCloseFrame(closeCode, closeReason);
                            closeSent = true;
                        }
                        open = false;
                    }
                } else if (frame.opcode == Frames.OP_CONTINUATION) {
                    if (fragmentOpcode == -1)
                        throw new WebSocketException(CLOSE_PROTOCOL_ERROR, "Continuation frame without a message to continue");
                    fragments.write(frame.payload, 0, frame.payload.length);
                    if (fragments.size() > Frames.MAX_PAYLOAD)
                        throw new WebSocketException(CLOSE_MESSAGE_TOO_BIG,
                            "Message exceeds the limit of " + Frames.MAX_PAYLOAD);
                    if (frame.fin) {
                        dispatch(fragmentOpcode, fragments.toByteArray());
                        fragmentOpcode = -1;
                        fragments.reset();
                    }
                } else {
                    if (fragmentOpcode != -1)
                        throw new WebSocketException(CLOSE_PROTOCOL_ERROR, "New message while an earlier one is still fragmented");
                    if (frame.fin) {
                        dispatch(frame.opcode, frame.payload);
                    } else {
                        fragmentOpcode = frame.opcode;
                        fragments.reset();
                        fragments.write(frame.payload, 0, frame.payload.length);
                    }
                }
            }
        } catch (final WebSocketException e) {
            closeCode = e.closeCode;
            closeReason = e.getMessage() == null ? "" : e.getMessage();
            synchronized (writeLock) {
                if (!closeSent) {
                    try {
                        sendCloseFrame(closeCode, closeReason);
                    } catch (final IOException ignored) {
                        // the peer is already gone: there is nowhere left to report the violation
                    }
                    closeSent = true;
                }
                open = false;
            }
            throw e;
        } finally {
            open = false;
            try {
                handler.onClose(this, closeCode, closeReason);
            } catch (final Exception ignored) {
                // the session is over either way: a failing onClose must not mask how it ended
            }
        }
    }

    private void dispatch(final int opcode, final byte[] payload) throws IOException {
        try {
            if (opcode == Frames.OP_TEXT) handler.onMessage(this, new String(payload, UTF_8));
            else handler.onMessage(this, payload);
        } catch (final Exception e) {
            throw handlerFailed(e);
        }
    }

    /**
     * A failing handler takes the connection down, but not silently: the peer gets an
     * internal error close instead of being left waiting on a half conversation. Returns
     * the failure to throw, keeping an IOException's identity.
     */
    private IOException handlerFailed(final Exception e) {
        synchronized (writeLock) {
            if (!closeSent) {
                try {
                    sendCloseFrame(CLOSE_INTERNAL_ERROR, "Internal error");
                } catch (final IOException ignored) {
                    // the peer is already gone
                }
                closeSent = true;
            }
            open = false;
        }
        return e instanceof final IOException io ? io : new IOException("WebSocket handler failed", e);
    }

    private void sendFrame(final boolean fin, final int opcode, final byte[] payload) throws IOException {
        synchronized (writeLock) {
            if (!open) throw new IOException("WebSocket is closed");
            Frames.write(out, fin, opcode, payload);
        }
    }

    private void sendCloseFrame(final int code, final String reason) throws IOException {
        final byte[] reasonBytes = reason.getBytes(UTF_8);
        final int length = Math.min(reasonBytes.length, 123); // 125, less the two bytes of code
        final byte[] payload = new byte[2 + length];
        payload[0] = (byte) (code >>> 8);
        payload[1] = (byte) code;
        System.arraycopy(reasonBytes, 0, payload, 2, length);
        sendFrame(true, Frames.OP_CLOSE, payload);
    }

    private static int closeCodeOf(final byte[] payload) throws WebSocketException {
        if (payload.length == 0) return CLOSE_NO_STATUS;
        if (payload.length == 1) throw new WebSocketException(CLOSE_PROTOCOL_ERROR, "Close frame with a one byte payload");
        final int code = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
        if (code < 1000 || code > 4999)
            throw new WebSocketException(CLOSE_PROTOCOL_ERROR, "Close status out of range: " + code);
        return code;
    }

    private static String closeReasonOf(final byte[] payload) {
        return payload.length <= 2 ? "" : new String(payload, 2, payload.length - 2, UTF_8);
    }

}
