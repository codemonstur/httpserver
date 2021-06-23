package httpserver.core;

import httpserver.error.InvalidInput;

import java.io.IOException;
import java.util.List;

import static httpserver.core.Headers.COOKIE;
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Strings.SEMI_COLON;

public interface SessionStore<T> {

    default String getSessionCookieName() {
        return "session";
    }
    default String getSessionCookieConfiguration() {
        return "; Path=/; Secure; HttpOnly; SameSite=strict";
    }

    void setSession(final HttpServerExchange exchange, final T session) throws IOException;
    boolean existsSession(final HttpServerExchange exchange) throws IOException, InvalidInput;
    T getSession(final HttpServerExchange exchange) throws IOException, InvalidInput;
    T getSession(final HttpServerExchange exchange, final T defaultValue) throws IOException, InvalidInput;
    void deleteSession(final HttpServerExchange exchange) throws IOException;

    public static String getValueForCookie(final HttpServerExchange exchange, final String cookieName) {
        final List<String> headers = exchange.getRequestHeaders(COOKIE);
        if (headers == null || headers.isEmpty()) return null;

        final String cookiePrefix = cookieName + EQUALS;
        for (final String header : headers) {
            for (final String cookie: header.split(SEMI_COLON)) {
                if (cookie.trim().startsWith(cookiePrefix)) {
                    return cookie.substring(cookie.indexOf(EQUALS)+1).trim();
                }
            }
        }
        return null;
    }

}
