package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;

import java.io.IOException;
import java.security.SecureRandom;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.session.SessionStore.getValueForCookie;
import static httpserver.util.Encoding.encodeHex;

public interface RandomIdStore<T> extends SessionStore<T> {

    default int lengthSessionId() {
        return 32;
    }

    /**
     * Session identifiers are credentials: they must come from a cryptographically strong
     * generator. A plain java.util.Random is seeded from the clock and its output can be
     * reconstructed, which would let an attacker guess other users' session ids.
     */
    SecureRandom prngSessionId();
    void storeSession(String sessionId, T session) throws IOException;
    T retrieveSession(String sessionId) throws IOException;
    void deleteSession(String sessionId) throws IOException;

    default void setSession(final HttpServerExchange exchange, final T session) throws IOException {
        final byte[] bytes = new byte[lengthSessionId()];
        prngSessionId().nextBytes(bytes);
        final String sessionId = encodeHex(bytes);

        storeSession(sessionId, session);
        // Added rather than set: a response can carry other Set-Cookie headers too.
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionId + sessionCookieConfiguration());
    }

    default boolean existsSession(final HttpServerExchange exchange) {
        try {
            return getSession(exchange, null) != null;
        } catch (final Exception e) {
            return false;
        }
    }

    default T getSession(final HttpServerExchange exchange) throws Forbidden, IOException {
        final T session = getSession(exchange, null);
        if (session == null) throw new Forbidden();
        return session;
    }

    default T getSession(final HttpServerExchange exchange, final T defaultValue) throws IOException {
        final String sessionId = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionId == null) return defaultValue;

        return retrieveSession(sessionId);
    }

    default void deleteSession(final HttpServerExchange exchange) throws IOException {
        final String sessionId = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionId == null) return;

        deleteSession(sessionId);
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionCookieConfiguration());
    }

}
