package ru.fluxvisuals.ui.gui.clickgui;
import ru.fluxvisuals.ui.gui.clickgui.util.render.GlTexture;


public final class TextureAnimationFrame {
    public final GifFrame frame;
    public final GlTexture texture;

    public TextureAnimationFrame(GifFrame gifFrame, GlTexture glTexture) {
        this.frame = gifFrame;
        this.texture = glTexture;
    }

    public int delay() {
        return this.frame.delay();
    }

        @Override
    public final String toString() {
        return getClass().getSimpleName() + "[" + "frame=" + this.frame + ", " + "texture=" + this.texture + "]";
    }
    @Override
    public final int hashCode() {
        return java.util.Objects.hash(this.frame, this.texture);
    }
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof TextureAnimationFrame)) return false;
        TextureAnimationFrame o = (TextureAnimationFrame) obj;
        return java.util.Objects.equals(this.frame, o.frame) && java.util.Objects.equals(this.texture, o.texture);
    }
public GifFrame frame() {
        return this.frame;
    }

    public GlTexture texture() {
        return this.texture;
    }
}
