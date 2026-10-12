package unittests;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * The library promises to run on Java 17, so every compiled class has to target class file
 * version 61 and must not be flagged as a preview feature. This stops the supported version
 * from creeping up unnoticed when the pom or the compiler settings change.
 */
public class ByteCodeVersionTest {

    private static final int CLASS_FILE_MAGIC = 0xCAFEBABE;
    private static final int MAJOR_VERSION_JAVA_17 = 61;
    private static final int MINOR_VERSION_NOT_PREVIEW = 0;

    private static final String[] CLASSES = {
        "httpserver/HttpServer.class",
        "httpserver/core/HttpServerExchange.class",
        "httpserver/core/FormParsing.class",
        "httpserver/handlers/Common.class",
        "httpserver/handlers/HostRouting.class",
        "httpserver/util/Encoding.class",
        "httpserver/session/TokenSessionStore.class"
    };

    @Test
    public void testClassesTargetJava17() throws IOException {
        for (final String clazz : CLASSES) {
            final byte[] header = readClassFileHeader(clazz);

            final int magic = readInt(header, 0);
            final int minor = readShort(header, 4);
            final int major = readShort(header, 6);

            assertEquals(clazz + " has a corrupted class file", CLASS_FILE_MAGIC, magic);
            assertEquals(clazz + " must not be flagged as a preview feature", MINOR_VERSION_NOT_PREVIEW, minor);
            assertEquals(clazz + " targets a class file version newer than Java 17", MAJOR_VERSION_JAVA_17, major);
        }
    }

    private static byte[] readClassFileHeader(final String clazz) throws IOException {
        try (final InputStream in = ByteCodeVersionTest.class.getClassLoader().getResourceAsStream(clazz)) {
            assertNotNull("Could not find " + clazz + " on the classpath", in);
            return in.readNBytes(8);
        }
    }

    private static int readShort(final byte[] data, final int offset) {
        return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
    }

    private static int readInt(final byte[] data, final int offset) {
        return (readShort(data, offset) << 16) | readShort(data, offset + 2);
    }

}
