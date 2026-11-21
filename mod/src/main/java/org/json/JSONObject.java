package org.json;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class JSONObject {
    private final Map<String, Object> values = new HashMap<>();

    public JSONObject() {}

    public JSONObject(String json) {
        // Parse JSON string - simplified
    }

    public JSONObject put(String key, Object value) {
        values.put(key, value);
        return this;
    }

    public Object get(String key) {
        return values.get(key);
    }

    public String getString(String key) {
        Object o = values.get(key);
        return o != null ? o.toString() : null;
    }

    public int getInt(String key) {
        Object o = values.get(key);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    public boolean getBoolean(String key) {
        Object o = values.get(key);
        return o instanceof Boolean ? (Boolean) o : false;
    }

    public JSONObject getJSONObject(String key) {
        Object o = values.get(key);
        return o instanceof JSONObject ? (JSONObject) o : null;
    }

    public JSONArray getJSONArray(String key) {
        Object o = values.get(key);
        return o instanceof JSONArray ? (JSONArray) o : null;
    }

    public boolean has(String key) {
        return values.containsKey(key);
    }

    public boolean isNull(String key) {
        Object o = values.get(key);
        return o == null || o == NULL;
    }

    public Object opt(String key) {
        return values.get(key);
    }

    public String optString(String key) {
        Object o = values.get(key);
        return o != null ? o.toString() : "";
    }

    public String optString(String key, String defaultValue) {
        Object o = values.get(key);
        return o != null ? o.toString() : defaultValue;
    }

    public int optInt(String key) {
        Object o = values.get(key);
        return o instanceof Number ? ((Number) o).intValue() : 0;
    }

    public boolean optBoolean(String key) {
        Object o = values.get(key);
        return o instanceof Boolean ? (Boolean) o : false;
    }

    public Iterator<String> keys() {
        return values.keySet().iterator();
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public int length() {
        return values.size();
    }

    public static final Object NULL = new Object() {
        @Override
        public String toString() {
            return "null";
        }
    };

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}