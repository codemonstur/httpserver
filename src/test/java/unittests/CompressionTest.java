package unittests;

import httpserver.handlers.Compression;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static httpserver.core.Headers.CONTENT_ENCODING;
import static httpserver.core.Headers.CONTENT_LENGTH;
import static httpserver.core.Headers.TRANSFER_ENCODING;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static unittests.Exchanges.newExchange;
import static unittests.Exchanges.request;

public class CompressionTest {

    @Test
    public void testKnownLengthBodyIsReframedWithTheCompressedLength() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(request("GET", "/"), new ByteArrayInputStream(new byte[0]), out);
        exchange.send("hello world hello world".getBytes(UTF_8));

        Compression.compressGzip(exchange);
        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertTrue(response.contains(CONTENT_ENCODING + ": gzip"));
        assertTrue(response.contains(CONTENT_LENGTH + ": "));
        assertFalse(response.contains(TRANSFER_ENCODING));
    }

    @Test
    public void testChunkedBodyStreamsThroughUncompressed() throws IOException {
        final var out = new ByteArrayOutputStream();
        final var exchange = newExchange(request("GET", "/"), new ByteArrayInputStream(new byte[0]), out);
        exchange.sendChunked(new ByteArrayInputStream("hello".getBytes(UTF_8)));

        // A body of unknown length exists to be streamed, so it is not buffered into memory
        // for recompression; it passes through untouched, framed chunked.
        Compression.compressGzip(exchange);
        exchange.sendResponse();

        final String response = out.toString(UTF_8);
        assertFalse(response.contains(CONTENT_ENCODING));
        assertTrue(response.contains(TRANSFER_ENCODING + ": chunked"));
        assertTrue(response.endsWith("\r\n0\r\n\r\n"));
        assertTrue(response.contains("hello"));
    }

}
