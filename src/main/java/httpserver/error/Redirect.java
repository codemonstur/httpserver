package httpserver.error;

import httpserver.core.HttpServerExchange;

import java.io.IOException;

import static httpserver.core.Headers.LOCATION;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.FOUND;

/**
 * The {@code Redirect} class.
 */
public final class Redirect extends IOException implements HttpError {

    /**
     * The redirect target included in the {@code Location} header.
     */
    private final String header;
    /**
     * Creates a new instance.
     *
     * @param header the header
     */
    public Redirect(final String header) {
        this.header = header;
    }

    /**
     * Performs the {@code processExchange} operation.
     *
     * @param exchange the exchange
     */
    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respond(exchange).status(FOUND).header(LOCATION, header).send();
    }

}
