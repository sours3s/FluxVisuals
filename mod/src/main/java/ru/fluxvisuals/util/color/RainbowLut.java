package ru.fluxvisuals.util.color;

public class RainbowLut {
    public static int getColor(float speed) {
        return 0;
    }

    public static int sample(int index, float saturation, float brightness) {
        float hue = (index % 360) / 360.0f;
        return java.awt.Color.HSBtoRGB(hue, saturation, brightness);
    }
}
