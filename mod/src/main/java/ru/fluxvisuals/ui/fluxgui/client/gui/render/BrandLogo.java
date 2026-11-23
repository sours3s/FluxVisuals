package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/** Именной логотип FluxVisuals: отдельная текстура, а не глиф MSDF-атласа. */
public final class BrandLogo {
    public static final Identifier TEXTURE = Identifier.of("fluxvisuals", "textures/logo.png");

    private BrandLogo() {
    }

    public static void draw(DrawContext context, float x, float y, float size) {
        draw(context, x, y, size, size);
    }

    public static void draw(DrawContext context, float x, float y, float width, float height) {
        if (width <= 0.0F || height <= 0.0F) {
            return;
        }
        context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE,
              Math.round(x), Math.round(y), 0.0F, 0.0F,
              Math.round(width), Math.round(height), 256, 256);
    }
}
