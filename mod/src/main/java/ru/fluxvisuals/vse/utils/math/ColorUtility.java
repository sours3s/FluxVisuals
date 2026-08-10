package ru.fluxvisuals.vse.utils.math;

import lombok.experimental.UtilityClass;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@UtilityClass
public class ColorUtility {
    private final Map<Integer, Color> COLOR_CACHE = new HashMap<>();

    public Color injectAlpha(Color color, float alpha) {
        int rgba = (color.getRGB() & 0x00FFFFFF) | (Math.clamp((int)alpha, 0, 255) << 24);
        return COLOR_CACHE.computeIfAbsent(rgba, k -> new Color(rgba, true));
    }

    public Color linear(Color src, Color dest, float amount) {
        amount = MathHelper.clamp(amount, 0, 1);
        int red = (int) MathUtility.linear(src.getRed(), dest.getRed(), amount);
        int green = (int) MathUtility.linear(src.getGreen(), dest.getGreen(), amount);
        int blue = (int) MathUtility.linear(src.getBlue(), dest.getBlue(), amount);
        int alpha = (int) MathUtility.linear(src.getAlpha(), dest.getAlpha(), amount);
        return new Color(MathHelper.clamp(red, 0, 255), MathHelper.clamp(green, 0, 255), MathHelper.clamp(blue, 0, 255), MathHelper.clamp(alpha, 0, 255));
    }
    public static Color blend(Color c1, Color c2, float t) {
        t = Math.max(0f, Math.min(1f, t));

        int r = (int)(c1.getRed() + (c2.getRed() - c1.getRed()) * t);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int)(c1.getBlue() + (c2.getBlue() - c1.getBlue()) * t);
        int a = (int)(c1.getAlpha() + (c2.getAlpha() - c1.getAlpha()) * t);

        return new Color(
                Math.max(0, Math.min(255, r)),
                Math.max(0, Math.min(255, g)),
                Math.max(0, Math.min(255, b)),
                Math.max(0, Math.min(255, a))
        );
    }
    public Color contrast(Color color) {
        return new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue());
    }
    public Color brightness(Color color, int amount) {
        return new Color(Math.clamp(color.getRed() - amount, 0, 255), Math.clamp(color.getGreen() - amount, 0, 255), Math.clamp(color.getBlue() - amount, 0, 255), color.getAlpha());
    }

    public Color transfusionEffect(int speed, int index, Color... colors) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        angle = (angle > 180 ? 360 - angle : angle) + 180;

        int colorIndex = (int) (angle / 360F * colors.length);

        if (colorIndex == colors.length) {
            colorIndex--;
        }

        Color color1 = colors[colorIndex];
        Color color2 = colors[colorIndex == colors.length - 1 ? 0 : colorIndex + 1];

        return ColorUtility.linear(color1, color2, angle / 360F * colors.length - colorIndex);
    }

    public Color rainbowEffect(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor(angle / 360f, 0.8f, 0.8f);
    }
    public Color rainbowEffectBright(int speed, int index) {
        int angle = (int) ((System.currentTimeMillis() / speed + index) % 360);
        return Color.getHSBColor(angle / 360f, 0.8f, 1);
    }

    public double distance(int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        double rmean = (r1 + r2) / 2.0;
        double r = r1 - r2;
        double g = g1 - g2;
        double b = b1 - b2;

        double weightR = 2 + rmean / 256.0;
        double weightG = 4.0;
        double weightB = 2 + (255 - rmean) / 256.0;

        return Math.sqrt(weightR * r * r + weightG * g * g + weightB * b * b);
    }
    public Color max(Color a, Color b) {
        if (a.getAlpha() > b.getAlpha() && a.getRed() >  b.getRed() && a.getGreen() > b.getGreen() && a.getBlue() > b.getBlue()) {
            return b;
        }
        return a;
    }
    
    public int fade(int offset) {
        return rainbowEffect(10, offset).getRGB();
    }
    
    public int replAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.clamp(alpha, 0, 255) << 24);
    }
}
