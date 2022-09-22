package httpserver.core;

import httpserver.HttpHandler;
import httpserver.HttpServer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static httpserver.core.Headers.CONNECTION;
import static httpserver.core.RequestParsing.readRequestHead;
import static httpserver.core.RequestParsing.discardRemainingRequestBody;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.util.Strings.*;

public class HttpServerBuilder {

    private int port = 8080;
    private int backlog = 100;
    private boolean daemon = false;
    private String address = "0.0.0.0";
    private HttpHandler handler;
    private int maxRequestSize = 8192;
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
            throw new IllegalArgumentException("Number of waiting connections must be a positive number");
        this.backlog = backlog;
        return this;
    }
    public HttpServerBuilder daemon(final boolean daemon) {
        this.daemon = daemon;
        return this;
    }
    public HttpServerBuilder maxRequestHeadSize(final int maxRequestSize) {
        if (maxRequestSize <= 1024)
            throw new IllegalArgumentException("Max request size must be larger than or equal to 1024");
        this.maxRequestSize = maxRequestSize;
        return this;
    }
    public HttpServerBuilder handler(final HttpHandler handler) {
        this.handler = handler;
        return this;
    }

    public HttpServer build() throws UnknownHostException {
        final var bindAddress = InetAddress.getByName(address);
        final var exec = executor != null ? executor : Executors.newCachedThreadPool();
        return new HttpServer(port, bindAddress, daemon, backlog, exec, (in, out) -> {
            final byte[] data = new byte[maxRequestSize];
            final var exchange = new HttpServerExchange(data, readRequestHead(data, in), in, out);
            try {
                handler.handleRequest(exchange);
            } catch (Exception e) {
                exchange.setStatusCode(INTERNAL_SERVER_ERROR);
            } finally {
                discardRemainingRequestBody(exchange);
                exchange.sendResponse();
                ifNeededCloseConnection(exchange);
            }
        });
    }

    private static void ifNeededCloseConnection(final HttpServerExchange exchange) throws IOException {
        if (HTTP_09.equals(exchange.getRequestProtocol()))
            throw new IOException("HTTP/0.9 closing connection");
        if (HTTP_10.equals(exchange.getRequestProtocol())) {
            if (!KEEP_ALIVE.equals(exchange.getRequestHeader(CONNECTION)))
                throw new IOException("HTTP/1.0 closing connection");
        }
        if (HTTP_11.equals(exchange.getRequestProtocol())) {
            if (CLOSE.equals(exchange.getRequestHeader(CONNECTION)))
                throw new IOException("HTTP/1.1 closing connection");
        }
    }

}
