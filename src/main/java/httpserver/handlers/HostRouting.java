package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;
import httpserver.core.StatusCode;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.Headers.HOST;
import static httpserver.core.StatusCode.NOT_FOUND;
import static java.util.Locale.ENGLISH;

/**
 * The {@code HostRouting} class.
 */
public class HostRouting {

    private HttpHandler fallback = exchange -> exchange.setStatusCode(NOT_FOUND);
    private Map<String, HttpHandler> hosts = new HashMap<>();

    /**
     * Returns the result of {@code hostRouting}.
     *
     * @return the result
     */
    public static HostRouting hostRouting() {
        return new HostRouting();
    }

    /**
     * Returns the result of {@code host}.
     *
     * @param host the host
     * @param handler the handler
     * @return the result
     */
    public HostRouting host(final String host, final HttpHandler handler) {
        this.hosts.put(host.toLowerCase(ENGLISH), handler);
        return this;
    }
    /**
     * Returns the result of {@code fallback}.
     *
     * @param fallback the fallback
     * @return the result
     */
    public HostRouting fallback(final HttpHandler fallback) {
        this.fallback = fallback;
        return this;
    }


    /**
     * Returns the result of {@code build}.
     *
     * @return the result
     */
    public HttpHandler build() {
        return exchange -> {
            final var hostHeader = removePortSection(exchange.getRequestHeader(HOST));
            final var handler = hostHeader == null
                ? fallback
                : hosts.getOrDefault(hostHeader.toLowerCase(ENGLISH), fallback);
            handler.handleRequest(exchange);
        };
    }

    private static String removePortSection(final String hostHeader) {
        if (hostHeader == null) return null;
        final String host = hostHeader.trim();

        // Bracketed IPv6 literal: "[::1]:8080" drops the port, "[::1]" is left alone. Splitting
        // on ':' here would cut the address in half.
        if (host.startsWith("[")) {
            final int closing = host.lastIndexOf(']');
            return closing == -1 ? host : host.substring(0, closing + 1);
        }

        // A single colon separates the port; two or more mean the value is IPv6 shorthand such
        // as "::1", where the last colon is part of the address rather than a port.
        final int offset = host.lastIndexOf(':');
        if (offset == -1) return host;
        return host.indexOf(':') == offset ? host.substring(0, offset) : host;
    }


    /**
     * Creates a new instance.
     */
    public HostRouting() {
    }
}
