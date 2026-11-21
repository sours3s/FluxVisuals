package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import java.util.Objects;

public final class HelsingStyleTtfFont$DrawEntry {
    float atX;
    float atY;
    int color;
    TtfGlyph glyph;

    public HelsingStyleTtfFont$DrawEntry(float atX, float atY, int color, TtfGlyph glyph) {
        this.atX = atX;
        this.atY = atY;
        this.color = color;
        this.glyph = glyph;
    }

    public float atX() {
        return this.atX;
    }

    public float atY() {
        return this.atY;
    }

    public int color() {
        return this.color;
    }

    public TtfGlyph glyph() {
        return this.glyph;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof HelsingStyleTtfFont$DrawEntry other)
                ? false
                : Objects.equals(this.atX, other.atX)
                    && Objects.equals(this.atY, other.atY)
                    && Objects.equals(this.color, other.color)
                    && Objects.equals(this.glyph, other.glyph);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.atX, this.atY, this.color, this.glyph);
    }

    @Override
    public String toString() {
        return "HelsingStyleTtfFont$DrawEntry[atX=" + this.atX + ", atY=" + this.atY + ", color=" + this.color + ", glyph=" + this.glyph + "]";
    }
}
