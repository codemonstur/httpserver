## Features

- HTTP/2

## Performance

Remaining work (NIO rewrite lives on branch `nio-rewrite`; not merged to main):

- Reduce per-request allocation churn further:
  - Pool or reuse `HttpServerExchange` header containers (`ArrayList`/`HashMap`) instead of allocating per request.
  - Avoid `String` allocation in request parsing by working with byte slices where the public API allows.
  - Pool response head byte buffers instead of `StringBuilder.toString().getBytes(US_ASCII)`.
  - Reduce `String.toLowerCase` cost in request-header lookup (e.g. callers pass pre-lowercased constants).
- Worker pool:
  - Add a bounded queue / rejection policy option for the default pool.
  - Evaluate work-stealing (`ForkJoinPool`) or adaptive sizing under burst load.
- Syscalls on the I/O path:
  - Evaluate larger or adaptive socket buffer sizes.
  - Use direct `ByteBuffer` and `SocketChannel` writes for large responses.
  - Use `FileChannel.transferTo` / `transferFrom` for file bodies.
  - Combine head and body into a single write for small responses (partially done via buffering).
- Mode switching / selector overhead:
  - Avoid blocking/non-blocking mode flips entirely (NIO.2 async channels or io_uring).
  - Shard selectors across cores for high connection counts.
  - Tune or make configurable the selector `select` timeout.
  - Batch pending channel registrations more aggressively.
- Hot-path micro-optimizations:
  - Optimize `indexOfCrlf` (SIMD/unrolled scan).
  - Bulk-read in `readRequestHead` instead of byte-by-byte.
  - Cache `getMessageForCode` results.
  - Pre-size collections and reuse `ThreadLocal` buffers more broadly.
  - Cache formatted status lines.
- Keep-alive follow-up detection:
  - Replace the fixed 128-iteration spin with adaptive backoff based on load.
  - Avoid busy-waiting when many connections are idle.
- High concurrency:
  - Stress test and tune beyond 100 concurrent connections.
  - Evaluate io_uring / kernel-bypass networking.
- GC and profiling:
  - Run async-profiler/JFR on httpserver-only workloads and address top allocators.
  - Consider object pooling for exchanges if GC pressure remains.
- Streaming:
  - Pool direct buffers for `ResponseBody` streaming paths.
  - Reduce chunk framing overhead for chunked responses.
- TCP tuning:
  - Expose socket buffer size and TCP options.
  - Tune accept backlog for high connection rates.

## Build and tooling

- maven-check-license unit tests:
  - Ensure tests are written in JUnit 6 style (no vintage engine).
  - Refactor test code to use JUnit 5+ features consistently.
  - Remove any JUnit 4 or vintage-specific test code.
  - Ensure all tests use proper JUnit 5 assertions and test patterns.
  - Remove junit-vintage-engine dependency if present.
  - Ensure test suite runs cleanly without deprecated test engines.