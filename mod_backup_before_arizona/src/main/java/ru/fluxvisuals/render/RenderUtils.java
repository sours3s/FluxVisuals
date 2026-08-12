package ru.fluxvisuals.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** 2D-рендер поверх HUD на доступных в 1.21.11 примитивах DrawContext. */
public final class RenderUtils {
    private static final MinecraftClient MC = MinecraftClient.getInstance();

    /** Исходный размер текстуры FluxVisuals icon.png */
    private static final int ICON_TEX_SIZE = 1024;

    /** Отрендерить текстуру из ресурсов мода в заданной области. */
    public static void drawTexture(DrawContext g, Identifier texture, float x, float y, float w, float h) {
        g.drawTexture(RenderPipelines.GUI_TEXTURED, texture,
                (int) x, (int) y, 0f, 0f,
                (int) w, (int) h, ICON_TEX_SIZE, ICON_TEX_SIZE);
    }

    private RenderUtils() {}

    // ---------- прямоугольники ----------

    public static void fill(DrawContext g, float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return;
        g.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
    }

    public static void fillGradientV(DrawContext g, float x, float y, float w, float h, int top, int bottom) {
        if (w <= 0 || h <= 0) return;
        g.fillGradient((int) x, (int) y, (int) (x + w), (int) (y + h), top, bottom);
    }

    public static void fillGradientH(DrawContext g, float x, float y, float w, float h, int left, int right) {
        if (w <= 0 || h <= 0) return;
        int steps = Math.max(1, (int) w);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / steps;
            g.fill((int) (x + i), (int) y, (int) (x + i + 1), (int) (y + h), ColorUtils.mix(left, right, t));
        }
    }

    // ---------- «скруглённые» прямоугольники (заменены ровными, т.к. в 1.21.11 нет GUI-тесселяции) ----------

    public static void drawRoundedRect(DrawContext g, float x, float y, float w, float h, float radius, int color) {
        fill(g, x, y, w, h, color);
    }

    public static void drawRoundedRectGradient(DrawContext g, float x, float y, float w, float h, float radius, int top, int bottom) {
        fillGradientV(g, x, y, w, h, top, bottom);
    }

    public static void drawRoundedRectBordered(DrawContext g, float x, float y, float w, float h, float radius, int fill, int border, float borderWidth) {
        fill(g, x, y, w, h, border);
        fill(g, x + borderWidth, y + borderWidth, w - borderWidth * 2, h - borderWidth * 2, fill);
    }

    public static void drawRoundedOutline(DrawContext g, float x, float y, float w, float h, float radius, float thickness, int color) {
        fill(g, x, y, w, thickness, color);
        fill(g, x, y + h - thickness, w, thickness, color);
        fill(g, x, y + thickness, thickness, h - thickness * 2, color);
        fill(g, x + w - thickness, y + thickness, thickness, h - thickness * 2, color);
    }

    // ---------- круг (приближение квадратом; для маркеров) ----------

    public static void drawCircleOutline(DrawContext g, float cx, float cy, float r, float thickness, int color, int segments) {
        fill(g, cx - r, cy - r, r * 2, r * 2, color);
    }

    // ---------- линии ----------

    /** Произвольный отрезок аппроксимируется цепочкой квадратов (1.21.11 не даёт GUI-линий). */
    public static void drawLine2D(DrawContext g, float x1, float y1, float x2, float y2, float thickness, int color) {
        if (x1 == x2 && y1 == y2) {
            fill(g, x1 - thickness / 2, y1 - thickness / 2, thickness, thickness, color);
            return;
        }
        float dx = x2 - x1, dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        int steps = Math.max(1, (int) (len / 1.5f));
        for (int i = 0; i <= steps; i++) {
            float t = (float) i / steps;
            float px = x1 + dx * t - thickness / 2f;
            float py = y1 + dy * t - thickness / 2f;
            fill(g, px, py, thickness, thickness, color);
        }
    }

    public static void drawHorizontalLine(DrawContext g, float x1, float x2, float y, int color) {
        g.drawHorizontalLine((int) x1, (int) x2, (int) y, color);
    }

    public static void drawVerticalLine(DrawContext g, float x, float y1, float y2, int color) {
        g.drawVerticalLine((int) x, (int) y1, (int) y2, color);
    }

    // ---------- тень (аппроксимация слоями) ----------

    public static void drawShadow(DrawContext g, float x, float y, float w, float h, float radius, int alpha) {
        int layers = 3;
        for (int i = layers - 1; i >= 1; i--) {
            float inset = i * 1.5f;
            int a = alpha / (layers - i + 1);
            fill(g, x - inset, y - inset, w + inset * 2, h + inset * 2, ColorUtils.withAlpha(0x000000, a));
        }
    }

    // ---------- текст ----------

    public static TextRenderer font() { return MC.textRenderer; }

    public static void text(DrawContext g, String text, float x, float y, int color) {
        g.drawText(font(), text, (int) x, (int) y, color, false);
    }

    public static void textShadow(DrawContext g, String text, float x, float y, int color) {
        g.drawTextWithShadow(font(), text, (int) x, (int) y, color);
    }

    public static void textCentered(DrawContext g, String text, float cx, float y, int color) {
        float w = font().getWidth(text);
        g.drawText(font(), text, (int) (cx - w / 2f), (int) y, color, false);
    }

    public static float textWidth(String text) { return font().getWidth(text); }

    public static float textHeight() { return font().fontHeight; }

    // ---------- scissor ----------

    public static void scissor(DrawContext g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
    }

    public static void unScissor(DrawContext g) {
        g.disableScissor();
    }
}
