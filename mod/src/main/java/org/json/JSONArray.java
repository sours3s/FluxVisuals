package org.json;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class JSONArray implements Iterable<Object> {
    private final List<Object> values = new ArrayList<>();

    public JSONArray() {}

    public JSONArray(String json) {
        // Parse JSON string - simplified
    }

    public JSONArray(Collection<?> collection) {
        values.addAll(collection);
    }

    public JSONArray put(Object value) {
        values.add(value);
        return this;
    }

    public Object get(int index) {
        return values.get(index);
    }

    public String getString(int index) {
        Object o = values.get(index);
        return o != null ? o.toString() : null;
    }

    public int getInt(int index) {
        Object o = values.get(index);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    public boolean getBoolean(int index) {
        Object o = values.get(index);
        return o instanceof Boolean ? (Boolean) o : false;
    }

    public JSONObject getJSONObject(int index) {
        Object o = values.get(index);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public JSONArray getJSONArray(int index) {
        Object o = values.get(index);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public int length() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    @Override
    public Iterator<Object> iterator() {
        return values.iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(values.get(i));
        }
        sb.append("]");
        return sb.toString();
    }
}