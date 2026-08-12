package ru.fluxvisuals.render;

import ru.fluxvisuals.config.ConfigManager;
import ru.fluxvisuals.module.Category;

/** Тема оформления: акцентные цвета, фон, текст. */
public final class Theme {
    private Theme() {}

    public static int accent() { return ConfigManager.INSTANCE.accent; }
    public static int accentSecond() { return ConfigManager.INSTANCE.accentSecond; }
    public static int accentMix(float t) { return ColorUtils.mix(accent(), accentSecond(), t); }

    public static int bg() {
        return ColorUtils.withAlpha(0xFF0B0B12, (int) (255f * ConfigManager.INSTANCE.bgAlpha));
    }

    public static int bgBright() {
        return ColorUtils.withAlpha(0xFF16161F, (int) (Math.min(1f, ConfigManager.INSTANCE.bgAlpha + 0.2f) * 255));
    }

    public static int border() { return ColorUtils.withAlpha(accent(), 90); }
    public static int text() { return 0xFFF4F4F6; }
    public static int textDim() { return 0xFF9CA3AF; }
    public static int textAccent() { return accent(); }

    public static int categoryColor(Category category) {
        return switch (category) {
            case HUD -> accent();
            case TARGET -> 0xFFFF5A6E;
            case VISUAL -> 0xFF4ADE80;
            case MISC -> 0xFFFACC15;
        };
    }
}
