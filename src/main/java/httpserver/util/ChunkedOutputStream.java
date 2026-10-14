package httpserver.util;

import java.io.IOException;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * Frames everything written through it as HTTP/1.1 chunked transfer encoding (RFC 9112
 * section 7.1): each write becomes one hex sized chunk, and closing writes the terminating
 * zero chunk. Flushes the underlying stream but never closes it: on a keep-alive connection
 * the socket outlives the body that was framed through here.
 */
public final class ChunkedOutputStream extends OutputStream {

    private static final byte[] CRLF = { '\r', '\n' };
    private static final byte[] TERMINATING_CHUNK = { '0', '\r', '\n', '\r', '\n' };

    private final OutputStream out;
    private boolean finished = false;

    public ChunkedOutputStream(final OutputStream out) {
        this.out = out;
    }

    @Override
    public void write(final int b) throws IOException {
        write(new byte[] { (byte) b }, 0, 1);
    }

    @Override
    public void write(final byte[] bytes, final int offset, final int length) throws IOException {
        if (length == 0) return;
        out.write(Integer.toHexString(length).getBytes(US_ASCII));
        out.write(CRLF);
        out.write(bytes, offset, length);
        out.write(CRLF);
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        if (finished) return;
        finished = true;
        out.write(TERMINATING_CHUNK);
        out.flush();
    }

}
