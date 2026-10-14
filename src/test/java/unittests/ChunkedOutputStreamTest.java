package unittests;

import httpserver.util.ChunkedOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ChunkedOutputStreamTest {

    @Test
    public void testFramesEachWriteAsAChunk() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var chunked = new ChunkedOutputStream(out);

        chunked.write("hel".getBytes(US_ASCII));
        chunked.write("lo".getBytes(US_ASCII));
        chunked.close();

        assertEquals("3\r\nhel\r\n2\r\nlo\r\n0\r\n\r\n", out.toString(US_ASCII));
    }

    @Test
    public void testSingleByteWriteIsAOneByteChunk() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var chunked = new ChunkedOutputStream(out);

        chunked.write('x');
        chunked.close();

        assertEquals("1\r\nx\r\n0\r\n\r\n", out.toString(US_ASCII));
    }

    @Test
    public void testEmptyWritesWriteNothing() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var chunked = new ChunkedOutputStream(out);

        chunked.write(new byte[0]);
        chunked.write(new byte[4], 2, 0);
        chunked.close();

        assertEquals("0\r\n\r\n", out.toString(US_ASCII));
    }

    @Test
    public void testCloseTerminatesOnlyOnce() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var chunked = new ChunkedOutputStream(out);

        chunked.write("hi".getBytes(US_ASCII));
        chunked.close();
        chunked.close();

        assertEquals("2\r\nhi\r\n0\r\n\r\n", out.toString(US_ASCII));
    }

    @Test
    public void testNeverClosesTheUnderlyingStream() throws IOException {
        final var out = new ByteArrayOutputStream() {
            boolean closed;
            @Override public void close() { closed = true; }
        };
        final var chunked = new ChunkedOutputStream(out);

        chunked.write("hi".getBytes(US_ASCII));
        chunked.close();

        assertFalse(out.closed);
        assertEquals("2\r\nhi\r\n0\r\n\r\n", out.toString(US_ASCII));
    }

}
