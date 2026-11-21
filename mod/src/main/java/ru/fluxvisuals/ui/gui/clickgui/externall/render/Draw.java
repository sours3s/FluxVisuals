package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import net.minecraft.util.Identifier;
import ru.fluxvisuals.ui.gui.clickgui.externall.Client;
import ru.fluxvisuals.ui.gui.clickgui.externall.theme.ThemeColor;
import ru.fluxvisuals.ui.gui.clickgui.externall.colors.ColorUtil;

public final class Draw {

    private static final float GLASS_DISTORTION = 125f;
    private static final float GLASS_WAVE_SIZE = 75f;

    private Draw() {
    }

    private static Render2D r2d() {
        return Client.get().render2D();
    }

    public static void flush() {
        r2d().flushAll();
    }

    public static void blur(float x, float y, float width, float height, float alpha, int tintColor) {
        blur(x, y, width, height, alpha, 0f, 0f, 0f, 0f, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float alpha,
                            float radius, int tintColor) {
        blur(x, y, width, height, alpha, radius, radius, radius, radius, tintColor);
    }

    public static void blur(float x, float y, float width, float height, float alpha,
                            float topLeft, float topRight, float bottomRight, float bottomLeft,
                            int tintColor) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        BlurPipeline pipeline = r2d().getBlurPipeline();
        if (ThemeColor.getDistortion()) {
            pipeline.drawGlassBlur(x, y, width, height, alpha, radii, tintColor,
                    GLASS_DISTORTION, GLASS_WAVE_SIZE, 0f, 0f);
        } else {
            pipeline.drawBlur(x, y, width, height, alpha, radii, tintColor);
        }
    }

    public static void glass(float x, float y, float width, float height,
                             float alpha, float topLeft, float topRight,
                             float bottomRight, float bottomLeft, int tintColor) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getBlurPipeline().drawGlassBlur(x, y, width, height, alpha, radii, tintColor,
                GLASS_DISTORTION, GLASS_WAVE_SIZE, 0f, 0f);
    }

    public static void rect(float x, float y, float width, float height, int color) {
        rect(x, y, width, height, color, 0f);
    }

    public static void rect(float x, float y, float width, float height, int color, float radius) {
        rect(x, y, width, height, color, radius, radius, radius, radius);
    }

    public static void rect(float x, float y, float width, float height, int color,
                            float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid(color);
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getRectPipeline().drawRect(x, y, width, height, colors, radii);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float radius) {
        gradientRect(x, y, width, height, colors, radius, radius, radius, radius);
    }

    public static void gradientRect(float x, float y, float width, float height,
                                    int[] colors, float topLeft, float topRight,
                                    float bottomRight, float bottomLeft) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getRectPipeline().drawRect(x, y, width, height, colors, radii);
    }

    public static void glow(float x, float y, float width, float height,
                            int color, float radius, float glowSize, float strength) {
        glow(x, y, width, height, color, radius, radius, radius, radius, glowSize, strength, 1f);
    }

    public static void glow(float x, float y, float width, float height,
                            int color, float radius, float glowSize, float strength, float softness) {
        glow(x, y, width, height, color, radius, radius, radius, radius, glowSize, strength, softness);
    }

    public static void glow(float x, float y, float width, float height,
                            int color, float topLeft, float topRight, float bottomRight, float bottomLeft,
                            float glowSize, float strength, float softness) {
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getRectPipeline().drawGlow(x, y, width, height, color, radii, glowSize, strength, softness);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color) {
        outline(x, y, width, height, thickness, color, 0f, 0f, 0f, 0f);
    }

    public static void outline(float x, float y, float width, float height,
                               float thickness, int color, float radius) {
        outline(x, y, width, height, thickness, color, radius, radius, radius, radius);
    }

    public static void outline(float x, float y, float width, float height, float thickness, int color,
                               float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = ColorUtil.solid8(color);
        float[] thicknesses = {thickness, thickness, thickness, thickness,
                thickness, thickness, thickness, thickness};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getOutlinePipeline().drawOutline(x, y, width, height, colors, thicknesses, radii, 1.0f);
    }

    public static void glassOutline(float x, float y, float width, float height,
                                    float thickness, float radius, float alpha, float shine) {
        glassOutline(x, y, width, height, thickness, radius, radius, radius, radius, alpha, shine);
    }

    public static void glassOutline(float x, float y, float width, float height,
                                    float thickness, float topLeft, float topRight,
                                    float bottomRight, float bottomLeft, float alpha, float shine) {
        float base = Math.max(0f, alpha);
        float hot = Math.max(0f, shine);

        int[] colors = {
                glassColor(base * 0.42f),
                glassColor(base * (0.55f + hot * 0.30f)),
                glassColor(base * (0.28f + hot * 0.10f)),
                glassColor(base * 0.24f),
                glassColor(base * (0.34f + hot * 0.16f)),
                glassColor(base * (0.48f + hot * 0.24f)),
                glassColor(base * 0.30f),
                glassColor(base * (0.62f + hot * 0.38f))
        };
        float[] thicknesses = {
                thickness,
                thickness + hot * 0.18f,
                thickness * 0.86f,
                thickness * 0.78f,
                thickness * 0.9f,
                thickness + hot * 0.16f,
                thickness * 0.84f,
                thickness + hot * 0.24f
        };
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getOutlinePipeline().drawOutline(x, y, width, height, colors, thicknesses, radii, 1.35f);
    }

    private static int glassColor(float alpha) {
        return ColorUtil.getColor(255, Math.min(alpha, 1f));
    }

    public static void texture(Identifier id, float x, float y, float width, float height, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, 1f, 0f);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float smoothness, float radius, int color) {
        texture(id, x, y, width, height, 0, 0, 1, 1, color, smoothness, radius);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1,
                               int color, float smoothness, float radius) {
        int[] colors = {color, color, color, color};
        float[] radii = {radius, radius, radius, radius};
        r2d().getTexturePipeline()
                .drawTexture(id, x, y, width, height, u0, v0, u1, v1, colors, radii, smoothness);
    }

    public static void texture(Identifier id, float x, float y, float width, float height,
                               float u0, float v0, float u1, float v1,
                               int color, float smoothness,
                               float topLeft, float topRight, float bottomRight, float bottomLeft) {
        int[] colors = {color, color, color, color};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getTexturePipeline()
                .drawTexture(id, x, y, width, height, u0, v0, u1, v1, colors, radii, smoothness);
    }

    public static void textureRadii(Identifier id, float x, float y, float width, float height,
                                    float topLeft, float topRight, float bottomRight, float bottomLeft, int color) {
        int[] colors = {color, color, color, color};
        float[] radii = {topLeft, topRight, bottomRight, bottomLeft};
        r2d().getTexturePipeline()
                .drawTexture(id, x, y, width, height, 0, 0, 1, 1, colors, radii, 1f);
    }

    public static void circleProgress(float centerX, float centerY, float radius, float thickness, float progress, int color) {
        r2d().getCircleProgressPipeline().draw(centerX, centerY, radius, thickness, progress, color);
    }
}

