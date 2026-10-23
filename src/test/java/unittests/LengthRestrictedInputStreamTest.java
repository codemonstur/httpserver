package unittests;

import httpserver.util.LengthRestrictedInputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.*;

public class LengthRestrictedInputStreamTest {

    private static LengthRestrictedInputStream stream(final String content, final long length) {
        return new LengthRestrictedInputStream(new ByteArrayInputStream(content.getBytes(US_ASCII)), length);
    }

    @Test
    public void testReadSingleBytesStopsAtLimit() throws IOException {
        final var in = stream("hello world", 5);

        assertEquals('h', in.read());
        assertEquals('e', in.read());
        assertEquals('l', in.read());
        assertEquals('l', in.read());
        assertEquals('o', in.read());
        assertEquals(-1, in.read());
        assertEquals(-1, in.read());
    }

    @Test
    public void testReadArrayStopsAtLimit() throws IOException {
        final var in = stream("hello world", 5);

        final byte[] buffer = new byte[64];
        final int read = in.read(buffer);

        assertEquals(5, read);
        assertEquals("hello", new String(buffer, 0, read, US_ASCII));
        assertEquals(-1, in.read(buffer));
    }

    @Test
    public void testReadWithOffsetStopsAtLimit() throws IOException {
        final var in = stream("hello world", 5);

        final byte[] buffer = new byte[64];
        final int read = in.read(buffer, 2, 64);

        assertEquals(5, read);
        assertEquals("hello", new String(buffer, 2, read, US_ASCII));
        assertEquals(-1, in.read(buffer, 2, 64));
    }

    @Test
    public void testReadWithSmallBufferIsStillLimited() throws IOException {
        final var in = stream("hello world", 5);

        final byte[] buffer = new byte[2];
        assertEquals(2, in.read(buffer));
        assertEquals(2, in.read(buffer));
        assertEquals(1, in.read(buffer));
        assertEquals(-1, in.read(buffer));
    }

    @Test
    public void testZeroLengthStreamIsExhausted() throws IOException {
        final var in = stream("hello", 0);

        assertEquals(-1, in.read());
        assertEquals(-1, in.read(new byte[8]));
        assertEquals(0, in.available());
    }

    @Test
    public void testPartialReadsDoNotConsumeTheWholeBudget() throws IOException {
        // A socket can deliver fewer bytes than requested, so the limit may only shrink
        // by the number of bytes actually read.
        final var chunked = new ChunkedStream("hello world".getBytes(US_ASCII), 2);
        final var in = new LengthRestrictedInputStream(chunked, 5);

        final byte[] buffer = new byte[64];
        final var out = new ByteArrayOutputStream();
        int read; while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);

        assertEquals("hello", out.toString(US_ASCII));
        assertEquals(-1, in.read(buffer));
    }

    @Test
    public void testAvailableIsCappedByLimit() throws IOException {
        final var in = stream("hello world", 5);

        assertEquals(5, in.available());
        in.read();
        assertEquals(4, in.available());
        in.read(new byte[4]);
        assertEquals(0, in.available());
    }

    @Test
    public void testSkipIsCappedByLimit() throws IOException {
        final var in = stream("hello world", 5);

        assertEquals(3, in.skip(3));
        assertEquals(2, in.skip(10));
        assertEquals(0, in.skip(1));
        assertEquals(-1, in.read());
    }

    @Test
    public void testCloseDoesNotCloseUnderlyingStream() throws IOException {
        final var underlying = new TrackingStream("hello".getBytes(US_ASCII));
        final var in = new LengthRestrictedInputStream(underlying, 5);

        in.close();

        assertFalse(underlying.closed);
    }

    @Test
    public void testMarkSupportIsDelegated() {
        final var underlying = new TrackingStream("hello".getBytes(US_ASCII));

        assertTrue(new LengthRestrictedInputStream(underlying, 5).markSupported());
    }

    private static final class TrackingStream extends ByteArrayInputStream {

        private boolean closed;

        TrackingStream(final byte[] data) {
            super(data);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class ChunkedStream extends InputStream {

        private final InputStream in;
        private final int chunk;

        ChunkedStream(final byte[] data, final int chunk) {
            this.in = new ByteArrayInputStream(data);
            this.chunk = chunk;
        }

        @Override
        public int read() throws IOException {
            return in.read();
        }
        @Override
        public int read(final byte[] buffer, final int offset, final int length) throws IOException {
            return in.read(buffer, offset, Math.min(length, chunk));
        }
    }

}
