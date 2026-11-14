package httpserver.websocket;

import java.io.IOException;

/**
 * A protocol violation detected while reading a frame: carries the close code that must be
 * reported to the peer before the connection goes down.
 */
public final class WebSocketException extends IOException {

    /**
     * The {@code closeCode} field.
     */
    public final int closeCode;

    /**
     * Creates a new instance.
     *
     * @param closeCode the close code
     * @param message the message
     */
    public WebSocketException(final int closeCode, final String message) {
        super(message);
        this.closeCode = closeCode;
    }

}
