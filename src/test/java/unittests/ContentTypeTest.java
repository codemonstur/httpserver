package unittests;

import org.junit.jupiter.api.Test;

import static httpserver.core.ContentType.application_json;
import static httpserver.core.ContentType.newContentType;
import static httpserver.core.ContentType.text_plain;
import static httpserver.core.ContentType.toContentType;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ContentTypeTest {

    @Test
    public void testToContentTypeForKnownExtensions() {
        assertEquals("image/x-icon", toContentType("favicon.ico"));
        assertEquals("image/gif", toContentType("animation.gif"));
        assertEquals("image/jpeg", toContentType("photo.jpg"));
        assertEquals("image/jpeg", toContentType("photo.jpeg"));
        assertEquals("image/png", toContentType("screenshot.png"));
        assertEquals("image/svg+xml", toContentType("logo.svg"));
        assertEquals("text/html", toContentType("index.htm"));
        assertEquals("text/html", toContentType("index.html"));
        assertEquals("application/json", toContentType("data.json"));
        assertEquals("application/pdf", toContentType("document.pdf"));
    }

    @Test
    public void testToContentTypeForUnknownExtensions() {
        assertEquals("application/octet-stream", toContentType("archive.zip"));
        assertEquals("application/octet-stream", toContentType("page.xhtml"));
        assertEquals("application/octet-stream", toContentType("script.exe"));
    }

    @Test
    public void testToContentTypeWithoutExtension() {
        assertEquals("application/octet-stream", toContentType("Makefile"));
        assertEquals("application/octet-stream", toContentType("noextension"));
        assertEquals("application/octet-stream", toContentType("trailingdot."));
    }

    @Test
    public void testConstants() {
        assertEquals("text/plain; charset=UTF-8", text_plain.toString());
        assertEquals("application/json", application_json.toString());
        assertEquals("text/custom", newContentType("text/custom").toString());
    }

}
