package httpserver;

import httpserver.core.HttpServerExchange;
import server.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static httpserver.core.Headers.CONNECTION;
import static httpserver.core.RequestParsing.readRequest;
import static httpserver.core.RequestParsing.readRequestBody;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.util.Strings.*;

public class HttpServer {

    private int port = 8080;
    private int backlog = 100;
    private String address = "0.0.0.0";
    private HttpHandler handler;
    private int maxRequestSize = 8192;

    public HttpServer bind(final int port, final String address) {
        if (port < 0 || port > 65535)
            throw new IllegalArgumentException("port value must be between 0 and 65535");
        this.port = port;
        this.address = address;
        return this;
    }
    public HttpServer backlog(final int backlog) {
        if (backlog < 0)
            throw new IllegalArgumentException("Number of waiting connection must be a positive number");
        this.backlog = backlog;
        return this;
    }
    public HttpServer maxRequestSize(final int maxRequestSize) {
        if (maxRequestSize < 0)
            throw new IllegalArgumentException("Max request size must be a positive number");
        this.maxRequestSize = maxRequestSize;
        return this;
    }
    public HttpServer handler(final HttpHandler handler) {
        this.handler = handler;
        return this;
    }

    public Server build() throws UnknownHostException {
        final var bindAddress = InetAddress.getByName(address);
        return new Server(port, bindAddress, backlog, (in, out) -> {
            final var exchange = new HttpServerExchange(readRequest(maxRequestSize, in), in, out);
            try {
                handler.handleRequest(exchange);
            } catch (Exception e) {
                exchange.setStatusCode(INTERNAL_SERVER_ERROR);
            } finally {
                readRequestBody(exchange);
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
