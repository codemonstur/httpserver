package unittests;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static httpserver.core.RequestParsing.readRequestHead;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class RequestParsingTest {

    @Test
    public void testReadsCompleteHead() throws IOException {
        final byte[] request = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\nbody data".getBytes(US_ASCII);

        final byte[] data = new byte[1024];
        final int length = readRequestHead(data, new ByteArrayInputStream(request));

        assertEquals("GET / HTTP/1.1\r\nHost: localhost\r\n\r\n", new String(data, 0, length, US_ASCII));
    }

    @Test
    public void testReadsHeadThatIsOnlyTheTerminator() throws IOException {
        final byte[] data = new byte[1024];

        final int length = readRequestHead(data, new ByteArrayInputStream("\r\n\r\n".getBytes(US_ASCII)));

        assertEquals(4, length);
    }

    @Test
    public void testThrowsWhenHeadIsTooLarge() throws IOException {
        final byte[] data = new byte[8];
        try {
            readRequestHead(data, new ByteArrayInputStream("aaaaaaaaaaaaaaaa".getBytes(US_ASCII)));
            fail("Expected IOException for an oversized request head");
        } catch (final IOException e) {
            assertEquals("Request header data too large", e.getMessage());
        }
    }

    @Test
    public void testReturnsWhatWasReadWhenStreamEndsEarly() throws IOException {
        final byte[] data = new byte[1024];

        final int length = readRequestHead(data, new ByteArrayInputStream("GET / HTTP/1.1".getBytes(US_ASCII)));

        assertEquals("GET / HTTP/1.1", new String(data, 0, length, US_ASCII));
    }

}
