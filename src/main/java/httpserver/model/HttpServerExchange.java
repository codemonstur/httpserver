package httpserver.model;

import httpserver.core.Chars;
import httpserver.core.HttpInputStream;
import httpserver.core.ResponseBody;

import java.io.*;
import java.nio.charset.Charset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static httpserver.core.Headers.*;
import static httpserver.core.ResponseBody.*;
import static httpserver.core.StatusCode.OK;
import static httpserver.core.StatusCode.getMessageForCode;
import static httpserver.core.Strings.*;
import static java.time.Instant.now;
import static java.util.Locale.ENGLISH;

public class HttpServerExchange {

    private final byte[] rawRequest;

    private final String method;
    private final String rawPath;
    private final String protocol;
    private final List<String> headers;

    private final InputStream in;
    private final OutputStream out;

    private int statusCode = OK;
    private String statusMessage;
    private final Map<String, String> responseHeaders = new HashMap<>();
    private ResponseBody responseBody = EMPTY_BODY;

    private boolean responseSent = false;

    public HttpServerExchange(final byte[] request, final InputStream in, final OutputStream out) throws IOException {
        this.rawRequest = request;
        this.in = in;
        this.out = out;

        try (final var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request)))) {
            final String firstLine = reader.readLine();
            final int firstSpaceOffset = firstLine.indexOf(Chars.SPACE);
            final int secondSpaceOffset = firstLine.indexOf(Chars.SPACE, firstSpaceOffset + 1);
            this.method = firstLine.substring(0, firstSpaceOffset);
            if (secondSpaceOffset != -1) {
                this.rawPath = firstLine.substring(firstSpaceOffset + 1, secondSpaceOffset);
                this.protocol = firstLine.substring(secondSpaceOffset + 1);
            } else {
                this.rawPath = firstLine.substring(firstSpaceOffset + 1);
                this.protocol = HTTP_09;
            }
            if (HTTP_10.equals(protocol)) responseHeaders.put(CONNECTION, KEEP_ALIVE);
            this.headers = new ArrayList<>();
            String headerLine; while ((headerLine = reader.readLine()) != null) {
                if (headerLine.isEmpty()) break;
                this.headers.add(headerLine);
            }
        }
    }

    public byte[] getRawRequest() {
        return rawRequest;
    }
    public String getRequestMethod() {
        return method;
    }
    public String getRequestRawPath() {
        return rawPath;
    }
    public String getRequestProtocol() {
        return protocol;
    }
    public String getRequestHeader(final String name) {
        final String searchName = name.toLowerCase()+HEADER_SEPARATOR;
        for (final var header : headers) {
            if (header.toLowerCase().startsWith(searchName))
                return header.substring(name.length() + HEADER_SEPARATOR.length());
        }
        return null;
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
        return this.responseHeaders.get(name);
    }
    public void setResponseHeader(final String name, final String value) {
        this.responseHeaders.put(name, value);
    }
    public void removeResponseHeader(final String name) {
        this.responseHeaders.remove(name);
    }

    public void send(final String data, final Charset charset) {
        send(data.getBytes(charset));
    }
    public void send(final byte[] data) {
        this.responseBody = newByteArrayBody(data);
    }
    public void send(final InputStream in, final long length) {
        this.responseBody = newInputStreamBody(in, length);
    }

    public InputStream getInputStream() {
        final String contentLength = getRequestHeader(CONTENT_LENGTH);
        if (contentLength == null) return new ByteArrayInputStream(new byte[0]);
        final long length = Long.parseLong(contentLength);
        return new HttpInputStream(in, length);
    }
    public OutputStream getOutputStream() {
        return out;
    }

    private static final DateTimeFormatter
        DATE_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ENGLISH)
            .withZone(ZoneId.of("GMT"));

    public void sendResponse() throws IOException {
        if (!responseSent) {
            if (shouldSendHeadResponse()) {
                final PrintWriter writer = new PrintWriter(out);
                writer.println(protocol + SPACE + statusCode + SPACE + getMessageForCode(statusCode, statusMessage));
                for (final var header : responseHeaders.entrySet()) {
                    writer.println(header.getKey() + HEADER_SEPARATOR + header.getValue());
                }
                if (methodAllowsResponseBody() && !responseHeaders.containsKey(CONTENT_LENGTH)) {
                    writer.println(CONTENT_LENGTH + HEADER_SEPARATOR + responseBody.getLength());
                }
                if (!responseHeaders.containsKey(DATE)) {
                    writer.println(DATE + HEADER_SEPARATOR + DATE_FORMAT.format(now()));
                }
                writer.print(CRLF);
                writer.flush();
            }
            responseBody.writeTo(out);
            responseSent = true;
        }
    }

    private boolean shouldSendHeadResponse() throws IOException {
        if (HTTP_09.equals(protocol)) return false;
        if (HTTP_10.equals(protocol)) return true;
        if (HTTP_11.equals(protocol)) return true;
        throw new IOException("HTTP protocol not recognised");
    }

    private boolean methodAllowsResponseBody() {
        return !HEAD.equals(method);
    }

}
