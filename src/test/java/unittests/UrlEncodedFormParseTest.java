package unittests;

import httpserver.core.HttpServerExchange;
import httpserver.error.InvalidInput;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static httpserver.core.FormParsing.parseUrlEncodedForm;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static unittests.Exchanges.newExchange;

public class UrlEncodedFormParseTest {

    private static HttpServerExchange exchangeWithForm(final String body) throws IOException {
        final var request = "POST /submit HTTP/1.1\r\n"
            + "Content-Type: application/x-www-form-urlencoded\r\n"
            + "Content-Length: " + body.getBytes(UTF_8).length + "\r\n"
            + "\r\n";
        return newExchange(request, body);
    }

    @Test
    public void testParsesUrlEncodedForm() throws IOException {
        final var exchange = exchangeWithForm("name=John+Doe&email=john%40example.com&age=42");

        final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8);

        assertEquals("John Doe", form.get("name"));
        assertEquals("john@example.com", form.get("email"));
        assertEquals("42", form.get("age"));
        assertEquals(3, form.size());
    }

    @Test
    public void testParsesParametersWithoutValues() throws IOException {
        final var exchange = exchangeWithForm("empty=&novalue");

        final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8);

        assertEquals("", form.get("empty"));
        assertEquals("", form.get("novalue"));
        assertEquals(2, form.size());
    }

    @Test
    public void testKeepsEverythingBeforeTheFirstEqualsSign() throws IOException {
        final var exchange = exchangeWithForm("data=a%3Db%26c");

        final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8);

        assertEquals("a=b&c", form.get("data"));
    }

    @Test
    public void testRejectsOtherContentTypes() throws IOException {
        final var request = "POST /submit HTTP/1.1\r\n"
            + "Content-Type: text/plain\r\n"
            + "Content-Length: 4\r\n"
            + "\r\n"
            + "data";
        final var exchange = newExchange(request, "data");

        try {
            parseUrlEncodedForm(exchange, UTF_8);
            fail("Expected IOException for a non url encoded form");
        } catch (final IOException e) {
            assertEquals("Content type no url encoded form. Found 'text/plain' instead.", e.getMessage());
        }
    }

    @Test
    public void testValidParametersAllowsKnownFields() throws IOException, InvalidInput {
        final var exchange = exchangeWithForm("name=John+Doe&age=42");

        final Map<String, String> form = parseUrlEncodedForm(exchange, UTF_8, Set.of("name", "age", "email"));

        assertEquals("John Doe", form.get("name"));
        assertEquals("42", form.get("age"));
    }

    @Test
    public void testValidParametersRejectsUnknownFields() throws IOException {
        final var exchange = exchangeWithForm("name=John+Doe&evil=1");

        try {
            parseUrlEncodedForm(exchange, UTF_8, Set.of("name"));
            fail("Expected InvalidInput for an unexpected form field");
        } catch (final InvalidInput e) {
            assertEquals("Form is not allowed to have these fields: [evil]", e.getMessage());
        }
    }

    @Test
    public void testValidParametersListsEveryUnknownFieldInOrder() throws IOException {
        final var exchange = exchangeWithForm("z=1&name=John+Doe&a=2");

        try {
            parseUrlEncodedForm(exchange, UTF_8, Set.of("name"));
            fail("Expected InvalidInput for unexpected form fields");
        } catch (final InvalidInput e) {
            assertEquals("Form is not allowed to have these fields: [a, z]", e.getMessage());
        }
    }

}
