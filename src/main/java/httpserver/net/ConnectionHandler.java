package httpserver.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ConnectionHandler {
    void handleInput(InputStream in, OutputStream out) throws IOException;
}
