package httpserver.handlers;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static httpserver.core.Headers.ACCEPT_ENCODING;
import static httpserver.core.Headers.CONTENT_ENCODING;
import static httpserver.util.Functions.isNullOrEmpty;

public enum Compression {;

    public static HttpHandler compress(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
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
            if (!exchange.isResponseSent() && acceptsEncoding(exchange, "gzip")) {
                compressGzip(exchange);
            }
        };
    }
    public static HttpHandler compressDeflate(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            if (!exchange.isResponseSent() && acceptsEncoding(exchange, "deflate")) {
                compressDeflate(exchange);
            }
        };
    }

    public static void compressGzip(final HttpServerExchange exchange) throws IOException {
        exchange.setResponseHeader(CONTENT_ENCODING, "gzip");
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (final GZIPOutputStream gzip = new GZIPOutputStream(out)) {
                exchange.getResponseBody().writeTo(gzip);
            }
            exchange.send(out.toByteArray());
        }
    }

    public static void compressDeflate(final HttpServerExchange exchange) throws IOException {
        exchange.setResponseHeader(CONTENT_ENCODING, "deflate");
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            try (final DeflaterOutputStream deflate = new DeflaterOutputStream(out)) {
                exchange.getResponseBody().writeTo(deflate);
            }
            exchange.send(out.toByteArray());
        }
    }

    public static boolean acceptsEncoding(final HttpServerExchange exchange, final String encoding) {
        final String acceptedEncodings = exchange.getRequestHeader(ACCEPT_ENCODING);
        if (isNullOrEmpty(acceptedEncodings)) return false;
        for (final String accepted : acceptedEncodings.split(",")) {
            if (isNullOrEmpty(accepted)) continue;
            if (encoding.equals(accepted.trim())) return true;
        }
        return false;
    }

}
