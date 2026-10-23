package unittests;

import org.junit.jupiter.api.Test;

import static httpserver.core.Headers.isContentHeader;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeadersTest {

    @Test
    public void testIsContentHeader() {
        assertTrue(isContentHeader("Content-Type"));
        assertTrue(isContentHeader("Content-Length"));
        assertTrue(isContentHeader("Content-Encoding"));
        assertTrue(isContentHeader("Content-Disposition"));
    }

    @Test
    public void testNonContentHeaders() {
        assertFalse(isContentHeader("Host"));
        assertFalse(isContentHeader("Content"));
        assertFalse(isContentHeader("X-Content-Type-Options"));
        assertFalse(isContentHeader(""));
        assertFalse(isContentHeader(null));
    }

}
