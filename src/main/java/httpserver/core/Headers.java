package httpserver.core;

import static httpserver.util.Functions.isNullOrEmpty;

/**
 * The {@code Headers} enum.
 */
public enum Headers {;

    /**
     * The {@code VARY} constant.
     */
    /**
     * The {@code UPGRADE} constant.
     */
    /**
     * The {@code TRANSFER_ENCODING} constant.
     */
    /**
     * The {@code SEC_WEBSOCKET_VERSION} constant.
     */
    /**
     * The {@code SEC_WEBSOCKET_KEY} constant.
     */
    /**
     * The {@code SEC_WEBSOCKET_ACCEPT} constant.
     */
    /**
     * The {@code PRAGMA} constant.
     */
    /**
     * The {@code CONTENT_ENCODING} constant.
     */
    /**
     * The {@code ACCEPT_ENCODING} constant.
     */
    /**
     * The {@code WWW_AUTHENTICATE} constant.
     */
    /**
     * The {@code USER_AGENT} constant.
     */
    /**
     * The {@code SET_COOKIE} constant.
     */
    /**
     * The {@code LOCATION} constant.
     */
    /**
     * The {@code IF_NONE_MATCH} constant.
     */
    /**
     * The {@code HOST} constant.
     */
    /**
     * The {@code EXPECT} constant.
     */
    /**
     * The {@code EXPIRES} constant.
     */
    /**
     * The {@code ETAG} constant.
     */
    /**
     * The {@code DATE} constant.
     */
    /**
     * The {@code REFERRER_POLICY} constant.
     */
    /**
     * The {@code X_CONTENT_TYPE_OPTIONS} constant.
     */
    /**
     * The {@code X_XSS_PROTECTION} constant.
     */
    /**
     * The {@code X_FRAME_OPTIONS} constant.
     */
    /**
     * The {@code STRICT_TRANSPORT_SECURITY} constant.
     */
    /**
     * The {@code CROSS_ORIGIN_OPENER_POLICY} constant.
     */
    /**
     * The {@code COOKIE} constant.
     */
    /**
     * The {@code CONTENT_TYPE} constant.
     */
    /**
     * The {@code CONTENT_LENGTH} constant.
     */
    /**
     * The {@code CONNECTION} constant.
     */
    /**
     * The {@code CACHE_CONTROL} constant.
     */
    /**
     * The {@code AUTHORIZATION} constant.
     */
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
        EXPECT = "Expect",
        HOST = "Host",
        IF_NONE_MATCH = "If-None-Match",
        LOCATION = "Location",
        SET_COOKIE = "Set-Cookie",
        USER_AGENT = "User-Agent",
        WWW_AUTHENTICATE = "WWW-Authenticate",
        ACCEPT_ENCODING = "Accept-Encoding",
        CONTENT_ENCODING = "Content-Encoding",
        PRAGMA = "Pragma",
        SEC_WEBSOCKET_ACCEPT = "Sec-WebSocket-Accept",
        SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key",
        SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version",
        TRANSFER_ENCODING = "Transfer-Encoding",
        UPGRADE = "Upgrade",
        VARY = "Vary";

    /**
     * Returns the result of {@code isContentHeader}.
     *
     * @param header the header
     * @return whether the condition holds
     */
    public static boolean isContentHeader(final String header) {
        return !isNullOrEmpty(header) && header.regionMatches(true, 0, "Content-", 0, "Content-".length());
    }

}
