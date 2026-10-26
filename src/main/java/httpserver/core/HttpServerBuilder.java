package httpserver.core;

import httpserver.HttpHandler;
import httpserver.HttpServer;
import httpserver.error.HttpError;
import httpserver.error.MalformedInput;
import httpserver.error.NotImplemented;
import httpserver.net.ConnectionHandler;
import httpserver.util.Value;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static httpserver.core.Headers.*;
import static httpserver.core.HttpServerExchange.DATE_FORMAT;
import static httpserver.core.RequestParsing.discardRemainingRequestBody;
import static httpserver.core.RequestParsing.readRequestHead;
import static httpserver.core.StatusCode.*;
import static httpserver.error.HttpError.respondInternalError;
import static httpserver.util.Strings.*;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.time.Instant.now;

public class HttpServerBuilder {

    private int port = 8080;
    private int backlog = 100;
    private boolean daemon = false;
    private String address = "0.0.0.0";
    private HttpHandler handler;
    private int maxRequestSize = 8192;
    private long keepAliveTimeout = 60_000;
    private int threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 4);
    private Executor executor;

    public HttpServerBuilder bind(final int port, final String address) {
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("port value must be between 0 and 65535");
        this.port = port;
        this.address = address;
        return this;
    }
    public HttpServerBuilder executor(final Executor executor) {
        this.executor = executor;
        return this;
    }
    public HttpServerBuilder backlog(final int backlog) {
        if (backlog < 0)
            throw new IllegalArgumentException("Number of waiting connections must not be negative");
        this.backlog = backlog;
        return this;
    }
    public HttpServerBuilder daemon(final boolean daemon) {
        this.daemon = daemon;
        return this;
    }
    public HttpServerBuilder maxRequestHeadSize(final int maxRequestSize) {
        if (maxRequestSize <= 1024)
            throw new IllegalArgumentException("Max request size must be larger than 1024");
        this.maxRequestSize = maxRequestSize;
        return this;
    }
    /**
     * How long an idle keep-alive connection may wait for the next request before it is closed.
     * Without a timeout an idle connection pins its worker thread for as long as the client
     * keeps the socket open, which starves the pool after enough connections.
     */
    public HttpServerBuilder keepAliveTimeout(final long millis) {
        if (millis <= 0)
            throw new IllegalArgumentException("Keep-alive timeout must be a positive number of milliseconds");
        this.keepAliveTimeout = millis;
        return this;
    }
    /**
     * Size of the default worker pool used when no executor is supplied. Has no effect when
     * an explicit executor is configured.
     */
    public HttpServerBuilder threads(final int threads) {
        if (threads <= 0)
            throw new IllegalArgumentException("Number of worker threads must be positive");
        this.threads = threads;
        return this;
    }

    public HttpServerBuilder handler(final HttpHandler handler) {
        this.handler = handler;
        return this;
    }

    public HttpServer build() throws UnknownHostException {
        final var bindAddress = InetAddress.getByName(address);
        // Reuse one request-head buffer per worker thread instead of allocating 8 KiB per
        // request; the exchange is fully processed before the worker picks up the next request,
        // so the buffer is never in use by two exchanges at once.
        final var requestBuffer = ThreadLocal.withInitial(() -> new byte[maxRequestSize]);
        final var exec = executor != null ? executor : newWorkerPool(threads);
        // setSoTimeout takes an int, so the configured long is capped rather than truncated.
        final int timeout = toSoTimeout(keepAliveTimeout);
        return new HttpServer(port, bindAddress, daemon, backlog, exec, (socket, in, out) -> {
            // Set while waiting for the start of the next request, so idle keep-alive connections
            // give their worker back instead of holding it until the client goes away.
            socket.setSoTimeout(timeout);

            final byte[] data = requestBuffer.get();
            final int length;
            try {
                length = readRequestHead(data, in);
            } catch (final SocketTimeoutException e) {
                throw e; // idle timeout: close the connection without a response
            } catch (final IOException e) {
                writeErrorResponse(out, data, REQUEST_HEADER_FIELDS_TOO_LARGE, "Request header data too large");
                throw e; // the head can no longer be framed, so the connection must go
            }
            // The head is complete: drop the timeout so body reads and handlers are not cut off.
            socket.setSoTimeout(0);

            final HttpServerExchange exchange;
            try {
                exchange = new HttpServerExchange(socket, data, length, in, out);
            } catch (final Exception e) {
                writeErrorResponse(out, data, BAD_REQUEST, "Malformed request line");
                throw new IOException("Malformed request line", e);
            }

            final var value = new Value<Exception>();
            // Set to false when the body cannot be located in the stream at all: draining it
            // would then block on bytes that are never going to arrive.
            boolean bodyFramed = true;
            try {
                validateContentLength(exchange);
                rejectUnsupportedTransferEncoding(exchange);
                handler.handleRequest(exchange);
            } catch (final Exception e) {
                value.value = e;
                if (e instanceof MalformedInput || e instanceof NotImplemented) bodyFramed = false;
                if (e instanceof final HttpError error) error.processExchange(exchange);
                else respondInternalError(exchange);
            } finally {
                try {
                    if (bodyFramed) {
                        // Bounded, so a peer that announces a body and then goes quiet cannot
                        // hold this worker until the process is restarted.
                        socket.setSoTimeout(timeout);
                        try {
                            discardRemainingRequestBody(exchange);
                        } catch (final IOException e) {
                            // The peer stopped sending. Answer anyway; the stream is not where
                            // the client thinks it is, so the connection goes after the response.
                            exchange.setResponseHeader(CONNECTION, CLOSE);
                        }
                    }
                    if (shouldCloseConnection(exchange)) exchange.setResponseHeader(CONNECTION, CLOSE);
                    exchange.sendResponse();
                } finally {
                    exchange.notifyCompleteListeners(value.value);
                }
                if (exchange.isConnectionClosed() || shouldCloseConnection(exchange))
                    throw new IOException("Closing connection");
            }
        });
    }

    /** Socket timeouts are ints, so a long configuration value is capped rather than truncated. */
    private static int toSoTimeout(final long millis) {
        return (int) Math.min(millis, Integer.MAX_VALUE);
    }

    private static Executor newWorkerPool(final int threads) {
        // A fixed pool sized up front so bursts do not pay thread-creation and teardown costs
        // on the hot path; idle workers stay put, which also avoids GC pressure from thread
        // churn. Callers that need elastic sizing can supply their own executor.
        return Executors.newFixedThreadPool(threads, runnable -> {
            final var thread = new Thread(runnable, "httpserver-worker");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Framing depends on Content-Length being unambiguous, so anything else is rejected before a
     * handler sees the request. The connection is marked for closing because the body cannot be
     * located in the stream reliably.
     */
    private static void validateContentLength(final HttpServerExchange exchange) {
        final var values = exchange.getRequestHeaders(CONTENT_LENGTH);
        if (values.isEmpty()) return;

        if (values.size() > 1) {
            exchange.setResponseHeader(CONNECTION, CLOSE);
            throw new MalformedInput("Request has more than one Content-Length header");
        }
        final String value = values.get(0).trim();
        if (!isUnsignedLong(value)) {
            exchange.setResponseHeader(CONNECTION, CLOSE);
            throw new MalformedInput("Content-Length is not a valid number: '" + value + "'");
        }
    }
    private static boolean isUnsignedLong(final String value) {
        if (value.isEmpty()) return false;
        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        try {
            Long.parseLong(value);
            return true;
        } catch (final NumberFormatException e) {
            return false; // does not fit in a long
        }
    }

    /**
     * Chunked (or any other) transfer coding is not implemented. Accepting such a request and
     * then ignoring the body would leave the framing bytes in the stream, where the keep-alive
     * loop would happily parse them as the next request.
     */
    private static void rejectUnsupportedTransferEncoding(final HttpServerExchange exchange) throws NotImplemented {
        final String transferEncoding = exchange.getRequestHeader(TRANSFER_ENCODING);
        if (transferEncoding == null) return;
        throw new NotImplemented("Transfer-Encoding is not supported: '" + transferEncoding.trim() + "'");
    }

    private static boolean shouldCloseConnection(final HttpServerExchange exchange) {
        if (HTTP_09.equals(exchange.getRequestProtocol())) return true;
        // A handler can ask for the close too, for instance because it could not frame a body.
        if (CLOSE.equalsIgnoreCase(exchange.getResponseHeader(CONNECTION))) return true;
        if (HTTP_10.equals(exchange.getRequestProtocol()))
            return !KEEP_ALIVE.equalsIgnoreCase(exchange.getRequestHeader(CONNECTION));
        if (HTTP_11.equals(exchange.getRequestProtocol()))
            return CLOSE.equalsIgnoreCase(exchange.getRequestHeader(CONNECTION));
        return true; // protocol not recognised, nothing safer than closing
    }

    /**
     * Writes a response for failures that happen before an exchange exists, such as an oversized
     * or unparsable request head. Never throws: if the peer is already gone the connection is
     * closed by the caller anyway.
     */
    private static void writeErrorResponse(final OutputStream out, final byte[] data,
                                           final int status, final String message) {
        try {
            final String protocol = detectProtocol(data);
            if (HTTP_09.equals(protocol)) return; // HTTP/0.9 has no status line at all

            final byte[] body = (message + CRLF).getBytes(US_ASCII);
            out.write((
                    protocol + SPACE + status + SPACE + getMessageForCode(status, null) + CRLF
                  + "Content-Type: text/plain; charset=UTF-8" + CRLF
                  + "Content-Length: " + body.length + CRLF
                  + "Connection: " + CLOSE + CRLF
                  + "Date: " + DATE_FORMAT.format(now()) + CRLF
                  + CRLF
            ).getBytes(US_ASCII));
            out.write(body);
            out.flush();
        } catch (final IOException ignored) {
            // The peer is gone; the caller closes the connection regardless.
        }
    }
    private static String detectProtocol(final byte[] data) {
        final var head = new String(data, US_ASCII);
        if (head.contains(HTTP_10)) return HTTP_10;
        if (head.contains(HTTP_09)) return HTTP_09;
        return HTTP_11;
    }

}
