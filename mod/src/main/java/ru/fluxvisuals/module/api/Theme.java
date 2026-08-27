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

    public static class SimpleAnimation {
        private float output = 1f;

        public void setDirection(Object dir) {}
        public float getOutput() { return output; }
    }
}