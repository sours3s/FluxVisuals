package ru.fluxvisuals.ui.fluxgui.util.render;

import java.awt.Color;
import net.minecraft.client.gui.DrawContext;

/**
 * Совместимость донорского HUD с текущим стеклом FluxVisuals.
 * Вызовы Delegates 1:1 к FluxGlassPipeline, имя оставлено как в исходнике.
 */
public final class MirageGlassPipeline {
    private MirageGlassPipeline() {
    }

    public static void captureFrame() {
        FluxGlassPipeline.captureFrame();
    }

    public static void drawLiquidAt(
          DrawContext context, float x, float y, float width, float height, float radius,
          float blur, float distortion, float shine, float alpha, Color tint
    ) {
        FluxGlassPipeline.drawLiquidAt(context, x, y, width, height, radius, blur, distortion, shine, alpha, tint);
    }

    public static void drawBlurAt(
          DrawContext context, float x, float y, float width, float height, float radius, float blur, float alpha
    ) {
        FluxGlassPipeline.drawBlurAt(context, x, y, width, height, radius, blur, alpha);
    }
}
