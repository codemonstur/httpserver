package benchmarks;

import httpserver.HttpHandler;
import httpserver.HttpServer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The library under test, configured with a fixed worker pool sized to the concurrency of the
 * run: a blocking server should handle exactly the connections it is given, no more and no less.
 * The handler decides what is served; scenarios pick their own.
 */
public final class HttpServerBench implements BenchServer {

    private final HttpHandler handler;
    private final int workers;

    private ExecutorService executor;
    private HttpServer server;

    public HttpServerBench(final HttpHandler handler, final int workers) {
        this.handler = handler;
        this.workers = workers;
    }

    @Override
    public void start() throws IOException {
        executor = Executors.newFixedThreadPool(workers, runnable -> {
            final var thread = new Thread(runnable, "bench-httpserver-worker");
            thread.setDaemon(true);
            return thread;
        });
        try {
            server = HttpServer.newHttpServer()
                    .bind(0, "127.0.0.1")
                    .executor(executor)
                    .handler(handler)
                    .build();
            server.start();
        } catch (final IOException | RuntimeException e) {
            executor.shutdown();
            throw e;
        }
    }

    @Override
    public int port() {
        return server.getPort();
    }

    @Override
    public void stop() throws IOException {
        try {
            server.stop();
        } finally {
            executor.shutdown();
        }
    }

}
