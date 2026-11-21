package ru.fluxvisuals.ui.fluxgui.util.color;

import java.awt.Color;

public class ColorUtil {
    public static int withAlpha(int color, int alpha) {
        return color & 16777215 | alpha << 24;
    }

    public static int getRed(int color) {
        return color >> 16 & 0xFF;
    }

    public static int getGreen(int color) {
        return color >> 8 & 0xFF;
    }

    public static int getBlue(int color) {
        return color & 0xFF;
    }

    public static int getAlpha(int color) {
        return color >> 24 & 0xFF;
    }

    public static int brighten(int color, float factor) {
        int r = getRed(color);
        int g = getGreen(color);
        int b = getBlue(color);
        int a = getAlpha(color);
        r = Math.min(255, (int)(r + (255 - r) * factor));
        g = Math.min(255, (int)(g + (255 - g) * factor));
        b = Math.min(255, (int)(b + (255 - b) * factor));
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int lerpColor(int color1, int color2, float amount) {
        amount = Math.min(1.0F, Math.max(0.0F, amount));
        int r1 = getRed(color1);
        int g1 = getGreen(color1);
        int b1 = getBlue(color1);
        int a1 = getAlpha(color1);
        int r2 = getRed(color2);
        int g2 = getGreen(color2);
        int b2 = getBlue(color2);
        int a2 = getAlpha(color2);
        int r = (int)(r1 + (r2 - r1) * amount);
        int g = (int)(g1 + (g2 - g1) * amount);
        int b = (int)(b1 + (b2 - b1) * amount);
        int a = (int)(a1 + (a2 - a1) * amount);
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static int getRainbow(float seconds, float saturation, float brightness, long index) {
        float hue = (float)((System.currentTimeMillis() + index) % (int)(seconds * 1000.0F)) / (seconds * 1000.0F);
        return Color.HSBtoRGB(hue, saturation, brightness);
    }
}
