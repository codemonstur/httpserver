package unit;

import httpserver.HttpServer;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class SimpleTest {

    public static void main(final String... args) throws IOException {
        new HttpServer()
            .handler(exchange -> {
                exchange.setStatusCode(201);
                exchange.setResponseHeader("Content-Type", "text/plain; charset=UTF-8");
                exchange.send("Hello, world!", UTF_8);
            })
            .build()
            .start();
    }

}
