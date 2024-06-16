package httpserver.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public interface ConnectionHandler {
    void handleInput(Socket socket, InputStream in, OutputStream out) throws IOException;
}
