package ru.fluxvisuals.util.render;

public class Renderer2D {
    public static class ColorUtil {
        public static int replAlpha(int color, int alpha) {
            return (color & 0xFFFFFF) | (alpha << 24);
        }
        public static int multDark(int color, float factor) {
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            return ((int)(r * factor) << 16) | ((int)(g * factor) << 8) | (int)(b * factor);
        }
        public static int getOutLineColor(int a, int b) {
            return 0xFF00FF00;
        }
        public static int getBackGroundColor(int a, int b) {
            return 0xFF111111;
        }
        public static int setAlpha(int color, int alpha) {
            return (color & 0xFFFFFF) | (alpha << 24);
        }
        public static int getTextTwoColor(int a, int b) {
            return 0xFFFFFFFF;
        }
    }
    public Renderer2D(Object backend) {}
    public void prepareBlur(float radius) {}
    public void blur(float x, float y, float width, float height, float radius, float alpha) {}
    public void rectOutline(float x, float y, float width, float height, float radius, int color, float thickness) {}
    public void rect(float x, float y, float width, float height, float radius, int color) {}
}