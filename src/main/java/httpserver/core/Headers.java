package httpserver.core;

import static httpserver.util.Functions.isNullOrEmpty;

public enum Headers {;

    public static final String
        AUTHORIZATION = "Authorization",
        CACHE_CONTROL = "Cache-Control",
        CONNECTION = "Connection",
        CONTENT_LENGTH = "Content-Length",
        CONTENT_TYPE = "Content-Type",
        COOKIE = "Cookie",
        CROSS_ORIGIN_OPENER_POLICY = "Cross-Origin-Opener-Policy",
        STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security",
        X_FRAME_OPTIONS = "X-Frame-Options",
        X_XSS_PROTECTION = "X-XSS-Protection",
        X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options",
        REFERRER_POLICY = "Referrer-Policy",
        DATE = "Date",
        ETAG = "ETag",
        EXPIRES = "Expires",
        HOST = "Host",
        IF_NONE_MATCH = "If-None-Match",
        LOCATION = "Location",
        SET_COOKIE = "Set-Cookie",
        USER_AGENT = "User-Agent",
        WWW_AUTHENTICATE = "WWW-Authenticate",
        ACCEPT_ENCODING = "Accept-Encoding",
        CONTENT_ENCODING = "Content-Encoding";

    public static boolean isContentHeader(final String header) {
        return !isNullOrEmpty(header) && header.startsWith("Content-");
    }

}
