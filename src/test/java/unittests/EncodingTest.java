package unittests;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static httpserver.util.Encoding.decodeBase64Url;
import static httpserver.util.Encoding.decodeUrl;
import static httpserver.util.Encoding.encodeBase64Url;
import static httpserver.util.Encoding.encodeHex;
import static httpserver.util.Encoding.encodeUrl;
import static httpserver.util.Encoding.escapeJson;
import static httpserver.util.Encoding.hmacSha256;
import static httpserver.util.Encoding.newSha256;
import static httpserver.util.Encoding.sha256;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class EncodingTest {

    @Test
    public void testSha256KnownVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                encodeHex(sha256(new byte[0])));
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                encodeHex(sha256("abc".getBytes(UTF_8))));
    }

    @Test
    public void testSha256WithCharsetMatchesByteArrayVersion() {
        assertEquals(encodeHex(sha256("abc".getBytes(UTF_8))), encodeHex(sha256("abc", UTF_8)));
    }

    @Test
    public void testNewSha256() {
        assertNotNull(newSha256());
    }

    @Test
    public void testHmacSha256Rfc4231TestCase2() {
        final byte[] mac = hmacSha256("what do ya want for nothing?".getBytes(UTF_8), "Jefe".getBytes(UTF_8));
        assertEquals("5bdcc146bf60754e6a042426089575c75a003f089d2739839dec58b964ec3843", encodeHex(mac));
    }

    @Test
    public void testEncodeHex() {
        assertEquals("", encodeHex(new byte[0]));
        assertEquals("00", encodeHex(new byte[]{0x00}));
        assertEquals("01ff", encodeHex(new byte[]{0x01, (byte) 0xff}));
    }

    @Test
    public void testEncodeAndDecodeUrl() {
        assertEquals("a+b%26c%3Dd", encodeUrl("a b&c=d"));
        assertEquals("a b&c=d", decodeUrl("a+b%26c%3Dd"));
        assertEquals("", encodeUrl(null));
        assertEquals("", decodeUrl(null));
    }

    @Test
    public void testBase64UrlRoundTrip() {
        final byte[] data = "some data with symbols +/=".getBytes(UTF_8);

        final String encoded = encodeBase64Url(data);
        assertEquals("c29tZSBkYXRhIHdpdGggc3ltYm9scyArLz0=", encoded);
        assertFalse(encoded.contains("+"));
        assertFalse(encoded.contains("/"));

        assertArrayEquals(data, decodeBase64Url(encoded));
    }

    @Test
    public void testBase64UrlUsesTheUrlSafeAlphabet() {
        final byte[] data = new byte[]{(byte) 0xfb, (byte) 0xff};

        final String encoded = encodeBase64Url(data);

        assertEquals("+/8=", Base64.getEncoder().encodeToString(data));
        assertEquals("-_8=", encoded);

        assertArrayEquals(data, decodeBase64Url(encoded));
    }

    @Test
    public void testEscapeJson() {
        assertEquals("", escapeJson(null));
        assertEquals("", escapeJson(""));
        assertEquals("plain text", escapeJson("plain text"));
        assertEquals("\\\"quoted\\\"", escapeJson("\"quoted\""));
        assertEquals("line\\nbreak", escapeJson("line\nbreak"));
        assertEquals("tab\\there", escapeJson("tab\there"));
        assertEquals("carriage\\rreturn", escapeJson("carriage\rreturn"));
        assertEquals("form\\ffeed", escapeJson("form\ffeed"));
        assertEquals("back\\bspace", escapeJson("back\bspace"));
        assertEquals("back\\\\slash", escapeJson("back\\slash"));
        assertEquals("forward\\/slash", escapeJson("forward/slash"));
    }

}
