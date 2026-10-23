package unittests;

import org.junit.jupiter.api.Test;

import static httpserver.core.StatusCode.ACCEPTED;
import static httpserver.core.StatusCode.CONTINUE;
import static httpserver.core.StatusCode.CREATED;
import static httpserver.core.StatusCode.INTERNAL_SERVER_ERROR;
import static httpserver.core.StatusCode.NOT_FOUND;
import static httpserver.core.StatusCode.NO_CONTENT;
import static httpserver.core.StatusCode.OK;
import static httpserver.core.StatusCode.SWITCHING_PROTOCOLS;
import static httpserver.core.StatusCode.getMessageForCode;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusCodeTest {

    @Test
    public void testInformationalCodes() {
        assertEquals(100, CONTINUE);
        assertEquals(101, SWITCHING_PROTOCOLS);
        assertEquals("Continue", getMessageForCode(CONTINUE, null));
        assertEquals("Switching Protocols", getMessageForCode(SWITCHING_PROTOCOLS, null));
    }

    @Test
    public void testAcceptedMessage() {
        assertEquals(202, ACCEPTED);
        assertEquals("Accepted", getMessageForCode(ACCEPTED, null));
    }

    @Test
    public void testGetMessageForKnownCodes() {
        assertEquals("Ok", getMessageForCode(OK, null));
        assertEquals("Created", getMessageForCode(CREATED, null));
        assertEquals("No Content", getMessageForCode(NO_CONTENT, null));
        assertEquals("Not Found", getMessageForCode(NOT_FOUND, null));
        assertEquals("Internal Server Error", getMessageForCode(INTERNAL_SERVER_ERROR, null));
    }

    @Test
    public void testGetMessageForUnknownCode() {
        assertEquals("Not Implemented", getMessageForCode(418, null));
        assertEquals("Not Implemented", getMessageForCode(999, null));
        assertEquals("Not Implemented", getMessageForCode(0, null));
    }

    @Test
    public void testCustomMessageOverridesMappedMessage() {
        assertEquals("Teapot", getMessageForCode(NOT_FOUND, "Teapot"));
        assertEquals("Custom", getMessageForCode(999, "Custom"));
    }

}
