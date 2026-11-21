package ru.fluxvisuals.ui.fluxgui.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Builder;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

public final class FluxRender {
    static final String NS = "fluxvisuals";
    public static final RenderPipeline POS_COLOR_QUADS = colorPipeline("pos_color_quads", DrawMode.QUADS, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_TRIS = colorPipeline("pos_color_tris", DrawMode.TRIANGLES, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_TRI_STRIP = colorPipeline("pos_color_tri_strip", DrawMode.TRIANGLE_STRIP, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_TRI_FAN = colorPipeline("pos_color_tri_fan", DrawMode.TRIANGLE_FAN, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_LINES = colorPipeline("pos_color_lines", DrawMode.DEBUG_LINES, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_LINE_STRIP = colorPipeline("pos_color_line_strip", DrawMode.DEBUG_LINE_STRIP, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_QUADS_ADD = colorPipeline("pos_color_quads_add", DrawMode.QUADS, BlendFunction.LIGHTNING);
    public static final RenderPipeline POS_COLOR_TRIS_ADD = colorPipeline("pos_color_tris_add", DrawMode.TRIANGLES, BlendFunction.LIGHTNING);
    public static final RenderPipeline POS_COLOR_LINES_ADD = colorPipeline("pos_color_lines_add", DrawMode.DEBUG_LINES, BlendFunction.LIGHTNING);
    public static final RenderPipeline POS_COLOR_LINE_STRIP_ADD = colorPipeline("pos_color_line_strip_add", DrawMode.DEBUG_LINE_STRIP, BlendFunction.LIGHTNING);
    public static final RenderPipeline POS_COLOR_QUADS_NODEPTH = colorPipelineNoDepth("pos_color_quads_nd", DrawMode.QUADS, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_TRIS_NODEPTH = colorPipelineNoDepth("pos_color_tris_nd", DrawMode.TRIANGLES, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_TRI_FAN_NODEPTH = colorPipelineNoDepth(
        "pos_color_tri_fan_nd", DrawMode.TRIANGLE_FAN, BlendFunction.TRANSLUCENT
    );
    public static final RenderPipeline POS_COLOR_LINES_NODEPTH = colorPipelineNoDepth("pos_color_lines_nd", DrawMode.DEBUG_LINES, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline POS_COLOR_LINE_STRIP_NODEPTH = colorPipelineNoDepth(
        "pos_color_line_strip_nd", DrawMode.DEBUG_LINE_STRIP, BlendFunction.TRANSLUCENT
    );
    public static final RenderPipeline POS_COLOR_QUADS_INVERT = colorPipeline(
        "pos_color_quads_invert", DrawMode.QUADS, BlendFunction.INVERT, DepthTestFunction.NO_DEPTH_TEST
    );
    public static final RenderPipeline POS_TEX_COLOR_QUADS = texPipeline("pos_tex_color_quads", DrawMode.QUADS, BlendFunction.TRANSLUCENT, true);
    public static final RenderPipeline POS_TEX_COLOR_TRIS = texPipeline("pos_tex_color_tris", DrawMode.TRIANGLES, BlendFunction.TRANSLUCENT, true);
    public static final RenderPipeline POS_TEX_COLOR_QUADS_ADD = texPipeline("pos_tex_color_quads_add", DrawMode.QUADS, BlendFunction.LIGHTNING, true);
    public static final RenderPipeline POS_TEX_COLOR_TRIS_ADD = texPipeline("pos_tex_color_tris_add", DrawMode.TRIANGLES, BlendFunction.LIGHTNING, true);
    public static final RenderPipeline POS_TEX_COLOR_QUADS_NODEPTH = texPipeline(
        "pos_tex_color_quads_nodepth", DrawMode.QUADS, BlendFunction.TRANSLUCENT, false
    );
    public static final RenderPipeline POS_TEX_COLOR_TRIS_NODEPTH = texPipeline(
        "pos_tex_color_tris_nodepth", DrawMode.TRIANGLES, BlendFunction.TRANSLUCENT, false
    );
    public static final RenderPipeline POS_TEX_COLOR_QUADS_ADD_NODEPTH = texPipeline(
        "pos_tex_color_quads_add_nodepth", DrawMode.QUADS, BlendFunction.LIGHTNING, false
    );
    public static final RenderPipeline POS_TEX_COLOR_TRIS_ADD_NODEPTH = texPipeline(
        "pos_tex_color_tris_add_nodepth", DrawMode.TRIANGLES, BlendFunction.LIGHTNING, false
    );
    public static final RenderPipeline POS_COLOR_TRIS_ADD_NODEPTH = colorPipeline(
        "pos_color_tris_add_nodepth", DrawMode.TRIANGLES, BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST
    );
    public static final RenderPipeline POS_TEX_COLOR_TRIS_SOLID = RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/pos_tex_color_tris_solid"))
        .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthWrite(true)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .build();
    public static final RenderPipeline POS_COLOR_TRIS_SOLID = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/pos_color_tris_solid"))
        .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthWrite(true)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .build();
    static final Map<FluxRender$PipeKey, RenderPipeline> FLEX = new HashMap<>();
    static final Map<RenderPipeline, RenderLayer> COLOR_LAYERS = new HashMap<>();
    static final Map<FluxRender$TexKey, RenderLayer> TEX_LAYERS = new HashMap<>();

    FluxRender() {
    }

    public static MatrixStack guiStack(DrawContext context) {
        MatrixStack ms = new MatrixStack();
        ms.multiplyPositionMatrix(new Matrix4f().mul(context.getMatrices()));
        return ms;
    }

    static RenderPipeline colorPipeline(String name, DrawMode mode, BlendFunction blend) {
        return colorPipeline(name, mode, blend, DepthTestFunction.LEQUAL_DEPTH_TEST);
    }

    static RenderPipeline colorPipelineNoDepth(String name, DrawMode mode, BlendFunction blend) {
        return colorPipeline(name, mode, blend, DepthTestFunction.NO_DEPTH_TEST);
    }

    static RenderPipeline colorPipeline(String name, DrawMode mode, BlendFunction blend, DepthTestFunction depth) {
        return RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of("fluxvisuals", "pipeline/" + name))
            .withVertexFormat(VertexFormats.POSITION_COLOR, mode)
            .withBlend(blend)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(depth)
            .build();
    }

    static RenderPipeline texPipeline(String name, DrawMode mode, BlendFunction blend, boolean depthTest) {
        return RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
            .withLocation(Identifier.of("fluxvisuals", "pipeline/" + name))
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, mode)
            .withBlend(blend)
            .withCull(false)
            .withDepthWrite(false)
            .withDepthTestFunction(depthTest ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST)
            .build();
    }

    public static RenderPipeline color(DrawMode mode, boolean additive, boolean depthTest) {
        return flex(false, mode, additive, depthTest, false);
    }

    public static RenderPipeline tex(DrawMode mode, boolean additive, boolean depthTest) {
        return flex(true, mode, additive, depthTest, false);
    }

    static RenderPipeline flex(boolean textured, DrawMode mode, boolean additive, boolean depthTest, boolean depthWrite) {
        return FLEX.computeIfAbsent(
            new FluxRender$PipeKey(textured, mode, additive, depthTest, depthWrite),
            k -> {
                BlendFunction blend = k.additive() ? BlendFunction.LIGHTNING : BlendFunction.TRANSLUCENT;
                String name = (k.textured() ? "flex_tex_" : "flex_col_")
                    + k.mode().name().toLowerCase()
                    + (k.additive() ? "_add" : "")
                    + (k.depthTest() ? "_dt" : "_nd")
                    + (k.depthWrite() ? "_dw" : "");
                Builder b = RenderPipeline.builder(k.textured() ? RenderPipelines.POSITION_TEX_COLOR_SNIPPET : RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("fluxvisuals", "pipeline/" + name))
                    .withVertexFormat(k.textured() ? VertexFormats.POSITION_TEXTURE_COLOR : VertexFormats.POSITION_COLOR, k.mode())
                    .withBlend(blend)
                    .withCull(false)
                    .withDepthWrite(k.depthWrite())
                    .withDepthTestFunction(k.depthTest() ? DepthTestFunction.LEQUAL_DEPTH_TEST : DepthTestFunction.NO_DEPTH_TEST);
                return b.build();
            }
        );
    }

    static RenderLayer colorLayer(RenderPipeline pipeline) {
        return COLOR_LAYERS.computeIfAbsent(pipeline, p -> RenderLayer.of("flux_" + p.getLocation().getPath(), RenderSetup.builder(p).build()));
    }

    static RenderLayer texLayer(RenderPipeline pipeline, Identifier texture) {
        return TEX_LAYERS.computeIfAbsent(
            new FluxRender$TexKey(pipeline, texture),
            k -> RenderLayer.of(
                "flux_" + k.pipeline().getLocation().getPath() + "_" + k.texture().getPath(),
                RenderSetup.builder(k.pipeline()).texture("Sampler0", k.texture()).build()
            )
        );
    }

    public static void drawColor(BufferBuilder buffer, RenderPipeline pipeline) {
        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                colorLayer(pipeline).draw(built);
            }
        }
    }

    public static void drawColorAuto(BufferBuilder buffer, boolean additive, boolean depthTest, float lineWidth) {
        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                DrawMode mode = built.getDrawParameters().comp_752();
                boolean lines = mode == DrawMode.DEBUG_LINES || mode == DrawMode.DEBUG_LINE_STRIP || mode == DrawMode.LINES;
                float previousWidth = lines ? GL11.glGetFloat(2849) : 1.0F;
                if (lines) {
                    GL11.glLineWidth(1.0F);
                }

                colorLayer(color(mode, additive, depthTest)).draw(built);
                if (lines) {
                    GL11.glLineWidth(previousWidth);
                }
            }
        }
    }

    public static void drawColorAuto(BufferBuilder buffer, boolean additive, boolean depthTest) {
        drawColorAuto(buffer, additive, depthTest, 1.0F);
    }

    public static void drawTexAuto(BufferBuilder buffer, Identifier texture, boolean additive, boolean depthTest) {
        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                texLayer(tex(built.getDrawParameters().comp_752(), additive, depthTest), texture).draw(built);
            }
        }
    }

    public static void drawColorLines(BufferBuilder buffer, RenderPipeline pipeline, float lineWidth) {
        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                float previousWidth = GL11.glGetFloat(2849);
                GL11.glLineWidth(1.0F);
                colorLayer(pipeline).draw(built);
                GL11.glLineWidth(previousWidth);
            }
        }
    }

    public static void drawTextured(BufferBuilder buffer, RenderPipeline pipeline, Identifier texture) {
        if (buffer != null) {
            BuiltBuffer built = buffer.endNullable();
            if (built != null) {
                texLayer(pipeline, texture).draw(built);
            }
        }
    }
}
