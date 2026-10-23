package unittests;

import httpserver.core.ResponseBody;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static httpserver.core.ResponseBody.EMPTY_BODY;
import static httpserver.core.ResponseBody.newByteArrayBody;
import static httpserver.core.ResponseBody.newInputStreamBody;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ResponseBodyTest {

    private static String writeTo(final ResponseBody body) throws IOException {
        final var out = new ByteArrayOutputStream();
        body.writeTo(out);
        return out.toString(UTF_8);
    }

    @Test
    public void testEmptyBody() throws IOException {
        assertEquals(0, EMPTY_BODY.getLength());
        assertEquals("", writeTo(EMPTY_BODY));
    }

    @Test
    public void testByteArrayBody() throws IOException {
        final var body = newByteArrayBody("hello".getBytes(UTF_8));

        assertEquals(5, body.getLength());
        assertEquals("hello", writeTo(body));
    }

    @Test
    public void testByteArrayBodyWithOffsetAndLength() throws IOException {
        final var body = newByteArrayBody("xhellox".getBytes(UTF_8), 1, 5);

        assertEquals(5, body.getLength());
        assertEquals("hello", writeTo(body));
    }

    @Test
    public void testInputStreamBodyIsLimitedByLength() throws IOException {
        final var body = newInputStreamBody(new ByteArrayInputStream("hello world".getBytes(UTF_8)), 5);

        assertEquals(5, body.getLength());
        assertEquals("hello", writeTo(body));
    }

    @Test
    public void testInputStreamBodyWithEmptyContent() throws IOException {
        final var body = newInputStreamBody(new ByteArrayInputStream(new byte[0]), 5);

        assertEquals(5, body.getLength());
        assertEquals("", writeTo(body));
    }

    @Test
    public void testInputStreamBodyShorterThanLength() throws IOException {
        final var body = newInputStreamBody(new ByteArrayInputStream("hi".getBytes(UTF_8)), 100);

        assertEquals("hi", writeTo(body));
    }

}
