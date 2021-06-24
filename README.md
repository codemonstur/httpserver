[![Build Status](https://travis-ci.org/codemonstur/httpserver.svg?branch=master)](https://travis-ci.org/codemonstur/httpserver)
[![GitHub Release](https://img.shields.io/github/release/codemonstur/httpserver.svg)](https://github.com/codemonstur/httpserver/releases)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.codemonstur/httpserver/badge.svg)](http://mvnrepository.com/artifact/com.github.codemonstur/httpserver)
[![MIT Licence](https://badges.frapsoft.com/os/mit/mit.svg?v=103)](https://opensource.org/licenses/mit-license.php)

# HTTP server

An HTTP server built on top of common-server.

Features it has:
- Support for HTTP/0.9
- Support for HTTP/1.0
- Support for HTTP/1.1
- Common headers and status codes
- Parsing url encoded forms
- Helpers for request parsing
- Helpers for response sending
- Session store
- Routing handler based on method and path

Missing features:
- No SSE
- No websockets
- No HTTP/2
- No output compression
- No multipart form parsing

## Usage

Add the maven dependency:

    <dependency>
        <groupId>codemonstur</groupId>
        <artifactId>httpserver</artifactId>
        <version>0.1.0</version>
    </dependency>

Instantiate the server from somewhere (your main() probably):

    new HttpServer()
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

## Threading

Common-server provides the underlying socket handling.
This library creates a new Thread for each incoming connection.
It is not advised to use this HTTP server in production.

However, the code is very simple and should be very easy to make work with Loom.
When it comes.

And it should also be easy to compile with Graal.
It would be amazing if we could have both.

