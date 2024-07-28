package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.Forbidden;

import java.io.IOException;
import java.nio.charset.Charset;

import static httpserver.core.Headers.SET_COOKIE;
import static httpserver.session.SessionStore.getValueForCookie;
import static httpserver.util.Encoding.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public interface TokenSessionStore<T> extends SessionStore<T> {

    byte[] getSessionValidationKey();

    default void setSession(final HttpServerExchange exchange, final T session) {
        final String sessionValue = toSessionValue(session);
        exchange.setResponseHeader(SET_COOKIE, getSessionCookieName() + "=" + sessionValue + getSessionCookieConfiguration());
    }
    default boolean existsSession(final HttpServerExchange exchange) {
        try {
            return getSession(exchange, null) != null;
        } catch (final Exception e) {
            return false;
        }
    }
    default T getSession(final HttpServerExchange exchange) throws IOException {
        final T session = getSession(exchange, null);
        if (session == null) throw new Forbidden();
        return session;
    }
    default T getSession(final HttpServerExchange exchange, final T defaultValue) throws IOException {
        final String sessionValue = getValueForCookie(exchange, getSessionCookieName(), null);
        if (sessionValue == null) return defaultValue;
        return fromSessionValue(sessionValue);
    }
    default void deleteSession(final HttpServerExchange exchange) {
        final String sessionId = getValueForCookie(exchange, getSessionCookieName(), null);
        if (sessionId == null) return;
        exchange.setResponseHeader(SET_COOKIE, getSessionCookieName() + "=" + getSessionCookieConfiguration());
    }

    byte[] sessionToJson(T session, Charset charset);
    T sessionFromJson(String json);

    private String toSessionValue(final T session) {
        final String encodedSession = encodeBase64Url(sessionToJson(session, UTF_8));
        return encodedSession + "." + toVerification(getSessionValidationKey(), encodedSession);
    }
    default T fromSessionValue(final String sessionValue) throws Forbidden {
        final String[] encodedSession = sessionValue.split("\\.");
        if (isValidSession(encodedSession, getSessionValidationKey()))
            throw new Forbidden("Invalid session");
        return sessionFromJson(new String(decodeBase64Url(encodedSession[0]), UTF_8));
    }

    private static boolean isValidSession(final String[] encodedSession, final byte[] key) {
        return encodedSession != null && encodedSession.length == 2
            && encodedSession[1].equals(toVerification(key, encodedSession[0]));
    }
    private static String toVerification(final byte[] key, final String data) {
        return encodeBase64Url(hmacSha256(data.getBytes(UTF_8), key));
    }

}
