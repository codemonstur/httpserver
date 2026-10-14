package benchmarks;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * The load generator: a fixed number of closed-loop workers, each holding one keep-alive
 * connection for the whole run. Every worker sends a prebuilt request, reads the full response
 * (Content-Length framed or chunked), and records that round trip into a per-thread histogram.
 * Connection setup happens before the clock starts, so only steady-state request handling is
 * measured.
 *
 * Closed-loop means the next request is only sent after the previous response arrives: results
 * compare servers well, but the latencies carry no coordinated-omission correction.
 */
public final class LoadGenerator {

    private static final byte[] REQUEST = "GET / HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes(US_ASCII);

    private LoadGenerator() {}

    public record LoadResult(long requests, double seconds, LatencyHistogram latencies) {
        public double requestsPerSecond() {
            return requests / seconds;
        }
    }

    public static LoadResult run(final int port, final int concurrency, final long durationMillis)
            throws InterruptedException {
        final var histograms = new LatencyHistogram[concurrency];
        final var ready = new CountDownLatch(concurrency);
        final var start = new CountDownLatch(1);
        final var done = new CountDownLatch(concurrency);
        final var failure = new AtomicReference<Throwable>();
        final long[] deadline = new long[1];

        for (int i = 0; i < concurrency; i++) {
            final var histogram = new LatencyHistogram();
            histograms[i] = histogram;
            final var thread = new Thread(() -> {
                try (final var socket = new Socket("127.0.0.1", port)) {
                    socket.setTcpNoDelay(true);
                    final var out = socket.getOutputStream();
                    final var connection = new Connection(socket.getInputStream());
                    ready.countDown();
                    start.await();
                    while (System.nanoTime() < deadline[0]) {
                        final long before = System.nanoTime();
                        out.write(REQUEST);
                        readResponse(connection);
                        histogram.record(System.nanoTime() - before);
                    }
                } catch (final Throwable e) {
                    failure.compareAndSet(null, e);
                    ready.countDown(); // a failure before the start must not hang the run
                } finally {
                    done.countDown();
                }
            }, "bench-client-" + i);
            thread.setDaemon(true);
            thread.start();
        }

        ready.await();
        if (failure.get() != null)
            throw new IllegalStateException("A client failed before the run started", failure.get());
        final long started = System.nanoTime();
        deadline[0] = started + durationMillis * 1_000_000L;
        start.countDown();
        done.await();
        final double seconds = (System.nanoTime() - started) / 1_000_000_000.0;
        if (failure.get() != null)
            throw new IllegalStateException("A client failed during the run", failure.get());

        final var merged = new LatencyHistogram();
        for (final var histogram : histograms) merged.mergeFrom(histogram);
        return new LoadResult(merged.count(), seconds, merged);
    }

    /** Reads one response: the head line by line, then the body in whichever framing the server chose. */
    private static void readResponse(final Connection connection) throws IOException {
        final var line = new byte[256];
        if (connection.readLine(line).isEmpty()) throw new IOException("Empty response head");

        Long contentLength = null;
        boolean chunked = false;
        String header;
        while (!(header = connection.readLine(line)).isEmpty()) {
            if (header.length() > 15 && header.regionMatches(true, 0, "Content-Length:", 0, 15))
                contentLength = Long.parseLong(header.substring(15).trim());
            if (header.length() > 18 && header.regionMatches(true, 0, "Transfer-Encoding:", 0, 18))
                chunked = header.substring(18).trim().toLowerCase().contains("chunked");
        }

        if (chunked) readChunkedBody(connection, line);
        else if (contentLength != null) connection.drain(contentLength);
        else throw new IOException("Response is framed by neither Content-Length nor chunked encoding");
    }

    private static void readChunkedBody(final Connection connection, final byte[] line) throws IOException {
        for (;;) {
            final String sizeLine = connection.readLine(line);
            final long size = Long.parseLong(sizeLine.split(";", 2)[0].trim(), 16);
            if (size == 0) {
                while (!connection.readLine(line).isEmpty()); // trailer section
                return;
            }
            connection.drain(size);
            if (!connection.readLine(line).isEmpty()) // the CRLF that terminates the chunk's data
                throw new IOException("Chunk is not terminated by CRLF");
        }
    }

    /**
     * A buffered window over the socket stream: line-oriented reads for the response head and
     * chunk sizes, bulk drains for bodies. Everything reads through the same buffer, so a body
     * that follows a line in the same TCP segment is never discarded.
     */
    private static final class Connection {

        private static final int BUFFER_SIZE = 32 * 1024;

        private final InputStream in;
        private final byte[] buffer = new byte[BUFFER_SIZE];
        private int position;
        private int available;

        Connection(final InputStream in) {
            this.in = in;
        }

        /** Reads one LF-terminated line without it, returning the content before the CR. */
        String readLine(final byte[] scratch) throws IOException {
            int length = 0;
            for (;;) {
                if (position >= available) fill();
                final byte b = buffer[position++];
                if (b == '\n') break;
                if (b != '\r' && length < scratch.length) scratch[length++] = b;
            }
            return new String(scratch, 0, length, US_ASCII);
        }

        void drain(final long count) throws IOException {
            long remaining = count;
            while (remaining > 0) {
                if (position >= available) fill();
                final int chunk = (int) Math.min(remaining, available - position);
                position += chunk;
                remaining -= chunk;
            }
        }

        private void fill() throws IOException {
            available = in.read(buffer);
            position = 0;
            if (available < 0) throw new IOException("Connection closed before the response was complete");
        }

    }

}
