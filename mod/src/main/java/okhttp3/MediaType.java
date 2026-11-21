package okhttp3;

public final class MediaType {
    private final String type;
    private final String subtype;
    private final String charset;

    private MediaType(String type, String subtype, String charset) {
        this.type = type;
        this.subtype = subtype;
        this.charset = charset;
    }

    public static MediaType parse(String string) {
        if (string == null) throw new NullPointerException("string == null");
        int slash = string.indexOf('/');
        if (slash == -1) throw new IllegalArgumentException("Invalid media type: " + string);
        String type = string.substring(0, slash);
        int semi = string.indexOf(';', slash);
        String subtype;
        String charset = null;
        if (semi == -1) {
            subtype = string.substring(slash + 1);
        } else {
            subtype = string.substring(slash + 1, semi);
            String charsetPart = string.substring(semi + 1).trim();
            if (charsetPart.startsWith("charset=")) {
                charset = charsetPart.substring(8);
            }
        }
        return new MediaType(type, subtype, charset);
    }

    public static MediaType get(String string) {
        return parse(string);
    }

    public String type() {
        return type;
    }

    public String subtype() {
        return subtype;
    }

    public String charset() {
        return charset;
    }

    public MediaType charset(String charset) {
        if (charset == null) throw new NullPointerException("charset == null");
        return new MediaType(type, subtype, charset);
    }

    @Override
    public String toString() {
        String result = type + '/' + subtype;
        if (charset != null) {
            result += "; charset=" + charset;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MediaType && ((MediaType) o).toString().equals(toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}