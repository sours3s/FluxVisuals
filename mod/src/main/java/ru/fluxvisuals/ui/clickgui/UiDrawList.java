package ru.fluxvisuals.ui.clickgui;

import java.util.ArrayList;
import java.util.List;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

public final class UiDrawList {
    private final List<DrawCommand> commands = new ArrayList<>();

    @FunctionalInterface
    public interface DrawCommand {
        void draw(Renderer2D r);
    }

    public void render(Renderer2D r) {
        for (DrawCommand cmd : commands) {
            cmd.draw(r);
        }
    }

    public UiDrawList rect(float x, float y, float w, float h, int color) {
        if (w <= 0 || h <= 0) return this;
        commands.add(r -> r.rect(x, y, w, h, color));
        return this;
    }

    public UiDrawList roundedRect(float x, float y, float w, float h, float radius, int color) {
        if (w <= 0 || h <= 0) return this;
        commands.add(r -> r.rect(x, y, w, h, radius, color));
        return this;
    }

    public UiDrawList outlineRoundedRect(float x, float y, float w, float h, float radius, float thickness, int color) {
        if (w <= 0 || h <= 0) return this;
        commands.add(r -> r.rectOutline(x, y, w, h, radius, color, thickness));
        return this;
    }

    public UiDrawList circle(float cx, float cy, float radius, int color) {
        if (radius <= 0) return this;
        commands.add(r -> r.circle(cx, cy, radius, 0f, 1.0f, color));
        return this;
    }

    public UiDrawList line(float x1, float y1, float x2, float y2, float thickness, int color) {
        if (thickness <= 0) return this;
        commands.add(r -> {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float len = (float) Math.hypot(dx, dy);
            if (len <= 0.001f) return;
            if (Math.abs(dy) < 0.001f) {
                float minX = Math.min(x1, x2);
                r.rect(minX, y1 - thickness * 0.5f, len, thickness, color);
            } else if (Math.abs(dx) < 0.001f) {
                float minY = Math.min(y1, y2);
                r.rect(x1 - thickness * 0.5f, minY, thickness, len, color);
            } else {
                r.rect(Math.min(x1, x2), Math.min(y1, y2), Math.max(thickness, Math.abs(dx)), Math.max(thickness, Math.abs(dy)), color);
            }
        });
        return this;
    }

    public UiDrawList gradientRoundedRect(float x, float y, float w, float h, float radius, int c1, int c2, boolean horizontal) {
        if (w <= 0 || h <= 0) return this;
        if (horizontal) {
            commands.add(r -> r.horizontalGradient(x, y, w, h, radius, c1, c2));
        } else {
            commands.add(r -> r.verticalGradient(x, y, w, h, radius, c1, c2));
        }
        return this;
    }

    public UiDrawList pushScissor(float x, float y, float w, float h) {
        commands.add(r -> r.pushClipRect((int) Math.floor(x), (int) Math.floor(y), (int) Math.ceil(w), (int) Math.ceil(h)));
        return this;
    }

    public UiDrawList popScissor() {
        commands.add(Renderer2D::popClipRect);
        return this;
    }

    public UiDrawList text(float x, float cy, float size, int color, String text) {
        if (text == null || text.isEmpty() || size <= 0) return this;
        commands.add(r -> r.text(FontRegistry.INTER_MEDIUM, x, cy - size * 0.25f, size * 2.0f, text, color));
        return this;
    }

    public UiDrawList strongText(float x, float cy, float size, int color, String text) {
        if (text == null || text.isEmpty() || size <= 0) return this;
        commands.add(r -> r.text(FontRegistry.INTER_SEMIBOLD, x, cy - size * 0.25f, size * 2.0f, text, color));
        return this;
    }

    public UiDrawList centeredText(float cx, float cy, float size, int color, String text, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return this;
        commands.add(r -> {
            var font = bold ? FontRegistry.INTER_SEMIBOLD : FontRegistry.INTER_MEDIUM;
            float tw = r.measureText(font, text, size * 2.0f).width;
            r.text(font, cx - tw * 0.5f, cy - size * 0.25f, size * 2.0f, text, color);
        });
        return this;
    }

    public UiDrawList rightText(float rx, float cy, float size, int color, String text, boolean bold) {
        if (text == null || text.isEmpty() || size <= 0) return this;
        commands.add(r -> {
            var font = bold ? FontRegistry.INTER_SEMIBOLD : FontRegistry.INTER_MEDIUM;
            float tw = r.measureText(font, text, size * 2.0f).width;
            r.text(font, rx - tw, cy - size * 0.25f, size * 2.0f, text, color);
        });
        return this;
    }

    public UiDrawList icon(float cx, float cy, float size, int color, int codepoint) {
        if (size <= 0) return this;
        commands.add(r -> {
            String str = new String(Character.toChars(codepoint));
            float tw = r.measureText(FontRegistry.ICONS, str, size * 2.0f).width;
            r.text(FontRegistry.ICONS, cx - (tw > 0 ? tw * 0.5f : size * 0.5f), cy - size * 0.25f, size * 2.0f, str, color);
        });
        return this;
    }

    public UiDrawList triangle(float x1, float y1, float x2, float y2, float x3, float y3, int color) {
        commands.add(r -> {
            float minX = Math.min(x1, Math.min(x2, x3));
            float maxX = Math.max(x1, Math.max(x2, x3));
            float minY = Math.min(y1, Math.min(y2, y3));
            float maxY = Math.max(y1, Math.max(y2, y3));
            r.rect(minX, minY, Math.max(1f, maxX - minX), Math.max(1f, maxY - minY), color);
        });
        return this;
    }
}
