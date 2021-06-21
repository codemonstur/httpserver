package unit;

import httpserver.HttpServer;

import java.io.IOException;

import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.StatusCode.CREATED;
import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleTest {

    public static void main(final String... args) throws IOException {
        new HttpServer()
            .handler(exchange -> {
                exchange.setStatusCode(CREATED);
                exchange.setResponseHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
                exchange.send("Hello, world!", UTF_8);
            })
            .build()
            .start();
    }

}
