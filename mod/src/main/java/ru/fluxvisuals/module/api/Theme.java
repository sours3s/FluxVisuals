package ru.fluxvisuals.module.api;

import java.awt.Color;

public enum Theme {
    THEME1,
    HUD,
    TARGET,
    VISUAL,
    MISC,
    NEON;

    public SimpleAnimation animation = new SimpleAnimation();

    public int accent() {
        return switch (this) {
            case THEME1 -> 0xFF4ADE80;
            case HUD -> 0xFF4ADE80;
            case TARGET -> 0xFFFF5A6E;
            case VISUAL -> 0xFF4ADE80;
            case MISC -> 0xFFFACC15;
            case NEON -> 0xFF00FFFF;
        };
    }

    public Color getMain() {
        return new Color(accent(), true);
    }

    public Color getText2() { return new Color(0x7267FF); }
    public Color getText()  { return new Color(0xBBBDFF); }
    public Color getBg()    { return new Color(0x1A1A2E); }
    public Color getBg2()   { return new Color(0x16213E); }
    public Color getOutline() { return new Color(0x7267FF); }

    public static class SimpleAnimation {
        private float output = 1f;

        public void setDirection(Object dir) {}
        public float getOutput() { return output; }
    }
}