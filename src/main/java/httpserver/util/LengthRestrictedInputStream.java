package httpserver.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * The {@code LengthRestrictedInputStream} class.
 */
public class LengthRestrictedInputStream extends InputStream {

    private final InputStream in;
    private long toRead;

    /**
     * Creates a new instance.
     *
     * @param in the in
     * @param length the length
     */
    public LengthRestrictedInputStream(final InputStream in, final long length) {
        this.in = in;
        this.toRead = length;
    }

    /**
     * Returns the result of {@code available}.
     *
     * @return the result
     * @throws IOException if the operation fails
     */
    @Override
    public int available() throws IOException {
        if (toRead <= 0) return 0;

        return Math.min(in.available(), (int)toRead);
    }

    /**
     * Closes this resource.
     */
    @Override
    public void close() {
        // skip, we don't want to close the actual stream
    }

    /**
     * Performs the {@code mark} operation.
     *
     * @param readLimit the read limit
     */
    @Override
    public void mark(final int readLimit) {
        in.mark(readLimit);
    }

    /**
     * Returns the result of {@code markSupported}.
     *
     * @return whether the condition holds
     */
    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    /**
     * Returns the result of {@code read}.
     *
     * @param buffer the buffer
     * @return the result
     * @throws IOException if the operation fails
     */
    @Override
    public int read(final byte[] buffer) throws IOException {
        if (toRead <= 0) return -1;

        final int bytesToRead = (int) Math.min(Math.min(buffer.length, toRead), 8192);
        final int bytesRead = in.read(buffer, 0, bytesToRead);
        if (bytesRead > 0) toRead -= bytesRead;
        return bytesRead;
    }

    /**
     * Returns the result of {@code read}.
     *
     * @param buffer the buffer
     * @param offset the offset
     * @param length the length
     * @return the result
     * @throws IOException if the operation fails
     */
    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (toRead <= 0) return -1;

        final int bytesToRead = Math.min( (int) Math.min(Math.min(buffer.length, toRead), 8192), length);
        final int bytesRead = in.read(buffer, offset, bytesToRead);
        if (bytesRead > 0) toRead -= bytesRead;
        return bytesRead;
    }

    /**
     * How many bytes of the declared body are still unread. Used to decide whether draining the
     * body is going to block at all.
     
     * @return the result
     */
    public long remaining() {
        return toRead;
    }

    /**
     * Returns the result of {@code read}.
     *
     * @return the result
     * @throws IOException if the operation fails
     */
    @Override
    public int read() throws IOException {
        if (toRead <= 0) return -1;

        final int b = in.read();
        if (b != -1) toRead--;
        return b;
    }

    /**
     * Performs the {@code reset} operation.
     *
     * @throws IOException if the operation fails
     */
    @Override
    public void reset() throws IOException {
        in.reset();
    }

    /**
     * Returns the result of {@code skip}.
     *
     * @param n the n
     * @return the result
     * @throws IOException if the operation fails
     */
    @Override
    public long skip(final long n) throws IOException {
        if (toRead <= 0) return 0;

        final long bytesToSkip = Math.min(n, toRead);
        final long skipped = in.skip(bytesToSkip);
        toRead -= skipped;
        return skipped;
    }

}
