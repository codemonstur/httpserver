package unittests;

import java.io.ByteArrayOutputStream;

/** Builds client frames the way a real peer would: masked, with a fixed test mask key. */
enum ClientFrames {;

    private static final byte[] MASK = { 1, 2, 3, 4 };

    static byte[] frame(final boolean fin, final int opcode, final byte[] payload) {
        final var out = new ByteArrayOutputStream();
        out.write((fin ? 0x80 : 0x00) | opcode);
        if (payload.length < 126) {
            out.write(0x80 | payload.length);
        } else if (payload.length <= 0xFFFF) {
            out.write(0x80 | 126);
            out.write(payload.length >>> 8);
            out.write(payload.length);
        } else {
            out.write(0x80 | 127);
            final long length = payload.length; // a long, or the shifts wrap and lie about the size
            for (int shift = 56; shift >= 0; shift -= 8) out.write((int) (length >>> shift));
        }
        out.writeBytes(MASK);
        for (int i = 0; i < payload.length; i++) out.write(payload[i] ^ MASK[i & 3]);
        return out.toByteArray();
    }

    /** The same framing without the mask, for the peers that break the rule. */
    static byte[] unmasked(final boolean fin, final int opcode, final byte[] payload) {
        final var out = new ByteArrayOutputStream();
        out.write((fin ? 0x80 : 0x00) | opcode);
        out.write(payload.length);
        out.writeBytes(payload);
        return out.toByteArray();
    }

    static byte[] text(final String message) {
        return frame(true, 0x1, message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    static byte[] binary(final byte[] payload) {
        return frame(true, 0x2, payload);
    }

    static byte[] ping(final byte[] payload) {
        return frame(true, 0x9, payload);
    }

    static byte[] close() {
        return frame(true, 0x8, new byte[0]);
    }

    static byte[] close(final int code, final String reason) {
        final byte[] reasonBytes = reason.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        final byte[] payload = new byte[2 + reasonBytes.length];
        payload[0] = (byte) (code >>> 8);
        payload[1] = (byte) code;
        System.arraycopy(reasonBytes, 0, payload, 2, reasonBytes.length);
        return frame(true, 0x8, payload);
    }

}
