package httpserver;

import httpserver.core.HttpServerExchange;

public interface HttpHandler {

    void handleRequest(HttpServerExchange exchange) throws Exception;

}
