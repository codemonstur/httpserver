package httpserver.util;

import java.io.IOException;
import java.io.InputStream;

public class LengthRestrictedInputStream extends InputStream {

    private final InputStream in;
    private long toRead;

    public LengthRestrictedInputStream(final InputStream in, final long length) {
        this.in = in;
        this.toRead = length;
    }

    @Override
    public int available() throws IOException {
        if (toRead <= 0) return 0;

        return Math.min(in.available(), (int)toRead);
    }

    @Override
    public void close() {
        // skip, we don't want to close the actual stream
    }

    @Override
    public void mark(final int readLimit) {
        in.mark(readLimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read(final byte[] buffer) throws IOException {
        if (toRead <= 0) return -1;

        final int bytesToRead = (int) Math.min(Math.min(buffer.length, toRead), 8192);
        final int bytesRead = in.read(buffer, 0, bytesToRead);
        toRead -= bytesToRead;
        return bytesRead;
    }

    @Override
    public int read(final byte[] buffer, final int offset, final int length) throws IOException {
        if (toRead <= 0) return -1;

        final int bytesToRead = Math.min( (int) Math.min(Math.min(buffer.length, toRead), 8192), length);
        final int bytesRead = in.read(buffer, offset, bytesToRead);
        toRead -= bytesToRead;
        return bytesRead;
    }

    @Override
    public int read() throws IOException {
        if (toRead <= 0) return -1;

        toRead--;
        return in.read();
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(final long n) throws IOException {
        if (toRead <= 0) return 0;

        final long bytesToSkip = Math.min(n, toRead);
        final long skipped = in.skip(bytesToSkip);
        toRead -= skipped;
        return skipped;
    }

}
