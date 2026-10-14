package benchmarks;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntFunction;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Two scenarios against Undertow, both over keep-alive connections on loopback:
 *
 *   hello:         the classic small fixed body
 *   stream-10MiB:  a 10 MiB body streamed from memory, with fixed length and chunked framing
 *
 * Both servers get the same body bytes, the same thread budget and the same warmup; the
 * measured repetitions rotate which server runs first, and the tables report medians.
 *
 * Usage: Bench [warmupSeconds] [durationSeconds] [repetitions]
 */
public final class Bench {

    private static final byte[] BODY = "Hello, World!".getBytes(UTF_8);
    // Streamed from memory: the disk is not part of the comparison.
    private static final byte[] FILE = new byte[10 * 1024 * 1024];
    // Shared content with an independent position per request.
    private static final ByteBuffer FILE_BUFFER = ByteBuffer.wrap(FILE);

    public static void main(final String[] args) throws Exception {
        final int warmupSeconds = args.length > 0 ? Integer.parseInt(args[0]) : 2;
        final int durationSeconds = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        final int reps = args.length > 2 ? Integer.parseInt(args[2]) : 3;
        new Random(0).nextBytes(FILE);

        System.out.printf("JVM %s on %d core(s): warmup %ds, duration %ds, %d rep(s)%n%n",
                System.getProperty("java.version"), Runtime.getRuntime().availableProcessors(),
                warmupSeconds, durationSeconds, reps);

        final var hello = new LinkedHashMap<String, IntFunction<BenchServer>>();
        hello.put("httpserver", concurrency -> new HttpServerBench(exchange -> exchange.send(BODY), concurrency));
        hello.put("undertow", concurrency -> new UndertowBench(exchange -> {
            exchange.setResponseContentLength(BODY.length);
            exchange.getResponseSender().send(ByteBuffer.wrap(BODY));
        }, concurrency));
        runScenario("hello (13 bytes)", BODY.length, new int[] {1, 4, 16, 64}, false,
                warmupSeconds, durationSeconds, reps, hello);

        final var stream = new LinkedHashMap<String, IntFunction<BenchServer>>();
        stream.put("httpserver", concurrency -> new HttpServerBench(exchange ->
                exchange.send(memoryStream(), FILE.length), concurrency));
        stream.put("httpserver-chunked", concurrency -> new HttpServerBench(exchange ->
                exchange.sendChunked(memoryStream()), concurrency));
        stream.put("undertow", concurrency -> new UndertowBench(exchange -> {
            exchange.setResponseContentLength(FILE.length);
            exchange.getResponseSender().send(FILE_BUFFER.duplicate());
        }, concurrency));
        stream.put("undertow-chunked", concurrency -> new UndertowBench(exchange -> {
            exchange.setResponseContentLength(-1); // unknown length: chunked on HTTP/1.1
            exchange.getResponseSender().send(FILE_BUFFER.duplicate());
        }, concurrency));
        runScenario("stream (10 MiB from memory)", FILE.length, new int[] {1, 4, 16}, true,
                warmupSeconds, durationSeconds, reps, stream);
    }

    /**
     * A fresh stream over the shared body that only implements the plain read methods, like a
     * real file stream would: its transferTo falls back to the JDK's 8 KiB loop, so the chunked
     * variant produces 8 KiB chunks on the wire instead of one giant chunk.
     */
    private static InputStream memoryStream() {
        return new InputStream() {
            private int position;
            @Override public int read() {
                return position >= FILE.length ? -1 : FILE[position++] & 0xff;
            }
            @Override public int read(final byte[] bytes, final int offset, final int length) {
                if (position >= FILE.length) return -1;
                final int count = Math.min(length, FILE.length - position);
                System.arraycopy(FILE, position, bytes, offset, count);
                position += count;
                return count;
            }
        };
    }

    private static void runScenario(final String name, final int bodyBytes, final int[] concurrencies,
                                    final boolean bandwidth, final int warmupSeconds, final int durationSeconds,
                                    final int reps, final Map<String, IntFunction<BenchServer>> servers) throws Exception {
        System.out.printf("=== %s: concurrency %s%n", name, Arrays.toString(concurrencies));

        final Map<String, List<double[]>> samples = new LinkedHashMap<>();
        for (final int concurrency : concurrencies) {
            for (int rep = 0; rep < reps; rep++) {
                // Rotated order so no server always runs first on a warming machine.
                final int first = rep % servers.size();
                final var labels = new ArrayList<>(servers.keySet());
                for (int i = 0; i < servers.size(); i++) {
                    final String label = labels.get((first + i) % labels.size());
                    final double[] sample = measure(label, servers.get(label).apply(concurrency),
                            concurrency, warmupSeconds, durationSeconds);
                    samples.computeIfAbsent(concurrency + "|" + label, key -> new ArrayList<>()).add(sample);
                }
            }
        }

        final var header = new StringBuilder("| concurrency | server             | req/s    | p50      | p90      | p99      | p999     |");
        final var divider = new StringBuilder("|------------|--------------------|----------|----------|----------|----------|----------|");
        if (bandwidth) {
            header.append(" MiB/s    |");
            divider.append("-----------|");
        }
        System.out.println();
        System.out.println(header);
        System.out.println(divider);
        for (final int concurrency : concurrencies) {
            for (final String label : servers.keySet()) {
                final var runs = samples.get(concurrency + "|" + label);
                final var row = new StringBuilder(String.format("| %10d | %18s | %s | %s | %s | %s | %s |",
                        concurrency, label,
                        formatRate(median(runs, 0)),
                        formatNanos(median(runs, 1)),
                        formatNanos(median(runs, 2)),
                        formatNanos(median(runs, 3)),
                        formatNanos(median(runs, 4))));
                if (bandwidth)
                    row.append(String.format(" %9s |", formatRate(median(runs, 0) * bodyBytes / (1024.0 * 1024))));
                System.out.println(row);
            }
        }
        System.out.println();
    }

    /** sample = {requests/second, p50, p90, p99, p999} with the latencies in nanoseconds */
    private static double[] measure(final String label, final BenchServer server, final int concurrency,
                                    final int warmupSeconds, final int durationSeconds) throws Exception {
        server.start();
        try {
            if (warmupSeconds > 0) LoadGenerator.run(server.port(), concurrency, warmupSeconds * 1000L);
            final var result = LoadGenerator.run(server.port(), concurrency, durationSeconds * 1000L);
            final double[] sample = {
                    result.requestsPerSecond(),
                    result.latencies().quantile(0.50),
                    result.latencies().quantile(0.90),
                    result.latencies().quantile(0.99),
                    result.latencies().quantile(0.999),
            };
            System.out.printf("conc=%-2d %-18s %s req/s, p50 %s, p99 %s%n",
                    concurrency, label, formatRate(sample[0]), formatNanos(sample[1]), formatNanos(sample[3]));
            return sample;
        } finally {
            server.stop();
        }
    }

    private static double median(final List<double[]> runs, final int metric) {
        final var sorted = runs.stream().mapToDouble(run -> run[metric]).sorted().toArray();
        final int middle = sorted.length / 2;
        return sorted.length % 2 == 1 ? sorted[middle] : (sorted[middle - 1] + sorted[middle]) / 2;
    }

    private static String formatRate(final double requestsPerSecond) {
        return String.format("%,.0f", requestsPerSecond);
    }
    private static String formatNanos(final double nanos) {
        if (nanos >= 1_000_000) return String.format("%.1f ms", nanos / 1_000_000);
        if (nanos >= 1_000) return String.format("%.1f us", nanos / 1_000);
        return String.format("%.0f ns", nanos);
    }

}
