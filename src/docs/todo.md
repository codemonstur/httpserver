
## Features

- HTTP/2

## Performance

What it would take to fully match Undertow

- High concurrency (64+): the fixed worker pool + blocking I/O. Undertow uses NIO with a small I/O thread pool and queues work; you'd need either a much larger pool (memory cost) or move to non-blocking I/O — which is essentially a rewrite of Connection/ConnectionListener.
- Streaming: the 10 MiB path is dominated by per-response flush() and writeTo chunking; streaming through direct buffers with transferTo would close the gap.
- Allocations: the per-request HttpServerExchange constructor allocates BufferedReader/InputStreamReader/ByteArrayInputStream wrappers; parsing the request line/headers directly from the byte array (like Undertow) would cut GC pressure at high RPS.

For the vast majority of real workloads (moderate concurrency, small-to-medium responses) the Nagle fix alone gets you to Undertow-class latency. If you want, I can take a pass at the header parsing (direct byte scan) or a date/keep-alive micro-optimization round and re-measure. All changes are uncommitted.
