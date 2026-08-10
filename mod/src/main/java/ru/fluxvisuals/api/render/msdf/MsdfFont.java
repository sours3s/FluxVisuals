package ru.fluxvisuals.api.render.msdf;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.textures.GpuTextureView;
import ru.fluxvisuals.MinecraftHolder;
import ru.fluxvisuals.api.render.system.sys2d.CRenderSystem;
import ru.fluxvisuals.api.render.system.sys2d.TextureAtlas;
import ru.fluxvisuals.vse.utils.client.text.ServerPrefix;
import ru.fluxvisuals.vse.utils.client.text.TextUtility;
import ru.fluxvisuals.vse.utils.client.text.EmojiUtility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.GlTexture;
import net.minecraft.client.texture.GlTextureView;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.api.render.util.ResourceProvider;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class MsdfFont implements MinecraftHolder {

    private final String name;
    private final AbstractTexture texture;
    private final FontData.AtlasData atlas;
    private final FontData.MetricsData metrics;
    public final Map<Integer, MsdfGlyph> glyphs;
    public final Map<Integer, Map<Integer, Float>> kernings;

    private final TextureUse textureUse;

    private MsdfFont(String name, AbstractTexture texture, FontData.AtlasData atlas, FontData.MetricsData metrics, Map<Integer, MsdfGlyph> glyphs, Map<Integer, Map<Integer, Float>> kernings, TextureUse textureUse) {
        this.name = name;
        this.texture = texture;
        this.atlas = atlas;
        this.metrics = metrics;
        this.glyphs = glyphs;
        this.kernings = kernings;
        this.textureUse = textureUse;
    }

    public GpuTextureView getTextureId() {
        return this.texture.getGlTextureView();
    }

    public void applyGlyphs(Matrix4f matrix, String text, float size, float thickness, float spacing, float x, float y, float z, Color color1, Color color2, Color color3, Color color4, TextureUse tex) {
        applyGlyphs(matrix, text, size, thickness, spacing, x, y, z, color1, color2, color3, color4, tex, 1);
    }
    public void applyGlyphs(Matrix4f matrix, String text, float size, float thickness, float spacing, float x, float y, float z, Color color, TextureUse tex, float smooth) {
        applyGlyphs(matrix, text, size, thickness, spacing, x, y, z, color, color, color, color, tex, smooth);
    }
    public void applyGlyphs(Matrix4f matrix, String text, float size, float thickness, float spacing, float x, float y, float z, Color color1, Color color2, Color color3, Color color4, TextureUse tex, float smooth) {
        CRenderSystem system = Client.RENDERER.getCrenderSystem();

        system.msdfRange(25).smoothness(smooth, smooth);

        float prevAlpha = system.alpha();

        float width = getWidth(text, size);
        float factor = 20.f;

        float lastAlpha = -1;
        int prevChar = -1;

        text = TextUtility.smallToNormal(text);

        MsdfFont emoji = Client.FONTS.get(TextureUse.EMOJIS);

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);

            int[] rw = ServerPrefix.rwFrom(String.valueOf(ch));
            if (rw[0] != -1 && rw[1] != -1) {
                Identifier id = Identifier.of("godweer", "images/ui/rw_prefix.png");
                TextureAtlas.UV uv = Client.RENDERER.getCrenderSystem().getAtlas().getOrCreate(id.toString(), id);
                if (uv != null) {
                    TextureAtlas atlas = Client.RENDERER.getCrenderSystem().getAtlas();

                    float width_rw = uv.width / 4F;
                    float height_rw  = uv.height / 16F;

                    float aspect = width_rw / height_rw;
                    float size_rw = size + 1;
                    Client.RENDERER.textureRaw(id, x, y - size_rw / 2F - 2, size_rw * aspect, size_rw, 0, new Vector4f(0),
                            new Vector4f((width_rw * rw[0]) / atlas.atlasWidth, (height_rw * rw[1]) / atlas.atlasHeight, width_rw / atlas.atlasWidth, height_rw / atlas.atlasHeight), Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
                }
                continue;
            }

            int _char = (int) TextUtility.charToNormal(ch);
            if (Character.isHighSurrogate(text.charAt(i)) && i + 1 < text.length()) {
                char low = text.charAt(i + 1);
                _char = Character.toCodePoint(ch, low);
            }
            boolean isEmoji = EmojiUtility.isEmoji(_char);

            MsdfGlyph glyph = isEmoji ?
                    emoji.glyphs.get(_char) : this.glyphs.get(_char);

            if (glyph == null) {
                continue;
            }

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                x += kerning.getOrDefault(_char, 0.0f) * size;
            }

            float alpha = 1;

            x += glyph.apply(matrix, size, x, y, z, color1, color2, color3, color4, isEmoji ? TextureUse.EMOJIS : tex, emoji) + thickness + spacing;
            prevChar = _char;

            lastAlpha = alpha;
        }

        system.alpha(prevAlpha);
    }

    public float getHeight(float size) {
        return this.metrics.lineHeight() * size;
    }

    public float getWidth(String text, float size) {
        MsdfFont emoji = Client.FONTS.get(TextureUse.EMOJIS);

        text = TextUtility.smallToNormal(text);

        int prevChar = -1;
        float width = 0.0f;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            int[] rw = ServerPrefix.rwFrom(String.valueOf(ch));
            if (rw[0] != -1 && rw[1] != -1) {
                Identifier id = Identifier.of("godweer", "images/ui/rw_prefix.png");
                TextureAtlas.UV uv = Client.RENDERER.getCrenderSystem().getAtlas().getOrCreate(id.toString(), id);
                if (uv != null) {
                    float aspect = uv.width / uv.height;
                    float scale = 1.0f / size;
                    width += size * 4.5F;
                }
                continue;
            }
            int _char = ch;
            if (Character.isHighSurrogate(text.charAt(i)) && i + 1 < text.length()) {
                char low = text.charAt(i + 1);
                _char = Character.toCodePoint(ch, low);
            }
            boolean isEmoji = EmojiUtility.isEmoji(_char);

            MsdfGlyph glyph = isEmoji ?
                    emoji.glyphs.get(_char) : this.glyphs.get(_char);

            if (glyph == null)
                continue;

            Map<Integer, Float> kerning = this.kernings.get(prevChar);
            if (kerning != null) {
                width += kerning.getOrDefault(_char, 0.0f) * size;
            }

            width += glyph.getWidth(size) + 0.3F;
            prevChar = _char;
        }

        return width;
    }

    public String getName() {
        return this.name;
    }

    public FontData.AtlasData getAtlas() {
        return this.atlas;
    }

    public FontData.MetricsData getMetrics() {
        return this.metrics;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String name = "?";
        private Identifier dataIdentifer;
        private Identifier atlasIdentifier;

        private Builder() {}

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder data(String dataFileName) {
            this.dataIdentifer = Identifier.of("godweer", "fonts/" + dataFileName + ".json");
            return this;
        }

        public Builder atlas(String atlasFileName) {
            this.atlasIdentifier = Identifier.of("godweer", "fonts/" + atlasFileName + ".png");
            return this;
        }

        public MsdfFont build(TextureUse textureUse) {
            FontData data = ResourceProvider.fromJsonToInstance(this.dataIdentifer, FontData.class);
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(this.atlasIdentifier);

            if (data == null) {
                throw new RuntimeException("Failed to read font data file: " + this.dataIdentifer.toString());
            }

            int glId = ((GlTexture)((GlTextureView)texture.getGlTextureView()).texture()).getGlId();
            GlStateManager._bindTexture(glId);

            GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                   GL11.GL_TEXTURE_MIN_FILTER,
                    GL11.GL_NEAREST_MIPMAP_LINEAR
            );
           GL11.glTexParameteri(
                    GL11.GL_TEXTURE_2D,
                    GL11.GL_TEXTURE_MAG_FILTER,
                   GL11.GL_NEAREST
            );

            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);

            float aWidth = data.atlas().width();
            float aHeight = data.atlas().height();
            Map<Integer, MsdfGlyph> glyphs = data.glyphs().stream()
                    .collect(Collectors.toMap(
                            FontData.GlyphData::unicode,
                            (glyphData) -> new MsdfGlyph(glyphData, aWidth, aHeight)
                    ));

            Map<Integer, Map<Integer, Float>> kernings = new HashMap<>();
            data.kernings().forEach((kerning) -> {
                kernings.computeIfAbsent(kerning.leftChar(), k -> new HashMap<>())
                        .put(kerning.rightChar(), kerning.advance());
            });

            Client.RENDERER.getCrenderSystem().putTex(textureUse, glId);

            return new MsdfFont(this.name, texture, data.atlas(), data.metrics(), glyphs, kernings, textureUse);
        }

    }

}