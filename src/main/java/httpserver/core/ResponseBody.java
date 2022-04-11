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
                return data.length;
            }
            public void writeTo(OutputStream out) throws IOException {
                out.write(data, offset, length);
                out.flush();
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
                final byte[] data = new byte[8192];
                int read; while ((read = in.read(data)) != -1) {
                    int toWrite = (leftToWrite < read) ? (int)leftToWrite : read;
                    out.write(data, 0, toWrite);
                    leftToWrite -= toWrite;
                    if (leftToWrite <= 0) break;
                }
                out.flush();
            }
        };
    }

}
