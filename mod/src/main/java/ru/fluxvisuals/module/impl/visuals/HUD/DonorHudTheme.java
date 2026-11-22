package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;

/**
 * Цвета исходного HUD FluxVisuals. Используются компактными элементами,
 * чтобы их вид не зависел от MSDF-шрифтов и global theme ClickGUI.
 */
@Environment(EnvType.CLIENT)
public final class DonorHudTheme {
    public static final int ACCENT = 0xFFA855F7;
    public static final int ACCENT_SECOND = 0xFFEC4899;
    public static final int TEXT = 0xFFF4F4F6;
    public static final int TEXT_DIM = 0xFF9CA3AF;
    public static final int BG = 0xFF0B0B12;
    public static final int ARMOR = 0xFF8FB8FF;
    public static final int BG_ALPHA = 114;

    private DonorHudTheme() {
    }

    public static int withAlpha(int color, int alpha) {
        return (clamp(alpha) << 24) | (color & 0x00FFFFFF);
    }

    public static int mulAlpha(int color, float multiplier) {
        float k = Math.max(0.0F, Math.min(1.0F, multiplier));
        return withAlpha(color, (int) (((color >>> 24) & 0xFF) * k));
    }

    public static int mix(int first, int second, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int a = blend((first >>> 24) & 0xFF, (second >>> 24) & 0xFF, k);
        int r = blend((first >>> 16) & 0xFF, (second >>> 16) & 0xFF, k);
        int g = blend((first >>> 8) & 0xFF, (second >>> 8) & 0xFF, k);
        int b = blend(first & 0xFF, second & 0xFF, k);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static int healthColor(float percent) {
        float p = Math.max(0.0F, Math.min(1.0F, percent));
        int r = (int) (255.0F * (1.0F - p) + 80.0F * p);
        int g = (int) (60.0F * (1.0F - p) + 200.0F * p);
        return withAlpha((r << 16) | (g << 8) | 40, 255);
    }

    public static int category(Category category) {
        if (category == null) {
            return ACCENT;
        }
        return switch (category) {
            case Visuals -> 0xFF4ADE80;
            case Utils -> ACCENT;
            case Configs -> 0xFFFACC15;
            case Friends -> 0xFFFF5A6E;
            case Cosmetics -> ACCENT_SECOND;
        };
    }

    private static int blend(int first, int second, float k) {
        return (int) (first * (1.0F - k) + second * k);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
