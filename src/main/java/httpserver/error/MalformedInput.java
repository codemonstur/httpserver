package httpserver.error;

import httpserver.core.HttpServerExchange;

import static httpserver.core.StatusCode.BAD_REQUEST;
import static httpserver.error.HttpError.respondError;

/**
 * Syntactically invalid request data: a bad percent escape in the query string, a bogus
 * Content-Length, and so on.
 *
 * This exception is deliberately unchecked so that parsing helpers can throw it without
 * widening their signatures, and it implements HttpError so that the request pipeline
 * answers with a 400 instead of a 500 Internal Server Error.
 */
public final class MalformedInput extends IllegalArgumentException implements HttpError {

    /**
     * Creates a new instance.
     *
     * @param message the message
     */
    public MalformedInput(final String message) {
        super(message);
    }
    /**
     * Creates a new instance.
     *
     * @param message the message
     * @param cause the cause
     */
    public MalformedInput(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Performs the {@code processExchange} operation.
     *
     * @param exchange the exchange
     */
    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respondError(exchange, BAD_REQUEST, BAD_REQUEST, getMessage());
    }

}
