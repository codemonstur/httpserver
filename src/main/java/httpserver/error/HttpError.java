package httpserver.error;

import httpserver.core.HttpServerExchange;

import static httpserver.core.ContentType.application_json;
import static httpserver.core.Headers.CONTENT_TYPE;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.util.Encoding.escapeJson;
import static java.nio.charset.StandardCharsets.UTF_8;

public interface HttpError {
    void processExchange(HttpServerExchange exchange);

    String RESPONSE_INTERNAL_ERROR = "{\"success\":false,\"code\":500,\"message\":\"Internal Error\"}";
    String ERROR_MESSAGE = "{\"success\":false,\"code\":%d,\"errorCode\":%d,\"message\":\"%s\"}";

    public static void respondInternalError(final HttpServerExchange exchange) {
        if (!exchange.isResponseSent()) {
            exchange.setStatusCode(INTERNAL_SERVER_ERROR);
            exchange.setResponseHeader(CONTENT_TYPE, application_json.toString());
            exchange.send(RESPONSE_INTERNAL_ERROR, UTF_8);
        }
    }
    public static void respondError(final HttpServerExchange exchange, final int statusCode, final int errorCode, final String message) {
        if (!exchange.isResponseSent()) {
            exchange.setStatusCode(statusCode);
            exchange.setResponseHeader(CONTENT_TYPE, application_json.toString());
            exchange.send(errorMessage(statusCode, errorCode, message), UTF_8);
        }
    }

    private static String errorMessage(final int statusCode, final int errorCode, final String message) {
        return String.format(ERROR_MESSAGE, statusCode, errorCode, escapeJson(message));
    }

}