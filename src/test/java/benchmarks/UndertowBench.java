package benchmarks;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Undertow configured as close to identically as its API allows: same bind address, and the
 * same number of I/O and worker threads as the fixed pool handed to the library under test.
 * The handler decides what is served; scenarios pick their own.
 */
public final class UndertowBench implements BenchServer {

    private final Consumer<HttpServerExchange> handler;
    private final int workers;

    private Undertow server;

    public UndertowBench(final Consumer<HttpServerExchange> handler, final int workers) {
        this.handler = handler;
        this.workers = workers;
    }

    static {
        // Undertow logs its startup and shutdown through JUL; the benchmark reports its own progress.
        java.util.logging.Logger.getLogger("io.undertow").setLevel(java.util.logging.Level.WARNING);
    }

    @Override
    public void start() throws IOException {
        server = Undertow.builder()
                .addHttpListener(0, "127.0.0.1")
                .setIoThreads(workers)
                .setWorkerThreads(workers)
                .setHandler(handler::accept)
                .build();
        server.start();
    }

    @Override
    public int port() {
        final var address = server.getListenerInfo().get(0).getAddress();
        return ((InetSocketAddress) address).getPort();
    }

    @Override
    public void stop() throws IOException {
        server.stop();
    }

}
