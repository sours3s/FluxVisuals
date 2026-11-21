package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.FilterMode;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class Gui {
    public static final RenderPipeline GUI_NOCULL = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/gui_nocull"))
        .withCull(false)
        .build();
    public static final RenderPipeline GUI_TEXTURED_NOCULL = RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/gui_textured_nocull"))
        .withCull(false)
        .build();
    public static final RenderPipeline GUI_INVERT = RenderPipeline.builder(RenderPipelines.GUI_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/gui_invert"))
        .withCull(false)
        .withBlend(BlendFunction.INVERT)
        .build();
    public static final RenderPipeline GUI_MSDF = RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/gui_msdf"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/msdf"))
        .withCull(false)
        .build();

    Gui() {
    }

    static Matrix3x2f pose(DrawContext ctx) {
        return new Matrix3x2f(ctx.getMatrices());
    }

    static ScreenRect computeBounds(float[] xy, int count, Matrix3x2fc m) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;

        for (int i = 0; i < count; i++) {
            float x = xy[i * 2];
            float y = xy[i * 2 + 1];
            if (x < minX) {
                minX = x;
            }

            if (y < minY) {
                minY = y;
            }

            if (x > maxX) {
                maxX = x;
            }

            if (y > maxY) {
                maxY = y;
            }
        }

        int x0 = (int)Math.floor(minX);
        int y0 = (int)Math.floor(minY);
        int w = Math.max(1, (int)Math.ceil(maxX) - x0);
        int h = Math.max(1, (int)Math.ceil(maxY) - y0);
        return new ScreenRect(x0, y0, w, h).transformEachVertex(m);
    }

    static ScreenRect computeBounds(float minX, float minY, float maxX, float maxY, Matrix3x2fc matrix) {
        int x = (int)Math.floor(minX);
        int y = (int)Math.floor(minY);
        int width = Math.max(1, (int)Math.ceil(maxX) - x);
        int height = Math.max(1, (int)Math.ceil(maxY) - y);
        return new ScreenRect(x, y, width, height).transformEachVertex(matrix);
    }

    public static Gui$Color color(DrawContext ctx) {
        return new Gui$Color(pose(ctx), GUI_NOCULL, 64);
    }

    public static Gui$Color color(DrawContext ctx, int expectedVertices) {
        return new Gui$Color(pose(ctx), GUI_NOCULL, expectedVertices);
    }

    public static Gui$Color color(DrawContext ctx, RenderPipeline pipeline) {
        return new Gui$Color(pose(ctx), pipeline, 64);
    }

    public static Gui$Tex tex(DrawContext ctx, Identifier texture) {
        return new Gui$Tex(pose(ctx), texture, FilterMode.LINEAR, GUI_TEXTURED_NOCULL, 16);
    }

    public static Gui$Tex tex(DrawContext ctx, Identifier texture, RenderPipeline pipeline) {
        return new Gui$Tex(pose(ctx), texture, FilterMode.LINEAR, pipeline, 16);
    }

    public static Gui$Tex tex(DrawContext ctx, Identifier texture, RenderPipeline pipeline, int expectedVertices) {
        return new Gui$Tex(pose(ctx), texture, FilterMode.LINEAR, pipeline, expectedVertices);
    }

    public static Gui$Tex texNearest(DrawContext ctx, Identifier texture) {
        return new Gui$Tex(pose(ctx), texture, FilterMode.NEAREST, GUI_TEXTURED_NOCULL, 16);
    }

    public static Gui$Tex texNearest(DrawContext ctx, Identifier texture, RenderPipeline pipeline, int expectedVertices) {
        return new Gui$Tex(pose(ctx), texture, FilterMode.NEAREST, pipeline, expectedVertices);
    }

    public static int argb(float r, float g, float b, float a) {
        int ai = Math.round(Math.max(0.0F, Math.min(1.0F, a)) * 255.0F);
        int ri = Math.round(Math.max(0.0F, Math.min(1.0F, r)) * 255.0F);
        int gi = Math.round(Math.max(0.0F, Math.min(1.0F, g)) * 255.0F);
        int bi = Math.round(Math.max(0.0F, Math.min(1.0F, b)) * 255.0F);
        return ai << 24 | ri << 16 | gi << 8 | bi;
    }

    public static void texQuad(
        DrawContext ctx, Identifier texture, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, int packedArgb
    ) {
        tex(ctx, texture)
            .vertex(x0, y1, u0, v1, packedArgb)
            .vertex(x1, y1, u1, v1, packedArgb)
            .vertex(x1, y0, u1, v0, packedArgb)
            .vertex(x0, y0, u0, v0, packedArgb)
            .submit(ctx);
    }
}
