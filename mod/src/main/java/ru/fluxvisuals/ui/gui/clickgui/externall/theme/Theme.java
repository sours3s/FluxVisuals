package ru.fluxvisuals.ui.gui.clickgui.externall.theme;

public class Theme {

    public static final int ACCENT        = 0;
    public static final int MENU_ACCENT_1 = 1;
    public static final int MENU_ACCENT_2 = 2;
    public static final int PANEL_ACCENT  = 3;
    public static final int PANEL_BG      = 4;
    public static final int GLASS         = 5;
    public static final int GLASS_OUTLINE = 6;
    public static final int HUD_ACCENT    = 7;
    public static final int HUD_BG        = 8;
    public static final int HUD_TEXT      = 9;
    public static final int HUD_OUTLINE   = 10;
    public static final int TEXT          = 11;
    public static final int TEXT2         = 12;
    public static final int BACKGROUND    = 13;
    public static final int OUTLINE       = 14;
    public static final int SEPARATOR     = 15;
    public static final int ELEMENT       = 7;
    public static final int LIGHT_BG      = 4;

    public static final int COLOR_COUNT   = 16;

    public static final String[] COLOR_NAMES = {
            "Акцент темы", "Акцент меню", "Акцент меню 2", "Акцент плашек", "Фон плашек",
            "Стекло", "Обводка стекла", "Акцент худа", "Фон худа", "Текст худа", "Обводка худа",
            "Светлый текст", "Тёмный текст", "Цвет фона", "Обводка", "Цвет разделителей"
    };

    public static final int OPACITY = 0;
    public static final int BLUR    = 1;

    public static final String[] SLIDER_NAMES = { "Прозрачность", "Сила размытия" };
    public static final float[]  SLIDER_MIN   = { 0.1F, 0.0F };
    public static final float[]  SLIDER_MAX   = { 1.0F, 6.0F };
    public static final float[]  SLIDER_DEF   = { 1.0F, 4.0F };
    public static final float[]  SLIDER_STEP  = { 0.0F, 1.0F };

    public static final int RAINBOW    = 0;
    public static final int SHADOW     = 1;
    public static final int DISTORTION = 2;

    public static final String[]  BOOL_NAMES = { "Радуга", "Эффект тени", "Эффект искажения" };
    public static final boolean[] BOOL_DEF   = { false, true, false };

    public String name;
    public final int[] colors = new int[COLOR_COUNT];
    public final float[] sliders = new float[SLIDER_NAMES.length];
    public final boolean[] booleans = new boolean[BOOL_NAMES.length];
    public boolean builtin = false;

    public static int rgb(int r, int g, int b) {
        return 0xFF000000 | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public static int rgba(int r, int g, int b, int a) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    public Theme(String name) {
        this.name = name;

        colors[ACCENT]        = 0xFF9B96F6;
        colors[MENU_ACCENT_1] = 0xFF9B96F6;
        colors[MENU_ACCENT_2] = 0xFFBA8BFF;
        colors[PANEL_ACCENT]  = 0xFF9B96F6;
        colors[PANEL_BG]      = 0xFF15141C;
        colors[GLASS]         = rgba(20, 19, 28, 180);
        colors[GLASS_OUTLINE] = rgba(255, 255, 255, 30);
        colors[HUD_ACCENT]    = 0xFF9B96F6;
        colors[HUD_BG]        = 0xFF0E0D13;
        colors[HUD_TEXT]      = 0xFFFFFFFF;
        colors[HUD_OUTLINE]   = rgba(255, 255, 255, 25);
        colors[TEXT]          = 0xFFFFFFFF;
        colors[TEXT2]         = 0xFF8A8997;
        colors[BACKGROUND]    = 0xFF0E0E12;
        colors[OUTLINE]       = rgba(255, 255, 255, 30);
        colors[SEPARATOR]     = rgba(255, 255, 255, 20);

        System.arraycopy(SLIDER_DEF, 0, sliders, 0, sliders.length);
        System.arraycopy(BOOL_DEF, 0, booleans, 0, booleans.length);
    }

    public int get(int index) {
        if (index < 0 || index >= colors.length) return colors[0];
        return colors[index];
    }

    public void set(int index, int argb) {
        if (index >= 0 && index < colors.length) {
            colors[index] = argb;
        }
    }

    public float getSlider(int index) {
        if (index < 0 || index >= sliders.length) return 0f;
        return clampSlider(index, sliders[index]);
    }

    public void setSlider(int index, float value) {
        if (index >= 0 && index < sliders.length) {
            sliders[index] = clampSlider(index, value);
        }
    }

    private static float clampSlider(int index, float value) {
        float step = SLIDER_STEP[index];
        if (step > 0) value = Math.round(value / step) * step;
        if (value < SLIDER_MIN[index]) return SLIDER_MIN[index];
        if (value > SLIDER_MAX[index]) return SLIDER_MAX[index];
        return value;
    }

    public boolean getBool(int index) {
        if (index < 0 || index >= booleans.length) return false;
        return booleans[index];
    }

    public void setBool(int index, boolean value) {
        if (index >= 0 && index < booleans.length) {
            booleans[index] = value;
        }
    }

    public Theme copy(String newName) {
        Theme t = new Theme(newName);
        System.arraycopy(this.colors, 0, t.colors, 0, this.colors.length);
        System.arraycopy(this.sliders, 0, t.sliders, 0, this.sliders.length);
        System.arraycopy(this.booleans, 0, t.booleans, 0, this.booleans.length);
        return t;
    }
}
