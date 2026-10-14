package benchmarks;

import java.io.IOException;

/**
 * A server under benchmark. Both implementations bind an ephemeral port on the loopback
 * interface and serve the identical response, so the only thing that differs between runs
 * is the server implementation itself.
 */
public interface BenchServer {

    void start() throws IOException;

    int port();

    void stop() throws IOException;

}
