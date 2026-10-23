package integration;

import httpserver.HttpHandler;
import httpserver.HttpServer;
import httpserver.core.FormPart;
import httpserver.session.TokenSessionStore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.GZIPInputStream;

import static httpserver.HttpServer.newHttpServer;
import static httpserver.core.ContentType.text_plain;
import static httpserver.core.FormParsing.parseMultipartForm;
import static httpserver.core.FormParsing.parseUrlEncodedForm;
import static httpserver.core.Headers.CONTENT_LENGTH;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.InputParser.getMandatoryString;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.OK;
import static httpserver.handlers.Common.logError;
import static httpserver.handlers.Common.redirect;
import static httpserver.handlers.Common.securityHeaders;
import static httpserver.handlers.Common.staticHtml;
import static httpserver.handlers.Compression.compress;
import static httpserver.handlers.MethodPathRouting.routing;
import static httpserver.util.Encoding.encodeBase64Url;
import static java.net.http.HttpClient.Redirect.NEVER;
import static java.net.http.HttpClient.Redirect.NORMAL;
import static java.net.http.HttpClient.Version.HTTP_1_1;
import static java.net.http.HttpResponse.BodyHandlers.ofByteArray;
import static java.net.http.HttpResponse.BodyHandlers.ofString;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Boots the real server on an ephemeral port and drives it with a real HTTP client, so the whole
 * pipeline (socket -> request parsing -> routing/handlers -> response writing) is exercised.
 */
public class RoundTripTest {

    private static final String GREETING = "Hello, world!";

    private static final TestSessionStore sessions = new TestSessionStore();

    private static HttpServer server;
    private static ExecutorService executor;
    private static HttpClient client;
    private static HttpClient noRedirectClient;
    private static String baseUrl;

    @BeforeAll
    public static void startServer() throws Exception {
        // The server is thread-per-connection and holds a worker for each keep-alive connection,
        // so the pool has to be larger than the number of concurrently open connections.
        executor = Executors.newFixedThreadPool(32, runnable -> {
            final var thread = new Thread(runnable, "round-trip-server");
            thread.setDaemon(true);
            return thread;
        });
        server = newHttpServer()
            .bind(0, "127.0.0.1")
            .daemon(true)
            .executor(executor)
            .handler(securityHeaders(true, compress(logError(e -> {}, e -> {}, routes()))))
            .build();
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getPort();

        client = HttpClient.newBuilder().version(HTTP_1_1).followRedirects(NORMAL)
                .connectTimeout(Duration.ofSeconds(5)).build();
        noRedirectClient = HttpClient.newBuilder().version(HTTP_1_1).followRedirects(NEVER)
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    @AfterAll
    public static void stopServer() throws IOException {
        server.stop();
        executor.shutdownNow();
    }

    private static HttpHandler routes() {
        return routing()
            .get("/hello", exchange -> respond(exchange).status(OK).contentType(text_plain)
                    .header("X-Custom", "round-trip").send(GREETING))
            .head("/hello", exchange -> respond(exchange).status(OK).contentType(text_plain)
                    .header("X-Custom", "round-trip").send(GREETING))
            .get("/query", exchange -> respond(exchange).status(OK)
                    .send("Hello " + getMandatoryString(exchange, "name")))
            .post("/form", exchange -> {
                final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8, Set.of("name", "age"));
                respond(exchange).status(OK).send(form.get("name") + " is " + form.get("age"));
            })
            .post("/upload", exchange -> {
                final var contentType = exchange.getRequestHeader(CONTENT_TYPE);
                final var contentLength = Integer.parseInt(exchange.getRequestHeader(CONTENT_LENGTH));
                final List<FormPart> parts = parseMultipartForm(contentType, contentLength, exchange.getInputStream());

                final var summary = new StringBuilder(parts.size() + " parts");
                for (final FormPart part : parts) {
                    summary.append(" | ").append(part.getDisposition())
                           .append(" => '").append(new String(part.getData(), UTF_8).trim()).append("'");
                }
                respond(exchange).status(OK).send(summary.toString());
            })
            .get("/redirect", redirect("/hello"))
            .get("/json", staticHtml("<html><body>round trip</body></html>"))
            .post("/login", exchange -> {
                final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8, Set.of("name"));
                sessions.setSession(exchange, form.get("name"));
                respond(exchange).status(OK).send("logged in");
            })
            .get("/me", exchange -> respond(exchange).status(OK)
                    .send("session says: " + sessions.getSession(exchange)))
            .post("/logout", exchange -> {
                sessions.deleteSession(exchange);
                respond(exchange).status(OK).send("logged out");
            });
    }

    private static HttpRequest.Builder request(final String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(10));
    }

    @Test
    public void testGetRoundTrip() throws Exception {
        final var response = client.send(request("/hello").GET().build(), ofString());

        assertEquals(200, response.statusCode());
        assertEquals(GREETING, response.body());
        assertEquals("text/plain; charset=UTF-8", response.headers().firstValue("content-type").orElseThrow());
        assertEquals("round-trip", response.headers().firstValue("x-custom").orElseThrow());
        assertEquals(Integer.toString(GREETING.length()), response.headers().firstValue("content-length").orElseThrow());
        assertTrue(response.headers().firstValue("date").isPresent(), "Expected a Date header");
    }

    @Test
    public void testSecurityHeadersRoundTrip() throws Exception {
        final var response = client.send(request("/hello").GET().build(), ofString());

        assertEquals("sameorigin", response.headers().firstValue("x-frame-options").orElseThrow());
        assertEquals("nosniff", response.headers().firstValue("x-content-type-options").orElseThrow());
        assertEquals("no-referrer", response.headers().firstValue("referrer-policy").orElseThrow());
        assertEquals("max-age=31536000; includeSubDomains",
                response.headers().firstValue("strict-transport-security").orElseThrow());
    }

    @Test
    public void testQueryStringRoundTrip() throws Exception {
        final var response = client.send(request("/query?name=Jane+Doe&page=2").GET().build(), ofString());

        assertEquals(200, response.statusCode());
        assertEquals("Hello Jane Doe", response.body());
    }

    @Test
    public void testInvalidRequestReturnsJsonError() throws Exception {
        final var response = client.send(request("/query").GET().build(), ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("\"message\":\"Missing parameter 'name'\""));
        assertEquals("application/json", response.headers().firstValue("content-type").orElseThrow());
    }

    @Test
    public void testUrlEncodedFormRoundTrip() throws Exception {
        final var response = client.send(request("/form")
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name=John+Doe&age=42")).build(), ofString());

        assertEquals(200, response.statusCode());
        assertEquals("John Doe is 42", response.body());
    }

    @Test
    public void testUrlEncodedFormWithUnknownFieldReturns400() throws Exception {
        final var response = client.send(request("/form")
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name=John+Doe&admin=true")).build(), ofString());

        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("these fields: [admin]"));
    }

    @Test
    public void testMultipartUploadRoundTrip() throws Exception {
        final var boundary = "314159265358979323846";
        final var body = "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"file1\"; filename=\"hello.txt\"\r\n"
            + "Content-Type: text/plain\r\n"
            + "\r\n"
            + "Hello, upload!\r\n"
            + "--" + boundary + "\r\n"
            + "Content-Disposition: form-data; name=\"field1\"\r\n"
            + "\r\n"
            + "some value\r\n"
            + "--" + boundary + "--\r\n";

        final var response = client.send(request("/upload")
                .header(CONTENT_TYPE, "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().startsWith("2 parts"));
        assertTrue(response.body().contains("name=\"file1\"; filename=\"hello.txt\" => 'Hello, upload!'"));
        assertTrue(response.body().contains("name=\"field1\" => 'some value'"));
    }

    @Test
    public void testRedirectRoundTrip() throws Exception {
        final var redirectResponse = noRedirectClient.send(request("/redirect").GET().build(), ofString());

        assertEquals(302, redirectResponse.statusCode());
        assertEquals("/hello", redirectResponse.headers().firstValue("location").orElseThrow());
        assertEquals("", redirectResponse.body());

        final var followedResponse = client.send(request("/redirect").GET().build(), ofString());

        assertEquals(200, followedResponse.statusCode());
        assertEquals(GREETING, followedResponse.body());
        assertEquals("/hello", followedResponse.uri().getPath());
    }

    @Test
    public void testHeadRequestRoundTrip() throws Exception {
        final var response = client.send(
                request("/hello").method("HEAD", HttpRequest.BodyPublishers.noBody()).build(), ofString());

        assertEquals(200, response.statusCode());
        assertEquals("", response.body());
    }

    @Test
    public void testConditionalGetRoundTrip() throws Exception {
        final var firstResponse = client.send(request("/json").GET().build(), ofString());

        assertEquals(200, firstResponse.statusCode());
        assertEquals("<html><body>round trip</body></html>", firstResponse.body());
        final var etag = firstResponse.headers().firstValue("etag").orElseThrow();

        final var secondResponse = client.send(
                request("/json").header("If-None-Match", etag).GET().build(), ofString());

        assertEquals(304, secondResponse.statusCode());
        assertEquals("", secondResponse.body());
    }

    @Test
    public void testGzipCompressionRoundTrip() throws Exception {
        final var response = client.send(
                request("/hello").header("Accept-Encoding", "gzip").GET().build(), ofByteArray());

        assertEquals(200, response.statusCode());
        assertEquals("gzip", response.headers().firstValue("content-encoding").orElseThrow());

        try (final var gzip = new GZIPInputStream(new ByteArrayInputStream(response.body()))) {
            assertEquals(GREETING, new String(gzip.readAllBytes(), UTF_8));
        }
    }

    @Test
    public void testSessionRoundTrip() throws Exception {
        final var loginResponse = client.send(request("/login")
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name=John+Doe")).build(), ofString());

        assertEquals(200, loginResponse.statusCode());
        final var sessionCookie = sessionCookieOf(loginResponse);

        final var meResponse = client.send(
                request("/me").header("Cookie", sessionCookie).GET().build(), ofString());
        assertEquals(200, meResponse.statusCode());
        assertEquals("session says: John Doe", meResponse.body());

        final var noCookieResponse = client.send(request("/me").GET().build(), ofString());
        assertEquals(403, noCookieResponse.statusCode());
        assertTrue(noCookieResponse.body().contains("\"code\":403"));

        final var logoutResponse = client.send(
                request("/logout").header("Cookie", sessionCookie).POST(HttpRequest.BodyPublishers.noBody()).build(),
                ofString());
        assertEquals(200, logoutResponse.statusCode());
        assertTrue(logoutResponse.headers().firstValue("set-cookie").orElseThrow().startsWith("session=;"));

        final var afterLogoutResponse = client.send(request("/me").GET().build(), ofString());
        assertEquals(403, afterLogoutResponse.statusCode());
    }

    @Test
    public void testTamperedSessionCookieIsRejected() throws Exception {
        final var loginResponse = client.send(request("/login")
                .header(CONTENT_TYPE, "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("name=John+Doe")).build(), ofString());

        final var sessionValue = sessionCookieOf(loginResponse).substring("session=".length());
        final var forged = "session=" + encodeBase64Url("someone-else".getBytes(UTF_8))
                + sessionValue.substring(sessionValue.indexOf('.'));

        final var response = client.send(
                request("/me").header("Cookie", forged).GET().build(), ofString());

        assertEquals(403, response.statusCode());
    }

    @Test
    public void testConcurrentRequestsAllGetTheirOwnResponse() throws Exception {
        final int requestCount = 16;
        final var futures = new ArrayList<CompletableFuture<HttpResponse<String>>>();
        for (int i = 0; i < requestCount; i++) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    return client.send(request("/hello").GET().build(), ofString());
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(30, SECONDS);

        for (final var future : futures) {
            final var response = future.get();
            assertEquals(200, response.statusCode());
            assertEquals(GREETING, response.body());
        }
    }

    @Test
    public void testWireFormatWithRawSocket() throws Exception {
        try (final var socket = new Socket("127.0.0.1", server.getPort())) {
            socket.setSoTimeout(10_000);

            socket.getOutputStream().write(
                    ("GET /hello HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n").getBytes(UTF_8));
            socket.getOutputStream().flush();

            final String response = new String(socket.getInputStream().readAllBytes(), UTF_8);
            final String[] lines = response.split("\r?\n");

            assertEquals("HTTP/1.1 200 Ok", lines[0]);
            assertTrue(response.contains("Content-Length: " + GREETING.length()));
            assertTrue(response.contains("X-Frame-Options: sameorigin"));
            assertTrue(response.contains("Date: "));
            assertTrue(response.endsWith(GREETING));
        }
    }

    private static String sessionCookieOf(final HttpResponse<String> response) {
        final var setCookie = response.headers().firstValue("set-cookie")
                .orElseThrow(() -> new AssertionError("Expected a Set-Cookie header, got: " + response.headers()));
        assertTrue(setCookie.startsWith("session="), "Expected a session cookie, found: " + setCookie);
        return setCookie.substring(0, setCookie.indexOf(';'));
    }

    private static final class TestSessionStore implements TokenSessionStore<String> {

        private final byte[] validationKey = "round-trip-validation-key".getBytes(UTF_8);

        @Override
        public byte[] getSessionValidationKey() {
            return validationKey;
        }
        @Override
        public byte[] sessionToJson(final String session, final Charset charset) {
            return session.getBytes(charset);
        }
        @Override
        public String sessionFromJson(final String json) {
            return json;
        }
    }

}
