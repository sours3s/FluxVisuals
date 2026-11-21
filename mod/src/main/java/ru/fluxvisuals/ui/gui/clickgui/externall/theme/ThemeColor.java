package ru.fluxvisuals.ui.gui.clickgui.externall.theme;

import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;
import java.awt.Color;

public final class ThemeColor {

    private ThemeColor() {}

    public static int getVisualColor() {
        return getAccent();
    }

    public static int getAccent() {
        Theme active = ThemeManager.get().getActive();
        if (active.getBool(Theme.RAINBOW)) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            return 0xFF000000 | (Color.HSBtoRGB(hue, 0.75f, 1.0f) & 0xFFFFFF);
        }
        return active.get(Theme.ACCENT);
    }

    public static int getAccent(float alpha) {
        return ColorUtil.multAlpha(getAccent(), alpha);
    }

    public static int getMenuAccent1() {
        Theme active = ThemeManager.get().getActive();
        if (active.getBool(Theme.RAINBOW)) {
            float hue = (System.currentTimeMillis() % 4000L) / 4000.0f;
            return 0xFF000000 | (Color.HSBtoRGB(hue, 0.75f, 1.0f) & 0xFFFFFF);
        }
        return active.get(Theme.MENU_ACCENT_1);
    }

    public static int getMenuAccent1(float alpha) {
        return ColorUtil.multAlpha(getMenuAccent1(), alpha);
    }

    public static int getMenuAccent2() {
        Theme active = ThemeManager.get().getActive();
        if (active.getBool(Theme.RAINBOW)) {
            float hue = ((System.currentTimeMillis() + 1000L) % 4000L) / 4000.0f;
            return 0xFF000000 | (Color.HSBtoRGB(hue, 0.75f, 1.0f) & 0xFFFFFF);
        }
        return active.get(Theme.MENU_ACCENT_2);
    }

    public static int getMenuAccent2(float alpha) {
        return ColorUtil.multAlpha(getMenuAccent2(), alpha);
    }

    public static int getPanelAccent() {
        return ThemeManager.get().getActive().get(Theme.PANEL_ACCENT);
    }

    public static int getPanelAccent(float alpha) {
        return ColorUtil.multAlpha(getPanelAccent(), alpha);
    }

    public static int getPanelBg() {
        return ThemeManager.get().getActive().get(Theme.PANEL_BG);
    }

    public static int getPanelBg(float alpha) {
        return ColorUtil.multAlpha(getPanelBg(), alpha);
    }

    public static int getGlass() {
        return ThemeManager.get().getActive().get(Theme.GLASS);
    }

    public static int getGlass(float alpha) {
        return ColorUtil.multAlpha(getGlass(), alpha);
    }

    public static int getGlassOutline() {
        return ThemeManager.get().getActive().get(Theme.GLASS_OUTLINE);
    }

    public static int getGlassOutline(float alpha) {
        return ColorUtil.multAlpha(getGlassOutline(), alpha);
    }

    public static int getHudColor() {
        return getAccent();
    }

    public static int getHudColor(float alpha) {
        return getAccent(alpha);
    }

    public static int getHudBg() {
        return ThemeManager.get().getActive().get(Theme.HUD_BG);
    }

    public static int getHudBg(float alpha) {
        return ColorUtil.multAlpha(getHudBg(), alpha);
    }

    public static int getHudText() {
        return ThemeManager.get().getActive().get(Theme.HUD_TEXT);
    }

    public static int getHudText(float alpha) {
        return ColorUtil.multAlpha(getHudText(), alpha);
    }

    public static int getHudOutline() {
        return ThemeManager.get().getActive().get(Theme.HUD_OUTLINE);
    }

    public static int getHudOutline(float alpha) {
        return ColorUtil.multAlpha(getHudOutline(), alpha);
    }

    public static int getTextColor() {
        return ThemeManager.get().getActive().get(Theme.TEXT);
    }

    public static int getTextColor(float alpha) {
        return ColorUtil.multAlpha(getTextColor(), alpha);
    }

    public static int getDarkTextColor() {
        return ThemeManager.get().getActive().get(Theme.TEXT2);
    }

    public static int getDarkTextColor(float alpha) {
        return ColorUtil.multAlpha(getDarkTextColor(), alpha);
    }

    public static int getBackgroundColor() {
        return ThemeManager.get().getActive().get(Theme.BACKGROUND);
    }

    public static int getBackgroundColor(float alpha) {
        return ColorUtil.multAlpha(getBackgroundColor(), alpha);
    }

    public static int getOutlineColor() {
        return ThemeManager.get().getActive().get(Theme.OUTLINE);
    }

    public static int getOutlineColor(float alpha) {
        return ColorUtil.multAlpha(getOutlineColor(), alpha);
    }

    public static int getSeparatorColor() {
        return ThemeManager.get().getActive().get(Theme.SEPARATOR);
    }

    public static int getSeparatorColor(float alpha) {
        return ColorUtil.multAlpha(getSeparatorColor(), alpha);
    }

    public static int getLightBackgroundColor() {
        return ThemeManager.get().getActive().get(Theme.LIGHT_BG);
    }

    public static int getLightBackgroundColor(float alpha) {
        return ColorUtil.multAlpha(getLightBackgroundColor(), alpha);
    }

    public static int byIndex(int index) {
        return ThemeManager.get().getActive().get(index);
    }

    public static float getOpacity() {
        return ThemeManager.get().getActive().getSlider(Theme.OPACITY);
    }

    public static int getBlur() {
        return Math.round(ThemeManager.get().getActive().getSlider(Theme.BLUR));
    }

    public static boolean getShadow() {
        return ThemeManager.get().getActive().getBool(Theme.SHADOW);
    }

    public static boolean getDistortion() {
        return ThemeManager.get().getActive().getBool(Theme.DISTORTION);
    }
}
