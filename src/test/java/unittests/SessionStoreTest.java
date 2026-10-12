package unittests;

import httpserver.core.HttpServerExchange;
import org.junit.Test;

import java.io.IOException;

import static httpserver.session.SessionStore.getValueForCookie;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static unittests.Exchanges.newExchange;

public class SessionStoreTest {

    private static HttpServerExchange withCookieHeaders(final String... cookies) throws IOException {
        final var builder = new StringBuilder("GET / HTTP/1.1\r\n");
        for (final String cookie : cookies) builder.append("Cookie: ").append(cookie).append("\r\n");
        builder.append("\r\n");
        return newExchange(builder.toString());
    }

    @Test
    public void testExtractsCookieValue() throws IOException {
        final var exchange = withCookieHeaders("session=abc123");

        assertEquals("abc123", getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testExtractsCookieAmongOthers() throws IOException {
        final var exchange = withCookieHeaders("theme=dark; session=abc123; other=x");

        assertEquals("abc123", getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testSupportsMultipleCookieHeaders() throws IOException {
        final var exchange = withCookieHeaders("theme=dark", "session=abc123");

        assertEquals("abc123", getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testTrimsWhitespaceAroundCookies() throws IOException {
        final var exchange = withCookieHeaders(" theme=dark ; session=abc123 ");

        assertEquals("abc123", getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testDoesNotMatchCookiesWithAPrefixOfTheName() throws IOException {
        final var exchange = withCookieHeaders("sessionid=abc123");

        assertNull(getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testReturnsDefaultWhenCookieIsMissing() throws IOException {
        final var exchange = withCookieHeaders("theme=dark");

        assertEquals("fallback", getValueForCookie(exchange, "session", "fallback"));
        assertNull(getValueForCookie(exchange, "session", null));
    }

    @Test
    public void testReturnsDefaultWithoutCookieHeaders() throws IOException {
        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");

        assertEquals("fallback", getValueForCookie(exchange, "session", "fallback"));
    }

}
