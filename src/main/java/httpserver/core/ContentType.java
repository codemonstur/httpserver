package httpserver.core;

import java.util.Map;

import static java.util.Map.entry;
import static java.util.Map.ofEntries;

/**
 * The {@code ContentType} interface.
 */
public interface ContentType {

    /**
     * Returns the result of {@code newContentType}.
     *
     * @param type the type
     * @return the result
     */
    static ContentType newContentType(final String type) {
        return new ContentType() {
            public String toString() {
                return type;
            }
        };
    }

    /**
     * The {@code application_pdf} field.
     */
    /**
     * The {@code application_json} field.
     */
    /**
     * The {@code text_html} field.
     */
    /**
     * The {@code text_plain} field.
     */
    ContentType
        text_plain = newContentType("text/plain; charset=UTF-8"),
        text_html = newContentType("text/html; charset=UTF-8"),
        application_json = newContentType("application/json"),
        application_pdf = newContentType("application/pdf");

    /**
     * The {@code CONTENT_TYPE_MAP} field.
     */
    Map<String, String> CONTENT_TYPE_MAP = ofEntries(
            entry(".ico", "image/x-icon"),
            entry(".gif", "image/gif"),
            entry(".jpg", "image/jpeg"),
            entry(".jpeg", "image/jpeg"),
            entry(".png", "image/png"),
            entry(".svg", "image/svg+xml"),
            entry(".htm", "text/html"),
            entry(".html", "text/html"),
            entry(".json", "application/json"),
            entry(".pdf", "application/pdf")
    );

    /**
     * Returns the result of {@code toContentType}.
     *
     * @param filename the filename
     * @return the result
     */
    static String toContentType(final String filename) {
        final var offset = filename.lastIndexOf('.');
        if (offset == -1) return "application/octet-stream";
        final var extension = filename.substring(offset);
        return CONTENT_TYPE_MAP.getOrDefault(extension, "application/octet-stream");
    }

}