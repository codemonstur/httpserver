package httpserver.core;

import java.io.IOException;
import java.io.InputStream;

public class HttpInputStream extends InputStream {

    private final InputStream in;
    private long toRead;

    public HttpInputStream(final InputStream in, final long length) {
        this.in = in;
        this.toRead = length;
    }

    @Override
    public int read() throws IOException {
        if (toRead <= 0) return -1;
        toRead--;
        return in.read();
    }

}
