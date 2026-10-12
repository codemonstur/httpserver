package httpserver.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;

public final class Connection {
    // Written by the accept/stop threads and read by the worker, so it has to be volatile.
    private volatile boolean running = true;
    private final Socket socket;

    public Connection(final Executor executor, final Socket socket, final ConnectionHandler handler)
            throws IOException {
        this.socket = socket;
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

    public void stop() {
        this.running = false;
        // Closing the socket is what actually unblocks a worker sitting in a keep-alive read;
        // without it stop() would leave those threads waiting forever.
        try {
            socket.close();
        } catch (final IOException ignored) {}
    }

    public boolean isAlive() {
        return running;
    }
}
