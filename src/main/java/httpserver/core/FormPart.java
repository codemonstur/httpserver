package httpserver.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;

// Content-Disposition header parsing can be difficult.
// Documented here:
// - https://datatracker.ietf.org/doc/html/rfc2231
// - https://datatracker.ietf.org/doc/html/rfc5987
// There are three types of headers. Bare, Quoted String, and Asterisk string.
//
// TODO add Content-Disposition header parsing
// TODO test if the byte[] containing the form data is correct
public final class FormPart {

    private static final byte[] CRLF_CRLF = "\r\n\r\n".getBytes(UTF_8);

    private static final int
        LENGTH_CONTENT_TYPE = "Content-Type: ".length(),
        LENGTH_CONTENT_DISPOSITION = "Content-Disposition: ".length();

    private String disposition;
    private String type;
    private final List<String> headers;
    private final byte[] data;
    private final int start;
    private final int middle;
    private final int end;

    public FormPart(final byte[] data, final int start, final int end) throws IOException {
        middle = FormParsing.indexOf(data, CRLF_CRLF, start, end);
        if (middle == -1) throw new IOException("Malformed form, no CRLFCRLF between boundaries");

        headers = new ArrayList<>();
        final String header = new String(data, start, middle - start);
        try (final var reader = new BufferedReader(new StringReader(header))) {
            String line; while ((line = reader.readLine()) != null) {
                if (line.isEmpty()) continue;
                headers.add(line);
                if (line.startsWith("Content-Type: "))
                    type = line.substring(LENGTH_CONTENT_TYPE);
                if (line.startsWith("Content-Disposition: "))
                    disposition = line.substring(LENGTH_CONTENT_DISPOSITION);
            }
        }

        this.data = data;
        this.start = start;
        this.end = end;
    }

    public String getContentType() {
        return type;
    }
    public String getDisposition() {
        return disposition;
    }
    public List<String> getHeaders() {
        return headers;
    }

    public byte[] getFormPartData() {
        final byte[] raw = new byte[end - start];
        System.arraycopy(data, start, raw, 0, raw.length);
        return raw;
    }
    public byte[] getData() {
        final byte[] raw = new byte[end - middle];
        System.arraycopy(data, middle, raw, 0, raw.length);
        return raw;
    }

}
