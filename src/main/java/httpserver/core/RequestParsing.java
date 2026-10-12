package httpserver.core;

import httpserver.util.LengthRestrictedInputStream;

import java.io.IOException;
import java.io.InputStream;

public enum RequestParsing {;

    public static int readRequestHead(final byte[] data, final InputStream in) throws IOException {
        data[0] = (byte) in.read();
        data[1] = (byte) in.read();
        data[2] = (byte) in.read();
        data[3] = (byte) in.read();
        if (hasReadRequest(data, 4)) return 4;

        int offset = 4; int b; while ((b = in.read()) != -1) {
            data[offset++] = (byte) b;
            if (hasReadRequest(data, offset)) break;
            if (offset >= data.length) throw new IOException("Request header data too large");
        }
        return offset;
    }

    private static boolean hasReadRequest(final byte[] data, final int offset) {
        return data[offset-4] == '\r'
            && data[offset-3] == '\n'
            && data[offset-2] == '\r'
            && data[offset-1] == '\n';
    }

    public static void discardRemainingRequestBody(final HttpServerExchange exchange) throws IOException {
        final var in = exchange.getInputStream();
        // A client waiting for a 100 Continue will never send the body we are about to drain,
        // so answer the expectation first instead of blocking on a body that is not coming.
        if (in instanceof final LengthRestrictedInputStream limited && limited.remaining() > 0)
            exchange.requestBodyAccepted();
        while (in.read() != -1);
    }

}
