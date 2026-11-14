package httpserver.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * The {@code ConnectionHandler} interface.
 */
public interface ConnectionHandler {
    /**
     * Performs the {@code handleInput} operation.
     *
     * @param socket the socket
     * @param in the in
     * @param out the out
     * @throws IOException if the operation fails
     */
    void handleInput(Socket socket, InputStream in, OutputStream out) throws IOException;
}
