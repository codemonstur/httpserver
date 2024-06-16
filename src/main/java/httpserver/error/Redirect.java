package httpserver.error;

import httpserver.core.HttpServerExchange;

import static httpserver.core.Headers.LOCATION;
import static httpserver.core.ResponseBuilder.respond;
import static httpserver.core.StatusCode.FOUND;

public final class Redirect extends Exception implements HttpError {

    private final String header;
    public Redirect(final String header) {
        this.header = header;
    }

    @Override
    public void processExchange(final HttpServerExchange exchange) {
        respond(exchange).status(FOUND).header(LOCATION, header).send();
    }

}
