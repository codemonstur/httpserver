package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;
import httpserver.error.HttpError;
import httpserver.util.Functions;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Consumer;

import static httpserver.core.CacheControlStrategy.NEVER_CACHE;
import static httpserver.core.CacheControlStrategy.STORE_BUT_CHECK_SERVER;
import static httpserver.core.ContentType.*;
import static httpserver.core.Headers.*;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.*;
import static httpserver.error.HttpError.respondError;
import static httpserver.error.HttpError.respondInternalError;
import static httpserver.util.Encoding.encodeHex;
import static httpserver.util.Encoding.sha256;
import static java.nio.charset.StandardCharsets.UTF_8;

public enum Common {;

    public static HttpHandler printText(final String text) {
        return exchange -> respond(exchange).status(OK).contentType(text_plain).send(text);
    }

    public static HttpHandler printVersion() {
        final var version = Functions.orDefault(Common.class.getPackage().getImplementationVersion(), "unknown");
        return exchange -> respond(exchange).status(OK).contentType(text_plain).send(version);
    }

    public static HttpHandler printBanner() {
        final var manifest = Common.class.getPackage();
        final var title = Functions.orDefault(manifest.getImplementationTitle(), "unknown");
        final var version = Functions.orDefault(manifest.getImplementationVersion(), "unknown");
        final var banner = title + " v" + version;
        return exchange -> respond(exchange).status(OK).contentType(text_plain).send(banner);
    }

    public static HttpHandler logError(final Consumer<Exception> onException, final Consumer<Error> onError, final HttpHandler next) {
        return exchange -> {
            try {
                next.handleRequest(exchange);
            } catch (final Exception e) {
                if (e instanceof final HttpError error)
                    error.processExchange(exchange);
                else {
                    onException.accept(e);
                    respondInternalError(exchange);
                }
            } catch (final Error e) {
                onError.accept(e);
                throw e;
            }
        };
    }

    public static HttpHandler pathPrefix(final String prefix, final HttpHandler ifTrue, final HttpHandler ifFalse) {
        return exchange -> (exchange.getRequestPath().startsWith(prefix) ? ifTrue : ifFalse).handleRequest(exchange);
    }

    public static HttpHandler staticHtml(final String content) {
        final String etag = String.format("\"%s\"", encodeHex(sha256(content, UTF_8)));

        return exchange -> {
            if (matchesEtag(exchange, etag)) {
                respond(exchange).status(NOT_MODIFIED).contentType(text_html).send();
            } else {
                respond(exchange).status(OK).contentType(text_html)
                        .cache(STORE_BUT_CHECK_SERVER)
                        .header(ETAG, etag).send(content);
            }
        };
    }

    /**
     * If-None-Match uses the weak comparison function, and the header may hold a list of tags or
     * the "*" wildcard, so a plain equality check would miss most conditional requests.
     */
    public static boolean matchesEtag(final HttpServerExchange request, final String etag) {
        final String header = request.getRequestHeader(IF_NONE_MATCH);
        if (header == null) return false;
        if ("*".equals(header.trim())) return true;

        final String expected = withoutWeakPrefix(etag);
        for (final String candidate : header.split(",")) {
            if (withoutWeakPrefix(candidate.trim()).equals(expected)) return true;
        }
        return false;
    }
    private static String withoutWeakPrefix(final String tag) {
        return tag.startsWith("W/") ? tag.substring(2) : tag;
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

    public static HttpHandler singlePageApp(final String pageContent, final Set<String> pageURIs) {
        final var notFound = html404(pageContent);
        final var found = staticHtml(pageContent);
        return exchange -> (pageURIs.contains(exchange.getRequestPath()) ? found : notFound).handleRequest(exchange);
    }

    public static HttpHandler resourceHandler(final String wwwroot, final HttpHandler fallback) {
        final var etagMap = new HashMap<String, String>();
        return exchange -> {
            // The request path, without the query string: "/style.css?v=1" is still style.css.
            final var requestPath = exchange.getRequestPath();
            final var data = loadResource(wwwroot, requestPath, null);

            if (data == null) fallback.handleRequest(exchange);
            else {
                final var type = toContentType(requestPath);
                final var etag = String.format("\"%s\"", etagMap.computeIfAbsent(requestPath, k-> encodeHex(sha256(data))));

                if (matchesEtag(exchange, etag)) {
                    respond(exchange).status(NOT_MODIFIED).cache(STORE_BUT_CHECK_SERVER).contentType(type).send();
                } else {
                    respond(exchange).status(OK).cache(STORE_BUT_CHECK_SERVER).contentType(type).header(ETAG, etag).send(data);
                }
            }
        };
    }
    private static byte[] loadResource(final String prefix, final String requestURI, final byte[] defaultValue) throws IOException {
        if (requestURI == null || requestURI.isBlank()
                || requestURI.endsWith("/") || requestURI.endsWith("\\")) return defaultValue;

        // Compare resolved paths, not strings: a plain startsWith(prefix) lets "/../<prefix>-other/x"
        // escape the resource root, because "wwwroot-other" starts with "wwwroot".
        final var root = Path.of(prefix).normalize();
        final var relative = requestURI.startsWith("/") ? requestURI.substring(1) : requestURI;
        final var requestedPath = root.resolve(relative).normalize();
        if (!requestedPath.startsWith(root)) return defaultValue;

        try (final InputStream in = Functions.class.getResourceAsStream(requestedPath.toString().replace('\\', '/'))) {
            if (in == null) return defaultValue;
            return in.readAllBytes();
        }
    }

}
