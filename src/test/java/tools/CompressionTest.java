package tools;

import java.io.IOException;

import static httpserver.HttpServer.newHttpServer;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.StatusCode.CREATED;
import static httpserver.handlers.Compression.compress;
import static java.nio.charset.StandardCharsets.UTF_8;

public enum CompressionTest {;

    public static void main(final String... args) throws IOException {
        newHttpServer()
            .bind(8081, "0.0.0.0")
            .handler(compress(exchange -> {
                exchange.setStatusCode(CREATED);
                exchange.setResponseHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
                exchange.send("Hello, world!", UTF_8);
            }))
            .build()
            .start();
    }

}
