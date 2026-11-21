package ru.fluxvisuals.ui.fluxgui.setting;

import java.awt.Color;
import java.util.function.Supplier;

public class ColorSetting extends Setting<Integer> {
    private int red;
    private int green;
    private int blue;
    private int alpha;

    public ColorSetting(String name, int defaultValue) {
        super(name, defaultValue);
        this.updateColorComponents(defaultValue);
    }

    public ColorSetting(String name, Color defaultColor) {
        super(name, defaultColor.getRGB());
        this.updateColorComponents(defaultColor.getRGB());
    }

    public ColorSetting(String name, Color defaultColor, Supplier<Boolean> visibility) {
        super(name, defaultColor.getRGB(), visibility);
        this.updateColorComponents(defaultColor.getRGB());
    }

    public void setValue(Integer value) {
        super.setValue(value);
        this.updateColorComponents(value);
    }

    private void updateColorComponents(int rgb) {
        Color color = new Color(rgb, true);
        this.red = color.getRed();
        this.green = color.getGreen();
        this.blue = color.getBlue();
        this.alpha = color.getAlpha();
    }

    public void setRed(int red) {
        this.red = red;
        this.updateValue();
    }

    public void setGreen(int green) {
        this.green = green;
        this.updateValue();
    }

    public void setBlue(int blue) {
        this.blue = blue;
        this.updateValue();
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
        this.updateValue();
    }

    public void setColor(Color color) {
        this.setValue(color.getRGB());
    }

    public Color getColor() {
        return new Color(this.value, true);
    }

    private void updateValue() {
        this.red = Math.max(0, Math.min(255, this.red));
        this.green = Math.max(0, Math.min(255, this.green));
        this.blue = Math.max(0, Math.min(255, this.blue));
        this.alpha = Math.max(0, Math.min(255, this.alpha));
        this.setValue(new Color(this.red, this.green, this.blue, this.alpha).getRGB());
    }

    public int getRed() {
        return this.red;
    }

    public int getGreen() {
        return this.green;
    }

    public int getBlue() {
        return this.blue;
    }

    public int getAlpha() {
        return this.alpha;
    }
}
