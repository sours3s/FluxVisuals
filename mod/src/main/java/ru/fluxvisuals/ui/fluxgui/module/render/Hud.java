package ru.fluxvisuals.ui.fluxgui.module.render;

import java.awt.Color;
import java.util.function.Supplier;
import ru.fluxvisuals.ui.fluxgui.setting.BooleanSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ColorSetting;
import ru.fluxvisuals.ui.fluxgui.setting.ModeSetting;
import ru.fluxvisuals.ui.fluxgui.setting.NumberSetting;

/**
 * Настройки донорского HUD. Логика и значения скопированы из исходного модуля
 * без блока пресетов и редактора — они относятся к другому интерфейсу.
 */
public final class Hud {
    private static final String STYLE_CLASSIC = "Основной";
    private static final String STYLE_LIQUID_GLASS = "Жидкое стекло";
    private static final Supplier<Boolean> GLASS_VISIBLE = Hud::isLiquidGlassStyle;

    public static final ModeSetting hudStyle = new ModeSetting("Стиль", STYLE_LIQUID_GLASS, STYLE_CLASSIC, STYLE_LIQUID_GLASS);
    public static final ColorSetting hudGlassColor = new ColorSetting("Цвет стекла", new Color(20, 16, 38, 88), GLASS_VISIBLE);
    public static final BooleanSetting hudGlassGlow = new BooleanSetting("HUD Edge Glow", true, GLASS_VISIBLE);
    public static final NumberSetting hudGlowStrength = new NumberSetting(
          "HUD Glow Strength", 1.0, 0.25, 2.0, 0.05,
          () -> isLiquidGlassStyle() && hudGlassGlow.getValue());
    public static final BooleanSetting customHotbar = new BooleanSetting("Хотбар", true);
    public static final BooleanSetting armorHud = new BooleanSetting("Броня", true);
    public static final BooleanSetting watermark = new BooleanSetting("Ватермарка", true);
    public static final ModeSetting language = new ModeSetting("Язык", "Русский", "Русский", "Английский");
    public static final ColorSetting accentColor = new ColorSetting("Акцентный цвет", new Color(0xA855F7));
    public static final BooleanSetting shadow = new BooleanSetting("Тень", true);
    public static final BooleanSetting blur = new BooleanSetting("Блюр", true);
    public static final BooleanSetting notifications = new BooleanSetting("Уведомления", true);
    public static final BooleanSetting target = new BooleanSetting("Таргет", true);
    public static final BooleanSetting hotkeys = new BooleanSetting("Хоткеи", true);
    public static final BooleanSetting potions = new BooleanSetting("Зелья", true);
    public static final BooleanSetting cooldowns = new BooleanSetting("Кулдауны", true);
    public static final BooleanSetting inventory = new BooleanSetting("Инвентарь", true);
    public static final BooleanSetting mediaPlayer = new BooleanSetting("Медиаплеер", false);

    private Hud() {
    }

    public static boolean isLiquidGlassStyle() {
        return hudStyle.is(STYLE_LIQUID_GLASS);
    }

    public static void setHudStyle(String style) {
        if (STYLE_LIQUID_GLASS.equalsIgnoreCase(style)) {
            hudStyle.setValue(STYLE_LIQUID_GLASS);
        } else if (STYLE_CLASSIC.equalsIgnoreCase(style)) {
            hudStyle.setValue(STYLE_CLASSIC);
        }
    }

    public static boolean isEnglish() {
        return language.is("Английский");
    }

    public static String text(String russian, String english) {
        return isEnglish() ? english : russian;
    }
}
