package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import java.util.Objects;

public final class TextRender$EmojiPlacement {
    int codepoint;
    float x;
    float y;
    float size;

    public TextRender$EmojiPlacement(int codepoint, float x, float y, float size) {
        this.codepoint = codepoint;
        this.x = x;
        this.y = y;
        this.size = size;
    }

    public int codepoint() {
        return this.codepoint;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float size() {
        return this.size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof TextRender$EmojiPlacement other)
                ? false
                : Objects.equals(this.codepoint, other.codepoint)
                    && Objects.equals(this.x, other.x)
                    && Objects.equals(this.y, other.y)
                    && Objects.equals(this.size, other.size);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.codepoint, this.x, this.y, this.size);
    }

    @Override
    public String toString() {
        return "TextRender$EmojiPlacement[codepoint=" + this.codepoint + ", x=" + this.x + ", y=" + this.y + ", size=" + this.size + "]";
    }
}
