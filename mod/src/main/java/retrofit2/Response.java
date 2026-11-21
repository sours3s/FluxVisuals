package retrofit2;

import java.io.IOException;

public class Response<T> {
    private final int code;
    private final T body;
    private final String message;
    private final boolean successful;

    private Response(int code, T body, String message, boolean successful) {
        this.code = code;
        this.body = body;
        this.message = message;
        this.successful = successful;
    }

    public static <T> Response<T> success(T body) {
        return new Response<>(200, body, "OK", true);
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(code, null, message, false);
    }

    public int code() {
        return code;
    }

    public T body() {
        return body;
    }

    public String message() {
        return message;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public ResponseBody errorBody() {
        return message == null ? null : ResponseBody.create(null, message);
    }

    public static class ResponseBody {
        private final String content;

        private ResponseBody(String content) {
            this.content = content;
        }

        public static ResponseBody create(okhttp3.MediaType contentType, String content) {
            return new ResponseBody(content);
        }

        public String string() throws IOException {
            return content;
        }
    }
}