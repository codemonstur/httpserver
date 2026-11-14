package httpserver.handlers;

import com.aayushatharva.brotli4j.Brotli4jLoader;
import com.aayushatharva.brotli4j.encoder.Encoder;
import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static httpserver.core.Headers.CONTENT_ENCODING;
import static httpserver.handlers.Compression.*;

/**
 * The {@code Brotli} enum.
 */
public enum Brotli {;

    /**
     * Returns the result of {@code compress}.
     *
     * @param next the next
     * @return the result
     */
    public static HttpHandler compress(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            if (!exchange.isResponseSent()) {
                if (acceptsEncoding(exchange, "gzip")) {
                    compressGzip(exchange);
                } else
                if (acceptsEncoding(exchange, "deflate")) {
                    compressDeflate(exchange);
                } else
                if (acceptsEncoding(exchange, "br")) {
                    compressBrotli(exchange);
                }
            }
        };
    }

    /**
     * Returns the result of {@code compressBrotli}.
     *
     * @param next the next
     * @return the result
     */
    public static HttpHandler compressBrotli(final HttpHandler next) {
        return exchange -> {
            next.handleRequest(exchange);
            if (!exchange.isResponseSent() && acceptsEncoding(exchange, "br")) {
                compressBrotli(exchange);
            }
        };
    }

    /**
     * Performs the {@code compressBrotli} operation.
     *
     * @param exchange the exchange
     * @throws IOException if the operation fails
     */
    public static void compressBrotli(final HttpServerExchange exchange) throws IOException {
        Brotli4jLoader.ensureAvailability();

        exchange.setResponseHeader(CONTENT_ENCODING, "br");
        try (final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            exchange.getResponseBody().writeTo(out);
            exchange.send(Encoder.compress(out.toByteArray()));
        }
    }

}
