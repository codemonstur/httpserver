package httpserver.error;

import httpserver.core.HttpServerExchange;

import java.io.IOException;

import static httpserver.core.Headers.CONNECTION;
import static httpserver.core.StatusCode.NOT_IMPLEMENTED;
import static httpserver.error.HttpError.respondError;
import static httpserver.util.Strings.CLOSE;

/**
 * Thrown when a request uses an HTTP feature this server does not implement, such as
 * Transfer-Encoding. The response marks the connection for closing because such a request
 * cannot be framed safely, so it must not be reused for a second request.
 */
public final class NotImplemented extends IOException implements HttpError {

    /**
     * The application-specific error code included in the error response.
     */
    private final int errorCode;
    /**
     * Creates a new instance.
     *
     * @param message the message
     */
    public NotImplemented(final String message) {
        this(NOT_IMPLEMENTED, message);
    }
    /**
     * Creates a new instance.
     *
     * @param errorCode the error code
     * @param message the message
     */
    public NotImplemented(final int errorCode, final String message) {
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
        exchange.setResponseHeader(CONNECTION, CLOSE);
        respondError(exchange, NOT_IMPLEMENTED, errorCode, getMessage());
    }

}
