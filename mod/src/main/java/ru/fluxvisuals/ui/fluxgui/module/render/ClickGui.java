package ru.fluxvisuals.ui.fluxgui.module.render;

import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.ui.fluxgui.setting.BindSetting;
import ru.fluxvisuals.ui.fluxgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ColorSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ModeSetting;
import ru.fluxvisuals.ui.fluxgui.setting.NumberSetting;

/** Настройки и управление ClickGUI FluxVisuals. */
public final class ClickGui {
    private static boolean toggleHandledByScreen;
    private static long toggleHandledAt;

    public static final BindSetting openBind = new BindSetting("Бинд ClickGUI", 344);
    public static final ModeSetting guiSize = new ModeSetting("Размер GUI", "100%", "50%", "75%", "100%", "125%", "150%");
    public static final ModeSetting guiStyle = new ModeSetting("Стиль", "Жидкое стекло", "Основной", "Жидкое стекло");
    public static final ColorSetting guiGlassColor = new ColorSetting("Цвет стекла", new Color(20, 16, 38, 88), ClickGui::isLiquidGlassStyle);
    public static final BooleanSetting guiGlassGlow = new BooleanSetting("Свечение краёв", true, ClickGui::isLiquidGlassStyle);
    public static final NumberSetting guiGlowStrength = new NumberSetting(
        "Сила свечения", 1.0, 0.25, 2.0, 0.05, () -> isLiquidGlassStyle() && guiGlassGlow.getValue()
    );
    public static final ColorSetting accentColor = new ColorSetting("Акцентный цвет", new Color(0xA855F7));
    public static final BooleanSetting blur = new BooleanSetting("Блюр панелей", true);
    public static final NumberSetting blurStrength = new NumberSetting("Сила блюра", 6.0, 1.0, 12.0, 1.0, () -> blur.getValue());

    private ClickGui() {
    }

    public static boolean isLiquidGlassStyle() {
        return guiStyle.is("Жидкое стекло") || guiStyle.is("Liquid Glass");
    }

    public static int getAccentColor() {
        return accentColor.getValue() & 0xFFFFFF;
    }

    public static int getAccentColor(float brightness, int alpha) {
        int accent = getAccentColor();
        int r = accent >> 16 & 0xFF;
        int g = accent >> 8 & 0xFF;
        int b = accent & 0xFF;
        if (brightness < 1.0F) {
            r = Math.round(r * brightness);
            g = Math.round(g * brightness);
            b = Math.round(b * brightness);
        } else {
            float light = Math.min(1.0F, brightness - 1.0F);
            r = Math.round(r + (255 - r) * light);
            g = Math.round(g + (255 - g) * light);
            b = Math.round(b + (255 - b) * light);
        }
        return Math.max(0, Math.min(255, alpha)) << 24
            | Math.max(0, Math.min(255, r)) << 16
            | Math.max(0, Math.min(255, g)) << 8
            | Math.max(0, Math.min(255, b));
    }

    public static void openMainGui(MinecraftClient client) {
        if (client != null) {
            client.setScreen(new ru.fluxvisuals.ui.clickgui.FluxClickGuiScreen());
        }
    }

    public static void toggleMainGui(MinecraftClient client) {
        if (client != null) {
            if (client.currentScreen instanceof ru.fluxvisuals.ui.clickgui.FluxClickGuiScreen screen) {
                screen.close();
            } else {
                openMainGui(client);
            }
        }
    }

    public static void markToggleHandledByScreen() {
        toggleHandledByScreen = true;
        toggleHandledAt = System.nanoTime();
    }

    public static boolean consumeToggleHandledByScreen() {
        boolean handled = toggleHandledByScreen && System.nanoTime() - toggleHandledAt < 500_000_000L;
        toggleHandledByScreen = false;
        return handled;
    }

    public static float getBackgroundOpacity() {
        return 0.7254902F;
    }

    public static int applyBackgroundOpacity(int color) {
        int alpha = color >> 24 & 0xFF;
        int newAlpha = Math.max(0, Math.min(255, Math.round(alpha * getBackgroundOpacity())));
        return newAlpha << 24 | color & 0xFFFFFF;
    }

    public static int getBlurStrength() {
        return Math.max(1, Math.min(12, blurStrength.getIntValue()));
    }

    public static float getGuiSizeFactor() {
        return switch (guiSize.getValue()) {
            case "50%" -> 0.5F;
            case "75%" -> 0.75F;
            case "125%" -> 1.25F;
            case "150%" -> 1.5F;
            default -> 1.0F;
        };
    }

    public static float animR(float baseRate) {
        return baseRate;
    }
}
