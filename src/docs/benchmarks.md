# Benchmarks

Compares this server against [Undertow](https://undertow.io/) in two scenarios, both over
keep-alive connections on the loopback interface:

- **hello** — the classic small fixed body (13 bytes)
- **stream** — a 10 MiB body streamed from memory, with fixed-length and chunked framing

## Running

    make bench

Or with custom parameters (warmup seconds, measured seconds, repetitions):

    MAVEN_OPTS="-Xms512m -Xmx512m" mvn -q test-compile exec:java \
        -Dexec.classpathScope=test -Dexec.mainClass=benchmarks.Bench -Dexec.args="2 10 3"

The default run sweeps concurrency 1, 4, 16 and 64 for hello and 1, 4 and 16 for stream,
runs three measured repetitions per combination, and prints a table with the median of the
repetitions. The stream table adds a MiB/s column, which is the interesting number for large
bodies:

| concurrency | server             | req/s    | p50      | p90      | p99      | p999     | MiB/s    |
|------------|--------------------|----------|----------|----------|----------|----------|----------|
|          1 |         httpserver | ...      | ...      | ...      | ...      | ...      | ...      |
|          1 | httpserver-chunked | ...      | ...      | ...      | ...      | ...      | ...      |
|          1 |           undertow | ...      | ...      | ...      | ...      | ...      | ...      |
|          1 |   undertow-chunked | ...      | ...      | ...      | ...      | ...      | ...      |

## How it measures

The load generator (`benchmarks.LoadGenerator`) is a closed loop: a fixed number of worker
threads, each holding a single keep-alive connection for the whole run. Every worker sends a
prebuilt `GET /` request, reads the full response — `Content-Length` framed or chunked, the
parser handles both — and records that round trip into a per-thread histogram. Connection
setup and the warmup phase happen before the clock starts, so only steady-state request
handling is counted. The histograms use 64 linear sub-buckets per power of two, which
reproduces every recorded latency with at most ~1.6% relative error.

Both servers serve the exact same bytes, both are framed the same way per variant, and both
get the same thread budget: at concurrency N, the library gets a fixed pool of N workers and
Undertow gets N worker threads. In the stream scenario the body is served from memory on both
sides (a fresh stream per request that behaves like a file stream, so the library's chunked
variant produces real 8 KiB chunks), which keeps the disk out of the comparison. The server
order rotates between repetitions so neither always runs first on a warming machine.

## Reading the results

- **Closed loop, no coordinated-omission correction.** A worker only sends the next request
  after the previous response arrives, so the latencies understate what an open-loop client
  would observe at saturation. For comparing the two servers the bias is identical, which is
  what matters here.
- **Loopback, same JVM.** Client and server share a machine and a JVM. That is fine for a
  relative comparison — both servers face the same client cost — but the absolute numbers do
  not generalize to other hardware, and will not match results from a remote load generator.
- Compare rows at the same concurrency. In hello the interesting cells are the small ones
  (1 and 4), where per-request overhead dominates; at 64 both servers are queueing. In stream,
  concurrency 1 shows the per-stream cost and MiB/s at 16 shows aggregate throughput, which
  approaches what the loopback and this machine's memory bandwidth can carry.
- Run on AC power, nothing else hammering the machine, and take the numbers from the median
  repetition. Single runs vary by several percent.

## Cross-checking with wrk

Two of the caveats above (shared JVM, closed-loop client) disappear when the load comes from
an external process. The `BenchServer` seam makes that easy: start a single variant in its
own JVM — a `main` that constructs the handler exactly like `Bench` does, calls `start()`,
prints `port()` and blocks until Ctrl-C — then drive it with
[wrk](https://github.com/wg/wrk), keeping wrk's connection count equal to the scenario's
concurrency so the thread budgets stay matched:

    wrk -t4 -c16 -d30s http://127.0.0.1:8080/

wrk numbers are not directly comparable to the in-repo tables (different client), but the
ordering of the servers should agree — if it does not, treat that as a finding.

## Extending it

`BenchServer` is the only thing a new comparison needs: implement `start`/`port`/`stop`
around another server, serve the same body, and add it to the scenario map in `Bench`. The
natural next scenario is SSE throughput.
