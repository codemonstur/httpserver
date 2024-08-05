package httpserver.handlers;

import httpserver.core.HttpServerExchange;

public interface SessionHttpHandler<T> {
    void handleRequest(T session, HttpServerExchange exchange) throws Exception;
}