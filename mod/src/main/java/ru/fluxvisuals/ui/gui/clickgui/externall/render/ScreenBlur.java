package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import com.mojang.blaze3d.textures.GpuTextureView;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import ru.fluxvisuals.ui.gui.clickgui.externall.theme.ThemeColor;

public class ScreenBlur {

    private static final KawaseBlurPipeline pipeline = new KawaseBlurPipeline();

    private static GpuTextureView blurredView = null;
    private static int cachedWidth = 0;
    private static int cachedHeight = 0;

    private static boolean usedThisFrame = false;
    private static boolean usedLastFrame = true;

    public static void markUsed() {
        usedThisFrame = true;
    }

    public static void frame() {
        boolean wanted = usedThisFrame || usedLastFrame;
        usedLastFrame = usedThisFrame;
        usedThisFrame = false;

        if (!wanted) {
            blurredView = null;
            return;
        }

        int strength = ThemeColor.getBlur();
        doCapture(3, 2);
    }

    public static void capture() {
        usedLastFrame = true;
        int strength = ThemeColor.getBlur();
        doCapture(3, 2);
    }

    public static void capture(int size) {
        usedLastFrame = true;
        doCapture(size, size);
    }

    private static void doCapture(int iterations, float offset) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        Framebuffer fb = mc.getFramebuffer();
        if (fb == null) return;

        DrawBatcher.flushPending();

        try {
            var colorAttachment = fb.getColorAttachment();
            var colorAttachmentView = fb.getColorAttachmentView();
            if (colorAttachment == null || colorAttachmentView == null) {
                blurredView = null;
                return;
            }

            int w = fb.textureWidth;
            int h = fb.textureHeight;
            if (w <= 0 || h <= 0) {
                blurredView = null;
                return;
            }

            blurredView = pipeline.blur(colorAttachment, colorAttachmentView, w, h, iterations, offset);
            if (blurredView != null) {
                cachedWidth = w;
                cachedHeight = h;
            }
        } catch (Exception e) {
            blurredView = null;
        }
    }

    public static boolean isReady() {
        return blurredView != null;
    }

    public static GpuTextureView getBlurredView() {
        return blurredView;
    }

    public static int getCachedWidth() {
        return cachedWidth;
    }

    public static int getCachedHeight() {
        return cachedHeight;
    }

    public static void invalidate() {
        blurredView = null;
    }

    public static void close() {
        blurredView = null;
        pipeline.close();
    }
}

