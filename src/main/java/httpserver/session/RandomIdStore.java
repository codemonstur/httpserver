package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;

import java.io.IOException;
import java.security.SecureRandom;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.session.SessionStore.getValueForCookie;
import static httpserver.util.Encoding.encodeHex;

/**
 * The {@code RandomIdStore} interface.
 *
 * @param <T> the type parameter
 */
public interface RandomIdStore<T> extends SessionStore<T> {

    /**
     * Returns the result of {@code lengthSessionId}.
     *
     * @return the result
     */
    default int lengthSessionId() {
        return 32;
    }

    /**
     * Session identifiers are credentials: they must come from a cryptographically strong
     * generator. A plain java.util.Random is seeded from the clock and its output can be
     * reconstructed, which would let an attacker guess other users' session ids.
     
     * @return the result
     */
    SecureRandom prngSessionId();
    /**
     * Performs the {@code storeSession} operation.
     *
     * @param sessionId the session id
     * @param session the session
     * @throws IOException if the operation fails
     */
    void storeSession(String sessionId, T session) throws IOException;
    /**
     * Returns the result of {@code retrieveSession}.
     *
     * @param sessionId the session id
     * @return the result
     * @throws IOException if the operation fails
     */
    T retrieveSession(String sessionId) throws IOException;
    /**
     * Performs the {@code deleteSession} operation.
     *
     * @param sessionId the session id
     * @throws IOException if the operation fails
     */
    void deleteSession(String sessionId) throws IOException;

    /**
     * Performs the {@code setSession} operation.
     *
     * @param exchange the new value
     * @param session the new value
     * @throws IOException if the operation fails
     */
    default void setSession(final HttpServerExchange exchange, final T session) throws IOException {
        final byte[] bytes = new byte[lengthSessionId()];
        prngSessionId().nextBytes(bytes);
        final String sessionId = encodeHex(bytes);

        storeSession(sessionId, session);
        // Added rather than set: a response can carry other Set-Cookie headers too.
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionId + sessionCookieConfiguration());
    }

    /**
     * Returns the result of {@code existsSession}.
     *
     * @param exchange the exchange
     * @return whether the condition holds
     */
    default boolean existsSession(final HttpServerExchange exchange) {
        try {
            return getSession(exchange, null) != null;
        } catch (final Exception e) {
            return false;
        }
    }

    /**
     * Returns the result of {@code getSession}.
     *
     * @param exchange the exchange
     * @return the result
     * @throws Forbidden if the operation fails
     * @throws IOException if the operation fails
     */
    default T getSession(final HttpServerExchange exchange) throws Forbidden, IOException {
        final T session = getSession(exchange, null);
        if (session == null) throw new Forbidden();
        return session;
    }

    /**
     * Returns the result of {@code getSession}.
     *
     * @param exchange the exchange
     * @param defaultValue the default value
     * @return the result
     * @throws IOException if the operation fails
     */
    default T getSession(final HttpServerExchange exchange, final T defaultValue) throws IOException {
        final String sessionId = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionId == null) return defaultValue;

        return retrieveSession(sessionId);
    }

    /**
     * Performs the {@code deleteSession} operation.
     *
     * @param exchange the exchange
     * @throws IOException if the operation fails
     */
    default void deleteSession(final HttpServerExchange exchange) throws IOException {
        final String sessionId = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionId == null) return;

        deleteSession(sessionId);
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionCookieConfiguration());
    }

}
