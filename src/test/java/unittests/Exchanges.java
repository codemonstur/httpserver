package unittests;

import httpserver.core.HttpServerExchange;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public enum Exchanges {;

    public static HttpServerExchange newExchange(final String request) throws IOException {
        return newExchange(request, new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream());
    }

    public static HttpServerExchange newExchange(final String request, final String body) throws IOException {
        return newExchange(request, new ByteArrayInputStream(body.getBytes(UTF_8)), new ByteArrayOutputStream());
    }

    public static HttpServerExchange newExchange(final String request, final InputStream in, final OutputStream out)
            throws IOException {
        final byte[] bytes = request.getBytes(UTF_8);
        return new HttpServerExchange(null, bytes, bytes.length, in, out);
    }

    public static String request(final String method, final String uri) {
        return method + " " + uri + " HTTP/1.1\r\nHost: localhost\r\n\r\n";
    }

}
