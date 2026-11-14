package httpserver;

import httpserver.core.HttpServerBuilder;
import httpserver.net.ConnectionHandler;
import httpserver.net.ConnectionListener;

import java.net.InetAddress;
import java.util.concurrent.Executor;

/**
 * The {@code HttpServer} class.
 */
public final class HttpServer extends ConnectionListener {

    /**
     * Returns the result of {@code newHttpServer}.
     *
     * @return the result
     */
    public static HttpServerBuilder newHttpServer() {
        return new HttpServerBuilder();
    }

    /**
     * Creates a new instance.
     *
     * @param port the port
     * @param bindAddress the bind address
     * @param daemon the daemon
     * @param backlog the backlog
     * @param executor the executor
     * @param handler the handler
     */
    public HttpServer(final int port, final InetAddress bindAddress, final boolean daemon,
                      final int backlog, final Executor executor, final ConnectionHandler handler) {
        super(port, bindAddress, daemon, backlog, executor, handler);
    }

    /**
     * Returns the port.
     *
     * @return the result
     */
    public int getPort() {
        return super.getPort();
    }

}
