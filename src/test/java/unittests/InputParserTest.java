package unittests;

import httpserver.error.InvalidInput;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static httpserver.core.InputParser.getBoundedInteger;
import static httpserver.core.InputParser.getBoundedLong;
import static httpserver.core.InputParser.getMandatoryBoolean;
import static httpserver.core.InputParser.getMandatoryDouble;
import static httpserver.core.InputParser.getMandatoryEmailAddress;
import static httpserver.core.InputParser.getMandatoryEnum;
import static httpserver.core.InputParser.getMandatoryInteger;
import static httpserver.core.InputParser.getMandatoryLong;
import static httpserver.core.InputParser.getMandatoryString;
import static httpserver.core.InputParser.getOptionalBoolean;
import static httpserver.core.InputParser.getOptionalDouble;
import static httpserver.core.InputParser.getOptionalEnum;
import static httpserver.core.InputParser.getOptionalInteger;
import static httpserver.core.InputParser.getOptionalLong;
import static httpserver.core.InputParser.getOptionalString;
import static org.junit.jupiter.api.Assertions.*;

public class InputParserTest {

    private enum Color { RED, GREEN, BLUE }

    private static Map<String, String> form(final String... pairs) {
        final var map = new HashMap<String, String>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    @Test
    public void testGetMandatoryString() throws Exception {
        assertEquals("value", getMandatoryString(form("param", "value"), "param"));
        try {
            getMandatoryString(form(), "param");
            fail("Expected InvalidInput for a missing parameter");
        } catch (final InvalidInput e) {
            assertEquals("Missing parameter 'param'", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryInteger() throws Exception {
        assertEquals(42, getMandatoryInteger(form("param", "42"), "param"));
        assertEquals(-42, getMandatoryInteger(form("param", "-42"), "param"));
        try {
            getMandatoryInteger(form("param", "abc"), "param");
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain an integer", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryLong() throws Exception {
        assertEquals(9_000_000_000L, getMandatoryLong(form("param", "9000000000"), "param"));
        try {
            getMandatoryLong(form("param", "1.5"), "param");
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain a long", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryDouble() throws Exception {
        assertEquals(3.5, getMandatoryDouble(form("param", "3.5"), "param"), 0.0);
        try {
            getMandatoryDouble(form("param", "abc"), "param");
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain a double", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryBoolean() throws Exception {
        assertTrue(getMandatoryBoolean(form("param", "true"), "param"));
        assertTrue(getMandatoryBoolean(form("param", "TRUE"), "param"));
        assertFalse(getMandatoryBoolean(form("param", "false"), "param"));
        assertFalse(getMandatoryBoolean(form("param", "False"), "param"));
        try {
            getMandatoryBoolean(form("param", "yes"), "param");
            fail("Expected InvalidInput for a non-boolean parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain a boolean", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryEmailAddress() throws Exception {
        assertEquals("john.doe@example.com", getMandatoryEmailAddress(form("email", "john.doe@example.com"), "email"));
        assertEquals("john.doe+tag@sub.example.com",
                getMandatoryEmailAddress(form("email", "john.doe+tag@sub.example.com"), "email"));
        try {
            getMandatoryEmailAddress(form("email", "john.doe"), "email");
            fail("Expected InvalidInput for an invalid email address");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'email' does not contain a valid email address", e.getMessage());
        }
    }

    @Test
    public void testGetMandatoryEnum() throws Exception {
        assertEquals(Color.GREEN, getMandatoryEnum(form("color", "GREEN"), Color.class, "color"));
        try {
            getMandatoryEnum(form("color", "PURPLE"), Color.class, "color");
            fail("Expected InvalidInput for an unknown enum value");
        } catch (final InvalidInput e) {
            assertEquals("Invalid enum 'PURPLE', possible values are: RED, GREEN, BLUE", e.getMessage());
        }
    }

    @Test
    public void testGetOptionalString() throws Exception {
        assertEquals("value", getOptionalString(form("param", "value"), "param", "default"));
        assertEquals("default", getOptionalString(form(), "param", "default"));
    }

    @Test
    public void testGetOptionalInteger() throws Exception {
        assertEquals(Integer.valueOf(42), getOptionalInteger(form("param", "42"), "param", 7));
        assertEquals(Integer.valueOf(7), getOptionalInteger(form(), "param", 7));
        try {
            getOptionalInteger(form("param", "abc"), "param", 7);
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain an integer", e.getMessage());
        }
    }

    @Test
    public void testGetOptionalLong() throws Exception {
        assertEquals(Long.valueOf(9_000_000_000L), getOptionalLong(form("param", "9000000000"), "param", 7L));
        assertEquals(Long.valueOf(7L), getOptionalLong(form(), "param", 7L));
        try {
            getOptionalLong(form("param", "abc"), "param", 7L);
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain an integer", e.getMessage());
        }
    }

    @Test
    public void testGetOptionalDouble() throws Exception {
        assertEquals(Double.valueOf(3.5), getOptionalDouble(form("param", "3.5"), "param", 7.0));
        assertEquals(Double.valueOf(7.0), getOptionalDouble(form(), "param", 7.0));
        try {
            getOptionalDouble(form("param", "abc"), "param", 7.0);
            fail("Expected InvalidInput for a non-numeric parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain an integer", e.getMessage());
        }
    }

    @Test
    public void testGetOptionalBoolean() throws Exception {
        assertTrue(getOptionalBoolean(form("param", "true"), "param", false));
        assertTrue(getOptionalBoolean(form(), "param", true));
        assertFalse(getOptionalBoolean(form("param", "false"), "param", true));
        try {
            getOptionalBoolean(form("param", "yes"), "param", true);
            fail("Expected InvalidInput for a non-boolean parameter");
        } catch (final InvalidInput e) {
            assertEquals("Parameter 'param' must contain a boolean", e.getMessage());
        }
    }

    @Test
    public void testGetOptionalEnum() throws Exception {
        assertEquals(Color.BLUE, getOptionalEnum(form("color", "BLUE"), Color.class, "color", Color.RED));
        assertEquals(Color.RED, getOptionalEnum(form(), Color.class, "color", Color.RED));
        assertEquals(Color.RED, getOptionalEnum(form("color", ""), Color.class, "color", Color.RED));
        try {
            getOptionalEnum(form("color", "PURPLE"), Color.class, "color", Color.RED);
            fail("Expected InvalidInput for an unknown enum value");
        } catch (final InvalidInput e) {
            assertEquals("Invalid enum 'PURPLE', possible values are: RED, GREEN, BLUE", e.getMessage());
        }
    }

    @Test
    public void testGetBoundedInteger() throws Exception {
        assertEquals(7, getBoundedInteger(form("param", "7"), "param", 1, 5, 10));
        assertEquals(5, getBoundedInteger(form(), "param", 1, 5, 10));
        assertEquals(1, getBoundedInteger(form("param", "-100"), "param", 1, 5, 10));
        assertEquals(10, getBoundedInteger(form("param", "100"), "param", 1, 5, 10));
    }

    @Test
    public void testGetBoundedLong() throws Exception {
        assertEquals(7L, getBoundedLong(form("param", "7"), "param", 1L, 5L, 10L));
        assertEquals(5L, getBoundedLong(form(), "param", 1L, 5L, 10L));
        assertEquals(1L, getBoundedLong(form("param", "-100"), "param", 1L, 5L, 10L));
        assertEquals(10L, getBoundedLong(form("param", "100"), "param", 1L, 5L, 10L));
    }

}
