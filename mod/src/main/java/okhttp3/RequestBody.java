package okhttp3;

import java.io.IOException;

public abstract class RequestBody {
    public abstract long contentLength() throws IOException;
    public abstract void writeTo(BufferedSink sink) throws IOException;
    public abstract MediaType contentType();
}