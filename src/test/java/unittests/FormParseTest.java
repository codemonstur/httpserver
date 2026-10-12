package unittests;

import httpserver.core.FormPart;
import org.junit.Test;

import java.io.*;
import java.util.List;

import static httpserver.core.FormParsing.indexOf;
import static httpserver.core.FormParsing.parseMultipartForm;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.*;

public class FormParseTest {

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
            final List<FormPart> formParts = parseMultipartForm(CONTENT_TYPE, FORM_DATA.length, is);
            assertEquals("Invalid form parts length", 3, formParts.size());

            verifyFormPart(formParts, 1, "r");
            verifyFormPart(formParts, 2, "g");
            verifyFormPart(formParts, 3, "b");
        }
    }

    @Test
    public void testRejectsNonMultipartContentTypes() {
        try (final var is = new ByteArrayInputStream(FORM_DATA)) {
            parseMultipartForm("application/json", FORM_DATA.length, is);
            fail("Expected IOException for a non multipart content type");
        } catch (final IOException e) {
            assertEquals("Not multipart form data", e.getMessage());
        }
    }

    @Test
    public void testRejectsOversizedForms() {
        try (final var is = new ByteArrayInputStream(FORM_DATA)) {
            parseMultipartForm(CONTENT_TYPE, 64 * 1024 + 1, is);
            fail("Expected IOException for an oversized form");
        } catch (final IOException e) {
            assertEquals("Form content too large", e.getMessage());
        }
    }

    @Test
    public void testIndexOfReturnsTheEndOffsetOfTheMatch() {
        final byte[] array = "abcdef".getBytes(US_ASCII);
        final byte[] abc = "abc".getBytes(US_ASCII);
        final byte[] cde = "cde".getBytes(US_ASCII);
        final byte[] xyz = "xyz".getBytes(US_ASCII);
        final byte[] empty = new byte[0];

        assertEquals(3, indexOf(array, abc, 0, array.length));
        assertEquals(5, indexOf(array, cde, 0, array.length));
        assertEquals(-1, indexOf(array, xyz, 0, array.length));
        assertEquals(0, indexOf(array, empty, 0, array.length));
    }

    @Test
    public void testIndexOfRespectsTheSearchRange() {
        final byte[] array = "abcabc".getBytes(US_ASCII);
        final byte[] abc = "abc".getBytes(US_ASCII);

        assertEquals(3, indexOf(array, abc, 0, array.length));
        assertEquals(-1, indexOf(array, abc, 4, array.length));
        assertEquals(-1, indexOf(array, abc, 0, 2));
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
