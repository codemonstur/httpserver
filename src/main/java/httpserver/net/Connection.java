package httpserver.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;

/**
 * The {@code Connection} class.
 */
public final class Connection {
    // Written by the accept/stop threads and read by the worker, so it has to be volatile.
    private volatile boolean running = true;
    private final Socket socket;

    /**
     * Creates a new instance.
     *
     * @param executor the executor
     * @param socket the socket
     * @param handler the handler
     * @throws IOException if the operation fails
     */
    public Connection(final Executor executor, final Socket socket, final ConnectionHandler handler)
            throws IOException {
        this.socket = socket;
        // Disable Nagle: the default delays small writes by up to ~40 ms, which dominates
        // the latency of small responses on keep-alive connections.
        socket.setTcpNoDelay(true);
        final var out = socket.getOutputStream();
        final var in = socket.getInputStream();
        try {
            executor.execute(() -> {
                try (out; in; socket) {
                    try {
                        while (running) {
                            handler.handleInput(socket, in, out);
                        }
                    } finally {
                        // Once the loop is done this connection is dead, whether it stopped
                        // because of stop() or because the peer went away.
                        running = false;
                    }
                } catch (final Exception ignored) {}
            });
        } catch (final RuntimeException e) {
            // The task never started (rejected executor, out of memory, ...): close the socket
            // here, because the try-with-resources that owns it will never run.
            try {
                socket.close();
            } catch (final IOException ignored) {}
            throw e;
        }
    }

    /**
     * Performs the {@code stop} operation.
     */
    public void stop() {
        this.running = false;
        // Closing the socket is what actually unblocks a worker sitting in a keep-alive read;
        // without it stop() would leave those threads waiting forever.
        try {
            socket.close();
        } catch (final IOException ignored) {}
    }

    /**
     * Returns whether the alive condition holds.
     *
     * @return whether the condition holds
     */
    public boolean isAlive() {
        return running;
    }
}
