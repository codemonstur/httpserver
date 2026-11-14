package httpserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The {@code ResponseBody} interface.
 */
public interface ResponseBody {

    /**
     * Returns the length.
     *
     * @return the result
     */
    long getLength();
    /**
     * Performs the {@code writeTo} operation.
     *
     * @param out the out
     * @throws IOException if the operation fails
     */
    void writeTo(OutputStream out) throws IOException;

    /**
     * The {@code EMPTY_BODY} field.
     */
    ResponseBody EMPTY_BODY = new ResponseBody() {
        public long getLength() {
            return 0;
        }
        public void writeTo(OutputStream out) {}
    };

    /**
     * Returns the result of {@code newByteArrayBody}.
     *
     * @param data the data
     * @return the result
     */
    public static ResponseBody newByteArrayBody(final byte[] data) {
        return newByteArrayBody(data, 0, data.length);
    }
    /**
     * Returns the result of {@code newByteArrayBody}.
     *
     * @param data the data
     * @param offset the offset
     * @param length the length
     * @return the result
     */
    public static ResponseBody newByteArrayBody(final byte[] data, final int offset, final int length) {
        return new ResponseBody() {
            public long getLength() {
                return length;
            }
            public void writeTo(OutputStream out) throws IOException {
                out.write(data, offset, length);
            }
        };
    }

    /**
     * The length of a body that is not known before it has been written. Framed as chunked
     * transfer encoding for HTTP/1.1, and delimited by connection close for older clients.
     */
    long UNKNOWN_LENGTH = -1;

    /**
     * A body streamed without knowing its size up front, which is the case where chunked
     * transfer encoding earns its keep: no stat before the read, and no race if the file
     * changes between the two.
     
     * @param in the in
     * @return the result
     */
    public static ResponseBody newInputStreamBody(final InputStream in) {
        return new ResponseBody() {
            public long getLength() {
                return UNKNOWN_LENGTH;
            }
            public void writeTo(final OutputStream out) throws IOException {
                // Stream through a larger buffer than InputStream.transferTo's default 8 KiB
                // to cut the number of chunk boundaries and write calls on the hot path.
                final byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
            }
        };
    }

    /**
     * Returns the result of {@code newInputStreamBody}.
     *
     * @param in the in
     * @param length the length
     * @return the result
     */
    public static ResponseBody newInputStreamBody(final InputStream in, final long length) {
        return new ResponseBody() {
            public long getLength() {
                return length;
            }
            public void writeTo(final OutputStream out) throws IOException {
                long leftToWrite = length;
                final byte[] data = new byte[64 * 1024];
                int read; while (leftToWrite > 0 && (read = in.read(data, 0, (int) Math.min(data.length, leftToWrite))) != -1) {
                    out.write(data, 0, read);
                    leftToWrite -= read;
                }
            }
        };
    }

}
