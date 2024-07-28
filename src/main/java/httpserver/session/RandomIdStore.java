package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;

import java.io.IOException;
import java.util.Random;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.session.SessionStore.getValueForCookie;
import static httpserver.util.Encoding.encodeHex;

public interface RandomIdStore<T> extends SessionStore<T> {

    default int lengthSessionId() {
        return 32;
    }

    Random prngSessionId();
    void storeSession(String sessionId, T session) throws IOException;
    T retrieveSession(String sessionId) throws IOException;
    void deleteSession(String sessionId) throws IOException;

    default void setSession(final HttpServerExchange exchange, final T session) throws IOException {
        final byte[] bytes = new byte[lengthSessionId()];
        prngSessionId().nextBytes(bytes);
        final String sessionId = encodeHex(bytes);

        storeSession(sessionId, session);
        exchange.setResponseHeader(SET_COOKIE, getSessionCookieName() + "=" + sessionId + getSessionCookieConfiguration());
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
        final String sessionId = getValueForCookie(exchange, getSessionCookieName(), null);
        if (sessionId == null) return defaultValue;

        return retrieveSession(sessionId);
    }

    default void deleteSession(final HttpServerExchange exchange) throws IOException {
        final String sessionId = getValueForCookie(exchange, getSessionCookieName(), null);
        if (sessionId == null) return;

        deleteSession(sessionId);
        exchange.setResponseHeader(SET_COOKIE, getSessionCookieName() + "=" + getSessionCookieConfiguration());
    }

}
