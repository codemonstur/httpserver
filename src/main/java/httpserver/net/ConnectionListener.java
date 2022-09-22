package httpserver.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executor;

public abstract class ConnectionListener {

    protected final int backlog;
    protected final ConnectionHandler handler;
    protected final int port;
    protected final InetAddress bindAddress;
    protected final boolean daemon;
    protected final Executor executor;

    protected ConnectionListener(final int port, final InetAddress bindAddress, final boolean daemon,
                                 final int backlog, final Executor executor, final ConnectionHandler handler) {
        this.port = port;
        this.bindAddress = bindAddress;
        this.daemon = daemon;
        this.backlog = backlog;
        this.executor = executor;
        this.handler = handler;
    }

    protected boolean running = true;
    protected ServerSocket serverSocket;
    protected final List<Connection> connections = new LinkedList<>();

    public ConnectionListener start() throws IOException {
        this.serverSocket = new ServerSocket(port, backlog, bindAddress);
        final var thread = new Thread(() -> {
            while (running) {
                try {
                    synchronized (connections) {
                        connections.add(new Connection(executor, serverSocket.accept(), handler));
                        connections.removeIf(connection -> !connection.isAlive());
                    }
                } catch (Exception e) {}
            }
        });
        thread.setDaemon(daemon);
        thread.start();

        return this;
    }

    public void stop() throws IOException {
        this.running = false;

        try {
            serverSocket.close();
        } finally {
            synchronized (connections) {
                connections.forEach(Connection::stop);
            }
        }
    }

}
