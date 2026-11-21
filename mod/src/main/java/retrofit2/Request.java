package retrofit2;

public class Request {
    private final String method;
    private final String url;
    private final okhttp3.RequestBody body;
    private final okhttp3.Headers headers;

    private Request(String method, String url, okhttp3.RequestBody body, okhttp3.Headers headers) {
        this.method = method;
        this.url = url;
        this.body = body;
        this.headers = headers;
    }

    public String method() {
        return method;
    }

    public String url() {
        return url;
    }

    public okhttp3.RequestBody body() {
        return body;
    }

    public okhttp3.Headers headers() {
        return headers;
    }

    public static class Builder {
        private String method = "GET";
        private String url;
        private okhttp3.RequestBody body;
        private okhttp3.Headers.Builder headers = new okhttp3.Headers.Builder();

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder get() {
            this.method = "GET";
            this.body = null;
            return this;
        }

        public Builder post(okhttp3.RequestBody body) {
            this.method = "POST";
            this.body = body;
            return this;
        }

        public Builder put(okhttp3.RequestBody body) {
            this.method = "PUT";
            this.body = body;
            return this;
        }

        public Builder delete() {
            this.method = "DELETE";
            this.body = null;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.add(name, value);
            return this;
        }

        public Request build() {
            return new Request(method, url, body, headers.build());
        }
    }
}