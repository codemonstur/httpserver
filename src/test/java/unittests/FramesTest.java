package unittests;

import httpserver.websocket.Frames;
import httpserver.websocket.WebSocketException;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static httpserver.websocket.Frames.OP_BINARY;
import static httpserver.websocket.Frames.OP_CLOSE;
import static httpserver.websocket.Frames.OP_TEXT;
import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static unittests.ClientFrames.frame;
import static unittests.ClientFrames.text;
import static unittests.ClientFrames.unmasked;

public class FramesTest {

    @Test
    public void testWritesTextFrameExactly() throws IOException {
        final var out = new ByteArrayOutputStream();

        Frames.write(out, true, OP_TEXT, "Hi".getBytes(US_ASCII));

        assertArrayEquals(new byte[] { (byte) 0x81, 2, 'H', 'i' }, out.toByteArray());
    }

    @Test
    public void testWritesSixteenBitLengthFrame() throws IOException {
        final var out = new ByteArrayOutputStream();
        final byte[] payload = new byte[300];

        Frames.write(out, true, OP_BINARY, payload);

        final byte[] bytes = out.toByteArray();
        assertEquals(304, bytes.length);
        assertEquals((byte) 0x82, bytes[0]);
        assertEquals((byte) 126, bytes[1]);
        assertEquals(1, bytes[2]);
        assertEquals(44, bytes[3]);
    }

    @Test
    public void testWritesSixtyFourBitLengthFrame() throws IOException {
        final var out = new ByteArrayOutputStream();
        final byte[] payload = new byte[70_000];

        Frames.write(out, true, OP_BINARY, payload);

        final byte[] bytes = out.toByteArray();
        assertEquals(70_010, bytes.length);
        assertEquals((byte) 0x82, bytes[0]);
        assertEquals((byte) 127, bytes[1]);
        long length = 0;
        for (int i = 2; i < 10; i++) length = (length << 8) | (bytes[i] & 0xFF);
        assertEquals(70_000, length);
    }

    @Test
    public void testReadsMaskedTextFrame() throws IOException {
        final var frame = Frames.read(new ByteArrayInputStream(text("hello")));

        assertTrue(frame.fin);
        assertEquals(OP_TEXT, frame.opcode);
        assertArrayEquals("hello".getBytes(US_ASCII), frame.payload);
    }

    @Test
    public void testReadsMaskedSixteenAndSixtyFourBitFrames() throws IOException {
        final byte[] small = new byte[300];
        final byte[] large = new byte[70_000];
        for (int i = 0; i < large.length; i++) large[i] = (byte) i;

        final var in = new ByteArrayInputStream(concat(frame(true, OP_BINARY, small), frame(true, OP_BINARY, large)));

        assertArrayEquals(small, Frames.read(in).payload);
        assertArrayEquals(large, Frames.read(in).payload);
    }

    @Test
    public void testReturnsNullAtTheCleanEndOfTheStream() throws IOException {
        assertNull(Frames.read(new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void testRejectsUnmaskedClientFrames() {
        assertCloseCode(1002, () -> Frames.read(new ByteArrayInputStream(unmasked(true, OP_TEXT, "hi".getBytes(US_ASCII)))));
    }

    @Test
    public void testRejectsReservedBits() {
        final byte[] frame = { (byte) 0xC1, (byte) 0x80, 1, 2, 3, 4 }; // RSV1 set, masked, empty
        assertCloseCode(1002, () -> Frames.read(new ByteArrayInputStream(frame)));
    }

    @Test
    public void testRejectsUnknownOpcodes() {
        assertCloseCode(1002, () -> Frames.read(new ByteArrayInputStream(frame(true, 0x3, new byte[0]))));
        assertCloseCode(1002, () -> Frames.read(new ByteArrayInputStream(frame(true, 0xB, new byte[0]))));
    }

    @Test
    public void testRejectsFragmentedControlFrames() {
        assertCloseCode(1002, () -> Frames.read(new ByteArrayInputStream(frame(false, OP_CLOSE, new byte[0]))));
    }

    @Test
    public void testRejectsOversizedFramesBeforeReadingThem() {
        final byte[] header = { (byte) 0x82, (byte) 0xFF, 0, 0, 1, 0, 0, 0, 0, 0, 1, 2, 3, 4 }; // a TiB claimed
        assertCloseCode(1009, () -> Frames.read(new ByteArrayInputStream(header)));
    }

    @Test
    public void testRejectsTruncatedFrames() {
        final byte[] truncated = { (byte) 0x81, (byte) 0x85, 1, 2, 3, 4, 'h', 'i' }; // claims 5 bytes, has 2
        try {
            Frames.read(new ByteArrayInputStream(truncated));
            fail("Expected the truncated frame to fail");
        } catch (final IOException e) {
            assertTrue(e.getMessage().contains("middle of a frame"));
        }
    }

    private static byte[] concat(final byte[] first, final byte[] second) {
        final var out = new ByteArrayOutputStream();
        out.writeBytes(first);
        out.writeBytes(second);
        return out.toByteArray();
    }

    @FunctionalInterface
    private interface FrameRead {
        void call() throws IOException;
    }

    private static void assertCloseCode(final int expected, final FrameRead read) {
        try {
            read.call();
            fail("Expected a WebSocketException with close code " + expected);
        } catch (final WebSocketException e) {
            assertEquals(expected, e.closeCode);
        } catch (final IOException e) {
            fail("Expected a WebSocketException, got " + e);
        }
    }

}
