package httpserver.core;

public interface ContentType {

    static ContentType newContentType(final String type) {
        return new ContentType() {
            public String toString() {
                return type;
            }
        };
    }

    ContentType
        text_html = newContentType("text/html; charset=UTF-8"),
        application_json = newContentType("application/json");
}