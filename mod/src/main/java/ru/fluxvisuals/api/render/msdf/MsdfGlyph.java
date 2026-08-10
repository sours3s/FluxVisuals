package ru.fluxvisuals.api.render.msdf;

import ru.fluxvisuals.api.render.system.sys2d.TextureAtlas;
import org.joml.Matrix4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.msdf.FontData.BoundsData;
import ru.fluxvisuals.api.render.msdf.FontData.GlyphData;
import ru.fluxvisuals.api.render.system.ShaderUse;
import ru.fluxvisuals.api.render.system.TextureUse;

import java.awt.*;

public final class MsdfGlyph {

    private final int code;
    private float minU;
    private float maxU;
    private float minV;
    private float maxV;
    private float advance, topPosition, width, height;

    private boolean emojiPatch = false;

    public MsdfGlyph(GlyphData data, float atlasWidth, float atlasHeight) {
        this.code = data.unicode();
        this.advance = data.advance();

        BoundsData atlasBounds = data.atlasBounds();
        if (atlasBounds != null) {
            this.minU = atlasBounds.left() / atlasWidth;
            this.maxU = atlasBounds.right() / atlasWidth;
            this.minV = 1.0F - atlasBounds.top() / atlasHeight;
            this.maxV = 1.0F - atlasBounds.bottom() / atlasHeight;
        } else {
            this.minU = this.maxU = this.minV = this.maxV = 0.0f;
        }

        BoundsData planeBounds = data.planeBounds();
        if (planeBounds != null) {
            this.width = planeBounds.right() - planeBounds.left();
            this.height = planeBounds.top() - planeBounds.bottom();
            this.topPosition = planeBounds.top();
        } else {
            this.width = this.height = this.topPosition = 0.0f;
        }
    }

    public float apply(Matrix4f matrix, float size, float x, float y, float z, Color color1, Color color2, Color color3, Color color4, TextureUse textureUse, MsdfFont emoji) {
        y -= this.topPosition * size;
        float width = this.width * size;
        float height = this.height * size;

        TextureAtlas.UV uv = Client.RENDERER.getCrenderSystem().getAtlas().getUV(textureUse.name().toLowerCase());

        Client.RENDERER.getCrenderSystem().shader(ShaderUse.MSDF)
                .texture(0)
                .rect(x, y, width, height)
                .uv(uv.u0 + minU * uv.u1, uv.v0 + minV * uv.v1, (maxU - minU) * uv.u1, (maxV - minV) * uv.v1)
                .color(color1, color2, color3, color4)
                .build();

        return this.advance * size;
    }


    public float getWidth(float size) {
        return this.advance * size;
    }

    public int getCharCode() {
        return code;
    }

}