package ru.fluxvisuals.ui.fluxgui.client.gui.font.icon;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import com.mojang.blaze3d.systems.RenderSystem;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.NativeImage.Format;
import net.minecraft.util.Identifier;

public final class TtfGlyphPage {
    private static final int GLYPH_INK_PADDING_PX = 3;
    private final Map<Character, TtfGlyph> glyphs = new HashMap<>();
    private final char fromIncl;
    private final char toExcl;
    private final Font font;
    public final Identifier bindToTexture;
    private final int pixelPadding;
    public int width;
    public int height;
    private boolean generated = false;

    public TtfGlyphPage(char from, char to, Font font, Identifier identifier, int padding) {
        this.fromIncl = from;
        this.toExcl = to;
        this.font = font;
        this.bindToTexture = identifier;
        this.pixelPadding = padding;
    }

    public TtfGlyph getGlyph(char c) {
        if (!this.generated) {
            this.generate();
        }

        return this.glyphs.get(c);
    }

    public boolean contains(char c) {
        return c >= this.fromIncl && c < this.toExcl;
    }

    private Font getFontForGlyph(char c) {
        return this.font;
    }

    public void generate() {
        if (!this.generated) {
            try {
                int range = this.toExcl - this.fromIncl - 1;
                int charsVert = (int)(Math.ceil(Math.sqrt(range)) * 1.5);
                this.glyphs.clear();
                int generatedChars = 0;
                int charNX = 0;
                int maxX = 0;
                int maxY = 0;
                int currentX = 0;
                int currentY = 0;
                int currentRowMaxY = 0;
                ArrayList<TtfGlyph> glyphList = new ArrayList<>();
                AffineTransform affineTransform = new AffineTransform();
                FontRenderContext fontRenderContext = new FontRenderContext(affineTransform, true, true);

                while (generatedChars <= range) {
                    char currentChar = (char)(this.fromIncl + generatedChars);
                    Font gFont = this.getFontForGlyph(currentChar);
                    GlyphVector gv = gFont.createGlyphVector(fontRenderContext, String.valueOf(currentChar));
                    Rectangle2D vb = gv.getVisualBounds();
                    int gw;
                    int gh;
                    if (vb.getWidth() > 0.0 && vb.getHeight() > 0.0) {
                        gw = (int)Math.ceil(vb.getWidth()) + 6 + 1;
                        gh = (int)Math.ceil(vb.getHeight()) + 6 + 1;
                    } else {
                        Rectangle2D stringBounds = gFont.getStringBounds(String.valueOf(currentChar), fontRenderContext);
                        gw = (int)Math.ceil(stringBounds.getWidth()) + 6 + 1;
                        gh = (int)Math.ceil(stringBounds.getHeight()) + 6 + 1;
                    }

                    generatedChars++;
                    maxX = Math.max(maxX, currentX + gw);
                    maxY = Math.max(maxY, currentY + gh);
                    if (charNX >= charsVert) {
                        currentX = 0;
                        currentY += currentRowMaxY + this.pixelPadding;
                        charNX = 0;
                        currentRowMaxY = 0;
                    }

                    currentRowMaxY = Math.max(currentRowMaxY, gh);
                    glyphList.add(new TtfGlyph(currentX, currentY, gw, gh, currentChar, this));
                    currentX += gw + this.pixelPadding;
                    charNX++;
                }

                BufferedImage bufferedImage = new BufferedImage(Math.max(maxX + this.pixelPadding, 1), Math.max(maxY + this.pixelPadding, 1), 2);
                this.width = bufferedImage.getWidth();
                this.height = bufferedImage.getHeight();
                Graphics2D g2d = bufferedImage.createGraphics();
                g2d.setColor(new Color(255, 255, 255, 0));
                g2d.fillRect(0, 0, this.width, this.height);
                g2d.setColor(Color.WHITE);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                for (TtfGlyph glyph : glyphList) {
                    Font gFontx = this.getFontForGlyph(glyph.value());
                    g2d.setFont(gFontx);
                    GlyphVector gvx = gFontx.createGlyphVector(fontRenderContext, String.valueOf(glyph.value()));
                    Rectangle2D vbx = gvx.getVisualBounds();
                    if (vbx.getWidth() > 0.0 && vbx.getHeight() > 0.0) {
                        float drawX = glyph.u() + 3 - (float)vbx.getX();
                        float drawY = glyph.v() + 3 - (float)vbx.getY();
                        g2d.drawGlyphVector(gvx, drawX, drawY);
                    } else {
                        FontMetrics fontMetrics = g2d.getFontMetrics();
                        g2d.drawString(String.valueOf(glyph.value()), glyph.u() + 3, glyph.v() + 3 + fontMetrics.getAscent());
                    }

                    this.glyphs.put(glyph.value(), glyph);
                }

                g2d.dispose();
                registerBufferedImageTexture(this.bindToTexture, bufferedImage);
            } catch (Throwable var25) {
                FluxVisuals.LOGGER.warn("TtfGlyphPage: failed to generate glyphs: {}", var25.getMessage());
            } finally {
                this.generated = true;
            }
        }
    }

    private static void registerBufferedImageTexture(Identifier textureIdentifier, BufferedImage inputImage) {
        int imageWidth = inputImage.getWidth();
        int imageHeight = inputImage.getHeight();
        NativeImage nativeImage = new NativeImage(Format.RGBA, imageWidth, imageHeight, false);

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                int argb = inputImage.getRGB(x, y);
                nativeImage.setColorArgb(x, y, argb & -16711936 | (argb & 0xFF0000) >> 16 | (argb & 0xFF) << 16);
            }
        }

        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "fluxvisuals", nativeImage);
        texture.upload();
        if (RenderSystem.isOnRenderThread()) {
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureIdentifier, texture);
        } else {
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureIdentifier, texture);
        }
    }
}
