package ru.fluxvisuals.ui.gui.clickgui.util.render;
import ru.fluxvisuals.ui.gui.clickgui.util.DrawEngine;
import ru.fluxvisuals.ui.gui.clickgui.util.model.GlyphBounds;
import ru.fluxvisuals.ui.gui.clickgui.util.model.GlyphData;

import org.joml.Matrix4f;

public class RenderedGlyph {
    public final int code;
    public final float minU;
    public final float maxU;
    public final float minV;
    public final float maxV;
    public final float advance;
    public final float bearingY;
    public final float width;
    public final float height;

    public RenderedGlyph(GlyphData glyphData, float f, float f2) {
        this.code = glyphData.unicode();
        this.advance = glyphData.advance();
        GlyphBounds glyphBounds = glyphData.atlasBounds();
        if (glyphBounds != null) {
            this.minU = glyphBounds.left() / f;
            this.maxU = glyphBounds.right() / f;
            this.minV = 1.0f - (glyphBounds.top() / f2);
            this.maxV = 1.0f - (glyphBounds.bottom() / f2);
        } else {
            this.maxV = 0.0f;
            this.minV = 0.0f;
            this.maxU = 0.0f;
            this.minU = 0.0f;
        }
        GlyphBounds glyphBounds295 = glyphData.planeBounds();
        if (glyphBounds295 != null) {
            this.width = glyphBounds295.right() - glyphBounds295.left();
            this.height = glyphBounds295.top() - glyphBounds295.bottom();
            this.bearingY = glyphBounds295.top();
        } else {
            this.bearingY = 0.0f;
            this.height = 0.0f;
            this.width = 0.0f;
        }
    }

    public float apply(Matrix4f matrix4f, DrawEngine drawEngine, float f, float f2, float f3, int i, int i2) {
        drawEngine.textureMsdf(matrix4f, f2, f3 - (this.bearingY * f), this.width * f, this.height * f, this.minU, this.minV, this.maxU, this.maxV, i2, i);
        return this.advance * f;
    }

    public float apply(Matrix4f matrix4f, DrawEngine drawEngine, float f, float f2, float f3, int i, int i2, int i3, int i4, int i5) {
        drawEngine.textureMsdf(matrix4f, f2, f3 - (this.bearingY * f), this.width * f, this.height * f, this.minU, this.minV, this.maxU, this.maxV, i5, i, i2, i3, i4);
        return this.advance * f;
    }

    public float getWidth(float f) {
        return this.advance * f;
    }

    public int getCode() {
        return this.code;
    }
}
