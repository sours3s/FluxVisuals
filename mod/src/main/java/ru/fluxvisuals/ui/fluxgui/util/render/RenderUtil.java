package ru.fluxvisuals.ui.fluxgui.util.render;

import ru.fluxvisuals.ui.fluxgui.FluxVisuals;
import ru.fluxvisuals.ui.fluxgui.client.gui.font.icon.IconTtfBridge;
import ru.fluxvisuals.ui.fluxgui.client.gui.render.RoundedRenderer;
import ru.fluxvisuals.ui.fluxgui.compat.FluxRender;
import ru.fluxvisuals.ui.fluxgui.compat.Gui;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Color;
import ru.fluxvisuals.ui.fluxgui.compat.Gui$Tex;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class RenderUtil {
    public static final int MAX_GUI_ICON_SOURCE_SIZE = 128;
    private static final float[] TINT = new float[]{1.0F, 1.0F, 1.0F, 1.0F};
    private static final Map<Identifier, Identifier> WHITE_MASK_CACHE = new HashMap<>();
    private static final Set<Identifier> MIPMAPPED_TEXTURES = new HashSet<>();
    private static int fboA = -1;
    private static int fboB = -1;
    private static int texA = -1;
    private static int texB = -1;
    private static int blurW;
    private static int blurH;
    private static boolean blurReady = false;
    private static boolean blurPreparePending;
    private static int pendingBlurPassCount;
    private static int pendingBlurDownscale;
    private static boolean blurFailureLogged;
    private static float nativeBlurRadius = 6.0F;
    public static final int BLUR_DOWNSCALE = 3;
    private static final int DOWNSCALE = 3;
    private static final int LIGHT_BLUR_DOWNSCALE = 4;
    private static final int GUI_BACKGROUND_BLUR_BASE = 5;

    public static void shaderColor(float r, float g, float b, float a) {
        TINT[0] = r;
        TINT[1] = g;
        TINT[2] = b;
        TINT[3] = a;
    }

    public static float[] shaderColor() {
        return TINT;
    }

    public static void resetShaderColor() {
        TINT[0] = 1.0F;
        TINT[1] = 1.0F;
        TINT[2] = 1.0F;
        TINT[3] = 1.0F;
    }

    public static void drawBuffer(BufferBuilder buffer) {
        if (buffer != null) {
            FluxRender.drawColorAuto(buffer, false, false);
        }
    }

    public static void closeBuffer(BufferBuilder buffer) {
        if (buffer != null) {
            try {
                buffer.endNullable();
            } catch (Exception var2) {
            }
        }
    }

    public static void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        context.fill(x, y, x + width, y + 1, color);
        context.fill(x, y + height - 1, x + width, y + height, color);
        context.fill(x, y + 1, x + 1, y + height - 1, color);
        context.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    public static Identifier getWhiteMaskTexture(Identifier original) {
        Identifier cached = WHITE_MASK_CACHE.get(original);
        if (cached != null) {
            return cached;
        }

        Identifier maskId = Identifier.of(original.getNamespace(), "generated/white_mask/" + original.getPath().replace('/', '_'));

        try (InputStream stream = MinecraftClient.getInstance().getResourceManager().open(original)) {
            NativeImage image = downscaleNativeImageMaxSide(NativeImage.read(stream), 128);
            int width = image.getWidth();
            int height = image.getHeight();

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int color = image.getColorArgb(x, y);
                    int alpha = color >>> 24 & 0xFF;
                    float a = alpha / 255.0F;
                    a = a > 0.35F ? 1.0F : 0.0F;
                    int newAlpha = (int)(a * 255.0F);
                    int white = newAlpha << 24 | 16777215;
                    image.setColorArgb(x, y, white);
                }
            }

            NativeImageBackedTexture maskTexture = new NativeImageBackedTexture(() -> "fluxvisuals", image);
            MinecraftClient.getInstance().getTextureManager().registerTexture(maskId, maskTexture);
            maskTexture.upload();
            WHITE_MASK_CACHE.put(original, maskId);
            bindAndGenerateMipmaps(maskTexture);
            MIPMAPPED_TEXTURES.add(maskId);
            return maskId;
        } catch (IOException e) {
            ensureLinearFilter(original);
            return original;
        }
    }

    public static void onResourcesReloaded() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            for (Identifier id : new ArrayList<>(WHITE_MASK_CACHE.values())) {
                try {
                    mc.getTextureManager().destroyTexture(id);
                } catch (Exception var4) {
                }
            }
        }

        WHITE_MASK_CACHE.clear();
        MIPMAPPED_TEXTURES.clear();
    }

    private static NativeImage downscaleNativeImageMaxSide(NativeImage src, int maxSide) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w <= maxSide && h <= maxSide) {
            return src;
        }

        float sc = Math.min((float)maxSide / w, (float)maxSide / h);
        int nw = Math.max(1, Math.round(w * sc));
        int nh = Math.max(1, Math.round(h * sc));
        NativeImage out = new NativeImage(nw, nh, false);

        for (int y = 0; y < nh; y++) {
            for (int x = 0; x < nw; x++) {
                float sx = (x + 0.5F) * w / nw - 0.5F;
                float sy = (y + 0.5F) * h / nh - 0.5F;
                out.setColorArgb(x, y, sampleBilinearArgb(src, sx, sy));
            }
        }

        src.close();
        return out;
    }

    private static int sampleBilinearArgb(NativeImage img, float fx, float fy) {
        int w = img.getWidth();
        int h = img.getHeight();
        if (w <= 0 || h <= 0) {
            return 0;
        }

        if (w == 1 && h == 1) {
            return img.getColorArgb(0, 0);
        }

        fx = Math.max(0.0F, Math.min(w - 1.0001F, fx));
        fy = Math.max(0.0F, Math.min(h - 1.0001F, fy));
        int x0 = (int)fx;
        int y0 = (int)fy;
        int x1 = Math.min(x0 + 1, w - 1);
        int y1 = Math.min(y0 + 1, h - 1);
        float tx = fx - x0;
        float ty = fy - y0;
        int c00 = img.getColorArgb(x0, y0);
        int c10 = img.getColorArgb(x1, y0);
        int c01 = img.getColorArgb(x0, y1);
        int c11 = img.getColorArgb(x1, y1);
        return lerpArgb2d(c00, c10, c01, c11, tx, ty);
    }

    private static int lerpArgb2d(int c00, int c10, int c01, int c11, float tx, float ty) {
        return lerpArgb(lerpArgb(c00, c10, tx), lerpArgb(c01, c11, tx), ty);
    }

    private static int lerpArgb(int a, int b, float t) {
        int aa = a >>> 24 & 0xFF;
        int ar = a >> 16 & 0xFF;
        int ag = a >> 8 & 0xFF;
        int ab = a & 0xFF;
        int ba = b >>> 24 & 0xFF;
        int br = b >> 16 & 0xFF;
        int bg = b >> 8 & 0xFF;
        int bb = b & 0xFF;
        int oa = Math.round(aa + (ba - aa) * t);
        int or = Math.round(ar + (br - ar) * t);
        int og = Math.round(ag + (bg - ag) * t);
        int ob = Math.round(ab + (bb - ab) * t);
        return oa << 24 | or << 16 | og << 8 | ob;
    }

    public static void ensureLinearFilter(Identifier textureId) {
    }

    private static void bindAndGenerateMipmaps(AbstractTexture texture) {
    }

    public static void ensureMipmapFilter(Identifier textureId) {
        if (!MIPMAPPED_TEXTURES.contains(textureId)) {
            AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
            if (texture != null) {
                MIPMAPPED_TEXTURES.add(textureId);
                bindAndGenerateMipmaps(texture);
            }
        }
    }

    public static int getGameColorFramebufferId(MinecraftClient mc) {
        return GL30.glGetInteger(36006);
    }

    private static int effectiveBlurPassCount(int requested) {
        int r = Math.max(1, requested);
        return Math.max(4, Math.min(16, r + 3));
    }

    private static int effectiveLightBlurPassCount(int requested) {
        int r = Math.max(1, requested);
        return Math.max(2, Math.min(6, r));
    }

    public static void prepareBlurLight(int passes) {
        nativeBlurRadius = Math.max(1.0F, Math.min(18.0F, passes * 1.5F));
        blurReady = true;
    }

    public static void prepareBlurGuiBackground() {
        nativeBlurRadius = 7.5F;
        blurReady = true;
    }

    private static void prepareNativeBlurFrame() {
        try {
            MirageGlassPipeline.captureFrame();
            blurReady = true;
            blurPreparePending = false;
            blurFailureLogged = false;
        } catch (Throwable error) {
            blurReady = false;
            if (!blurFailureLogged) {
                blurFailureLogged = true;
                FluxVisuals.LOGGER.error("Failed to capture the frame for GUI blur", error);
            }
        }
    }

    private static void runBlurPrepare(int passCount, int downscaleDivisor) {
        pendingBlurPassCount = Math.max(1, passCount);
        pendingBlurDownscale = Math.max(1, downscaleDivisor);
        blurPreparePending = true;
        blurReady = false;
    }

    private static boolean ensureBlurPrepared(DrawContext context) {
        if (!RawTextureRenderer.isSupported()) {
            return false;
        }

        try {
            if (blurPreparePending) {
                executeBlurPrepare(pendingBlurPassCount, pendingBlurDownscale);
                blurPreparePending = false;
            }

            return blurReady && fboA > 0 && texA > 0;
        } catch (Throwable error) {
            blurReady = false;
            blurPreparePending = false;
            if (!blurFailureLogged) {
                blurFailureLogged = true;
                FluxVisuals.LOGGER.error("Failed to prepare the GUI framebuffer blur", error);
            }

            return false;
        }
    }

    private static void executeBlurPrepare(int passCount, int downscaleDivisor) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int fw = mc.getWindow().getFramebufferWidth();
        int fh = mc.getWindow().getFramebufferHeight();
        if (fw > 0 && fh > 0) {
            int div = Math.max(1, downscaleDivisor);
            int newW = Math.max(1, fw / div);
            int newH = Math.max(1, fh / div);
            int savedDrawFbo = GL30.glGetInteger(36006);
            int savedReadFbo = GL30.glGetInteger(36010);
            int readGame = getGameColorFramebufferId(mc);
            boolean scissorWasOn = GL11.glIsEnabled(3089);

            try {
                if (scissorWasOn) {
                    GL11.glDisable(3089);
                }

                if (fboA == -1 || blurW != newW || blurH != newH) {
                    destroyBlur();
                    blurW = newW;
                    blurH = newH;
                    createBlurResources(savedDrawFbo);
                }

                if (fboA > 0 && fboB > 0) {
                    GL30.glBindFramebuffer(36008, readGame);
                    GL30.glBindFramebuffer(36009, fboA);
                    GL30.glBlitFramebuffer(0, 0, fw, fh, 0, 0, blurW, blurH, 16384, 9729);
                    int halfW = Math.max(1, blurW / 2);
                    int halfH = Math.max(1, blurH / 2);

                    for (int i = 0; i < passCount; i++) {
                        GL30.glBindFramebuffer(36008, fboA);
                        GL30.glBindFramebuffer(36009, fboB);
                        GL30.glBlitFramebuffer(0, 0, blurW, blurH, 0, 0, halfW, halfH, 16384, 9729);
                        GL30.glBindFramebuffer(36008, fboB);
                        GL30.glBindFramebuffer(36009, fboA);
                        GL30.glBlitFramebuffer(0, 0, halfW, halfH, 0, 0, blurW, blurH, 16384, 9729);
                    }

                    blurReady = true;
                    blurFailureLogged = false;
                    return;
                }

                blurReady = false;
            } catch (Throwable error) {
                blurReady = false;
                if (!blurFailureLogged) {
                    blurFailureLogged = true;
                    FluxVisuals.LOGGER.error("Failed to build the GUI framebuffer blur", error);
                }

                return;
            } finally {
                if (scissorWasOn) {
                    GL11.glEnable(3089);
                }

                GL30.glBindFramebuffer(36009, savedDrawFbo);
                GL30.glBindFramebuffer(36008, savedReadFbo);
            }
        } else {
            blurReady = false;
        }
    }

    public static void prepareBlur(int passes) {
        nativeBlurRadius = Math.max(1.0F, Math.min(24.0F, passes * 1.5F));
        blurReady = true;
    }

    public static void drawBlurBehind(DrawContext context, float x, float y, float w, float h) {
        drawRoundedBlurBehind(context, x, y, w, h, 0.0F, 1.0F);
    }

    public static void drawBlur(DrawContext context, float x, float y, float w, float h, int passes) {
        prepareBlur(passes);
        drawRoundedBlurBehind(context, x, y, w, h, 0.0F, 1.0F);
    }

    public static void drawRoundedBlur(DrawContext context, float x, float y, float w, float h, float radius, int passes) {
        prepareBlur(passes);
        drawRoundedBlurBehind(context, x, y, w, h, radius, 1.0F);
    }

    public static void drawRoundedBlurBehind(DrawContext context, float x, float y, float w, float h, float radius) {
        drawRoundedBlurBehind(context, x, y, w, h, radius, 1.0F);
    }

    public static void drawRoundedBlurBehind(DrawContext context, float x, float y, float w, float h, float radius, float alpha) {
        float a = MathHelper.clamp(alpha, 0.0F, 1.0F);
        if (!(a <= 0.0F) && !(w <= 0.0F) && !(h <= 0.0F)) {
            try {
                MirageGlassPipeline.captureFrame();
                MirageGlassPipeline.drawBlurAt(context, x, y, w, h, Math.max(0.0F, radius), nativeBlurRadius, a);
                blurReady = true;
                blurFailureLogged = false;
            } catch (Throwable error) {
                blurReady = false;
                if (!blurFailureLogged) {
                    blurFailureLogged = true;
                    FluxVisuals.LOGGER.error("Failed to render GPU GUI blur", error);
                }
            }
        }
    }

    public static void destroyBlur() {
        blurReady = false;
        blurPreparePending = false;
        if (fboA != -1) {
            GL30.glDeleteFramebuffers(fboA);
            fboA = -1;
        }

        if (fboB != -1) {
            GL30.glDeleteFramebuffers(fboB);
            fboB = -1;
        }

        if (texA != -1) {
            GL11.glDeleteTextures(texA);
            texA = -1;
        }

        if (texB != -1) {
            GL11.glDeleteTextures(texB);
            texB = -1;
        }

        blurW = 0;
        blurH = 0;
    }

    private static void createBlurResources(int restoreFbo) {
        int previousTexture = GL11.glGetInteger(32873);
        texA = GL11.glGenTextures();
        GL11.glBindTexture(3553, texA);
        GL11.glTexImage2D(3553, 0, 32856, blurW, blurH, 0, 6408, 5121, (ByteBuffer)null);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glBindTexture(3553, 0);
        fboA = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(36160, fboA);
        GL30.glFramebufferTexture2D(36160, 36064, 3553, texA, 0);
        if (GL30.glCheckFramebufferStatus(36160) != 36053) {
            throw new IllegalStateException("GUI blur framebuffer A is incomplete");
        }

        GL30.glBindFramebuffer(36160, restoreFbo);
        texB = GL11.glGenTextures();
        GL11.glBindTexture(3553, texB);
        GL11.glTexImage2D(3553, 0, 32856, blurW, blurH, 0, 6408, 5121, (ByteBuffer)null);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glBindTexture(3553, 0);
        fboB = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(36160, fboB);
        GL30.glFramebufferTexture2D(36160, 36064, 3553, texB, 0);
        if (GL30.glCheckFramebufferStatus(36160) != 36053) {
            throw new IllegalStateException("GUI blur framebuffer B is incomplete");
        }

        GL11.glBindTexture(3553, previousTexture);
        GL30.glBindFramebuffer(36160, restoreFbo);
    }

    public static void drawHighQualityIcon(DrawContext context, Identifier texture, int x, int y, int size) {
        drawHighQualityIcon(context, texture, (float)x, (float)y, (float)size, (float)size);
    }

    public static void drawHighQualityIcon(DrawContext context, Identifier texture, int x, int y, int width, int height) {
        drawHighQualityIcon(context, texture, (float)x, (float)y, (float)width, (float)height);
    }

    public static void drawHighQualityIcon(DrawContext context, Identifier texture, float x, float y, float size) {
        drawHighQualityIcon(context, texture, x, y, size, size);
    }

    private static int shaderColorToArgb(float[] sc) {
        int r = Math.min(255, Math.max(0, Math.round(sc[0] * 255.0F)));
        int g = Math.min(255, Math.max(0, Math.round(sc[1] * 255.0F)));
        int b = Math.min(255, Math.max(0, Math.round(sc[2] * 255.0F)));
        int a = Math.min(255, Math.max(0, Math.round(sc[3] * 255.0F)));
        return a << 24 | r << 16 | g << 8 | b;
    }

    public static void drawIconTexturedSharp(DrawContext context, Identifier texture, float x, float y, float width, float height) {
        drawIconTexturedSharpColored(context, texture, x, y, width, height, shaderColorToArgb(shaderColor()));
    }

    public static void drawIconTexturedSharpColored(DrawContext context, Identifier texture, float x, float y, float width, float height, int argb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            float rx = Math.round(x);
            float ry = Math.round(y);
            float rw = Math.max(1.0F, Math.round(width));
            float rh = Math.max(1.0F, Math.round(height));
            int col = argb;
            if ((argb >>> 24 & 0xFF) == 0) {
                col = 0xFF000000 | argb & 16777215;
            }

            Gui.tex(context, texture)
                .vertex(rx, ry + rh, 0.0F, 1.0F, col)
                .vertex(rx + rw, ry + rh, 1.0F, 1.0F, col)
                .vertex(rx + rw, ry, 1.0F, 0.0F, col)
                .vertex(rx, ry, 0.0F, 0.0F, col)
                .submit(context);
        }
    }

    public static void drawGuiTexture(DrawContext context, Identifier id, float x, float y, float w, float h, boolean maskMode) {
        Identifier tex = maskMode ? getWhiteMaskTexture(id) : id;
        if (!maskMode && "fluxvisuals".equals(id.getNamespace())) {
            ensureMipmapFilter(id);
        }

        drawIconTexturedSharp(context, tex, x, y, w, h);
    }

    public static void drawHighQualityIcon(DrawContext context, Identifier texture, float x, float y, float width, float height) {
        drawIconTexturedSharp(context, texture, x, y, width, height);
    }

    public static void drawGuiIconWithFallback(DrawContext context, Identifier id, float x, float y, float w, float h, boolean maskFallback) {
        if (maskFallback && IconTtfBridge.tryDrawHudHeader(context, id, x, y, w, h)) {
            resetShaderColor();
        } else {
            drawGuiTexture(context, id, x, y, w, h, maskFallback);
            resetShaderColor();
        }
    }

    public static void setFogStart(float start) {
    }

    public static void setFogEnd(float end) {
    }

    public static void drawRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, radius, color);
    }

    public static void drawAnimatedGradientRoundedRect(DrawContext context, float x, float y, float width, float height, float radius, int color1, int color2) {
        RoundedRenderer.drawAnimatedGradientRoundedRect(context, x, y, width, height, radius, color1, color2);
    }

    public static void drawVerticalGradientRoundedRect(
        DrawContext context, float x, float y, float width, float height, float radius, int colorTop, int colorBottom
    ) {
        RoundedRenderer.drawVerticalGradientRoundedRect(context, x, y, width, height, radius, colorTop, colorBottom);
    }

    public static void drawFourCornerGradientRoundedRect(
        DrawContext context,
        float x,
        float y,
        float width,
        float height,
        float radius,
        int colorTopLeft,
        int colorTopRight,
        int colorBottomRight,
        int colorBottomLeft
    ) {
        RoundedRenderer.drawFourCornerGradientRoundedRect(context, x, y, width, height, radius, colorTopLeft, colorTopRight, colorBottomRight, colorBottomLeft);
    }

    public static void drawRotatingGradientRoundedRect(
        DrawContext context, float x, float y, float width, float height, float radius, int colorDark, int colorLight, float speed
    ) {
        RoundedRenderer.drawRotatingGradientRoundedRect(context, x, y, width, height, radius, colorDark, colorLight, speed);
    }

    public static void drawCustomRoundedRect(
        DrawContext context,
        float x,
        float y,
        float width,
        float height,
        float radius,
        int color,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft
    ) {
        RoundedRenderer.drawRoundedRect(context, x, y, width, height, radius, color, roundTopLeft, roundTopRight, roundBottomRight, roundBottomLeft);
    }

    public static void drawRoundedTexture(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float radius,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight
    ) {
        drawRoundedTexture(
            context,
            texture,
            x,
            y,
            width,
            height,
            radius,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            1.0F
        );
    }

    public static void drawRoundedTexture(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float radius,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight,
        float alpha
    ) {
        drawRoundedTextureInternal(
            context,
            texture,
            x,
            y,
            width,
            height,
            radius,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            alpha,
            false
        );
    }

    public static void drawRoundedTextureNearest(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float radius,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight
    ) {
        drawRoundedTextureInternal(
            context,
            texture,
            x,
            y,
            width,
            height,
            radius,
            roundTopLeft,
            roundTopRight,
            roundBottomRight,
            roundBottomLeft,
            u,
            v,
            regionWidth,
            regionHeight,
            textureWidth,
            textureHeight,
            1.0F,
            true
        );
    }

    private static void drawRoundedTextureInternal(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float radius,
        boolean roundTopLeft,
        boolean roundTopRight,
        boolean roundBottomRight,
        boolean roundBottomLeft,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight,
        float alpha,
        boolean nearest
    ) {
        ensureLinearFilter(texture);
        Gui$Tex tex = nearest ? Gui.texNearest(context, texture) : Gui.tex(context, texture);
        int col = Gui.argb(1.0F, 1.0F, 1.0F, alpha);
        boolean smoothAllCorners = roundTopLeft && roundTopRight && roundBottomRight && roundBottomLeft;
        float geometryRadius = smoothAllCorners ? Math.max(0.0F, radius - 0.5F) : radius;
        float centerX = x + width / 2.0F;
        float centerY = y + height / 2.0F;
        float u1 = u / textureWidth;
        float v1 = v / textureHeight;
        float uRegion = regionWidth / textureWidth;
        float vRegion = regionHeight / textureHeight;
        float centerU = u1 + (centerX - x) / width * uRegion;
        float centerV = v1 + (centerY - y) / height * vRegion;
        tex.fanVertex(centerX, centerY, centerU, centerV, col);
        int SEGMENTS = 20;
        if (roundTopRight) {
            float trCenterX = x + width - radius;
            float trCenterY = y + radius;

            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float)(0.0 - (Math.PI / 2) * i / SEGMENTS);
                float px = trCenterX + (float)Math.cos(angle) * geometryRadius;
                float py = trCenterY + (float)Math.sin(angle) * geometryRadius;
                tex.fanVertex(px, py, u1 + (px - x) / width * uRegion, v1 + (py - y) / height * vRegion, col);
            }
        } else {
            tex.fanVertex(x + width, y, u1 + uRegion, v1, col);
        }

        if (roundTopLeft) {
            float tlCenterX = x + radius;
            float tlCenterY = y + radius;

            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float)((-Math.PI / 2) - (Math.PI / 2) * i / SEGMENTS);
                float px = tlCenterX + (float)Math.cos(angle) * geometryRadius;
                float py = tlCenterY + (float)Math.sin(angle) * geometryRadius;
                tex.fanVertex(px, py, u1 + (px - x) / width * uRegion, v1 + (py - y) / height * vRegion, col);
            }
        } else {
            tex.fanVertex(x, y, u1, v1, col);
        }

        if (roundBottomLeft) {
            float blCenterX = x + radius;
            float blCenterY = y + height - radius;

            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float)(-Math.PI - (Math.PI / 2) * i / SEGMENTS);
                float px = blCenterX + (float)Math.cos(angle) * geometryRadius;
                float py = blCenterY + (float)Math.sin(angle) * geometryRadius;
                tex.fanVertex(px, py, u1 + (px - x) / width * uRegion, v1 + (py - y) / height * vRegion, col);
            }
        } else {
            tex.fanVertex(x, y + height, u1, v1 + vRegion, col);
        }

        if (roundBottomRight) {
            float brCenterX = x + width - radius;
            float brCenterY = y + height - radius;

            for (int i = 0; i <= SEGMENTS; i++) {
                float angle = (float)((-Math.PI * 3.0 / 2.0) - (Math.PI / 2) * i / SEGMENTS);
                float px = brCenterX + (float)Math.cos(angle) * geometryRadius;
                float py = brCenterY + (float)Math.sin(angle) * geometryRadius;
                tex.fanVertex(px, py, u1 + (px - x) / width * uRegion, v1 + (py - y) / height * vRegion, col);
            }
        } else {
            tex.fanVertex(x + width, y + height, u1 + uRegion, v1 + vRegion, col);
        }

        if (roundTopRight) {
            float closeX = x + width - radius + geometryRadius;
            tex.fanVertex(closeX, y + radius, u1 + (closeX - x) / width * uRegion, v1 + radius / height * vRegion, col);
        } else {
            tex.fanVertex(x + width, y, u1 + uRegion, v1, col);
        }

        tex.submit(context);
        if (smoothAllCorners) {
            drawRoundedTextureAaFringe(context, texture, x, y, width, height, radius, u1, v1, uRegion, vRegion, alpha, nearest);
        }
    }

    private static void drawRoundedTextureAaFringe(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float radius,
        float u1,
        float v1,
        float uRegion,
        float vRegion,
        float alpha,
        boolean nearest
    ) {
        Gui$Tex fringe = nearest ? Gui.texNearest(context, texture) : Gui.tex(context, texture);
        int innerColor = Gui.argb(1.0F, 1.0F, 1.0F, alpha);
        int outerColor = Gui.argb(1.0F, 1.0F, 1.0F, 0.0F);
        float innerRadius = Math.max(0.0F, radius - 0.5F);
        float outerRadius = radius + 0.5F;
        int segments = 24;
        appendRoundedTextureAaCorner(
            fringe,
            x + width - radius,
            y + radius,
            innerRadius,
            outerRadius,
            0,
            segments,
            x,
            y,
            width,
            height,
            u1,
            v1,
            uRegion,
            vRegion,
            innerColor,
            outerColor
        );
        appendRoundedTextureAaCorner(
            fringe, x + radius, y + radius, innerRadius, outerRadius, -1, segments, x, y, width, height, u1, v1, uRegion, vRegion, innerColor, outerColor
        );
        appendRoundedTextureAaCorner(
            fringe,
            x + radius,
            y + height - radius,
            innerRadius,
            outerRadius,
            -2,
            segments,
            x,
            y,
            width,
            height,
            u1,
            v1,
            uRegion,
            vRegion,
            innerColor,
            outerColor
        );
        appendRoundedTextureAaCorner(
            fringe,
            x + width - radius,
            y + height - radius,
            innerRadius,
            outerRadius,
            -3,
            segments,
            x,
            y,
            width,
            height,
            u1,
            v1,
            uRegion,
            vRegion,
            innerColor,
            outerColor
        );
        fringe.submit(context);
    }

    private static void appendRoundedTextureAaCorner(
        Gui$Tex fringe,
        float centerX,
        float centerY,
        float innerRadius,
        float outerRadius,
        int quadrant,
        int segments,
        float x,
        float y,
        float width,
        float height,
        float u1,
        float v1,
        float uRegion,
        float vRegion,
        int innerColor,
        int outerColor
    ) {
        for (int i = 0; i <= segments; i++) {
            float angle = (float)(quadrant * Math.PI / 2.0 - (Math.PI / 2) * i / segments);
            float cos = (float)Math.cos(angle);
            float sin = (float)Math.sin(angle);
            float innerX = centerX + cos * innerRadius;
            float innerY = centerY + sin * innerRadius;
            float outerX = centerX + cos * outerRadius;
            float outerY = centerY + sin * outerRadius;
            fringe.stripVertex(innerX, innerY, roundedTextureUv(innerX, x, width, u1, uRegion), roundedTextureUv(innerY, y, height, v1, vRegion), innerColor);
            fringe.stripVertex(outerX, outerY, roundedTextureUv(outerX, x, width, u1, uRegion), roundedTextureUv(outerY, y, height, v1, vRegion), outerColor);
        }
    }

    private static float roundedTextureUv(float position, float start, float length, float uvStart, float uvLength) {
        float factor = (position - start) / Math.max(0.001F, length);
        factor = Math.max(0.0F, Math.min(1.0F, factor));
        return uvStart + factor * uvLength;
    }

    public static void drawRoundedOutline(DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color) {
        RoundedRenderer.drawRoundedOutline(context, x, y, width, height, radius, thickness, color);
    }

    public static void drawRoundedOutlineSmooth(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        drawRoundedOutlineSmoothInternal(context, x, y, width, height, radius, color, false);
    }

    public static void drawRoundedOutlineSmoothDock(DrawContext context, float x, float y, float width, float height, float radius, int color) {
        drawRoundedOutlineSmoothInternal(context, x, y, width, height, radius, color, true);
    }

    private static void drawRoundedOutlineSmoothInternal(
        DrawContext context, float x, float y, float width, float height, float radius, int color, boolean dockExtra
    ) {
        int a = color >> 24 & 0xFF;
        if (a > 0 && !(width <= 0.0F) && !(height <= 0.0F)) {
            int rgb = color & 16777215;
            float[][] jitter = new float[][]{{0.0F, 0.0F}, {0.33F, 0.0F}, {0.0F, 0.33F}, {0.33F, 0.33F}};
            if (dockExtra) {
                float[][] extraJ = new float[][]{{0.17F, 0.0F}, {0.0F, 0.17F}, {0.17F, 0.17F}, {-0.17F, 0.17F}};

                for (float[] j : extraJ) {
                    int na = Math.max(1, Math.round(a * 0.045F));
                    int c = na << 24 | rgb;
                    RoundedRenderer.drawRoundedOutline(context, x + j[0], y + j[1], width, height, radius, 1.65F, c);
                }
            }

            float[][] outerFeather = new float[][]{{1.95F, 0.07F}, {1.25F, 0.14F}};
            if (dockExtra) {
                outerFeather = new float[][]{{2.05F, 0.06F}, {1.55F, 0.1F}, {1.05F, 0.15F}};
            }

            for (float[] j : jitter) {
                for (float[] p : outerFeather) {
                    int na = Math.max(1, Math.round(a * p[1] * 0.22F));
                    int c = na << 24 | rgb;
                    RoundedRenderer.drawRoundedOutline(context, x + j[0], y + j[1], width, height, radius, p[0], c);
                }
            }

            float[][] fArrayArray;
            if (dockExtra) {
                float[][] fArrayArray2 = new float[][]{{1.08F, 0.38F}, {0.72F, 0.68F}, {0.44F, 0.95F}, null};
                fArrayArray = fArrayArray2;
                fArrayArray2[3] = new float[]{0.28F, 0.55F};
            } else {
                float[][] fArrayArray3 = new float[][]{{1.12F, 0.42F}, {0.74F, 0.72F}, null};
                fArrayArray = fArrayArray3;
                fArrayArray3[2] = new float[]{0.46F, 0.98F};
            }

            for (float[] p : fArrayArray) {
                int na = Math.max(1, Math.round(a * p[1]));
                int c = na << 24 | rgb;
                RoundedRenderer.drawRoundedOutline(context, x, y, width, height, radius, p[0], c);
            }
        }
    }

    public static void drawDockCategorySelectionChrome(
        DrawContext context, float x, float y, float width, float height, float radius, int accentArgb, float reveal
    ) {
        if (!(reveal <= 0.01F) && !(width <= 0.0F) && !(height <= 0.0F)) {
            int r = accentArgb >> 16 & 0xFF;
            int g = accentArgb >> 8 & 0xFF;
            int b = accentArgb & 0xFF;
            int glowA = Math.max(1, Math.round(42.0F * reveal));
            int glowCol = glowA << 24 | r << 16 | g << 8 | b;
            drawOuterGlow(context, x - 0.5F, y - 0.5F, width + 1.0F, height + 1.0F, radius + 0.2F, 4, 1.05F, glowCol);
            int outlineA = Math.max(1, Math.round(255.0F * reveal));
            int outlineCol = outlineA << 24 | r << 16 | g << 8 | b;
            drawRoundedOutlineSmoothDock(context, x, y, width, height, radius, outlineCol);
        }
    }

    public static void drawSmoothCircleSelectionRing(DrawContext context, float cx, float cy, float midRadius, int color) {
        RoundedRenderer.drawSmoothCircleSelectionRing(context, cx, cy, midRadius, color);
    }

    public static void drawGradientRoundedOutline(
        DrawContext context, float x, float y, float width, float height, float radius, float thickness, int color1, int color2
    ) {
        RoundedRenderer.drawGradientRoundedOutline(context, x, y, width, height, radius, thickness, color1, color2);
    }

    public static void drawGradientGlow(
        DrawContext context, float x, float y, float width, float height, float radius, int glowRadius, float thickness, int color1, int color2
    ) {
        int alpha1 = color1 >> 24 & 0xFF;
        if (alpha1 == 0) {
            alpha1 = 255;
        }

        int alpha2;
        if ((alpha2 = color2 >> 24 & 0xFF) == 0) {
            alpha2 = 255;
        }

        int optimizedRadius;
        int step = (optimizedRadius = Math.max(1, Math.min(glowRadius, 8))) > 5 ? 2 : 1;

        for (int i = 1; i <= optimizedRadius; i += step) {
            float progress = (float)i / optimizedRadius;
            int a1 = (int)(alpha1 * (1.0F - progress) * 0.4F);
            int a2 = (int)(alpha2 * (1.0F - progress) * 0.4F);
            if (a1 > 0 || a2 > 0) {
                int passColor1 = a1 << 24 | color1 & 16777215;
                int passColor2 = a2 << 24 | color2 & 16777215;
                drawGradientRoundedOutline(context, x - i, y - i, width + i * 2, height + i * 2, radius + i, thickness + 1.0F, passColor1, passColor2);
            }
        }

        drawGradientRoundedOutline(context, x, y, width, height, radius, thickness, color1, color2);
    }

    public static void drawGlow(DrawContext context, float x, float y, float width, float height, float radius, int glowRadius, float thickness, int color) {
        drawGlow(context, x, y, width, height, radius, glowRadius, thickness, color, true);
    }

    public static void drawOuterGlow(DrawContext context, float x, float y, float width, float height, float radius, int glowRadius, float thickness, int color) {
        drawGlow(context, x, y, width, height, radius, glowRadius, thickness, color, false);
    }

    private static void drawGlow(
        DrawContext context, float x, float y, float width, float height, float radius, int glowRadius, float thickness, int color, boolean drawInnerOutline
    ) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;
        int baseAlpha = color >> 24 & 0xFF;
        if (baseAlpha == 0) {
            baseAlpha = 255;
        }

        for (int i = 1; i <= glowRadius; i++) {
            float progress = (float)i / glowRadius;
            int alpha = (int)(baseAlpha * (1.0F - progress) * 0.4F);
            if (alpha > 0) {
                int passColor = alpha << 24 | r << 16 | g << 8 | b;
                RoundedRenderer.drawRoundedOutline(context, x - i, y - i, width + i * 2, height + i * 2, radius + i, thickness + 1.0F, passColor);
            }
        }

        if (drawInnerOutline) {
            RoundedRenderer.drawRoundedOutline(context, x, y, width, height, radius, thickness, color);
        }
    }

    public static void drawGradientLine(DrawContext context, float x1, float y, float x2, float y2, int centerColor) {
        float centerX = (x1 + x2) / 2.0F;
        int alpha = centerColor >> 24 & 0xFF;
        if (alpha == 0) {
            int var9 = 255;
        }

        int transparentColor = centerColor & 16777215;
        drawGradientRect(context, x1, y, centerX, y + 1.0F, transparentColor, centerColor);
        drawGradientRect(context, centerX, y, x2, y + 1.0F, centerColor, transparentColor);
    }

    public static void drawHorizontalGradientRect(DrawContext context, float x, float y, float width, float height, int startColor, int endColor) {
        Gui.color(context)
            .vertex(x + width, y, endColor)
            .vertex(x, y, startColor)
            .vertex(x, y + height, startColor)
            .vertex(x + width, y + height, endColor)
            .submit(context);
    }

    public static void drawGradientRect(DrawContext context, float x1, float y1, float x2, float y2, int colorStart, int colorEnd) {
        Gui.color(context).vertex(x1, y2, colorStart).vertex(x2, y2, colorEnd).vertex(x2, y1, colorEnd).vertex(x1, y1, colorStart).submit(context);
    }

    public static void drawVerticalGradientRect(DrawContext context, float x1, float y1, float x2, float y2, int colorStart, int colorEnd) {
        Gui.color(context).vertex(x1, y2, colorEnd).vertex(x2, y2, colorEnd).vertex(x2, y1, colorStart).vertex(x1, y1, colorStart).submit(context);
    }

    public static void drawCircle(DrawContext context, float x, float y, float radius, int color) {
        if (!(radius < 0.5F)) {
            Gui$Color gc = Gui.color(context);
            gc.fanVertex(x, y, color);

            for (int i = 0; i <= 360; i++) {
                float rad = (float)Math.toRadians(i);
                float px = x + (float)Math.cos(rad) * radius;
                float py = y + (float)Math.sin(rad) * radius;
                gc.fanVertex(px, py, color);
            }

            gc.submit(context);
        }
    }

    public static void drawTextureInCircle(DrawContext context, Identifier texture, float x, float y, float radius) {
        drawTextureInCircle(context, texture, x, y, radius, 0.0F, 0.0F, 1.0F, 1.0F);
    }

    public static void drawPlayerFaceCircle(DrawContext context, Identifier texture, float x, float y, float radius) {
        float u1 = 0.125F;
        float v1 = 0.125F;
        float u2 = 0.25F;
        float v2 = 0.25F;
        drawTextureInCircle(context, texture, x, y, radius, u1, v1, u2, v2);
        float h_u1 = 0.625F;
        float h_v1 = 0.125F;
        float h_u2 = 0.75F;
        float h_v2 = 0.25F;
        drawTextureInCircle(context, texture, x, y, radius, h_u1, h_v1, h_u2, h_v2);
    }

    public static void drawTexture(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight
    ) {
        drawTexture(context, texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight, true);
    }

    public static void drawTexture(
        DrawContext context,
        Identifier texture,
        float x,
        float y,
        float width,
        float height,
        float u,
        float v,
        float regionWidth,
        float regionHeight,
        float textureWidth,
        float textureHeight,
        boolean smooth
    ) {
        if (smooth) {
            ensureLinearFilter(texture);
        }

        float u1 = u / textureWidth;
        float v1 = v / textureHeight;
        float u2 = (u + regionWidth) / textureWidth;
        float v2 = (v + regionHeight) / textureHeight;
        int col = -1;
        Gui.tex(context, texture)
            .vertex(x, y + height, u1, v2, col)
            .vertex(x + width, y + height, u2, v2, col)
            .vertex(x + width, y, u2, v1, col)
            .vertex(x, y, u1, v1, col)
            .submit(context);
    }

    public static void drawTextureInCircle(DrawContext context, Identifier texture, float x, float y, float radius, float u1, float v1, float u2, float v2) {
        Gui$Tex tex = Gui.tex(context, texture);
        int col = -1;
        float centerU = (u1 + u2) / 2.0F;
        float centerV = (v1 + v2) / 2.0F;
        float radiusU = (u2 - u1) / 2.0F;
        float radiusV = (v2 - v1) / 2.0F;
        tex.fanVertex(x, y, centerU, centerV, col);

        for (int i = 0; i <= 360; i += 10) {
            float rad = (float)Math.toRadians(i);
            float px = x + (float)Math.cos(rad) * radius;
            float py = y + (float)Math.sin(rad) * radius;
            float u = centerU + (float)Math.cos(rad) * radiusU;
            float v = centerV + (float)Math.sin(rad) * radiusV;
            tex.fanVertex(px, py, u, v, col);
        }

        tex.submit(context);
    }

    public static void drawCheckmark(DrawContext context, float x, float y, float size, int color) {
        float thickness = 1.5F;
        drawLine(context, x + size * 0.2F, y + size * 0.5F, x + size * 0.45F, y + size * 0.8F, thickness, color);
        drawLine(context, x + size * 0.45F, y + size * 0.8F, x + size * 0.8F, y + size * 0.2F, thickness, color);
    }

    public static void drawCross(DrawContext context, float x, float y, float size, int color) {
        float thickness = 1.5F;
        float padding = size * 0.25F;
        drawLine(context, x + padding, y + padding, x + size - padding, y + size - padding, thickness, color);
        drawLine(context, x + size - padding, y + padding, x + padding, y + size - padding, thickness, color);
    }

    public static void drawLine(DrawContext context, float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float)Math.sqrt(dx * dx + dy * dy);
        if (len != 0.0F) {
            float nx = dx / len;
            float ny = dy / len;
            float px = -ny * (thickness / 2.0F);
            float py = nx * (thickness / 2.0F);
            float v1x = x1 + px;
            float v1y = y1 + py;
            float v2x = x1 - px;
            float v2y = y1 - py;
            float v3x = x2 - px;
            float v3y = y2 - py;
            float v4x = x2 + px;
            float v4y = y2 + py;
            Gui.color(context).vertex(v1x, v1y, color).vertex(v2x, v2y, color).vertex(v3x, v3y, color).vertex(v4x, v4y, color).submit(context);
        }
    }

    public static void fillCircleCorner(DrawContext context, int centerX, int centerY, int radius, int color, int corner) {
        for (int i = 0; i < radius; i++) {
            for (int j = 0; j < radius; j++) {
                double dist = Math.sqrt(i * i + j * j);
                if (dist <= radius) {
                    int px = centerX + (corner != 1 && corner != 3 ? -i - 1 : i);
                    int py = centerY + (corner != 2 && corner != 3 ? -j - 1 : j);
                    context.fill(px, py, px + 1, py + 1, color);
                }
            }
        }
    }
}
