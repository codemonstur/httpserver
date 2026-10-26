package httpserver.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ResponseBody {

    long getLength();
    void writeTo(OutputStream out) throws IOException;

    ResponseBody EMPTY_BODY = new ResponseBody() {
        public long getLength() {
            return 0;
        }
        public void writeTo(OutputStream out) {}
    };

    public static ResponseBody newByteArrayBody(final byte[] data) {
        return newByteArrayBody(data, 0, data.length);
    }
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
