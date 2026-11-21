package ru.fluxvisuals.ui.fluxgui.module.render;

/** Данные одной метки мира для GUI FluxVisuals. */
public final class Markers$MarkerData {
    private final String name;
    private final double x;
    private final double y;
    private final double z;
    private final char iconChar;
    private final int iconBackgroundColor;
    private final int textColor;
    private final int iconColor;
    private final int iconAlpha;
    private final int background;

    public Markers$MarkerData(String name, double x, double y, double z, char iconChar,
                              int iconBackgroundColor, int textColor, int iconColor,
                              int iconAlpha, float size, int background) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.z = z;
        this.iconChar = iconChar;
        this.iconBackgroundColor = iconBackgroundColor;
        this.textColor = textColor;
        this.iconColor = iconColor;
        this.iconAlpha = iconAlpha;
        this.background = background;
    }

    public String getName() { return name; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public char getIconChar() { return iconChar; }
    public int getIconColor() { return iconColor; }
    public int getTextColor() { return textColor; }
    public int getIconBackgroundColor() { return iconBackgroundColor; }
}
