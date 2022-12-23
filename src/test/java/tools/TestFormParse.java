package tools;

import httpserver.core.FormPart;
import httpserver.core.MultipartForm;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static httpserver.core.MultipartForm.parseForm;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class TestFormParse {

    private static final String CONTENT_TYPE =
        "multipart/form-data; boundary=2a8ae6ad-f4ad-4d9a-a92c-6d217011fe0f";
    private static final byte[] FORM_DATA = """
        \r
        --2a8ae6ad-f4ad-4d9a-a92c-6d217011fe0f\r
        Content-Disposition: form-data; name="datafile1"; filename="r.gif"\r
        Content-Type: image/gif\r
        \r
        GIF87a.............,...........D..;\r
        --2a8ae6ad-f4ad-4d9a-a92c-6d217011fe0f\r
        Content-Disposition: form-data; name="datafile2"; filename="g.gif"\r
        Content-Type: image/gif\r
        \r
        GIF87a.............,...........D..;\r
        --2a8ae6ad-f4ad-4d9a-a92c-6d217011fe0f\r
        Content-Disposition: form-data; name="datafile3"; filename="b.gif"\r
        Content-Type: image/gif\r
        \r
        GIF87a.............,...........D..;\r
        --2a8ae6ad-f4ad-4d9a-a92c-6d217011fe0f--""".getBytes(UTF_8);

    @Test
    public void testFormParsing() throws IOException {
        try (final var is = new ByteArrayInputStream(FORM_DATA)) {
            final List<FormPart> formParts = parseForm(CONTENT_TYPE, FORM_DATA.length, is);
            assertEquals("Invalid form parts length", 3, formParts.size());

            verifyFormPart(formParts, 1, "r");
            verifyFormPart(formParts, 2, "g");
            verifyFormPart(formParts, 3, "b");
        }
    }

    private static void verifyFormPart(final List<FormPart> parts, final int id, final String letter) {
        verifyFormPart(parts.get(id - 1), "form-data; name=\"datafile" + id + "\"; filename=\"" + letter + ".gif\""
                , "image/gif", 2, 35);
    }
    private static void verifyFormPart(final FormPart part, final String disposition
            , final String type, final int headerSize, final int dataSize) {
        assertEquals("Invalid Disposition", disposition, part.getDisposition());
        assertEquals("Invalid content type", type, part.getContentType());
        assertEquals("Invalid number of headers", headerSize, part.getHeaders().size());
        assertEquals("Invalid number of headers", dataSize, part.getData().length);
    }

}
