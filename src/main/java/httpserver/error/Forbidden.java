package httpserver.error;

import httpserver.core.HttpServerExchange;

import java.io.IOException;

import static httpserver.core.StatusCode.FORBIDDEN;
import static httpserver.error.HttpError.respondError;

/**
 * The {@code Forbidden} class.
 */
public final class Forbidden extends IOException implements HttpError {

    /**
     * The application-specific error code included in the error response.
     */
    private final int errorCode;
    /**
     * Creates a new instance.
     */
    public Forbidden() {
        this(FORBIDDEN, "Forbidden");
    }
    /**
     * Creates a new instance.
     *
     * @param message the message
     */
    public Forbidden(final String message) {
        this(FORBIDDEN, message);
    }
    /**
     * Creates a new instance.
     *
     * @param errorCode the error code
     * @param message the message
     */
    public Forbidden(final int errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Performs the {@code processExchange} operation.
     *
     * @param exchange the exchange
     */
    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respondError(exchange, FORBIDDEN, errorCode, getMessage());
    }

}
