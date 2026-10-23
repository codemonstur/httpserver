package unittests;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;
import httpserver.session.TokenSessionStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.Charset;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.util.Encoding.encodeBase64Url;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;
import static unittests.Exchanges.newExchange;

public class TokenSessionStoreTest {

    private static final class TestSessionStore implements TokenSessionStore<String> {

        private final byte[] validationKey;

        TestSessionStore(final String validationKey) {
            this.validationKey = validationKey.getBytes(UTF_8);
        }

        @Override
        public byte[] getSessionValidationKey() {
            return validationKey;
        }
        @Override
        public byte[] sessionToJson(final String session, final Charset charset) {
            return session.getBytes(charset);
        }
        @Override
        public String sessionFromJson(final String json) {
            return json;
        }
    }

    private static String sessionValueFrom(final HttpServerExchange exchange) {
        final String setCookie = exchange.getResponseHeader(SET_COOKIE);
        assertTrue(setCookie.startsWith("session="), "Expected a session cookie, found: " + setCookie);
        return setCookie.substring("session=".length(), setCookie.indexOf(';'));
    }

    private static HttpServerExchange exchangeWithSessionCookie(final String sessionValue) throws IOException {
        return newExchange("GET / HTTP/1.1\r\nCookie: session=" + sessionValue + "\r\n\r\n");
    }

    private static void assertForbidden(final TestSessionStore store, final String sessionValue) {
        try {
            store.fromSessionValue(sessionValue);
            fail("Expected Forbidden for session value: " + sessionValue);
        } catch (final Forbidden e) {
            assertEquals("Invalid session", e.getMessage());
        }
    }

    @Test
    public void testSessionRoundTrip() throws IOException {
        final var store = new TestSessionStore("validation-key");

        final var setSessionExchange = newExchange("GET / HTTP/1.1\r\n\r\n");
        store.setSession(setSessionExchange, "my-session");
        final String sessionValue = sessionValueFrom(setSessionExchange);

        assertEquals("my-session", store.fromSessionValue(sessionValue));
        assertEquals("my-session", store.getSession(exchangeWithSessionCookie(sessionValue)));
        assertTrue(store.existsSession(exchangeWithSessionCookie(sessionValue)));
    }

    @Test
    public void testSetSessionIncludesTheCookieConfiguration() throws IOException {
        final var store = new TestSessionStore("validation-key");
        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");

        store.setSession(exchange, "my-session");

        assertTrue(exchange.getResponseHeader(SET_COOKIE)
                .contains("; Path=/; Secure; HttpOnly; SameSite=strict"));
    }

    @Test
    public void testTamperedSessionIsRejected() throws IOException {
        final var store = new TestSessionStore("validation-key");
        final var setSessionExchange = newExchange("GET / HTTP/1.1\r\n\r\n");
        store.setSession(setSessionExchange, "my-session");
        final String sessionValue = sessionValueFrom(setSessionExchange);

        final String tampered = encodeBase64Url("evil-session".getBytes(UTF_8))
                + sessionValue.substring(sessionValue.indexOf('.'));

        assertForbidden(store, tampered);
    }

    @Test
    public void testSessionWithoutSignatureIsRejected() {
        assertForbidden(new TestSessionStore("validation-key"), encodeBase64Url("my-session".getBytes(UTF_8)));
    }

    @Test
    public void testGarbageSessionValuesAreRejected() {
        final var store = new TestSessionStore("validation-key");

        assertForbidden(store, "not-a-session");
        assertForbidden(store, "");
        assertForbidden(store, "a.b.c");
    }

    @Test
    public void testSessionSignedWithAnotherKeyIsRejected() throws IOException {
        final var storeA = new TestSessionStore("key-a");
        final var storeB = new TestSessionStore("key-b");

        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");
        storeA.setSession(exchange, "my-session");
        final String sessionValue = sessionValueFrom(exchange);

        assertEquals("my-session", storeA.fromSessionValue(sessionValue));
        assertForbidden(storeB, sessionValue);
    }

    @Test
    public void testGetSessionWithoutCookieThrowsForbidden() throws IOException {
        final var store = new TestSessionStore("validation-key");
        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");

        try {
            store.getSession(exchange);
            fail("Expected Forbidden when no session cookie is present");
        } catch (final Forbidden e) {
            assertEquals("Forbidden", e.getMessage());
        }
        assertFalse(store.existsSession(exchange));
    }

    @Test
    public void testGetSessionFallsBackToTheDefaultValue() throws IOException {
        final var store = new TestSessionStore("validation-key");
        final var exchange = newExchange("GET / HTTP/1.1\r\n\r\n");

        assertEquals("default", store.getSession(exchange, "default"));
    }

}
