package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;
import httpserver.core.StatusCode;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.Headers.HOST;
import static httpserver.core.StatusCode.NOT_FOUND;
import static java.util.Locale.ENGLISH;
import static java.util.Locale.of;

public class HostRouting {

    private HttpHandler fallback = exchange -> exchange.setStatusCode(NOT_FOUND);
    private Map<String, HttpHandler> hosts = new HashMap<>();

    public static HostRouting hostRouting() {
        return new HostRouting();
    }

    public HostRouting host(final String host, final HttpHandler handler) {
        this.hosts.put(host.toLowerCase(ENGLISH), handler);
        return this;
    }
    public HostRouting fallback(final HttpHandler fallback) {
        this.fallback = fallback;
        return this;
    }


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
        final int offset = hostHeader.lastIndexOf(':');
        return offset == -1 ? hostHeader : hostHeader.substring(0, offset);
    }

}
