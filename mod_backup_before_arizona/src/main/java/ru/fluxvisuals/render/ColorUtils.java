package ru.fluxvisuals.render;

/** Хелперы цветов (ARGB-инт). */
public final class ColorUtils {
    public static int rgba(int r, int g, int b, int a) {
        return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    public static int rgb(int r, int g, int b) { return rgba(r, g, b, 255); }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    public static int red(int c) { return (c >> 16) & 0xFF; }
    public static int green(int c) { return (c >> 8) & 0xFF; }
    public static int blue(int c) { return c & 0xFF; }
    public static int alpha(int c) { return (c >> 24) & 0xFF; }

    public static int withAlpha(int c, int a) {
        return (clamp(a) << 24) | (c & 0x00FFFFFF);
    }

    /** Умножает альфа-канал на коэффициент (для fade-анимаций). */
    public static int mulAlpha(int c, float mult) {
        return withAlpha(c, (int) (alpha(c) * Math.max(0, Math.min(1, mult))));
    }

    /** Линейная интерполяция между двумя цветами. */
    public static int mix(int c1, int c2, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (red(c1) + (red(c2) - red(c1)) * t);
        int g = (int) (green(c1) + (green(c2) - green(c1)) * t);
        int b = (int) (blue(c1) + (blue(c2) - blue(c1)) * t);
        int a = (int) (alpha(c1) + (alpha(c2) - alpha(c1)) * t);
        return rgba(r, g, b, a);
    }

    /** Переливающийся цвет (HSB → ARGB). */
    public static int rainbow(float offset, float speed) {
        double angle = ((System.currentTimeMillis() / 1000.0 * speed) + offset) % 1.0;
        int rgb = java.awt.Color.HSBtoRGB((float) angle, 0.7f, 1f);
        return rgb | 0xFF000000;
    }

    /** Цвет от процента HP: зелёный → жёлтый → красный. */
    public static int healthColor(float hpPercent) {
        hpPercent = Math.max(0, Math.min(1, hpPercent));
        int r = (int) (255 * (1 - hpPercent) + 80 * hpPercent);
        int g = (int) (60 * (1 - hpPercent) + 200 * hpPercent);
        return rgb(r, g, 40);
    }

    public static int fromHex(String hex) {
        try {
            return (int) Long.parseLong(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return 0xFFA855F7;
        }
    }
}
