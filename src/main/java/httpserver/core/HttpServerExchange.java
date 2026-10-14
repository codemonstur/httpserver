package httpserver.core;

import httpserver.util.Chars;
import httpserver.util.ChunkedOutputStream;
import httpserver.util.LengthRestrictedInputStream;
import httpserver.websocket.WebSocket;
import httpserver.websocket.WebSocketHandler;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static httpserver.core.ContentType.text_plain;
import static httpserver.core.Headers.*;
import static httpserver.core.ResponseBody.*;
import static httpserver.core.StatusCode.BAD_REQUEST;
import static httpserver.core.StatusCode.NOT_MODIFIED;
import static httpserver.core.StatusCode.NO_CONTENT;
import static httpserver.core.StatusCode.OK;
import static httpserver.core.StatusCode.SWITCHING_PROTOCOLS;
import static httpserver.core.StatusCode.UPGRADE_REQUIRED;
import static httpserver.core.StatusCode.getMessageForCode;
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Chars.QUESTION_MARK;
import static httpserver.util.Encoding.decodeUrl;
import static httpserver.util.Strings.*;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.util.Locale.ENGLISH;

public class HttpServerExchange {

    private final byte[] rawRequest;

    private final String method;
    private final String uri;
    private final String protocol;
    private final List<String> headers;

    private final Socket socket;
    private final InputStream in;
    private final OutputStream out;

    private String path;
    private Map<String, String> queryParameters;
    private InputStream requestBody;

    private int statusCode = OK;
    private String statusMessage;
    private final Map<String, List<String>> responseHeaders = new LinkedHashMap<>();
    private ResponseBody responseBody = EMPTY_BODY;

    private boolean noContentLength = false;
    private boolean responseSent = false;
    private boolean continueSent = false;
    private boolean connectionClosed = false;

    private List<ExchangeCompleteListener> exchangeCompleteListeners;

    public HttpServerExchange(final Socket socket, final byte[] request, final int length, final InputStream in, final OutputStream out) throws IOException {
        this.socket = socket;
        this.rawRequest = request;
        this.in = in;
        this.out = out;

        try (final var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request, 0, length)))) {
            final String firstLine = reader.readLine();
            if (firstLine == null) throw new IOException("Request head is empty");
            final int firstSpaceOffset = firstLine.indexOf(Chars.SPACE);
            if (firstSpaceOffset <= 0)
                throw new IOException("Malformed request line: '" + firstLine + "'");

            final int secondSpaceOffset = firstLine.indexOf(Chars.SPACE, firstSpaceOffset + 1);
            this.method = firstLine.substring(0, firstSpaceOffset);
            if (secondSpaceOffset != -1) {
                this.uri = firstLine.substring(firstSpaceOffset + 1, secondSpaceOffset);
                this.protocol = firstLine.substring(secondSpaceOffset + 1);
            } else {
                this.uri = firstLine.substring(firstSpaceOffset + 1);
                this.protocol = HTTP_09;
            }
            if (HTTP_10.equals(protocol)) responseHeaders.put(CONNECTION, new ArrayList<>(List.of(KEEP_ALIVE)));
            this.headers = new ArrayList<>();
            String headerLine; while ((headerLine = reader.readLine()) != null) {
                if (headerLine.isEmpty()) break;
                // We do not do header concatenation, it has been deprecated in RFC 7230
                this.headers.add(headerLine);
            }
        }
    }

    public InetAddress getSourceIpAddress() {
        return socket.getInetAddress();
    }
    public byte[] getRawRequest() {
        return rawRequest;
    }
    public String getRequestMethod() {
        return method;
    }
    public String getRequestURI() {
        return uri;
    }
    public String getRequestProtocol() {
        return protocol;
    }
    public String getRequestHeader(final String name) {
        final var values = getRequestHeaders(name);
        return values.isEmpty() ? null : values.get(0);
    }
    public List<String> getRequestHeaders(final String name) {
        final var headers = new ArrayList<String>();

        final String searchName = name.toLowerCase(ENGLISH) + HEADER_SEPARATOR;
        for (final var header : this.headers) {
            if (header.toLowerCase(ENGLISH).startsWith(searchName))
                headers.add(header.substring(name.length() + HEADER_SEPARATOR.length()));
        }

        return headers;
    }

    public String getRequestPath() {
        if (path != null) return path;

        final int questionOffset = uri.indexOf(QUESTION_MARK);
        this.path = uri.substring(0, questionOffset != -1 ? questionOffset : uri.length());
        return path;
    }
    public String getQueryString() {
        final int questionOffset = uri.indexOf(QUESTION_MARK);
        return questionOffset == -1 ? null : uri.substring(questionOffset + 1);
    }

    public String getQueryParameter(final String name) {
        if (queryParameters != null) return queryParameters.get(name);

        final int questionOffset = uri.indexOf(QUESTION_MARK);
        if (questionOffset == -1) {
            this.queryParameters = Map.of();
            return null;
        }

        // Parsed into a local map first: a malformed escape part way through must not leave a
        // half populated cache behind for the next call to read.
        final Map<String, String> parsed = new HashMap<>();
        final String[] parameters = uri.substring(questionOffset + 1).split(AMPERSAND);
        for (final String paramPair : parameters) {
            final int equalsOffset = paramPair.indexOf(EQUALS);
            if (equalsOffset == -1) parsed.put(paramPair, EMPTY);
            else {
                final String paramName = decodeUrl(paramPair.substring(0, equalsOffset));
                final String paramValue = decodeUrl(paramPair.substring(equalsOffset + 1));
                parsed.put(paramName, paramValue);
            }
        }
        this.queryParameters = parsed;
        return parsed.get(name);
    }

    public int getStatusCode() {
        return statusCode;
    }
    public void setStatusCode(final int code) {
        this.statusCode = code;
    }
    public String getStatusMessage() {
        return statusMessage;
    }
    public void setStatusMessage(final String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public String getResponseHeader(final String name) {
        final var key = responseHeaderKey(name);
        if (key == null) return null;
        final var values = responseHeaders.get(key);
        return values.isEmpty() ? null : values.get(0);
    }
    /**
     * Replaces the header entirely, whatever casing the previous value was stored under.
     */
    public void setResponseHeader(final String name, final String value) {
        removeResponseHeader(name);
        addResponseHeader(name, value);
    }
    /**
     * Appends a value, which is what headers such as Set-Cookie need: a response may carry
     * several of them and none may be dropped.
     */
    public void addResponseHeader(final String name, final String value) {
        if (value == null) {
            removeResponseHeader(name);
            return;
        }
        final var key = responseHeaderKey(name);
        responseHeaders.computeIfAbsent(key == null ? name : key, k -> new ArrayList<>()).add(value);
    }
    public void removeResponseHeader(final String name) {
        final var key = responseHeaderKey(name);
        if (key != null) responseHeaders.remove(key);
    }
    private String responseHeaderKey(final String name) {
        for (final var key : responseHeaders.keySet()) if (key.equalsIgnoreCase(name)) return key;
        return null;
    }

    public void setNoContentLength() {
        this.noContentLength = true;
    }

    public void send(final String data, final Charset charset) {
        send(data.getBytes(charset));
    }
    public void send(final byte[] data) {
        this.responseBody = newByteArrayBody(data);
    }
    public void send(final byte[] data, final int offset, final int length) {
        this.responseBody = newByteArrayBody(data, offset, length);
    }
    public void send(final InputStream in, final long length) {
        this.responseBody = newInputStreamBody(in, length);
    }
    /**
     * Streams a response body of unknown length with chunked transfer encoding. Overrides any
     * earlier setNoContentLength(): the body is framed, not left to the connection. Clients
     * that predate HTTP/1.1 cannot read chunked, so for them the connection is marked to close
     * and the body is delimited by that close instead.
     */
    public void sendChunked(final InputStream in) {
        this.responseBody = newInputStreamBody(in);
        this.noContentLength = false;
        if (!HTTP_11.equals(protocol)) setResponseHeader(CONNECTION, CLOSE);
    }

    private static final byte[] RESPONSE_CONTINUE = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(US_ASCII);
    /**
     * Answers an Expect: 100-continue. Idempotent, because both a handler and the server drain
     * of the remaining body may want to call it for the same exchange.
     */
    public void requestBodyAccepted() throws IOException {
        if (continueSent) return;
        if (HTTP_11.equals(protocol)) {
            final String expect = getRequestHeader(EXPECT);
            if ("100-continue".equals(expect)) {
                continueSent = true;
                out.write(RESPONSE_CONTINUE);
                out.flush();
            }
        }
    }

    public InputStream getInputStream() {
        if (requestBody != null) return requestBody;

        final String contentLength = getRequestHeader(CONTENT_LENGTH);
        if (contentLength == null) {
            requestBody = new ByteArrayInputStream(new byte[0]);
        } else {
            // Asking for the body is the handler saying it is ready to read it, which is exactly
            // what an Expect: 100-continue client is blocked on.
            try {
                requestBodyAccepted();
            } catch (final IOException e) {
                // The peer is gone; the read that follows will report it in its own time.
            }
            requestBody = parseBody(contentLength);
        }
        return requestBody;
    }
    private InputStream parseBody(final String contentLength) {
        try {
            final long length = Long.parseLong(contentLength.trim());
            // A negative length cannot describe a body; a value we cannot parse at all has
            // already been rejected with a 400 before any handler ran.
            return new LengthRestrictedInputStream(in, Math.max(0, length));
        } catch (final NumberFormatException e) {
            return new ByteArrayInputStream(new byte[0]);
        }
    }
    public OutputStream getOutputStream() {
        return out;
    }

    /**
     * Blocks until the client closes its side of the connection. Used by long lived handlers,
     * such as server sent events, that take the connection over and must hold on to their
     * worker until the stream really ends.
     */
    public void awaitClientDisconnect() throws IOException {
        try {
            final byte[] buffer = new byte[1024];
            while (in.read(buffer) != -1);
        } finally {
            connectionClosed = true;
        }
    }
    public boolean isConnectionClosed() {
        return connectionClosed;
    }

    /**
     * True when the client asked to switch this connection to the websocket protocol. The
     * version and the key are checked by acceptWebSocket, which rejects an attempt that gets
     * them wrong; callers use this to choose between a websocket and their fallback.
     */
    public boolean isWebSocketUpgrade() {
        return containsToken(UPGRADE, "websocket") && containsToken(CONNECTION, "Upgrade");
    }
    private boolean containsToken(final String name, final String token) {
        for (final String value : getRequestHeaders(name))
            for (final String part : value.split(","))
                if (token.equalsIgnoreCase(part.trim())) return true;
        return false;
    }

    /**
     * Completes the opening handshake and runs the websocket session to its end, blocking the
     * worker exactly as server sent events do: the connection belongs to the session until it
     * closes. Returns the (by then closed) session, or null when the handshake was rejected,
     * in which case the response says why: 426 for an unsupported version, 400 for a
     * malformed attempt.
     */
    public WebSocket acceptWebSocket(final WebSocketHandler handler) throws IOException {
        if (!isWebSocketUpgrade()) {
            setStatusCode(BAD_REQUEST);
            setResponseHeader(CONTENT_TYPE, text_plain.toString());
            send("Expected an Upgrade: websocket request", UTF_8);
            return null;
        }
        final String version = getRequestHeader(SEC_WEBSOCKET_VERSION);
        if (version == null || !"13".equals(version.trim())) {
            setStatusCode(UPGRADE_REQUIRED);
            setResponseHeader(CONTENT_TYPE, text_plain.toString());
            setResponseHeader(SEC_WEBSOCKET_VERSION, "13");
            send("Only websocket version 13 is supported", UTF_8);
            return null;
        }
        final String key = getRequestHeader(SEC_WEBSOCKET_KEY);
        if (key == null || decodeNonce(key) == null) {
            setStatusCode(BAD_REQUEST);
            setResponseHeader(CONTENT_TYPE, text_plain.toString());
            send("Sec-WebSocket-Key is missing or malformed", UTF_8);
            return null;
        }

        setStatusCode(SWITCHING_PROTOCOLS);
        setResponseHeader(UPGRADE, "websocket");
        setResponseHeader(CONNECTION, "Upgrade");
        setResponseHeader(SEC_WEBSOCKET_ACCEPT, WebSocket.acceptKey(key));
        sendResponse();

        final var webSocket = new WebSocket(in, out, handler);
        try {
            webSocket.run();
        } finally {
            // The keep-alive loop must never read this connection as HTTP again.
            connectionClosed = true;
        }
        return webSocket;
    }

    /** The client's nonce: a valid key is base64 of exactly 16 bytes, anything else is not one. */
    private static byte[] decodeNonce(final String key) {
        try {
            final byte[] nonce = Base64.getDecoder().decode(key);
            return nonce.length == 16 ? nonce : null;
        } catch (final IllegalArgumentException e) {
            return null;
        }
    }

    public static final DateTimeFormatter
        DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ENGLISH)
            .withZone(ZoneId.of("GMT"));

    public void sendResponse() throws IOException {
        if (responseSent) return;

        final boolean chunked = shouldSendChunked();

        if (shouldSendHeadResponse()) {
            final PrintWriter writer = new PrintWriter(out);
            // HTTP requires CRLF line endings, whatever the platform separator happens to be.
            writer.print(protocol + SPACE + statusCode + SPACE + getMessageForCode(statusCode, statusMessage) + CRLF);
            for (final var header : responseHeaders.entrySet())
                for (final String value : header.getValue())
                    writer.print(header.getKey() + HEADER_SEPARATOR + value + CRLF);
            if (chunked)
                writer.print(TRANSFER_ENCODING + HEADER_SEPARATOR + CHUNKED + CRLF);
            if (shouldSendContentLength())
                writer.print(CONTENT_LENGTH + HEADER_SEPARATOR + responseBody.getLength() + CRLF);
            if (getResponseHeader(DATE) == null)
                writer.print(DATE + HEADER_SEPARATOR + DATE_FORMAT.format(now()) + CRLF);
            writer.print(CRLF);
            writer.flush();
        }
        if (methodAllowsResponseBody()) {
            if (chunked) {
                // Closed only once the body has been written in full: if the write fails part
                // way through, the missing terminator is what lets the client notice that the
                // body was truncated, and the exception that follows closes the connection.
                final var chunkedOut = new ChunkedOutputStream(out);
                responseBody.writeTo(chunkedOut);
                chunkedOut.close();
            } else {
                responseBody.writeTo(out);
            }
        }
        responseSent = true;
    }

    public boolean isResponseSent() {
        return responseSent;
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }
    /**
     * False for HEAD (the headers describe the representation the equivalent GET would return)
     * and for the status codes that must never carry a body.
     */
    public boolean methodAllowsResponseBody() {
        if (HEAD.equals(method)) return false;
        if (statusCode < 200) return false;
        if (statusCode == NO_CONTENT || statusCode == NOT_MODIFIED) return false;
        return true;
    }

    /**
     * RFC 9112 forbids Content-Length on 1xx and 204, and forbids it on a 304 unless it is the
     * length of the selected representation, which we never know here. A body of unknown length
     * has no value to send either way; it is framed as chunked instead.
     */
    private boolean shouldSendContentLength() {
        if (noContentLength) return false;
        if (getResponseHeader(CONTENT_LENGTH) != null) return false;
        if (responseBody.getLength() == UNKNOWN_LENGTH) return false;
        if (statusCode < 200) return false;
        if (statusCode == NO_CONTENT || statusCode == NOT_MODIFIED) return false;
        return true;
    }

    /**
     * A body whose length is not known before it has been written is framed as chunked transfer
     * encoding, but only for HTTP/1.1: sendChunked() marks older connections to close, so their
     * body is delimited by that close instead. Framing a handler chose explicitly, and the status
     * codes that may carry neither a body nor a framing header, are left alone.
     */
    private boolean shouldSendChunked() {
        if (noContentLength) return false;
        if (responseBody.getLength() != UNKNOWN_LENGTH) return false;
        if (getResponseHeader(TRANSFER_ENCODING) != null) return false;
        if (statusCode < 200) return false;
        if (statusCode == NO_CONTENT || statusCode == NOT_MODIFIED) return false;
        return HTTP_11.equals(protocol);
    }

    private boolean shouldSendHeadResponse() throws IOException {
        if (HTTP_09.equals(protocol)) return false;
        if (HTTP_10.equals(protocol)) return true;
        if (HTTP_11.equals(protocol)) return true;
        throw new IOException("HTTP protocol not recognised");
    }

    public void addExchangeCompleteListener(final ExchangeCompleteListener listener) {
        if (exchangeCompleteListeners == null) exchangeCompleteListeners = new ArrayList<>();
        exchangeCompleteListeners.add(listener);
    }

    public void notifyCompleteListeners(final Exception exception) {
        if (exchangeCompleteListeners != null) {
            for (final var listener : exchangeCompleteListeners) {
                listener.onExchangeComplete(exception, this);
            }
        }
    }

}
