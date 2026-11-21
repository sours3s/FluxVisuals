package ru.fluxvisuals.config;

/**
 * Одна настройка модуля. Универсальный класс: BOOLEAN / INT / FLOAT / MODE / COLOR / STRING.
 * Легко сериализуется в JSON и авто-рисуется в ClickGUI.
 */
public class Setting {
    public enum Type { BOOLEAN, INT, FLOAT, MODE, COLOR, STRING }

    public final String name;
    public final String description;
    public final Type type;
    private Object value;
    public final Object defaultValue;
    public final float min;
    public final float max;
    public final String[] modes;

    private Setting(String name, String description, Type type, Object value, float min, float max, String[] modes) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.value = value;
        this.defaultValue = value;
        this.min = min;
        this.max = max;
        this.modes = modes;
    }

    public static Setting bool(String name, String description, boolean def) {
        return new Setting(name, description, Type.BOOLEAN, def, 0, 0, null);
    }

    public static Setting int_(String name, String description, int def, int min, int max) {
        return new Setting(name, description, Type.INT, def, min, max, null);
    }

    public static Setting float_(String name, String description, float def, float min, float max) {
        return new Setting(name, description, Type.FLOAT, def, min, max, null);
    }

    public static Setting mode(String name, String description, String... modes) {
        return new Setting(name, description, Type.MODE, 0, 0, 0, modes);
    }

    public static Setting color(String name, String description, int def) {
        return new Setting(name, description, Type.COLOR, def, 0, 0, null);
    }

    public static Setting str(String name, String description, String def) {
        return new Setting(name, description, Type.STRING, def, 0, 0, null);
    }

    public boolean getBoolean() { return (Boolean) value; }
    public void setBoolean(boolean v) { value = v; }

    public int getInt() { return ((Number) value).intValue(); }
    public void setInt(int v) { value = v; }

    public float getFloat() { return ((Number) value).floatValue(); }
    public void setFloat(float v) { value = v; }

    public int getModeIndex() { return ((Number) value).intValue(); }
    public void setModeIndex(int v) { value = v; }
    public String getMode() { return modes[Math.max(0, Math.min(modes.length - 1, getModeIndex()))]; }
    public void cycleMode() { setModeIndex((getModeIndex() + 1) % modes.length); }

    public int getColor() { return (Integer) value; }
    public void setColor(int v) { value = v; }

    public String getString() { return (String) value; }
    public void setString(String v) { value = v; }

    /** Применяет значение из конфига (JSON). */
    public void load(Object v) {
        if (v == null) return;
        try {
            switch (type) {
                case BOOLEAN -> value = v instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(v));
                case INT -> value = v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v));
                case FLOAT -> value = v instanceof Number n ? n.floatValue() : Float.parseFloat(String.valueOf(v));
                case MODE -> {
                    int idx = -1;
                    if (v instanceof Number n) idx = n.intValue();
                    else if (v instanceof String s) {
                        for (int i = 0; i < modes.length; i++) if (modes[i].equals(s)) { idx = i; break; }
                    }
                    value = idx >= 0 ? idx : 0;
                }
                case COLOR -> value = v instanceof Number n ? n.intValue() : Integer.decode(String.valueOf(v));
                case STRING -> value = String.valueOf(v);
            }
        } catch (Exception e) {
            value = defaultValue;
        }
    }

    public Object getValue() { return value; }
}
