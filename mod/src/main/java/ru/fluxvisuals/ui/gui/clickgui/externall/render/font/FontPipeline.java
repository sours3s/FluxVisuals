package ru.fluxvisuals.ui.gui.clickgui.externall.render.font;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import ru.fluxvisuals.ui.gui.clickgui.externall.render.DrawBatcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorFormatting;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

public class FontPipeline implements DrawBatcher.Batched {

    private static final Identifier PIPELINE_ID = Identifier.of("client", "pipeline/msdf");
    private static final Identifier SHADER_ID = Identifier.of("client", "core/msdf");

    private static final float FIXED_GUI_SCALE = 2.0f;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER_ID)
                    .withFragmentShader(SHADER_ID)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("FontData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build());

    private static final int[] LEGACY_COLORS = new int[32];

    static {
        for (int i = 0; i < 16; ++i) {
            int j = (i >> 3 & 1) * 85;
            int r = (i >> 2 & 1) * 170 + j;
            int g = (i >> 1 & 1) * 170 + j;
            int b = (i & 1) * 170 + j;
            if (i == 6) r += 85;
            LEGACY_COLORS[i] = (255 << 24) | (r << 16) | (g << 8) | b;
            LEGACY_COLORS[i + 16] = ((r & 0xFCFCFC) >> 2 << 24) | (r << 16) | (g << 8) | b;
        }
    }

    private static final String LEGACY_CODE_CHARS = new String(new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            'a', 'b', 'c', 'd', 'e', 'f', 'k', 'l', 'm', 'n', 'o', 'r'
    });

    private static final int MAX_CHARS = 256;
    private static final int BUFFER_SIZE = 64 + MAX_CHARS * 64;

    private static final int PREPROCESS_CACHE_SIZE = 256;
    private static final Map<String, String> preprocessCache = new LinkedHashMap<String, String>(PREPROCESS_CACHE_SIZE + 1, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> eldest) {
            return size() > PREPROCESS_CACHE_SIZE;
        }
    };

    private static final int UNIFORM_RING = 256;

    private boolean sealing = false;
    private GpuBuffer[] uniformBuffers;
    private int uniformRingIndex = 0;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;
    private boolean initialized = false;

    private final Map<GpuTexture, GpuTextureView> textureViewCache = new java.util.HashMap<>();

    private static final int HEADER_SIZE = 64;

    private int batchedChars = 0;
    private FontAtlas currentAtlas = null;
    private float currentOutlineWidth = 0;
    private int currentOutlineColor = 0;

    private void appendGlyph(float x, float y, float w, float h,
                             float u0, float v0, float u1, float v1,
                             int color, float rotation, float pivotX, float pivotY, float glyphScale) {
        if (batchedChars == 0) {
            dataBuffer.clear();
            dataBuffer.position(HEADER_SIZE);
        }

        dataBuffer.putFloat(x);
        dataBuffer.putFloat(y);
        dataBuffer.putFloat(w );
        dataBuffer.putFloat(h);

        dataBuffer.putFloat(u0 );
        dataBuffer.putFloat(v0 );
        dataBuffer.putFloat(u1);
        dataBuffer.putFloat(v1);

        float a = ((color >> 24) & 0xFF) / 255.0f;
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);

        dataBuffer.putFloat(rotation);
        dataBuffer.putFloat(pivotX);
        dataBuffer.putFloat(pivotY);
        dataBuffer.putFloat(glyphScale);

        batchedChars++;
    }

    private int getFixedScaledWidth() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 960;
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getWindow() == null) return 540;
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private void ensureInitialized() {
        if (initialized) return;

        this.dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        this.dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:font_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData);
        MemoryUtil.memFree(dummyData);

        this.uniformBuffers = new GpuBuffer[UNIFORM_RING];

        initialized = true;
    }

    private GpuBuffer nextUniformBuffer() {
        GpuBuffer buf = uniformBuffers[uniformRingIndex];
        if (buf == null) {
            final int idx = uniformRingIndex;
            buf = RenderSystem.getDevice().createBuffer(
                    () -> "client:font_uniform_" + idx,
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    BUFFER_SIZE);
            uniformBuffers[uniformRingIndex] = buf;
        }
        uniformRingIndex = (uniformRingIndex + 1) % UNIFORM_RING;
        return buf;
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color) {
        drawText(atlas, text, x, y, size, color, 0, 0, 0);
    }

    public void drawText(FontAtlas atlas, String text, float x, float y, float size, int color,
                         float outlineWidth, int outlineColor, float rotation) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        text = preprocessText(text);
        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth
                || currentOutlineColor != outlineColor)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float cursorX = x;
        float cursorY = y;

        float rotationRad = (float) Math.toRadians(rotation);

        float pivotX = 0, pivotY = 0;
        if (rotation != 0) {
            pivotX = x + getTextWidth(atlas, text, size) / 2;
            pivotY = y + getTextHeight(atlas, text, size) / 2;
        }

        int currentColor = color;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = tryParseColorCode(text, i, color, currentColor);
            if (colorAdvance.matched()) {
                currentColor = colorAdvance.color();
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += atlas.getLineHeight() * scale;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    cursorX += fallback.xAdvance * scale;
                } else {
                    cursorX += size * 0.5f;
                }
                i += charCount;
                continue;
            }

            float glyphX = cursorX + glyph.xOffset * scale;
            float glyphY = cursorY + glyph.yOffset * scale;
            float glyphW = glyph.width * scale;
            float glyphH = glyph.height * scale;

            if (glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        glyphX, glyphY, glyphW, glyphH,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        currentColor, rotationRad, pivotX, pivotY, scale);
            }

            cursorX += glyph.xAdvance * scale;

            if (batchedChars >= MAX_CHARS) {
                flush();

                currentAtlas = atlas;
            }

            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    public void drawTextFading(FontAtlas atlas, String text, float x, float y, float size,
                               float maxWidth, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        if (getTextWidth(atlas, text, size) <= maxWidth) {
            drawText(atlas, text, x, y, size, color, 0, 0, 0);
            return;
        }

        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha == 0) return;
        int rgb = color & 0xFFFFFF;

        text = preprocessText(text);
        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != 0
                || currentOutlineColor != 0)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = 0;
        currentOutlineColor = 0;

        float scale = size / atlas.getFontSize();
        float currentX = 0;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = trySkipColorCode(text, i);
            if (colorAdvance.matched()) {
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            Glyph glyph = atlas.getGlyph(codePoint);
            float advance;
            if (glyph != null) {
                advance = glyph.xAdvance * scale;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                advance = fallback != null ? fallback.xAdvance * scale : size * 0.5f;
            }

            if (currentX + advance > maxWidth) break;

            if (glyph != null && glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        x + currentX + glyph.xOffset * scale,
                        y + glyph.yOffset * scale,
                        glyph.width * scale, glyph.height * scale,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        (baseAlpha << 24) | rgb, 0, 0, 0, scale);

                if (batchedChars >= MAX_CHARS) {
                    flush();
                    currentAtlas = atlas;
                }
            }

            currentX += advance;
            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    public void drawTextFadingReverse(FontAtlas atlas, String text, float x, float y, float size,
                                      float maxWidth, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        if (getTextWidth(atlas, text, size) <= maxWidth) {
            drawText(atlas, text, x, y, size, color, 0, 0, 0);
            return;
        }

        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha == 0) return;
        int rgb = color & 0xFFFFFF;

        text = preprocessText(text);
        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != 0
                || currentOutlineColor != 0)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = 0;
        currentOutlineColor = 0;

        float scale = size / atlas.getFontSize();
        float totalWidth = getTextWidth(atlas, text, size);
        float skipWidth = totalWidth - maxWidth;
        float skipped = 0f;
        float drawX = 0f;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = trySkipColorCode(text, i);
            if (colorAdvance.matched()) {
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            Glyph glyph = atlas.getGlyph(codePoint);
            float advance;
            if (glyph != null) {
                advance = glyph.xAdvance * scale;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                advance = fallback != null ? fallback.xAdvance * scale : size * 0.5f;
            }

            if (skipped < skipWidth) {
                skipped += advance;
                i += charCount;
                continue;
            }

            if (drawX + advance > maxWidth) break;

            if (glyph != null && glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        x + drawX + glyph.xOffset * scale,
                        y + glyph.yOffset * scale,
                        glyph.width * scale, glyph.height * scale,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        (baseAlpha << 24) | rgb, 0, 0, 0, scale);

                if (batchedChars >= MAX_CHARS) {
                    flush();
                    currentAtlas = atlas;
                }
            }

            drawX += advance;
            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    public void drawTextRightAlignFadeLeft(FontAtlas atlas, String text, float rightX, float y, float size,
                                           float fadeStartX, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        float totalWidth = getTextWidth(atlas, text, size);
        float startX = rightX - totalWidth;

        if (startX >= fadeStartX) {
            drawText(atlas, text, startX, y, size, color, 0, 0, 0);
            return;
        }

        int baseAlpha = (color >> 24) & 0xFF;
        if (baseAlpha == 0) return;
        int rgb = color & 0xFFFFFF;

        text = preprocessText(text);
        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != 0
                || currentOutlineColor != 0)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = 0;
        currentOutlineColor = 0;

        float scale = size / atlas.getFontSize();
        float cursorX = 0f;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = trySkipColorCode(text, i);
            if (colorAdvance.matched()) {
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            Glyph glyph = atlas.getGlyph(codePoint);
            float advance;
            if (glyph != null) {
                advance = glyph.xAdvance * scale;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                advance = fallback != null ? fallback.xAdvance * scale : size * 0.5f;
            }

            float glyphX = startX + cursorX;
            float glyphRight = glyphX + advance;

            if (glyphRight > fadeStartX && glyph != null && glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        glyphX + glyph.xOffset * scale,
                        y + glyph.yOffset * scale,
                        glyph.width * scale, glyph.height * scale,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        (baseAlpha << 24) | rgb, 0, 0, 0, scale);

                if (batchedChars >= MAX_CHARS) {
                    flush();
                    currentAtlas = atlas;
                }
            }

            cursorX += advance;
            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    public void drawTextRotatedAroundPoint(FontAtlas atlas, String text, float x, float y, float size, int color,
                                           float outlineWidth, int outlineColor, float rotation, float pivotX, float pivotY) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() == null) return;
        if (text == null || text.isEmpty()) return;

        text = preprocessText(text);
        text = ColorFormatting.get(text);

        atlas.ensureLoaded();
        if (atlas.getGlyphCount() == 0) return;

        ensureInitialized();

        if (DrawBatcher.isEnabled()) {
            DrawBatcher.register(this);
        }

        if (currentAtlas != null && (currentAtlas != atlas || currentOutlineWidth != outlineWidth
                || currentOutlineColor != outlineColor)) {
            flush();
        }

        currentAtlas = atlas;
        currentOutlineWidth = outlineWidth;
        currentOutlineColor = outlineColor;

        float scale = size / atlas.getFontSize();
        float cursorX = x;
        float cursorY = y;

        float rotationRad = (float) Math.toRadians(rotation);

        int currentColor = color;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = tryParseColorCode(text, i, color, currentColor);
            if (colorAdvance.matched()) {
                currentColor = colorAdvance.color();
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n') {
                cursorX = x;
                cursorY += atlas.getLineHeight() * scale;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph == null) {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    cursorX += fallback.xAdvance * scale;
                } else {
                    cursorX += size * 0.5f;
                }
                i += charCount;
                continue;
            }

            float glyphX = cursorX + glyph.xOffset * scale;
            float glyphY = cursorY + glyph.yOffset * scale;
            float glyphW = glyph.width * scale;
            float glyphH = glyph.height * scale;

            if (glyph.width > 0 && glyph.height > 0) {
                appendGlyph(
                        glyphX, glyphY, glyphW, glyphH,
                        glyph.u0, glyph.v0, glyph.u1, glyph.v1,
                        currentColor, rotationRad, pivotX, pivotY, scale);
            }

            cursorX += glyph.xAdvance * scale;

            if (batchedChars >= MAX_CHARS) {
                flush();

                currentAtlas = atlas;
            }

            i += charCount;
        }

        if (!DrawBatcher.isEnabled()) {
            flush();
        }
    }

    @Override
    public int batchLayer() {
        return 3;
    }

    private record FontChunk(GpuBuffer buffer, GpuTextureView view, int count) {
    }

    private final java.util.ArrayList<FontChunk> chunks = new java.util.ArrayList<>();

    private void sealChunk() {
        if (sealing) return;
        if (batchedChars == 0 || currentAtlas == null) {
            batchedChars = 0;
            currentAtlas = null;
            return;
        }

        sealing = true;
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getFramebuffer() == null) {
                batchedChars = 0;
                currentAtlas = null;
                return;
            }

            AbstractTexture texture = client.getTextureManager().getTexture(currentAtlas.getTextureId());
            if (texture == null) {
                batchedChars = 0;
                currentAtlas = null;
                return;
            }

            GpuTexture gpuTexture;
            try {
                gpuTexture = texture.getGlTexture();
            } catch (Exception e) {
                batchedChars = 0;
                currentAtlas = null;
                return;
            }

            FontAtlas atlasToSeal = currentAtlas;
            int count = batchedChars;
            int endPosition = dataBuffer.position();

            dataBuffer.position(0);
            dataBuffer.putFloat(getFixedScaledWidth());
            dataBuffer.putFloat(getFixedScaledHeight());
            dataBuffer.putFloat(FIXED_GUI_SCALE);
            dataBuffer.putFloat(currentOutlineWidth);

            dataBuffer.putFloat(((currentOutlineColor >> 16) & 0xFF) / 255.0f);
            dataBuffer.putFloat(((currentOutlineColor >> 8) & 0xFF) / 255.0f);
            dataBuffer.putFloat((currentOutlineColor & 0xFF) / 255.0f);
            dataBuffer.putFloat(((currentOutlineColor >> 24) & 0xFF) / 255.0f);

            dataBuffer.putFloat(atlasToSeal.getAtlasWidth());
            dataBuffer.putFloat(atlasToSeal.getAtlasHeight());
            dataBuffer.putFloat(atlasToSeal.getDistanceRange());
            dataBuffer.putFloat(atlasToSeal.getFontSize());

            dataBuffer.putInt(count);
            dataBuffer.putInt(0);
            dataBuffer.putInt(0);
            dataBuffer.putInt(0);

            dataBuffer.position(0);
            dataBuffer.limit(endPosition);

            GpuBuffer uniformBuffer = nextUniformBuffer();
            RenderSystem.getDevice().createCommandEncoder()
                    .writeToBuffer(uniformBuffer.slice(0, dataBuffer.remaining()), dataBuffer);
            dataBuffer.limit(dataBuffer.capacity());

            chunks.add(new FontChunk(uniformBuffer, getCachedTextureView(gpuTexture), count));

            batchedChars = 0;
            currentAtlas = null;

            if (chunks.size() >= UNIFORM_RING - 1) {
                if (DrawBatcher.isEnabled()) {
                    DrawBatcher.flushPending();
                } else {
                    DrawBatcher.drawImmediate(this, false);
                }
            }
        } finally {
            sealing = false;
        }
    }

    @Override
    public void uploadBatch(CommandEncoder encoder) {
        sealChunk();
    }

    @Override
    public void drawBatch(RenderPass pass) {
        if (chunks.isEmpty()) return;

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        pass.setPipeline(PIPELINE);
        pass.setVertexBuffer(0, dummyVertexBuffer);

        for (FontChunk chunk : chunks) {
            if (chunk.view().isClosed()) continue;
            pass.bindTexture("Sampler0", chunk.view(), sampler);
            pass.setUniform("FontData", chunk.buffer());
            pass.draw(0, chunk.count() * 6);
        }

        chunks.clear();
    }

    @Override
    public void discardBatch() {

        chunks.clear();
    }

    public void flush() {
        sealChunk();
        if (!DrawBatcher.isEnabled() && !chunks.isEmpty()) {
            DrawBatcher.drawImmediate(this, false);
        }
    }

    private GpuTextureView getCachedTextureView(GpuTexture gpuTexture) {
        GpuTextureView view = textureViewCache.get(gpuTexture);
        if (view == null || view.isClosed()) {

            textureViewCache.entrySet().removeIf(e -> {
                if (e.getKey().isClosed() || e.getValue().isClosed()) {
                    if (!e.getValue().isClosed()) e.getValue().close();
                    return true;
                }
                return false;
            });
            view = RenderSystem.getDevice().createTextureView(gpuTexture);
            textureViewCache.put(gpuTexture, view);
        }
        return view;
    }

    public float getTextWidth(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();

        text = preprocessText(text);

        Float cached = atlas.widthCache.get(text);
        if (cached != null) {
            return cached * scale;
        }

        String stripped = ColorFormatting.stripForWidth(text);
        Float base = stripped.equals(text) ? null : atlas.widthCache.get(stripped);
        if (base == null) {
            base = computeBaseWidth(atlas, stripped);
            atlas.widthCache.put(stripped, base);
        }
        atlas.widthCache.put(text, base);
        return base * scale;
    }

    private float computeBaseWidth(FontAtlas atlas, String text) {
        float width = 0;
        float maxWidth = 0;

        int i = 0;
        while (i < text.length()) {
            ColorAdvance colorAdvance = trySkipColorCode(text, i);
            if (colorAdvance.matched()) {
                i += colorAdvance.skip();
                continue;
            }

            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);

            if (codePoint == '\n') {
                maxWidth = Math.max(maxWidth, width);
                width = 0;
                i += charCount;
                continue;
            }

            Glyph glyph = atlas.getGlyph(codePoint);
            if (glyph != null) {
                width += glyph.xAdvance;
            } else {
                Glyph fallback = atlas.getGlyph('?');
                if (fallback != null) {
                    width += fallback.xAdvance;
                } else {
                    width += atlas.getFontSize() * 0.5f;
                }
            }

            i += charCount;
        }

        return Math.max(maxWidth, width);
    }

    public float getTextHeight(FontAtlas atlas, String text, float size) {
        atlas.ensureLoaded();
        float scale = size / atlas.getFontSize();
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\n') lines++;
        }
        return lines * atlas.getLineHeight() * scale;
    }

    private static final Map<Integer, Integer> SMALL_CAPS_MAP = new java.util.HashMap<>();
    static {
        SMALL_CAPS_MAP.put(0x1D00, (int)'a');
        SMALL_CAPS_MAP.put(0x0299, (int)'b');
        SMALL_CAPS_MAP.put(0x1D04, (int)'c');
        SMALL_CAPS_MAP.put(0x1D05, (int)'d');
        SMALL_CAPS_MAP.put(0x1D07, (int)'e');
        SMALL_CAPS_MAP.put(0x0493, (int)'f');
        SMALL_CAPS_MAP.put(0x0262, (int)'g');
        SMALL_CAPS_MAP.put(0x029C, (int)'h');
        SMALL_CAPS_MAP.put(0x026A, (int)'i');
        SMALL_CAPS_MAP.put(0x1D0A, (int)'j');
        SMALL_CAPS_MAP.put(0x1D0B, (int)'k');
        SMALL_CAPS_MAP.put(0x029F, (int)'l');
        SMALL_CAPS_MAP.put(0x1D0D, (int)'m');
        SMALL_CAPS_MAP.put(0x0274, (int)'n');
        SMALL_CAPS_MAP.put(0x1D0F, (int)'o');
        SMALL_CAPS_MAP.put(0x1D18, (int)'p');
        SMALL_CAPS_MAP.put(0x01EB, (int)'q');
        SMALL_CAPS_MAP.put(0x0280, (int)'r');
        SMALL_CAPS_MAP.put(0x1D1B, (int)'t');
        SMALL_CAPS_MAP.put(0x1D1C, (int)'u');
        SMALL_CAPS_MAP.put(0x1D20, (int)'v');
        SMALL_CAPS_MAP.put(0x1D21, (int)'w');
        SMALL_CAPS_MAP.put(0x028F, (int)'y');
        SMALL_CAPS_MAP.put(0x1D22, (int)'z');
    }

    private static final int LIGHTNING_CP = "⚡".codePointAt(0);

    private String preprocessText(String text) {
        String cached = preprocessCache.get(text);
        if (cached != null) return cached;

        StringBuilder sb = null;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int cc = Character.charCount(cp);

            if (cp == LIGHTNING_CP) {
                if (sb == null) sb = new StringBuilder(text.substring(0, i));

            } else {
                Integer rep = SMALL_CAPS_MAP.get(cp);
                if (rep != null) {
                    if (sb == null) sb = new StringBuilder(text.substring(0, i));
                    sb.appendCodePoint(rep);
                } else if (sb != null) {
                    sb.appendCodePoint(cp);
                }
            }
            i += cc;
        }

        String result = sb != null ? sb.toString() : text;
        preprocessCache.put(text, result);
        return result;
    }

    private record ColorAdvance(int skip, int color, boolean matched) {
    }

    private ColorAdvance tryParseColorCode(String text, int index, int defaultColor, int currentColor) {
        ColorFormatting.ColorTag tag = ColorFormatting.parseTag(text, index, defaultColor);
        if (tag != null) {
            return new ColorAdvance(tag.length(), tag.color(), true);
        }

        int codePoint = text.codePointAt(index);
        int charCount = Character.charCount(codePoint);
        if ((codePoint == '\u00A7' || codePoint == '&') && index + charCount < text.length()) {
            int nextCodePoint = text.codePointAt(index + charCount);
            if (nextCodePoint == '#' && index + charCount + 6 < text.length()) {
                try {
                    String hex = text.substring(index + charCount + 1, index + charCount + 7);
                    int parsed = (0xFF << 24) | Integer.parseInt(hex, 16);
                    return new ColorAdvance(charCount + 7, parsed, true);
                } catch (Exception ignored) {
                }
            }
            int code = LEGACY_CODE_CHARS.indexOf(Character.toLowerCase((char) nextCodePoint));
            if (code >= 0) {
                int parsed = code < 16 ? LEGACY_COLORS[code] : (code == 21 ? defaultColor : currentColor);
                return new ColorAdvance(charCount + Character.charCount(nextCodePoint), parsed, true);
            }
        }

        return new ColorAdvance(0, currentColor, false);
    }

    private ColorAdvance trySkipColorCode(String text, int index) {
        return tryParseColorCode(text, index, 0, 0);
    }

    public void close() {
        batchedChars = 0;
        currentAtlas = null;
        discardBatch();
        for (GpuTextureView view : textureViewCache.values()) {
            if (!view.isClosed()) view.close();
        }
        textureViewCache.clear();
        if (uniformBuffers != null) {
            for (GpuBuffer buf : uniformBuffers) {
                if (buf != null) buf.close();
            }
            uniformBuffers = null;
        }
        uniformRingIndex = 0;
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        initialized = false;
    }
}
