package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import java.util.Objects;

public final class TextRender$Glyph {
    float advance;
    float planeLeft;
    float planeTop;
    float planeRight;
    float planeBottom;
    float u0;
    float v0;
    float u1;
    float v1;
    float width;

    public TextRender$Glyph(
        float advance, float planeLeft, float planeTop, float planeRight, float planeBottom, float u0, float v0, float u1, float v1, float width
    ) {
        this.advance = advance;
        this.planeLeft = planeLeft;
        this.planeTop = planeTop;
        this.planeRight = planeRight;
        this.planeBottom = planeBottom;
        this.u0 = u0;
        this.v0 = v0;
        this.u1 = u1;
        this.v1 = v1;
        this.width = width;
    }

    public float advance() {
        return this.advance;
    }

    public float planeLeft() {
        return this.planeLeft;
    }

    public float planeTop() {
        return this.planeTop;
    }

    public float planeRight() {
        return this.planeRight;
    }

    public float planeBottom() {
        return this.planeBottom;
    }

    public float u0() {
        return this.u0;
    }

    public float v0() {
        return this.v0;
    }

    public float u1() {
        return this.u1;
    }

    public float v1() {
        return this.v1;
    }

    public float width() {
        return this.width;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof TextRender$Glyph other)
                ? false
                : Objects.equals(this.advance, other.advance)
                    && Objects.equals(this.planeLeft, other.planeLeft)
                    && Objects.equals(this.planeTop, other.planeTop)
                    && Objects.equals(this.planeRight, other.planeRight)
                    && Objects.equals(this.planeBottom, other.planeBottom)
                    && Objects.equals(this.u0, other.u0)
                    && Objects.equals(this.v0, other.v0)
                    && Objects.equals(this.u1, other.u1)
                    && Objects.equals(this.v1, other.v1)
                    && Objects.equals(this.width, other.width);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.advance, this.planeLeft, this.planeTop, this.planeRight, this.planeBottom, this.u0, this.v0, this.u1, this.v1, this.width);
    }

    @Override
    public String toString() {
        return "TextRender$Glyph[advance="
            + this.advance
            + ", planeLeft="
            + this.planeLeft
            + ", planeTop="
            + this.planeTop
            + ", planeRight="
            + this.planeRight
            + ", planeBottom="
            + this.planeBottom
            + ", u0="
            + this.u0
            + ", v0="
            + this.v0
            + ", u1="
            + this.u1
            + ", v1="
            + this.v1
            + ", width="
            + this.width
            + "]";
    }
}
