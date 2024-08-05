package httpserver.core;

import httpserver.util.Chars;
import httpserver.util.LengthRestrictedInputStream;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
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
import static httpserver.util.Chars.EQUALS;
import static httpserver.util.Chars.QUESTION_MARK;
import static httpserver.util.Encoding.decodeUrl;
import static httpserver.util.Strings.*;
import static java.nio.charset.StandardCharsets.US_ASCII;
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

    private int statusCode = OK;
    private String statusMessage;
    private final Map<String, String> responseHeaders = new HashMap<>();
    private ResponseBody responseBody = EMPTY_BODY;

    private boolean noContentLength = false;
    private boolean responseSent = false;

    private List<ExchangeCompleteListener> exchangeCompleteListeners;

    public HttpServerExchange(final Socket socket, final byte[] request, final int length, final InputStream in, final OutputStream out) throws IOException {
        this.socket = socket;
        this.rawRequest = request;
        this.in = in;
        this.out = out;

        try (final var reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(request, 0, length)))) {
            final String firstLine = reader.readLine();
            final int firstSpaceOffset = firstLine.indexOf(Chars.SPACE);
            final int secondSpaceOffset = firstLine.indexOf(Chars.SPACE, firstSpaceOffset + 1);
            this.method = firstLine.substring(0, firstSpaceOffset);
            if (secondSpaceOffset != -1) {
                this.uri = firstLine.substring(firstSpaceOffset + 1, secondSpaceOffset);
                this.protocol = firstLine.substring(secondSpaceOffset + 1);
            } else {
                this.uri = firstLine.substring(firstSpaceOffset + 1);
                this.protocol = HTTP_09;
            }
            if (HTTP_10.equals(protocol)) responseHeaders.put(CONNECTION, KEEP_ALIVE);
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
        final String searchName = name.toLowerCase() + HEADER_SEPARATOR;
        for (final var header : headers) {
            if (header.toLowerCase().startsWith(searchName))
                return header.substring(name.length() + HEADER_SEPARATOR.length());
        }
        return null;
    }
    public List<String> getRequestHeaders(final String name) {
        final var headers = new ArrayList<String>();

        final String searchName = name.toLowerCase() + HEADER_SEPARATOR;
        for (final var header : this.headers) {
            if (header.toLowerCase().startsWith(searchName))
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
        if (queryParameters != null) {
            return queryParameters.get(name);
        }

        this.queryParameters = new HashMap<>();
        final int questionOffset = uri.indexOf(QUESTION_MARK);
        if (questionOffset == -1) return null;

        final String[] parameters = uri.substring(questionOffset + 1).split(AMPERSAND);
        for (final String paramPair : parameters) {
            final int equalsOffset = paramPair.indexOf(EQUALS);
            if (equalsOffset == -1) queryParameters.put(paramPair, EMPTY);
            else {
                final String paramName = decodeUrl(paramPair.substring(0, equalsOffset));
                final String paramValue = decodeUrl(paramPair.substring(equalsOffset + 1));
                queryParameters.put(paramName, paramValue);
            }
        }
        return queryParameters.get(name);
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

    private static final byte[] RESPONSE_CONTINUE = "HTTP/1.1 100 Continue\r\n\r\n".getBytes(US_ASCII);
    public void requestBodyAccepted() throws IOException {
        if ("HTTP/1.1".equals(protocol)) {
            final String expect = getRequestHeader(EXPECT);
            if ("100-continue".equals(expect) ) {
                out.write(RESPONSE_CONTINUE);
            }
        }
    }

    public InputStream getInputStream() {
        final String contentLength = getRequestHeader(CONTENT_LENGTH);
        if (contentLength == null) return new ByteArrayInputStream(new byte[0]);
        final long length = Long.parseLong(contentLength);
        return new LengthRestrictedInputStream(in, length);
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
                if (!noContentLength && methodAllowsResponseBody() && !responseHeaders.containsKey(CONTENT_LENGTH)) {
                    writer.println(CONTENT_LENGTH + HEADER_SEPARATOR + responseBody.getLength());
                }
                if (!responseHeaders.containsKey(DATE)) {
                    writer.println(DATE + HEADER_SEPARATOR + DATE_FORMAT.format(now()));
                }
                writer.print(CRLF);
                writer.flush();
            }
            if (methodAllowsResponseBody()) {
                responseBody.writeTo(out);
            }
            responseSent = true;
        }
    }

    public boolean isResponseSent() {
        return responseSent;
    }

    public ResponseBody getResponseBody() {
        return responseBody;
    }
    public boolean methodAllowsResponseBody() {
        return !HEAD.equals(method);
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
