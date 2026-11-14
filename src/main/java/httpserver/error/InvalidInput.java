package httpserver.error;

import httpserver.core.HttpServerExchange;

import static httpserver.core.StatusCode.BAD_REQUEST;
import static httpserver.error.HttpError.respondError;

/**
 * The {@code InvalidInput} class.
 */
public final class InvalidInput extends Exception implements HttpError {

    /**
     * The application-specific error code included in the error response.
     */
    private final int errorCode;
    /**
     * Creates a new instance.
     *
     * @param message the message
     */
    public InvalidInput(final String message) {
        this(BAD_REQUEST, message, null);
    }
    /**
     * Creates a new instance.
     *
     * @param message the message
     * @param cause the cause
     */
    public InvalidInput(final String message, final Throwable cause) {
        this(BAD_REQUEST, message, cause);
    }
    /**
     * Creates a new instance.
     *
     * @param errorCode the error code
     * @param message the message
     */
    public InvalidInput(final int errorCode, final String message) {
        this(errorCode, message, null);
    }
    /**
     * Creates a new instance.
     *
     * @param errorCode the error code
     * @param message the message
     * @param cause the cause
     */
    public InvalidInput(final int errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Performs the {@code processExchange} operation.
     *
     * @param exchange the exchange
     */
    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respondError(exchange, BAD_REQUEST, errorCode, getMessage());
    }

}