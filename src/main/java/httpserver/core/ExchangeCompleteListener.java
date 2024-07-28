package httpserver.core;

public interface ExchangeCompleteListener {

    void onExchangeComplete(Exception exception, HttpServerExchange exchange);

}
