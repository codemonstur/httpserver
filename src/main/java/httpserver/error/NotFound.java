package httpserver.error;

import httpserver.core.HttpServerExchange;

import java.io.IOException;

import static httpserver.core.StatusCode.NOT_FOUND;

public final class NotFound extends IOException implements HttpError {

    public NotFound() {}
    public NotFound(final String message) {
        super(message);
    }

    @Override
    public void processExchange(final HttpServerExchange exchange) {
        exchange.setStatusCode(NOT_FOUND);
    }

}
