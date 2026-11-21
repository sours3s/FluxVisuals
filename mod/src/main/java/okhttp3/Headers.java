package okhttp3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class Headers {
    private final String[] namesAndValues;

    private Headers(Builder builder) {
        this.namesAndValues = builder.namesAndValues.toArray(new String[0]);
    }

    public String get(String name) {
        for (int i = 0, size = namesAndValues.length; i < size; i += 2) {
            if (namesAndValues[i].equalsIgnoreCase(name)) {
                return namesAndValues[i + 1];
            }
        }
        return null;
    }

    public List<String> values(String name) {
        List<String> result = new ArrayList<>();
        for (int i = 0, size = namesAndValues.length; i < size; i += 2) {
            if (namesAndValues[i].equalsIgnoreCase(name)) {
                result.add(namesAndValues[i + 1]);
            }
        }
        return result;
    }

    public String name(int index) {
        return namesAndValues[index * 2];
    }

    public String value(int index) {
        return namesAndValues[index * 2 + 1];
    }

    public int size() {
        return namesAndValues.length / 2;
    }

    public Builder newBuilder() {
        Builder builder = new Builder();
        Collections.addAll(builder.namesAndValues, namesAndValues);
        return builder;
    }

    public static final class Builder {
        final List<String> namesAndValues = new ArrayList<>();

        public Builder add(String name, String value) {
            if (name == null) throw new NullPointerException("name == null");
            if (value == null) throw new NullPointerException("value == null");
            namesAndValues.add(name);
            namesAndValues.add(value);
            return this;
        }

        public Builder add(String line) {
            int colon = line.indexOf(':');
            if (colon == -1) throw new IllegalArgumentException("Unexpected header: " + line);
            return add(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
        }

        public Builder removeAll(String name) {
            for (int i = 0; i < namesAndValues.size(); i += 2) {
                if (namesAndValues.get(i).equalsIgnoreCase(name)) {
                    namesAndValues.remove(i);
                    namesAndValues.remove(i);
                    i -= 2;
                }
            }
            return this;
        }

        public Headers build() {
            return new Headers(this);
        }
    }
}