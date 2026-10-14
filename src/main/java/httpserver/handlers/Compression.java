package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static httpserver.core.Headers.ACCEPT_ENCODING;
import static httpserver.core.Headers.CONTENT_ENCODING;
import static httpserver.core.Headers.CONTENT_LENGTH;
import static httpserver.core.Headers.VARY;
import static httpserver.core.StatusCode.NOT_MODIFIED;
import static httpserver.core.StatusCode.NO_CONTENT;
import static httpserver.util.Functions.isNullOrEmpty;
import static java.util.Locale.ENGLISH;

public enum Compression {;

    public static HttpHandler compress(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            markVary(exchange);
            if (!exchange.isResponseSent()) {
                if (acceptsEncoding(exchange, "gzip")) {
                   compressGzip(exchange);
                } else
                if (acceptsEncoding(exchange, "deflate")) {
                    compressDeflate(exchange);
                }
            }
        };
    }

    public static HttpHandler compressGzip(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            markVary(exchange);
            if (!exchange.isResponseSent() && acceptsEncoding(exchange, "gzip")) {
                compressGzip(exchange);
            }
        };
    }
    public static HttpHandler compressDeflate(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            markVary(exchange);
            if (!exchange.isResponseSent() && acceptsEncoding(exchange, "deflate")) {
                compressDeflate(exchange);
            }
        };
    }

    public static void compressGzip(final HttpServerExchange exchange) throws IOException {
        if (!canCompress(exchange)) return;
        exchange.setResponseHeader(CONTENT_ENCODING, "gzip");
        prepareRecompress(exchange);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (final GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                exchange.getResponseBody().writeTo(gzip);
            }
            exchange.send(out.toByteArray());
        }
    }

    public static void compressDeflate(final HttpServerExchange exchange) throws IOException {
        if (!canCompress(exchange)) return;
        exchange.setResponseHeader(CONTENT_ENCODING, "deflate");
        prepareRecompress(exchange);
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (final DeflaterOutputStream deflate = new DeflaterOutputStream(out)) {
                exchange.getResponseBody().writeTo(deflate);
            }
            exchange.send(out.toByteArray());
        }
    }

    /**
     * A body that must not be sent (1xx, 204, 304), or one that is empty, is left alone:
     * wrapping it would either violate the status code or just add a gzip header around nothing.
     * So is a body of unknown length (sendChunked): it exists to be streamed, and buffering it
     * into memory here to recompress would defeat that. Such a body goes out uncompressed.
     */
    private static boolean canCompress(final HttpServerExchange exchange) {
        if (exchange.isResponseSent()) return false;
        final int status = exchange.getStatusCode();
        if (status < 200 || status == NO_CONTENT || status == NOT_MODIFIED) return false;
        return exchange.getResponseBody().getLength() > 0;
    }

    private static void prepareRecompress(final HttpServerExchange exchange) {
        // The stored Content-Length describes the uncompressed body, so it has to go;
        // sendResponse writes the length of the compressed payload instead.
        exchange.removeResponseHeader(CONTENT_LENGTH);
        markVary(exchange);
    }

    private static void markVary(final HttpServerExchange exchange) {
        final String vary = exchange.getResponseHeader(VARY);
        if (vary == null) exchange.setResponseHeader(VARY, "Accept-Encoding");
        else if (!vary.toLowerCase(ENGLISH).contains("accept-encoding"))
            exchange.setResponseHeader(VARY, vary + ", Accept-Encoding");
    }

    /**
     * Honours the quality value: "gzip;q=0" means the client does not want gzip, and "*"
     * accepts anything that is not listed explicitly.
     */
    public static boolean acceptsEncoding(final HttpServerExchange exchange, final String encoding) {
        final String acceptedEncodings = exchange.getRequestHeader(ACCEPT_ENCODING);
        if (isNullOrEmpty(acceptedEncodings)) return false;

        Double wildcard = null;
        for (final String entry : acceptedEncodings.split(",")) {
            if (isNullOrEmpty(entry)) continue;
            final int semicolon = entry.indexOf(';');
            final String name = (semicolon == -1 ? entry : entry.substring(0, semicolon)).trim();

            if (name.equalsIgnoreCase(encoding)) return qualityOf(entry) > 0;
            // Remember "*" but keep looking: "*, gzip;q=0" must still refuse gzip.
            if ("*".equals(name)) wildcard = qualityOf(entry);
        }
        return wildcard != null && wildcard > 0;
    }

    private static double qualityOf(final String entry) {
        final int semicolon = entry.indexOf(';');
        if (semicolon == -1) return 1;

        for (final String parameter : entry.substring(semicolon + 1).split(";")) {
            final String compact = parameter.replace(" ", "");
            if (!compact.toLowerCase(ENGLISH).startsWith("q=")) continue;
            try {
                return Double.parseDouble(compact.substring(2));
            } catch (final NumberFormatException e) {
                return 1; // an unusable quality is treated as "no preference"
            }
        }
        return 1;
    }

}
