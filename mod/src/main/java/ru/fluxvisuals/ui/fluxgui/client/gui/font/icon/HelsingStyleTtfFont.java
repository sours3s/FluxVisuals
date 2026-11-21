package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import ru.fluxvisuals.ui.fluxgui.compat.FluxRender;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Tex;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

public final class HelsingStyleTtfFont {
    final Map<Identifier, List<HelsingStyleTtfFont$DrawEntry>> glyphPageCache = new HashMap<>();
    final List<TtfGlyphPage> maps = new ArrayList<>();
    Font awtFont;

    public HelsingStyleTtfFont(Font createFontRoot, float logicalSize) {
        try {
            Font half = createFontRoot.deriveFont(logicalSize / 2.0F);
            this.awtFont = half.deriveFont(logicalSize);
        } catch (Throwable var4) {
            this.awtFont = createFontRoot;
        }
    }

    TtfGlyphPage generateMap(char from, char to) {
        TtfGlyphPage page = new TtfGlyphPage(from, to, this.awtFont, TtfIconFonts.nextGlyphTextureId(), 8);
        this.maps.add(page);
        return page;
    }

    TtfGlyph locateGlyph(char glyph) {
        try {
            for (TtfGlyphPage map : this.maps) {
                if (map.contains(glyph)) {
                    return map.getGlyph(glyph);
                }
            }

            char base = (char)floorNearestMulN(glyph, 128);
            return this.generateMap(base, (char)(base + 128)).getGlyph(glyph);
        } catch (Throwable var4) {
            return null;
        }
    }

    static int floorNearestMulN(int x, int n) {
        return n * (int)Math.floor((double)x / n);
    }

    public void drawString(MatrixStack matrix, String text, double x, double y, int color) {
        char[] chars = text.toCharArray();
        float xOffset = 0.0F;
        float yOffset = 0.0F;

        for (char c : chars) {
            TtfGlyph glyph = this.locateGlyph(c);
            if (glyph != null) {
                if (glyph.value() != ' ') {
                    Identifier texId = glyph.owner().bindToTexture;
                    this.glyphPageCache.computeIfAbsent(texId, k -> new ArrayList<>()).add(new HelsingStyleTtfFont$DrawEntry(xOffset, yOffset, color, glyph));
                }

                xOffset += glyph.width();
            }
        }

        if (!this.glyphPageCache.isEmpty()) {
            this.drawGlyphs(matrix, x, y);
        }

        this.glyphPageCache.clear();
    }

    public void drawCenteredString(MatrixStack stack, String s, double x, double y, int color) {
        double left = x - this.getStringWidth(s) / 2.0F;
        this.drawString(stack, s, left, y, color);
    }

    public void drawVerticallyCenteredString(MatrixStack stack, String s, double x, double yCenter, int color) {
        if (s != null && !s.isEmpty()) {
            float maxGh = 0.0F;

            for (char c : s.toCharArray()) {
                if (c != ' ' && c != '\n') {
                    TtfGlyph g = this.locateGlyph(c);
                    if (g != null) {
                        maxGh = Math.max(maxGh, g.height());
                    }
                }
            }

            if (maxGh <= 0.0F) {
                this.drawCenteredString(stack, s, x, yCenter, color);
            } else {
                double y = yCenter + 3.0 - maxGh / 4.0;
                this.drawCenteredString(stack, s, x, y, color);
            }
        }
    }

    void drawGlyphs(MatrixStack matrix, double x, double y) {
        matrix.push();
        matrix.translate(x, y - 3.0, 0.0);
        matrix.scale(0.5F, 0.5F, 1.0F);
        Matrix4f matrix4f = matrix.peek().getPositionMatrix();

        for (Identifier identifier : this.glyphPageCache.keySet()) {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

            for (HelsingStyleTtfFont$DrawEntry drawEntry : this.glyphPageCache.get(identifier)) {
                float x1 = drawEntry.atX();
                float y1 = drawEntry.atY();
                TtfGlyph glyph = drawEntry.glyph();
                TtfGlyphPage glyphMap = glyph.owner();
                float gw = glyph.width();
                float gh = glyph.height();
                float u1 = (float)glyph.u() / glyphMap.width;
                float v1 = (float)glyph.v() / glyphMap.height;
                float u2 = (float)(glyph.u() + glyph.width()) / glyphMap.width;
                float v2 = (float)(glyph.v() + glyph.height()) / glyphMap.height;
                int col = drawEntry.color();
                float cr = (col >> 16 & 0xFF) / 255.0F;
                float cg = (col >> 8 & 0xFF) / 255.0F;
                float cb = (col & 0xFF) / 255.0F;
                float ca = (col >>> 24 & 0xFF) / 255.0F;
                if (ca <= 0.0F) {
                    ca = 1.0F;
                }

                buffer.vertex(matrix4f, x1 + 0.0F, y1 + gh, 0.0F).texture(u1, v2).color(cr, cg, cb, ca);
                buffer.vertex(matrix4f, x1 + gw, y1 + gh, 0.0F).texture(u2, v2).color(cr, cg, cb, ca);
                buffer.vertex(matrix4f, x1 + gw, y1 + 0.0F, 0.0F).texture(u2, v1).color(cr, cg, cb, ca);
                buffer.vertex(matrix4f, x1 + 0.0F, y1 + 0.0F, 0.0F).texture(u1, v1).color(cr, cg, cb, ca);
            }

            FluxRender.drawTexAuto(buffer, identifier, false, false);
        }

        matrix.pop();
    }

    public void drawString(DrawContext context, String text, double x, double y, int color) {
        char[] chars = text.toCharArray();
        float xOffset = 0.0F;
        float yOffset = 0.0F;

        for (char c : chars) {
            TtfGlyph glyph = this.locateGlyph(c);
            if (glyph != null) {
                if (glyph.value() != ' ') {
                    Identifier texId = glyph.owner().bindToTexture;
                    this.glyphPageCache.computeIfAbsent(texId, k -> new ArrayList<>()).add(new HelsingStyleTtfFont$DrawEntry(xOffset, yOffset, color, glyph));
                }

                xOffset += glyph.width();
            }
        }

        if (!this.glyphPageCache.isEmpty()) {
            this.drawGlyphs(context, x, y);
        }

        this.glyphPageCache.clear();
    }

    public void drawCenteredString(DrawContext context, String s, double x, double y, int color) {
        double left = x - this.getStringWidth(s) / 2.0F;
        this.drawString(context, s, left, y, color);
    }

    public void drawVerticallyCenteredString(DrawContext context, String s, double x, double yCenter, int color) {
        if (s != null && !s.isEmpty()) {
            float maxGh = 0.0F;

            for (char c : s.toCharArray()) {
                if (c != ' ' && c != '\n') {
                    TtfGlyph g = this.locateGlyph(c);
                    if (g != null) {
                        maxGh = Math.max(maxGh, g.height());
                    }
                }
            }

            if (maxGh <= 0.0F) {
                this.drawCenteredString(context, s, x, yCenter, color);
            } else {
                double y = yCenter + 3.0 - maxGh / 4.0;
                this.drawCenteredString(context, s, x, y, color);
            }
        }
    }

    public boolean drawVerticallyCenteredChar(DrawContext context, char value, double centerX, double centerY, int color) {
        TtfGlyph glyph = this.locateGlyph(value);
        if (glyph != null && glyph.value() != ' ') {
            float glyphWidth = glyph.width();
            float glyphHeight = glyph.height();
            float drawX = (float)(centerX - glyphWidth / 4.0F);
            float drawY = (float)(centerY - glyphHeight / 4.0F);
            TtfGlyphPage page = glyph.owner();
            float u0 = (float)glyph.u() / page.width;
            float v0 = (float)glyph.v() / page.height;
            float u1 = (float)(glyph.u() + glyph.width()) / page.width;
            float v1 = (float)(glyph.v() + glyph.height()) / page.height;
            int argb = (color >>> 24 & 0xFF) == 0 ? 0xFF000000 | color & 16777215 : color;
            Matrix3x2fStack pose = context.getMatrices();
            pose.pushMatrix();
            pose.translate(drawX, drawY);
            pose.scale(0.5F, 0.5F);
            Gui.tex(context, page.bindToTexture)
                .vertex(0.0F, glyphHeight, u0, v1, argb)
                .vertex(glyphWidth, glyphHeight, u1, v1, argb)
                .vertex(glyphWidth, 0.0F, u1, v0, argb)
                .vertex(0.0F, 0.0F, u0, v0, argb)
                .submit(context);
            pose.popMatrix();
            return true;
        } else {
            return false;
        }
    }

    void drawGlyphs(DrawContext context, double x, double y) {
        Matrix3x2fStack pose = context.getMatrices();
        pose.pushMatrix();
        pose.translate((float)x, (float)(y - 3.0));
        pose.scale(0.5F, 0.5F);

        for (Identifier identifier : this.glyphPageCache.keySet()) {
            Gui$Tex tex = Gui.tex(context, identifier);

            for (HelsingStyleTtfFont$DrawEntry drawEntry : this.glyphPageCache.get(identifier)) {
                float x1 = drawEntry.atX();
                float y1 = drawEntry.atY();
                TtfGlyph glyph = drawEntry.glyph();
                TtfGlyphPage glyphMap = glyph.owner();
                float gw = glyph.width();
                float gh = glyph.height();
                float u1 = (float)glyph.u() / glyphMap.width;
                float v1 = (float)glyph.v() / glyphMap.height;
                float u2 = (float)(glyph.u() + glyph.width()) / glyphMap.width;
                float v2 = (float)(glyph.v() + glyph.height()) / glyphMap.height;
                int col = drawEntry.color();
                if ((col >>> 24 & 0xFF) == 0) {
                    col = 0xFF000000 | col & 16777215;
                }

                tex.vertex(x1, y1 + gh, u1, v2, col).vertex(x1 + gw, y1 + gh, u2, v2, col).vertex(x1 + gw, y1, u2, v1, col).vertex(x1, y1, u1, v1, col);
            }

            tex.submit(context);
        }

        pose.popMatrix();
    }

    public float getStringWidth(String text) {
        float currentLine = 0.0F;
        float maxPreviousLines = 0.0F;

        for (char c : text.toCharArray()) {
            if (c == '\n') {
                maxPreviousLines = Math.max(currentLine, maxPreviousLines);
                currentLine = 0.0F;
            } else {
                TtfGlyph glyph = this.locateGlyph(c);
                currentLine += glyph == null ? 0.0F : glyph.width();
            }
        }

        return Math.max(currentLine, maxPreviousLines) / 2.0F;
    }
}
