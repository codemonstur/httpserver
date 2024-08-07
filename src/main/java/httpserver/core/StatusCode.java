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
        RESET_CONTENT = 205,
        PARTIAL_CONTENT = 206,
        MOVED_PERMANENTLY = 301,
        FOUND = 302,
        SEE_OTHER = 303,
        NOT_MODIFIED = 304,
        USE_PROXY = 305,
        TEMPORARY_REDIRECT = 307,
        BAD_REQUEST = 400,
        UNAUTHORIZED = 401,
        PAYMENT_REQUIRED = 402,
        FORBIDDEN = 403,
        NOT_FOUND = 404,
        METHOD_NOT_ALLOWED = 405,
        NOT_ACCEPTABLE = 406,
        PROXY_AUTHENTICATION_REQUIRED = 407,
        REQUEST_TIMEOUT = 408,
        CONFLICT = 409,
        GONE = 410,
        LENGTH_REQUIRED = 411,
        PRECONDITION_FAILED = 412,
        REQUEST_ENTITY_TOO_LARGE = 413,
        REQUEST_URI_TOO_LONG = 414,
        UNSUPPORTED_MEDIA_TYPE = 415,
        REQUESTED_RANGE_NOT_SATISFIABLE = 416,
        EXPECTATION_FAILED = 417,
        PRECONDITION_REQUIRED = 428,
        TOO_MANY_REQUESTS = 429,
        REQUEST_HEADER_FIELDS_TOO_LARGE = 431,
        INTERNAL_SERVER_ERROR = 500,
        NOT_IMPLEMENTED = 501,
        BAD_GATEWAY = 502,
        SERVICE_UNAVAILABLE = 503,
        GATEWAY_TIMEOUT = 504,
        HTTP_VERSION_NOT_SUPPORTED = 505,
        NETWORK_AUTHENTICATION_REQUIRED = 511;

    public static final String
        MESSAGE_CONTINUE = "Continue",
        MESSAGE_OK = "Ok",
        MESSAGE_CREATED = "Created",
        MESSAGE_ACCEPTED = "Acceptd",
        MESSAGE_NO_CONTENT = "No Content",
        MESSAGE_RESET_CONTENT = "Reset Content",
        MESSAGE_PARTIAL_CONTENT = "Partial Content",
        MESSAGE_MOVED_PERMANENTLY = "Moved Permanently",
        MESSAGE_FOUND = "Found",
        MESSAGE_SEE_OTHER = "See Other",
        MESSAGE_NOT_MODIFIED = "Not Modified",
        MESSAGE_USE_PROXY = "Use Proxy",
        MESSAGE_TEMPORARY_REDIRECT = "Temporary Redirect",
        MESSAGE_BAD_REQUEST = "Bad Request",
        MESSAGE_UNAUTHORIZED = "Unauthorized",
        MESSAGE_PAYMENT_REQUIRED = "Payment Required",
        MESSAGE_FORBIDDEN = "Forbidden",
        MESSAGE_NOT_FOUND = "Not Found",
        MESSAGE_METHOD_NOT_ALLOWED = "Method Not Allowed",
        MESSAGE_NOT_ACCEPTABLE = "Not Acceptable",
        MESSAGE_PROXY_AUTHENTICATION_REQUIRED = "Proxy Authentication Required",
        MESSAGE_REQUEST_TIMEOUT = "Request Timeout",
        MESSAGE_CONFLICT = "Conflict",
        MESSAGE_GONE = "Gone",
        MESSAGE_LENGTH_REQUIRED = "Length Required",
        MESSAGE_PRECONDITION_FAILED = "Precondition Failed",
        MESSAGE_REQUEST_ENTITY_TOO_LARGE = "Request Entity Too Large",
        MESSAGE_REQUEST_URI_TOO_LONG = "Request-URI Too Long",
        MESSAGE_UNSUPPORTED_MEDIA_TYPE = "Unsupported Media Type",
        MESSAGE_REQUESTED_RANGE_NOT_SATISFIABLE = "Requested Range Not Satisfiable",
        MESSAGE_EXPECTATION_FAILED = "Expectation Failed",
        MESSAGE_PRECONDITION_REQUIRED = "Precondition Required",
        MESSAGE_TOO_MANY_REQUESTS = "Too Many Requests",
        MESSAGE_REQUEST_HEADER_FIELDS_TOO_LARGE = "Request Header Fields Too Large",
        MESSAGE_INTERNAL_SERVER_ERROR = "Internal Server Error",
        MESSAGE_NOT_IMPLEMENTED = "Not Implemented",
        MESSAGE_BAD_GATEWAY = "Bad Gateway",
        MESSAGE_SERVICE_UNAVAILABLE = "Service Unavailable",
        MESSAGE_GATEWAY_TIMEOUT = "Gateway Timeout",
        MESSAGE_HTTP_VERSION_NOT_SUPPORTED = "HTTP Version Not Supported",
        MESSAGE_NETWORK_AUTHENTICATION_REQUIRED = "Network Authentication Required";

    private static final Map<Integer, String> codeToMessage = Map.ofEntries(
        entry(CONTINUE, MESSAGE_CONTINUE),
        entry(OK, MESSAGE_OK),
        entry(CREATED, MESSAGE_CREATED),
        entry(ACCEPTED, MESSAGE_ACCEPTED),
        entry(NO_CONTENT, MESSAGE_NO_CONTENT),
        entry(RESET_CONTENT, MESSAGE_RESET_CONTENT),
        entry(PARTIAL_CONTENT, MESSAGE_PARTIAL_CONTENT),
        entry(MOVED_PERMANENTLY, MESSAGE_MOVED_PERMANENTLY),
        entry(FOUND, MESSAGE_FOUND),
        entry(SEE_OTHER, MESSAGE_SEE_OTHER),
        entry(NOT_MODIFIED, MESSAGE_NOT_MODIFIED),
        entry(USE_PROXY, MESSAGE_USE_PROXY),
        entry(TEMPORARY_REDIRECT, MESSAGE_TEMPORARY_REDIRECT),
        entry(BAD_REQUEST, MESSAGE_BAD_REQUEST),
        entry(UNAUTHORIZED, MESSAGE_UNAUTHORIZED),
        entry(PAYMENT_REQUIRED, MESSAGE_PAYMENT_REQUIRED),
        entry(FORBIDDEN, MESSAGE_FORBIDDEN),
        entry(NOT_FOUND, MESSAGE_NOT_FOUND),
        entry(METHOD_NOT_ALLOWED, MESSAGE_METHOD_NOT_ALLOWED),
        entry(NOT_ACCEPTABLE, MESSAGE_NOT_ACCEPTABLE),
        entry(PROXY_AUTHENTICATION_REQUIRED, MESSAGE_PROXY_AUTHENTICATION_REQUIRED),
        entry(REQUEST_TIMEOUT, MESSAGE_REQUEST_TIMEOUT),
        entry(CONFLICT, MESSAGE_CONFLICT),
        entry(GONE, MESSAGE_GONE),
        entry(LENGTH_REQUIRED, MESSAGE_LENGTH_REQUIRED),
        entry(PRECONDITION_FAILED, MESSAGE_PRECONDITION_FAILED),
        entry(REQUEST_ENTITY_TOO_LARGE, MESSAGE_REQUEST_ENTITY_TOO_LARGE),
        entry(REQUEST_URI_TOO_LONG, MESSAGE_REQUEST_URI_TOO_LONG),
        entry(UNSUPPORTED_MEDIA_TYPE, MESSAGE_UNSUPPORTED_MEDIA_TYPE),
        entry(REQUESTED_RANGE_NOT_SATISFIABLE, MESSAGE_REQUESTED_RANGE_NOT_SATISFIABLE),
        entry(EXPECTATION_FAILED, MESSAGE_EXPECTATION_FAILED),
        entry(PRECONDITION_REQUIRED, MESSAGE_PRECONDITION_REQUIRED),
        entry(TOO_MANY_REQUESTS, MESSAGE_TOO_MANY_REQUESTS),
        entry(REQUEST_HEADER_FIELDS_TOO_LARGE, MESSAGE_REQUEST_HEADER_FIELDS_TOO_LARGE),
        entry(INTERNAL_SERVER_ERROR, MESSAGE_INTERNAL_SERVER_ERROR),
        entry(NOT_IMPLEMENTED, MESSAGE_NOT_IMPLEMENTED),
        entry(BAD_GATEWAY, MESSAGE_BAD_GATEWAY),
        entry(SERVICE_UNAVAILABLE, MESSAGE_SERVICE_UNAVAILABLE),
        entry(GATEWAY_TIMEOUT, MESSAGE_GATEWAY_TIMEOUT),
        entry(HTTP_VERSION_NOT_SUPPORTED, MESSAGE_HTTP_VERSION_NOT_SUPPORTED),
        entry(NETWORK_AUTHENTICATION_REQUIRED, MESSAGE_NETWORK_AUTHENTICATION_REQUIRED)
    );
    public static String getMessageForCode(final int statusCode, final String statusMessage) {
        if (statusMessage != null) return statusMessage;
        return codeToMessage.getOrDefault(statusCode, "Not Implemented");
    }

}
