package httpserver.error;

import httpserver.core.HttpServerExchange;

import static httpserver.core.StatusCode.BAD_REQUEST;
import static httpserver.error.HttpError.respondError;

public final class InvalidInput extends Exception implements HttpError {

    private final int errorCode;
    public InvalidInput(final String message) {
        this(BAD_REQUEST, message, null);
    }
    public InvalidInput(final String message, final Throwable cause) {
        this(BAD_REQUEST, message, cause);
    }
    public InvalidInput(final int errorCode, final String message) {
        this(errorCode, message, null);
    }
    public InvalidInput(final int errorCode, final String message, final Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respondError(exchange, BAD_REQUEST, errorCode, getMessage());
    }

}