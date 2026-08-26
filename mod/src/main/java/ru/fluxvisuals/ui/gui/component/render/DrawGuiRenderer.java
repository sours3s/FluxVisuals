package ru.fluxvisuals.ui.gui.component.render;

import java.util.ArrayDeque;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix3x2fStack;
import ru.fluxvisuals.util.render.text.FontObject;
import ru.fluxvisuals.util.render.text.TextRenderer;

public class DrawGuiRenderer {

    private final DrawContext dc;
    private final MinecraftClient mc;
    private final ArrayDeque<Float> alphaStack = new ArrayDeque<>();

    public DrawGuiRenderer(DrawContext dc) {
        this.dc = dc;
        this.mc = MinecraftClient.getInstance();
        this.alphaStack.push(1.0F);
    }

    public DrawGuiRenderer() {
        this.dc = null;
        this.mc = MinecraftClient.getInstance();
        this.alphaStack.push(1.0F);
    }

    public DrawContext getDrawContext() { return this.dc; }

    public void begin(int width, int height) {}
    public void end() {}
    public void flush() {}

    // ── Alpha stack ──

    public void pushAlpha(float alpha) {
        float parent = this.alphaStack.isEmpty() ? 1.0F : this.alphaStack.peek();
        this.alphaStack.push(parent * Math.max(0.0F, Math.min(1.0F, alpha)));
    }

    public void popAlpha() {
        if (this.alphaStack.size() > 1) { this.alphaStack.pop(); }
    }

    private float currentAlpha() {
        return this.alphaStack.isEmpty() ? 1.0F : this.alphaStack.peek();
    }

    private int modulateAlpha(int color) {
        float alpha = currentAlpha();
        if (alpha >= 0.999F) return color;
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        a = Math.min(255, Math.round(a * alpha));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int[] unpackColor(int color) {
        return new int[]{
            (color >> 16) & 0xFF,
            (color >> 8) & 0xFF,
            color & 0xFF,
            (color >> 24) & 0xFF
        };
    }

    // ── Rounded rect via Tessellator TRIANGLE_FAN ──

    private void fillRoundedQuad(float x, float y, float w, float h, float rTL, float rTR, float rBR, float rBL, int color) {
        if (this.dc == null) return;
        int[] rgba = unpackColor(color);
        if (rgba[3] == 0) return;

        float maxR = Math.min(Math.min(rTL, rTR), Math.min(rBR, rBL));
        if (maxR < 1.0F) {
            this.dc.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
            return;
        }
        maxR = Math.min(maxR, Math.min(w / 2.0F, h / 2.0F));

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);

        float cx = x + w / 2.0F;
        float cy = y + h / 2.0F;
        buf.vertex(cx, cy, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]);

        int segs = 6;

        // Top-left corner arc
        float tlCx = x + rTL, tlCy = y + rTL;
        float tlR = Math.min(rTL, maxR);
        for (int i = 0; i <= segs; i++) {
            float a = (float) (Math.PI + Math.PI / 2.0 * i / segs);
            buf.vertex(tlCx + tlR * (float) Math.cos(a), tlCy + tlR * (float) Math.sin(a), 0.0F)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        // Top-right corner arc
        float trCx = x + w - rTR, trCy = y + rTR;
        float trR = Math.min(rTR, maxR);
        for (int i = 0; i <= segs; i++) {
            float a = (float) (-Math.PI / 2.0 + Math.PI / 2.0 * i / segs);
            buf.vertex(trCx + trR * (float) Math.cos(a), trCy + trR * (float) Math.sin(a), 0.0F)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        // Bottom-right corner arc
        float brCx = x + w - rBR, brCy = y + h - rBR;
        float brR = Math.min(rBR, maxR);
        for (int i = 0; i <= segs; i++) {
            float a = (float) (0 + Math.PI / 2.0 * i / segs);
            buf.vertex(brCx + brR * (float) Math.cos(a), brCy + brR * (float) Math.sin(a), 0.0F)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        // Bottom-left corner arc
        float blCx = x + rBL, blCy = y + h - rBL;
        float blR = Math.min(rBL, maxR);
        for (int i = 0; i <= segs; i++) {
            float a = (float) (Math.PI / 2.0 + Math.PI / 2.0 * i / segs);
            buf.vertex(blCx + blR * (float) Math.cos(a), blCy + blR * (float) Math.sin(a), 0.0F)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        // Close the fan back to the left edge
        buf.vertex(x, y + rBL, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]);

        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            RenderLayers.debugTriangleFan().draw(built);
        }
    }

    // ── Rect fill ──

    public void rect(float x, float y, float w, float h, int color) {
        this.rect(x, y, w, h, 0, 0, 0, 0, color);
    }

    public void rect(float x, float y, float w, float h, float rounding, int color) {
        this.rect(x, y, w, h, rounding, rounding, rounding, rounding, color);
    }

    public void rect(float x, float y, float w, float h,
                     float rTL, float rTR, float rBR, float rBL, int color) {
        if (this.dc == null) return;
        int c = modulateAlpha(color);
        float maxR = Math.max(Math.max(rTL, rTR), Math.max(rBR, rBL));
        if (maxR < 1.0F) {
            this.dc.fill((int) x, (int) y, (int) (x + w), (int) (y + h), c);
            return;
        }
        fillRoundedQuad(x, y, w, h, rTL, rTR, rBR, rBL, c);
    }

    // ── Rect outline ──

    public void rectOutline(float x, float y, float w, float h, int color, float thickness) {
        this.rectOutline(x, y, w, h, 0, 0, 0, 0, color, thickness);
    }

    public void rectOutline(float x, float y, float w, float h, float rounding, int color, float thickness) {
        this.rectOutline(x, y, w, h, rounding, rounding, rounding, rounding, color, thickness);
    }

    public void rectOutline(float x, float y, float w, float h,
                            float rTL, float rTR, float rBR, float rBL, int color, float thickness) {
        if (this.dc == null) return;
        int c = modulateAlpha(color);
        int t = Math.max(1, (int) thickness);
        int ix = (int) x, iy = (int) y, ix2 = (int) (x + w), iy2 = (int) (y + h);
        this.dc.fill(ix, iy, ix2, iy + t, c);
        this.dc.fill(ix, iy2 - t, ix2, iy2, c);
        this.dc.fill(ix, iy + t, ix + t, iy2 - t, c);
        this.dc.fill(ix2 - t, iy + t, ix2, iy2 - t, c);
    }

    // ── Text ──

    public void text(FontObject fo, float x, float y, float size, String s, int color) {
        if (this.dc == null || s == null || s.isEmpty()) return;
        int c = modulateAlpha(color);
        float defaultSize = this.mc.textRenderer.fontHeight;
        if (Math.abs(size - defaultSize) < 1.0F) {
            this.dc.drawTextWithShadow(this.mc.textRenderer, s, (int) x, (int) y, c);
            return;
        }
        Matrix3x2fStack matrices = this.dc.getMatrices();
        matrices.pushMatrix();
        float scale = size / defaultSize;
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        this.dc.drawTextWithShadow(this.mc.textRenderer, s, 0, 0, c);
        matrices.popMatrix();
    }

    public void text(FontObject fo, float x, float y, float size, String s, int color, String alignKey) {
        this.text(fo, x, y, size, s, color);
    }

    public TextRenderer.TextMetrics measureText(FontObject fo, String text, float size) {
        if (text == null || text.isEmpty()) return new TextRenderer.TextMetrics(0.0F, 0.0F);
        int w = this.mc.textRenderer.getWidth(text);
        float defaultSize = this.mc.textRenderer.fontHeight;
        float scale = size / defaultSize;
        return new TextRenderer.TextMetrics((float) w * scale, size);
    }

    // ── Shadow ──

    public void shadow(float x, float y, float w, float h, float rounding, float blur, float spread, int color) {
        if (this.dc == null) return;
        int a = (color >> 24) & 0xFF;
        if (a <= 0) return;
        for (int i = 3; i >= 1; i--) {
            float off = i * 1.0F;
            int sa = Math.max(1, Math.round(a * 0.12F / i));
            int sc = (sa << 24);
            this.dc.fill((int) (x - off), (int) (y - off), (int) (x + w + off), (int) (y + h + off), sc);
        }
    }

    public void shadow(float x, float y, float w, float h,
                       float rTL, float rTR, float rBR, float rBL,
                       float blur, float spread, int color) {
        shadow(x, y, w, h, 0, blur, spread, color);
    }

    // ── Blur (no-op — needs separate FBO pipeline) ──

    public void prepareBlur(float strength) {}
    public void blur(float x, float y, float w, float h, float rounding) {}
    public void blur(float x, float y, float w, float h, float rounding, float alpha) {}
    public void blur(float x, float y, float w, float h,
                     float rTL, float rTR, float rBR, float rBL, float alpha) {}

    // ── Clip ──

    public void pushRoundedClipRect(float x, float y, float w, float h,
                                    float rTL, float rTR, float rBR, float rBL) {
        if (this.dc == null) return;
        this.dc.enableScissor((int) x, (int) y, (int) (x + w), (int) (y + h));
    }

    public void pushClipRect(int x, int y, int w, int h) {
        if (this.dc == null) return;
        this.dc.enableScissor(x, y, x + w, y + h);
    }

    public void popClipRect() {
        if (this.dc == null) return;
        this.dc.disableScissor();
    }

    // ── Transform (no-ops for DrawContext) ──

    public void pushScale(float scale) {}
    public void pushScale(float sx, float sy) {}
    public void pushScale(float sx, float sy, float centerX, float centerY) {}
    public void popTransform() {}

    // ── Texture ──

    public void drawRgbaTexture(int texture, float x, float y, float w, float h) {}
    public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tint) {}
    public void drawRgbaTexture(int texture, float x, float y, float w, float h, int tint, boolean flip) {}

    // ── Circle ──

    public void circle(float cx, float cy, float radius, float startDeg, float pct, int color) {
        if (this.dc == null) return;
        int c = modulateAlpha(color);
        int[] rgba = unpackColor(c);
        if (rgba[3] == 0) return;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buf = tessellator.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        buf.vertex(cx, cy, 0.0F).color(rgba[0], rgba[1], rgba[2], rgba[3]);

        float startRad = (float) Math.toRadians(startDeg);
        float sweepRad = (float) Math.toRadians(pct * 360.0F);
        int segments = Math.max(12, (int) (Math.abs(pct) * 24));
        for (int i = 0; i <= segments; i++) {
            float a = startRad + sweepRad * i / segments;
            buf.vertex(cx + radius * (float) Math.cos(a), cy + radius * (float) Math.sin(a), 0.0F)
                .color(rgba[0], rgba[1], rgba[2], rgba[3]);
        }

        BuiltBuffer built = buf.endNullable();
        if (built != null) {
            RenderLayers.debugTriangleFan().draw(built);
        }
    }

    // ── Gradient ──

    public void horizontalGradient(float x, float y, float w, float h, int leftColor, int rightColor) {
        if (this.dc == null) return;
        int lc = modulateAlpha(leftColor);
        int rc = modulateAlpha(rightColor);
        int bands = Math.max(2, (int) (w / 2.0F));
        for (int i = 0; i < bands; i++) {
            float t = (float) i / bands;
            float t2 = (float) (i + 1) / bands;
            int c = lerpColor(lc, rc, t, t2);
            int x1 = (int) (x + w * t);
            int x2 = (int) (x + w * t2);
            this.dc.fill(x1, (int) y, x2, (int) (y + h), c);
        }
    }

    public void horizontalGradient(float x, float y, float w, float h, float rounding,
                                   int leftColor, int rightColor) {
        this.horizontalGradient(x, y, w, h, leftColor, rightColor);
    }

    public void horizontalGradient(float x, float y, float w, float h,
                                   float rTL, float rTR, float rBR, float rBL,
                                   int leftColor, int rightColor) {
        this.horizontalGradient(x, y, w, h, leftColor, rightColor);
    }

    public void verticalGradient(float x, float y, float w, float h, int topColor, int bottomColor) {
        if (this.dc == null) return;
        int tc = modulateAlpha(topColor);
        int bc = modulateAlpha(bottomColor);
        int bands = Math.max(2, (int) (h / 2.0F));
        for (int i = 0; i < bands; i++) {
            float t = (float) i / bands;
            float t2 = (float) (i + 1) / bands;
            int c = lerpColor(tc, bc, t, t2);
            int y1 = (int) (y + h * t);
            int y2 = (int) (y + h * t2);
            this.dc.fill((int) x, y1, (int) (x + w), y2, c);
        }
    }

    public void verticalGradient(float x, float y, float w, float h, float rounding,
                                 int topColor, int bottomColor) {
        this.verticalGradient(x, y, w, h, topColor, bottomColor);
    }

    public void verticalGradient(float x, float y, float w, float h,
                                 float rTL, float rTR, float rBR, float rBL,
                                 int topColor, int bottomColor) {
        this.verticalGradient(x, y, w, h, topColor, bottomColor);
    }

    public void gradient(float x, float y, float w, float h,
                         int c00, int c10, int c11, int c01) {
        if (this.dc == null) return;
        // Bilinear interpolation with bands
        int xBands = Math.max(2, (int) (w / 4.0F));
        int yBands = Math.max(2, (int) (h / 4.0F));
        for (int yi = 0; yi < yBands; yi++) {
            float ty = (float) yi / yBands;
            float ty2 = (float) (yi + 1) / yBands;
            int y1 = (int) (y + h * ty);
            int y2 = (int) (y + h * ty2);
            for (int xi = 0; xi < xBands; xi++) {
                float tx = (float) xi / xBands;
                float tx2 = (float) (xi + 1) / xBands;
                int x1 = (int) (x + w * tx);
                int x2 = (int) (x + w * tx2);
                int cTop = lerpColor(c00, c10, tx, tx2);
                int cBot = lerpColor(c01, c11, tx, tx2);
                int c = lerpColor(cTop, cBot, ty, ty2);
                this.dc.fill(x1, y1, x2, y2, c);
            }
        }
    }

    public void gradient(float x, float y, float w, float h, float rounding,
                         int c00, int c10, int c11, int c01) {
        this.gradient(x, y, w, h, c00, c10, c11, c01);
    }

    public void gradient(float x, float y, float w, float h,
                         float rTL, float rTR, float rBR, float rBL,
                         int c00, int c10, int c11, int c01) {
        this.gradient(x, y, w, h, c00, c10, c11, c01);
    }

    private static int lerpColor(int c1, int c2, float t1, float t2) {
        float t = (t1 + t2) / 2.0F;
        int a1 = (c1 >> 24) & 0xFF, r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF, r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int a = Math.round(a1 + (a2 - a1) * t);
        int r = Math.round(r1 + (r2 - r1) * t);
        int g = Math.round(g1 + (g2 - g1) * t);
        int b = Math.round(b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
