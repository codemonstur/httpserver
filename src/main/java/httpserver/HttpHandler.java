package httpserver;

import httpserver.model.HttpServerExchange;

public interface HttpHandler {

    void handle(HttpServerExchange exchange) throws Exception;

}
