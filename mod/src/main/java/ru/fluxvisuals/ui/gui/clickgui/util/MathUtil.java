package ru.fluxvisuals.ui.gui.clickgui.util;
import ru.fluxvisuals.ui.gui.clickgui.util.type.Mc;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class MathUtil {
    public static int clamp(int i, int i2, int i3) {
        return Math.min(Math.max(i, i2), i3);
    }

    public static float clamp(float f, float f2, float f3) {
        return Math.min(Math.max(f, f2), f3);
    }

    public static double clamp(double d, double d2, double d3) {
        return Math.min(Math.max(d, d2), d3);
    }

    public static int interpolate(int i, int i2, float f) {
        return Math.round(i + ((i2 - i) * f));
    }

    public static float interpolate(float f, float f2, float f3) {
        return f + ((f2 - f) * f3);
    }

    public static double interpolate(double d, double d2, double d3) {
        return d + ((d2 - d) * d3);
    }

    public static float interpolate(float f, float f2) {
        return f + ((f2 - f) * Mc.INSTANCE.getRenderTickCounter().getTickProgress(true));
    }

    public static double interpolate(double d, double d2) {
        return d + ((d2 - d) * Mc.INSTANCE.getRenderTickCounter().getTickProgress(true));
    }

    
    public static int getRandom(int i, int i2) {
        return (int) getRandom(i, i2 + 1.0f);
    }

    public static float getRandom(float f, float f2) {
        return (float) getRandom(f, f2);
    }

    public static double getRandom(double d, double d2) {
        if (d == d2) {
            return d;
        }
        if (d > d2) {
            d = d2;
            d2 = d;
        }
        return ThreadLocalRandom.current().nextDouble(d, d2);
    }

    public static float lerp(float f, float f2, float f3) {
        return (float) (((double) f) + (((double) (f2 - f)) * clamp(deltaTime() * ((double) f3), 0.0d, 1.0d)));
    }

    public static Vec3d lerp(Vec3d vec3d, Vec3d vec3d2, double d) {
        return new Vec3d(vec3d.x + ((vec3d2.x - vec3d.x) * d), vec3d.y + ((vec3d2.y - vec3d.y) * d), vec3d.z + ((vec3d2.z - vec3d.z) * d));
    }

    public static double lerp(double d, double d2, double d3) {
        return d + ((d2 - d) * clamp(deltaTime() * d3, 0.0d, 1.0d));
    }

    public static double computeGcd() {
        double dDoubleValue = (((Double) Mc.INSTANCE.getGameOptions().getMouseSensitivity().getValue()).doubleValue() * 0.6000000238418579d) + 0.20000000298023224d;
        return dDoubleValue * dDoubleValue * dDoubleValue * 8.0d * 0.15d;
    }

    public static float rad(float f) {
        return (float) ((((double) f) * 3.141592653589793d) / 180.0d);
    }

    public static float fastPow(float f, float f2) {
        return (float) Math.pow(f, f2);
    }

    public static int roundToInt(double d) {
        return (int) Math.round(d);
    }

    public static double round(double d, double d2) {
        return Math.round((Math.round(d / d2) * d2) * 100.0d) / 100.0d;
    }

    public static float textScrolling(float f) {
        int i = (int) (f * 75.0f);
        return ((float) clamp(((System.currentTimeMillis() % ((long) i)) * 3.141592653589793d) / ((double) i), 0.0d, 1.0d)) * f;
    }

    public static boolean isHovered(double d, double d2, double d3, double d4, double d5, double d6) {
        return d >= d3 && d <= d3 + d5 && d2 >= d4 && d2 <= d4 + d6;
    }

    public static int floorNearestMulN(int i, int i2) {
        return i2 * ((int) Math.floor(((double) i) / ((double) i2)));
    }

    public static double absSinAnimation(double d) {
        return Math.abs(1.0d + Math.sin(d)) / 2.0d;
    }

    public static String tickToElapsedTime(int i) {
        int i2 = i / 20;
        int i3 = i2 / 60;
        int i4 = i2 % 60;
        return i4 < 10 ? i3 + ":0" + i4 : i3 + ":" + i4;
    }

    public static String decimalFormat(Number number, int i) {
        DecimalFormat decimalFormat = new DecimalFormat("0", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        decimalFormat.setMaximumFractionDigits(i);
        return decimalFormat.format(number);
    }

    public static double deltaTime() {
        int currentFps = MinecraftClient.getInstance().getCurrentFps();
        if (currentFps > 0) {
            return 1.0d / ((double) currentFps);
        }
        return 1.0d;
    }

    public MathUtil() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}