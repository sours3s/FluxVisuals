package okhttp3;

import java.io.IOException;

public interface BufferedSink {
    BufferedSink write(byte[] source) throws IOException;
    BufferedSink writeByte(int b) throws IOException;
    BufferedSink writeUtf8(String string) throws IOException;
    void flush() throws IOException;
    void close() throws IOException;
}