
[![GitHub Release](https://img.shields.io/github/release/codemonstur/httpserver.svg)](https://github.com/codemonstur/httpserver/releases)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.codemonstur/httpserver.svg)](https://central.sonatype.com/artifact/com.github.codemonstur/httpserver)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# HTTP server

A standalone embedded HTTP server.

Features it has:
- Support for HTTP/0.9
- Support for HTTP/1.0
- Support for HTTP/1.1
- Common headers and status codes
- Parsing url encoded forms
- Parsing multipart forms
- Helpers for request parsing
- Helpers for response sending
- Server sent events
- Session store
- Routing handler based on method and path
- Compression using gz and deflate
- Compilable with Graal

Missing features:
- No websockets
- No HTTP/2
- No chunked transfer encoding

## Usage

Add the maven dependency:

    <dependency>
        <groupId>com.github.codemonstur</groupId>
        <artifactId>httpserver</artifactId>
        <version>1.5.0</version>
    </dependency>

Instantiate the server from somewhere (your main() probably):

    HttpServer.newHttpServer()
        .bind(8080, "0.0.0.0")
        .handler(handler)
        .build()
        .start();

Optionally define some routes:

    MethodPathRouting.methodPathRouting()
        .put("/api/v1/endpoint", putEndpoint())
        .post("/api/v1/endpoint", postEndpoint())
        .get("/api/v1/endpoint", getEndpoint())
        .delete("/api/v1/endpoint", deleteEndpoint())

Define a handler by implementing this interface:

    public interface HttpHandler {
        void handleRequest(HttpServerExchange exchange) throws Exception;
    }

Throw an Exception, and the server will return a 500 Internal Server Error.
Parse your request and send your response using the HttpServerExchange object.

An example of a handler:

    public static HttpHandler postEndpoint() {
        return exchange -> {
            final var form = parseForm(exchange, UTF_8);
            final long id = getMandatoryLong(form, "id");
            ... do something here ...
            respond(exchange).status(NO_CONTENT).send();
        };
    }

## Brotli compression

The optional `httpserver.handlers.Brotli` handler adds `br` compression alongside the gzip and
deflate support of `httpserver.handlers.Compression`. It is backed by
[brotli4j](https://github.com/aayushatharva/brotli4j), which is an optional dependency of this
library, so add it explicitly to use the handler:

    <dependency>
        <groupId>com.aayushatharva.brotli4j</groupId>
        <artifactId>brotli4j</artifactId>
        <version>1.12.0</version>
    </dependency>

The matching native library for your platform is pulled in automatically. Wrap your handler with
`Brotli.compress(...)` for gzip/deflate/br, or `Brotli.compressBrotli(...)` for br only.

## Threading

Each connection is handled by a worker thread taken from a cached thread pool, so the number of
threads grows and shrinks with the number of connections instead of being fixed. A worker is only
held while a request is being handled or while a connection waits for the next request on a
keep-alive connection; that wait is bounded by `keepAliveTimeout` (60 seconds by default), after
which the idle connection is closed and its worker released. Server sent event handlers take their
connection over and keep their worker for as long as the client stays connected.

Because the library targets Java 17, it uses platform threads rather than virtual threads.
This should be sufficient to use in production.

## Testing

The library has a test suite under `src/test`, covering the parsing, routing, session and util
classes, plus an integration test (`integration.RoundTripTest`) that boots a real server and drives
it over real sockets. Run it with:

    mvn test

The suite includes a guard (`unittests.ByteCodeVersionTest`) that fails the build if the compiled
classes ever target a class file version newer than Java 17, or get marked as preview features.

## Security

- Form parsing has a Denial of Service issue.
- There are no limits on resource use.
- There is no protection against header injection

## Graal support

The library targets Java 17, an LTS that GraalVM supports, so there is no dependency on preview
features such as virtual threads to wait for.
