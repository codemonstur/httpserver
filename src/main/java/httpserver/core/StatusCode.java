package httpserver.core;

import java.util.Map;

import static java.util.Map.entry;

public enum StatusCode {;

    public static final int
        CONTINUE = 101,
        OK = 200,
        CREATED = 201,
        ACCEPTED = 202,
        NO_CONTENT = 204,
        MOVED_PERMANENTLY = 301,
        FOUND = 302,
        SEE_OTHER = 303,
        NOT_MODIFIED = 304,
        BAD_REQUEST = 400,
        UNAUTHORIZED = 401,
        PAYMENT_REQUIRED = 402,
        FORBIDDEN = 403,
        NOT_FOUND = 404,
        METHOD_NOT_ALLOWED = 405,
        GONE = 410,
        TOO_MANY_REQUESTS = 429,
        INTERNAL_SERVER_ERROR = 500,
        BAD_GATEWAY = 502;

    public static final String
        MESSAGE_CONTINUE = "Continue",
        MESSAGE_OK = "Ok",
        MESSAGE_CREATED = "Created",
        MESSAGE_ACCEPTED = "Acceptd",
        MESSAGE_NO_CONTENT = "No Content",
        MESSAGE_MOVED_PERMANENTLY = "Moved Permanently",
        MESSAGE_FOUND = "Found",
        MESSAGE_SEE_OTHER = "See Other",
        MESSAGE_NOT_MODIFIED = "Not Modified",
        MESSAGE_BAD_REQUEST = "Bad Request",
        MESSAGE_UNAUTHORIZED = "Unauthorized",
        MESSAGE_PAYMENT_REQUIRED = "Payment Required",
        MESSAGE_FORBIDDEN = "Forbidden",
        MESSAGE_NOT_FOUND = "Not Found",
        MESSAGE_METHOD_NOT_ALLOWED = "Method Not Allowed",
        MESSAGE_GONE = "Gone",
        MESSAGE_TOO_MANY_REQUESTS = "Too Many Requests",
        MESSAGE_INTERNAL_SERVER_ERROR = "Internal Server Error",
        MESSAGE_BAD_GATEWAY = "Bad Gateway";

    private static final Map<Integer, String> codeToMessage = Map.ofEntries(
        entry(CONTINUE, MESSAGE_CONTINUE),
        entry(OK, MESSAGE_OK),
        entry(CREATED, MESSAGE_CREATED),
        entry(ACCEPTED, MESSAGE_ACCEPTED),
        entry(NO_CONTENT, MESSAGE_NO_CONTENT),
        entry(MOVED_PERMANENTLY, MESSAGE_MOVED_PERMANENTLY),
        entry(FOUND, MESSAGE_FOUND),
        entry(SEE_OTHER, MESSAGE_SEE_OTHER),
        entry(NOT_MODIFIED, MESSAGE_NOT_MODIFIED),
        entry(BAD_REQUEST, MESSAGE_BAD_REQUEST),
        entry(UNAUTHORIZED, MESSAGE_UNAUTHORIZED),
        entry(PAYMENT_REQUIRED, MESSAGE_PAYMENT_REQUIRED),
        entry(FORBIDDEN, MESSAGE_FORBIDDEN),
        entry(NOT_FOUND, MESSAGE_NOT_FOUND),
        entry(METHOD_NOT_ALLOWED, MESSAGE_METHOD_NOT_ALLOWED),
        entry(GONE, MESSAGE_GONE),
        entry(TOO_MANY_REQUESTS, MESSAGE_TOO_MANY_REQUESTS),
        entry(INTERNAL_SERVER_ERROR, MESSAGE_INTERNAL_SERVER_ERROR),
        entry(BAD_GATEWAY, MESSAGE_BAD_GATEWAY)
    );
    public static String getStatusMessage(final int statusCode, final String statusMessage) {
        if (statusMessage != null) return statusMessage;
        return codeToMessage.getOrDefault(statusCode, "Not Implemented");
    }

}
