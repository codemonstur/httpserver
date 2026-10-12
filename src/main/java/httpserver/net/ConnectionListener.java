package httpserver.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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

    // Written by the thread calling stop() and read by the accept loop.
    protected volatile boolean running = true;
    protected ServerSocket serverSocket;
    protected final List<Connection> connections = new LinkedList<>();

    public ConnectionListener start() throws IOException {
        this.serverSocket = new ServerSocket(port, backlog, bindAddress);
        final var thread = new Thread(() -> {
            while (running) {
                try {
                    // Accept outside the lock: holding it while blocked in accept() would keep
                    // stop() from ever reaching the connections it has to shut down.
                    final Socket socket = serverSocket.accept();

                    final Connection connection;
                    try {
                        connection = new Connection(executor, socket, handler);
                    } catch (final IOException e) {
                        try {
                            socket.close();
                        } catch (final IOException ignored) {}
                        continue;
                    }
                    synchronized (connections) {
                        if (!running) {
                            // stop() ran while this connection was being handed over; make sure it
                            // does not outlive the server as an untracked, open socket.
                            connection.stop();
                        } else {
                            connections.add(connection);
                            connections.removeIf(connection2 -> !connection2.isAlive());
                        }
                    }
                } catch (final Exception e) {}
            }
        });
        thread.setDaemon(daemon);
        thread.start();

        return this;
    }

    public void stop() throws IOException {
        this.running = false;

        try {
            if (serverSocket != null) serverSocket.close();
        } finally {
            synchronized (connections) {
                connections.forEach(Connection::stop);
                connections.clear();
            }
        }
    }

}
