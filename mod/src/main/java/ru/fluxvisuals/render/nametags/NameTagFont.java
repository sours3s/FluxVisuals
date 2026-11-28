package ru.fluxvisuals.render.nametags;

import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.fluxvisuals.api.render.msdf.FontData;
import ru.fluxvisuals.api.render.util.ResourceProvider;

/** MSDF font loaded from the NameTags resource namespace. */
final class NameTagFont {
    private final GpuTextureView atlasView;
    private final FontData.AtlasData atlas;
    private final FontData.MetricsData metrics;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();

    private NameTagFont(FontData data, GpuTextureView atlasView) {
        this.atlasView = atlasView;
        this.atlas = data.atlas();
        this.metrics = data.metrics();
        float atlasWidth = Math.max(1.0F, this.atlas.width());
        float atlasHeight = Math.max(1.0F, this.atlas.height());
        for (FontData.GlyphData glyph : data.glyphs()) {
            this.glyphs.put(glyph.unicode(), new Glyph(glyph, atlasWidth, atlasHeight));
        }
        if (data.kernings() != null) {
            for (FontData.KerningData kerning : data.kernings()) {
                this.kernings.computeIfAbsent(kerning.leftChar(), key -> new HashMap<>())
                      .put(kerning.rightChar(), kerning.advance());
            }
        }
    }

    static NameTagFont load(String key) {
        Identifier dataId = Identifier.of("fluxvisuals", "nametags/fonts/" + key + ".json");
        Identifier atlasId = Identifier.of("fluxvisuals", "nametags/fonts/" + key + ".png");
        FontData data = ResourceProvider.fromJsonToInstance(dataId, FontData.class);
        if (data == null) {
            throw new IllegalStateException("Missing NameTags font data: " + dataId);
        }
        AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(atlasId);
        return new NameTagFont(data, texture.getGlTextureView());
    }

    GpuTextureView atlasView() {
        return this.atlasView;
    }

    float atlasWidth() {
        return Math.max(1.0F, this.atlas.width());
    }

    float atlasHeight() {
        return Math.max(1.0F, this.atlas.height());
    }

    float distanceRange() {
        return this.atlas.range();
    }

    Glyph glyph(int codePoint) {
        return this.glyphs.get(codePoint);
    }

    Float kerning(int left, int right) {
        Map<Integer, Float> kerning = this.kernings.get(left);
        return kerning == null ? null : kerning.get(right);
    }

    float lineHeight() {
        return this.metrics.lineHeight();
    }

    float width(String text, float size) {
        float width = 0.0F;
        int previous = -1;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            Glyph glyph = this.glyphs.get(codePoint);
            if (glyph == null) continue;
            Map<Integer, Float> kerning = this.kernings.get(previous);
            if (kerning != null) {
                width += kerning.getOrDefault(codePoint, 0.0F) * size;
            }
            width += glyph.advance * size;
            previous = codePoint;
        }
        return width;
    }

    void emit(BufferBuilder buffer, Matrix4f matrix, String text, float x, float y,
              float z, float size, int argb, float spacing) {
        float penX = x;
        int previous = -1;
        for (int index = 0; index < text.length();) {
            int codePoint = text.codePointAt(index);
            index += Character.charCount(codePoint);
            Glyph glyph = this.glyphs.get(codePoint);
            if (glyph == null) continue;

            Map<Integer, Float> kerning = this.kernings.get(previous);
            if (kerning != null) {
                penX += kerning.getOrDefault(codePoint, 0.0F) * size;
            }

            float topY = y - glyph.top * size;
            float bottomY = y - glyph.bottom * size;
            float rightX = penX + glyph.width * size;

            buffer.vertex(matrix, penX, topY, z).texture(glyph.u0, glyph.v0).color(argb);
            buffer.vertex(matrix, penX, bottomY, z).texture(glyph.u0, glyph.v1).color(argb);
            buffer.vertex(matrix, rightX, bottomY, z).texture(glyph.u1, glyph.v1).color(argb);
            buffer.vertex(matrix, rightX, topY, z).texture(glyph.u1, glyph.v0).color(argb);

            penX += glyph.advance * size + spacing;
            previous = codePoint;
        }
    }

    static final class Glyph {
        final float advance;
        final float width;
        final float top;
        final float bottom;
        final float u0;
        final float u1;
        final float v0;
        final float v1;

        Glyph(FontData.GlyphData data, float atlasWidth, float atlasHeight) {
            this.advance = data.advance();
            FontData.BoundsData plane = data.planeBounds();
            FontData.BoundsData atlasBounds = data.atlasBounds();
            if (plane != null) {
                this.width = plane.right() - plane.left();
                this.top = plane.top();
                this.bottom = plane.bottom();
            } else {
                this.width = 0.0F;
                this.top = 0.0F;
                this.bottom = 0.0F;
            }
            if (atlasBounds != null) {
                this.u0 = atlasBounds.left() / atlasWidth;
                this.u1 = atlasBounds.right() / atlasWidth;
                this.v0 = 1.0F - atlasBounds.top() / atlasHeight;
                this.v1 = 1.0F - atlasBounds.bottom() / atlasHeight;
            } else {
                this.u0 = 0.0F;
                this.u1 = 0.0F;
                this.v0 = 0.0F;
                this.v1 = 0.0F;
            }
        }
    }
}
