package httpserver;

import httpserver.core.HttpServerExchange;

/**
 * The {@code HttpHandler} interface.
 */
public interface HttpHandler {

    /**
     * Performs the {@code handleRequest} operation.
     *
     * @param exchange the exchange
     * @throws Exception if the operation fails
     */
    void handleRequest(HttpServerExchange exchange) throws Exception;

}
