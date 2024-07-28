package httpserver.error;

import httpserver.core.HttpServerExchange;

import java.io.IOException;

import static httpserver.core.StatusCode.FORBIDDEN;
import static httpserver.error.HttpError.respondError;

public final class Forbidden extends IOException implements HttpError {

    private final int errorCode;
    public Forbidden() {
        this(FORBIDDEN, "Forbidden");
    }
    public Forbidden(final String message) {
        this(FORBIDDEN, message);
    }
    public Forbidden(final int errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respondError(exchange, FORBIDDEN, errorCode, getMessage());
    }

}
