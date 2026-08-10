package ru.fluxvisuals.util.client;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.MathHelper;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class ColorUtility {
    private static final Map<Integer, Color> COLOR_CACHE = new HashMap<>();

    public static Color injectAlpha(Color color, int alpha) {
        int rgba = (color.getRGB() & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
        return COLOR_CACHE.computeIfAbsent(rgba, k -> new Color(rgba, true));
    }

    public static int injectAlphaInt(Color color, int alpha) {
        int rgba = (color.getRGB() & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
        return rgba;
    }

    public static Color linear(Color src, Color dest, float amount) {
        amount = MathHelper.clamp(amount, 0, 1);
        int red = (int) (src.getRed() + (dest.getRed() - src.getRed()) * amount);
        int green = (int) (src.getGreen() + (dest.getGreen() - src.getGreen()) * amount);
        int blue = (int) (src.getBlue() + (dest.getBlue() - src.getBlue()) * amount);
        int alpha = (int) (src.getAlpha() + (dest.getAlpha() - src.getAlpha()) * amount);
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

    public static int toIntRgba(Color color) {
        return color.getRGB();
    }
}