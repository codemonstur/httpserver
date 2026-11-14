package httpserver.session;

import httpserver.core.HttpServerExchange;
import httpserver.error.InvalidInput;

import java.io.IOException;
import java.util.List;

import static httpserver.core.Headers.COOKIE;
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Strings.SEMI_COLON;

/**
 * The {@code SessionStore} interface.
 *
 * @param <T> the type parameter
 */
public interface SessionStore<T> {

    /**
     * Returns the result of {@code sessionCookieName}.
     *
     * @return the result
     */
    default String sessionCookieName() {
        return "session";
    }
    /**
     * Returns the result of {@code sessionCookieConfiguration}.
     *
     * @return the result
     */
    default String sessionCookieConfiguration() {
        return "; Path=/; Secure; HttpOnly; SameSite=strict";
    }

    /**
     * Performs the {@code setSession} operation.
     *
     * @param exchange the new value
     * @param session the new value
     * @throws IOException if the operation fails
     */
    void setSession(final HttpServerExchange exchange, final T session) throws IOException;
    /**
     * Returns the result of {@code existsSession}.
     *
     * @param exchange the exchange
     * @return whether the condition holds
     * @throws IOException if the operation fails
     * @throws InvalidInput if the operation fails
     */
    boolean existsSession(final HttpServerExchange exchange) throws IOException, InvalidInput;
    /**
     * Returns the result of {@code getSession}.
     *
     * @param exchange the exchange
     * @return the result
     * @throws IOException if the operation fails
     * @throws InvalidInput if the operation fails
     */
    T getSession(final HttpServerExchange exchange) throws IOException, InvalidInput;
    /**
     * Returns the result of {@code getSession}.
     *
     * @param exchange the exchange
     * @param defaultValue the default value
     * @return the result
     * @throws IOException if the operation fails
     * @throws InvalidInput if the operation fails
     */
    T getSession(final HttpServerExchange exchange, final T defaultValue) throws IOException, InvalidInput;
    /**
     * Performs the {@code deleteSession} operation.
     *
     * @param exchange the exchange
     * @throws IOException if the operation fails
     */
    void deleteSession(final HttpServerExchange exchange) throws IOException;

    /**
     * Returns the result of {@code getValueForCookie}.
     *
     * @param exchange the exchange
     * @param cookieName the cookie name
     * @param defaultValue the default value
     * @return the result
     */
    public static String getValueForCookie(final HttpServerExchange exchange, final String cookieName,
                                           final String defaultValue) {
        final List<String> headers = exchange.getRequestHeaders(COOKIE);
        if (headers == null || headers.isEmpty()) return defaultValue;

        final String cookiePrefix = cookieName + EQUALS;
        for (final String header : headers) {
            for (final String cookie: header.split(SEMI_COLON)) {
                if (cookie.trim().startsWith(cookiePrefix)) {
                    return cookie.substring(cookie.indexOf(EQUALS)+1).trim();
                }
            }
        }
        return defaultValue;
    }

}
