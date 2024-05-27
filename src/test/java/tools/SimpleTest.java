package tools;

import httpserver.HttpHandler;
import httpserver.core.HttpServerExchange;
import httpserver.handlers.MethodPathRouting;

import java.io.IOException;

import static httpserver.HttpServer.newHttpServer;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.CREATED;
import static httpserver.handlers.MethodPathRouting.methodPathRouting;
import static java.nio.charset.StandardCharsets.UTF_8;

public enum SimpleTest {;

    public static void main(final String... args) throws IOException {
        newHttpServer()
            .bind(8080, "0.0.0.0")
            .handler(methodPathRouting()
                .get("/boe", (exchange) ->
                    respond(exchange).status(CREATED).contentType("text/plain; charset=UTF-8").send("Hello, world!")))
            .build()
            .start();
    }

}
