package httpserver.core;

/**
 * The {@code ExchangeCompleteListener} interface.
 */
public interface ExchangeCompleteListener {

    /**
     * Performs the {@code onExchangeComplete} operation.
     *
     * @param exception the exception
     * @param exchange the exchange
     */
    void onExchangeComplete(Exception exception, HttpServerExchange exchange);

}
