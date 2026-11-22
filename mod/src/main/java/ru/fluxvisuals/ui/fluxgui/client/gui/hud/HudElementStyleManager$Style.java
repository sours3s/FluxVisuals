package ru.fluxvisuals.ui.fluxgui.client.gui.hud;

import com.google.gson.JsonObject;

public final class HudElementStyleManager$Style {
    final JsonObject value;

    HudElementStyleManager$Style(JsonObject value) {
        this.value = value;
    }

    public String material(String fallback) {
        return this.string("material", fallback);
    }

    public int panelColor(int fallback) {
        return this.color("panelColor", fallback);
    }

    public Integer panelColor2() {
        return this.has("panelColor2") ? this.color("panelColor2", 0) : null;
    }

    public int panelOpacity(int fallback) {
        return this.integer("panelOpacity", fallback);
    }

    public float radius(float fallback) {
        return this.number("radius", fallback);
    }

    public Integer textColor() {
        return this.has("textColor") ? this.color("textColor", 16777215) : null;
    }

    public int accentColor(int fallback) {
        return this.color("accentColor", fallback);
    }

    public boolean hasIcon() {
        return this.has("icon");
    }

    public String icon(String fallback) {
        return this.string("icon", fallback);
    }

    public float iconScale() {
        return this.number("iconScale", 1.0F);
    }

    public boolean shadow(boolean fallback) {
        return this.bool("shadow", fallback);
    }

    public int shadowColor(int fallback) {
        return this.color("shadowColor", fallback);
    }

    public float shadowStrength(float fallback) {
        return this.number("shadowStrength", fallback);
    }

    public boolean blur(boolean fallback) {
        return this.bool("blur", fallback);
    }

    public float blurStrength(float fallback) {
        return this.number("blurStrength", fallback);
    }

    public boolean glow(boolean fallback) {
        return this.bool("glow", fallback);
    }

    public int glowColor(int fallback) {
        return this.color("glowColor", fallback);
    }

    public float glowStrength(float fallback) {
        return this.number("glowStrength", fallback);
    }

    public boolean outline() {
        return this.bool("outline", false);
    }

    public int outlineColor(int fallback) {
        return this.color("outlineColor", fallback);
    }

    public float outlineWidth() {
        return this.number("outlineWidth", 1.0F);
    }

    public boolean stripe() {
        return this.bool("stripe", false);
    }

    public int stripeColor(int fallback) {
        return this.color("stripeColor", fallback);
    }

    public float stripeWidth() {
        return this.number("stripeWidth", 3.0F);
    }

    public String stripeSide() {
        return this.string("stripeSide", "left");
    }

    public String label() {
        return this.string("label", "");
    }

    public int labelColor(int fallback) {
        return this.color("labelColor", fallback);
    }

    boolean has(String key) {
        return HudElementStyleManager.primitive(this.value, key);
    }

    String string(String key, String fallback) {
        try {
            return this.has(key) ? this.value.get(key).getAsString() : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }

    boolean bool(String key, boolean fallback) {
        try {
            return this.has(key) ? this.value.get(key).getAsBoolean() : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }

    int integer(String key, int fallback) {
        try {
            return this.has(key) ? this.value.get(key).getAsInt() : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }

    float number(String key, float fallback) {
        try {
            return this.has(key) ? this.value.get(key).getAsFloat() : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }

    int color(String key, int fallback) {
        try {
            return this.has(key) ? Integer.parseInt(this.value.get(key).getAsString().substring(1), 16) : fallback;
        } catch (Exception var4) {
            return fallback;
        }
    }
}
