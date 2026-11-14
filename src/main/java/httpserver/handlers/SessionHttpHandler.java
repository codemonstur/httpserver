package httpserver.handlers;

import httpserver.core.HttpServerExchange;

/**
 * The {@code SessionHttpHandler} interface.
 *
 * @param <T> the type parameter
 */
public interface SessionHttpHandler<T> {
    /**
     * Performs the {@code handleRequest} operation.
     *
     * @param session the session
     * @param exchange the exchange
     * @throws Exception if the operation fails
     */
    void handleRequest(T session, HttpServerExchange exchange) throws Exception;
}