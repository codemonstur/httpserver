package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.session.SessionStore.getValueForCookie;
import static httpserver.util.Encoding.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * The {@code TokenSessionStore} interface.
 *
 * @param <T> the type parameter
 */
public interface TokenSessionStore<T> extends SessionStore<T> {

    /**
     * Returns the session validation key.
     *
     * @return the result
     */
    byte[] getSessionValidationKey();

    /**
     * Performs the {@code setSession} operation.
     *
     * @param exchange the new value
     * @param session the new value
     */
    default void setSession(final HttpServerExchange exchange, final T session) {
        final String sessionValue = toSessionValue(session);
        // Added rather than set: a response can carry other Set-Cookie headers, and replacing
        // the whole header would silently drop them.
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionValue + sessionCookieConfiguration());
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
     * @throws IOException if the operation fails
     */
    default T getSession(final HttpServerExchange exchange) throws IOException {
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
        final String sessionValue = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionValue == null) return defaultValue;
        return fromSessionValue(sessionValue);
    }
    /**
     * Performs the {@code deleteSession} operation.
     *
     * @param exchange the exchange
     */
    default void deleteSession(final HttpServerExchange exchange) {
        final String sessionId = getValueForCookie(exchange, sessionCookieName(), null);
        if (sessionId == null) return;
        exchange.addResponseHeader(SET_COOKIE, sessionCookieName() + "=" + sessionCookieConfiguration());
    }

    /**
     * Returns the result of {@code sessionToJson}.
     *
     * @param session the session
     * @param charset the charset
     * @return the result
     */
    byte[] sessionToJson(T session, Charset charset);
    /**
     * Returns the result of {@code sessionFromJson}.
     *
     * @param json the json
     * @return the result
     */
    T sessionFromJson(String json);

    private String toSessionValue(final T session) {
        final String encodedSession = encodeBase64Url(sessionToJson(session, UTF_8));
        return encodedSession + "." + toVerification(getSessionValidationKey(), encodedSession);
    }
    /**
     * Returns the result of {@code fromSessionValue}.
     *
     * @param sessionValue the session value
     * @return the result
     * @throws Forbidden if the operation fails
     */
    default T fromSessionValue(final String sessionValue) throws Forbidden {
        final String[] encodedSession = sessionValue.split("\\.");
        if (!isValidSession(encodedSession, getSessionValidationKey()))
            throw new Forbidden("Invalid session");
        return sessionFromJson(new String(decodeBase64Url(encodedSession[0]), UTF_8));
    }

    private static boolean isValidSession(final String[] encodedSession, final byte[] key) {
        if (encodedSession == null || encodedSession.length != 2) return false;
        // Constant time comparison: an equals() here leaks how many signature bytes matched,
        // which is enough to forge a valid session one byte at a time.
        final byte[] expected = toVerification(key, encodedSession[0]).getBytes(UTF_8);
        final byte[] actual = encodedSession[1].getBytes(UTF_8);
        return MessageDigest.isEqual(expected, actual);
    }
    private static String toVerification(final byte[] key, final String data) {
        return encodeBase64Url(hmacSha256(data.getBytes(UTF_8), key));
    }

}
