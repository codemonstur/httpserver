package httpserver.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The websocket framing defined by RFC 6455 section 5: reading client frames, which must
 * always be masked, and writing server frames, which never are. The limits that keep a
 * hostile peer from exhausting memory live here too: no frame larger than MAX_PAYLOAD, and
 * control frames capped at the 125 bytes the spec allows them.
 */
public enum Frames {;

    public static final int
        OP_CONTINUATION = 0x0,
        OP_TEXT = 0x1,
        OP_BINARY = 0x2,
        OP_CLOSE = 0x8,
        OP_PING = 0x9,
        OP_PONG = 0xA;

    public static final long MAX_PAYLOAD = 16 * 1024 * 1024;

    public static final class Frame {
        public final boolean fin;
        public final int opcode;
        public final byte[] payload;

        Frame(final boolean fin, final int opcode, final byte[] payload) {
            this.fin = fin;
            this.opcode = opcode;
            this.payload = payload;
        }
    }

    /**
     * Reads the next frame, or returns null at the clean end of the stream. A peer that
     * violates the protocol fails with a WebSocketException carrying the close code that
     * must be reported before the connection goes.
     */
    public static Frame read(final InputStream in) throws IOException {
        final int first = in.read();
        if (first == -1) return null;

        final boolean fin = (first & 0x80) != 0;
        if ((first & 0x70) != 0)
            throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Reserved bits set with no negotiated extension");
        final int opcode = first & 0x0F;

        final int second = readByte(in);
        if ((second & 0x80) == 0)
            throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Client frames must be masked");
        long length = second & 0x7F;
        if (length == 126) length = readUnsignedShort(in);
        else if (length == 127) {
            length = readUnsignedLong(in);
            if (length < 0)
                throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Frame length does not fit in 63 bits");
        }
        if (length > MAX_PAYLOAD)
            throw new WebSocketException(WebSocket.CLOSE_MESSAGE_TOO_BIG,
                "Frame of " + length + " bytes exceeds the limit of " + MAX_PAYLOAD);

        if ((opcode & 0x08) != 0) { // control frame
            if (!fin) throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Control frames may not be fragmented");
            if (length > 125) throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Control frames are limited to 125 bytes");
            if (opcode != OP_CLOSE && opcode != OP_PING && opcode != OP_PONG)
                throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Unknown control opcode: " + opcode);
        } else if (opcode != OP_CONTINUATION && opcode != OP_TEXT && opcode != OP_BINARY)
            throw new WebSocketException(WebSocket.CLOSE_PROTOCOL_ERROR, "Unknown opcode: " + opcode);

        final byte[] mask = readFully(in, 4);
        final byte[] payload = readFully(in, (int) length);
        for (int i = 0; i < payload.length; i++) payload[i] ^= mask[i & 3];
        return new Frame(fin, opcode, payload);
    }

    /**
     * Writes an unmasked server frame and flushes it: half a frame held back on a buffer
     * helps nobody, least of all a client waiting on the rest of a message.
     */
    public static void write(final OutputStream out, final boolean fin, final int opcode,
                             final byte[] payload) throws IOException {
        final long length = payload.length;
        out.write((fin ? 0x80 : 0x00) | opcode);
        if (length < 126) {
            out.write((int) length);
        } else if (length <= 0xFFFF) {
            out.write(126);
            out.write((int) (length >>> 8));
            out.write((int) length);
        } else {
            out.write(127);
            for (int shift = 56; shift >= 0; shift -= 8) out.write((int) (length >>> shift));
        }
        out.write(payload);
        out.flush();
    }

    private static int readByte(final InputStream in) throws IOException {
        final int value = in.read();
        if (value == -1) throw new IOException("Connection closed in the middle of a frame");
        return value;
    }

    private static int readUnsignedShort(final InputStream in) throws IOException {
        return (readByte(in) << 8) | readByte(in);
    }

    private static long readUnsignedLong(final InputStream in) throws IOException {
        long value = 0;
        for (int i = 0; i < 8; i++) value = (value << 8) | readByte(in);
        return value;
    }

    private static byte[] readFully(final InputStream in, final int length) throws IOException {
        final byte[] bytes = in.readNBytes(length);
        if (bytes.length != length) throw new IOException("Connection closed in the middle of a frame");
        return bytes;
    }

}
