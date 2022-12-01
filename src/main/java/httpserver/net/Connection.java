package httpserver.net;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.Executor;

public final class Connection {
    private boolean running = true;

    public Connection(final Executor executor, final Socket socket, final ConnectionHandler handler)
            throws IOException {
        final var out = socket.getOutputStream();
        final var in = socket.getInputStream();
        executor.execute(() -> {
            try (out; in; socket) {
                while (running) {
                    handler.handleInput(in, out);
                }
            } catch (Exception e) {}
        });
    }

    public void stop() {
        this.running = false;
    }

    public boolean isAlive() {
        return running;
    }
}