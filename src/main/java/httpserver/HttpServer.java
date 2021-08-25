package httpserver;

import httpserver.core.HttpServerBuilder;
import httpserver.net.ConnectionHandler;
import httpserver.net.ConnectionListener;

import java.net.InetAddress;

public final class HttpServer extends ConnectionListener {

    public static HttpServerBuilder newHttpServer() {
        return new HttpServerBuilder();
    }

    public HttpServer(final int port, final InetAddress bindAddress, final boolean daemon, final int backlog, final ConnectionHandler handler) {
        super(port, bindAddress, daemon, backlog, handler);
    }

    public int getPort() {
        return serverSocket.getLocalPort();
    }

}
