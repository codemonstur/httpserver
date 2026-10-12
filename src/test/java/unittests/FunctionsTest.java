package unittests;

import org.junit.Test;

import static httpserver.util.Functions.isNullOrEmpty;
import static httpserver.util.Functions.orDefault;
import static httpserver.util.Functions.rangeBound;
import static httpserver.util.Functions.requireNotNull;
import static httpserver.util.Functions.requireTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class FunctionsTest {

    @Test
    public void testIsNullOrEmpty() {
        assertTrue(isNullOrEmpty(null));
        assertTrue(isNullOrEmpty(""));
        assertFalse(isNullOrEmpty(" "));
        assertFalse(isNullOrEmpty("value"));
    }

    @Test
    public void testOrDefault() {
        assertEquals("default", orDefault(null, "default"));
        assertEquals("default", orDefault("", "default"));
        assertEquals("value", orDefault("value", "default"));
    }

    @Test
    public void testRequireNotNull() {
        assertEquals("value", requireNotNull("value", "must not be null"));
        try {
            requireNotNull(null, "must not be null");
            fail("Expected IllegalArgumentException for a null input");
        } catch (final IllegalArgumentException e) {
            assertEquals("must not be null", e.getMessage());
        }
    }

    @Test
    public void testRequireTrue() {
        assertEquals("value", requireTrue(true, "value", "must be valid"));
        try {
            requireTrue(false, "value", "must be valid");
            fail("Expected IllegalArgumentException for an invalid input");
        } catch (final IllegalArgumentException e) {
            assertEquals("must be valid", e.getMessage());
        }
    }

    @Test
    public void testRangeBoundInt() {
        assertEquals(5, rangeBound(1, 10, 5));
        assertEquals(1, rangeBound(1, 10, 1));
        assertEquals(10, rangeBound(1, 10, 10));
        assertEquals(1, rangeBound(1, 10, 0));
        assertEquals(1, rangeBound(1, 10, -100));
        assertEquals(10, rangeBound(1, 10, 11));
        assertEquals(10, rangeBound(1, 10, 100));
    }

    @Test
    public void testRangeBoundLong() {
        assertEquals(5L, rangeBound(1L, 10L, 5L));
        assertEquals(1L, rangeBound(1L, 10L, 0L));
        assertEquals(10L, rangeBound(1L, 10L, 11L));
    }

}
