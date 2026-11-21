package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import java.util.Objects;

public final class TtfGlyph {
    int u;
    int v;
    int width;
    int height;
    char value;
    TtfGlyphPage owner;

    public TtfGlyph(int u, int v, int width, int height, char value, TtfGlyphPage owner) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.value = value;
        this.owner = owner;
    }

    public int u() {
        return this.u;
    }

    public int v() {
        return this.v;
    }

    public int width() {
        return this.width;
    }

    public int height() {
        return this.height;
    }

    public char value() {
        return this.value;
    }

    public TtfGlyphPage owner() {
        return this.owner;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof TtfGlyph other)
                ? false
                : Objects.equals(this.u, other.u)
                    && Objects.equals(this.v, other.v)
                    && Objects.equals(this.width, other.width)
                    && Objects.equals(this.height, other.height)
                    && Objects.equals(this.value, other.value)
                    && Objects.equals(this.owner, other.owner);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.u, this.v, this.width, this.height, this.value, this.owner);
    }

    @Override
    public String toString() {
        return "TtfGlyph[u="
            + this.u
            + ", v="
            + this.v
            + ", width="
            + this.width
            + ", height="
            + this.height
            + ", value="
            + this.value
            + ", owner="
            + this.owner
            + "]";
    }
}
