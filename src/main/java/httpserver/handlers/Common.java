package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import static httpserver.caching.CacheControlStrategy.NEVER_CACHE;
import static httpserver.caching.CacheControlStrategy.STORE_BUT_CHECK_SERVER;
import static httpserver.core.ContentType.text_html;
import static httpserver.core.Headers.*;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.*;
import static httpserver.error.HttpError.respondError;
import static httpserver.util.Encoding.encodeHex;
import static httpserver.util.Encoding.sha256;
import static java.nio.charset.StandardCharsets.UTF_8;

public enum Common {;

    public static HttpHandler staticHtml(final String content) {
        final String etag = String.format("\"%s\"", encodeHex(sha256(content, UTF_8)));

        return exchange -> {
            final var requestEtag = exchange.getRequestHeader(IF_NONE_MATCH);
            if (etag.equals(requestEtag)) {
                respond(exchange).status(NOT_MODIFIED).contentType(text_html).send();
            } else {
                respond(exchange).status(OK).contentType(text_html)
                        .cache(STORE_BUT_CHECK_SERVER)
                        .header(ETAG, etag).send(content);
            }
        };
    }

    public static boolean matchesEtag(final HttpServerExchange request, final String etag) {
        return (etag.equals(request.getRequestHeader(IF_NONE_MATCH)));
    }

    public static void checkEtag(final String etag, final HttpServerExchange exchange, final HttpHandler next) throws Exception {
        if (matchesEtag(exchange, etag)) {
            respond(exchange).status(NOT_MODIFIED).send();
            return;
        }
        next.handleRequest(exchange);
    }

    public static HttpHandler html404(final String html) {
        return exchange -> respond(exchange).status(NOT_FOUND)
            .cache(NEVER_CACHE).contentType(text_html).send(html);
    }

    public static HttpHandler statusCode(final int code) {
        return exchange -> exchange.setStatusCode(code);
    }

    public static HttpHandler redirect(final String url) {
        return exchange -> respond(exchange).status(FOUND).header(LOCATION, url).send();
    }

    public static HttpHandler noSuchApiEndpoint() {
        return exchange -> respondError(exchange, BAD_REQUEST, BAD_REQUEST, "No such API endpoint");
    }

    public static HttpHandler securityHeaders(final boolean isSecure, final HttpHandler next) {
        return exchange -> {
            if (isSecure) {
                exchange.setResponseHeader(STRICT_TRANSPORT_SECURITY, "max-age=31536000; includeSubDomains");
                exchange.setResponseHeader(CROSS_ORIGIN_OPENER_POLICY, "same-origin");
            }
            exchange.setResponseHeader(X_FRAME_OPTIONS, "sameorigin");
            exchange.setResponseHeader(X_XSS_PROTECTION, "1; mode=block");
            exchange.setResponseHeader(X_CONTENT_TYPE_OPTIONS, "nosniff");
            exchange.setResponseHeader(REFERRER_POLICY, "no-referrer");
            next.handleRequest(exchange);
        };
    }

}
