package ru.fluxvisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class BlockOutlineShaderUtil {
    private static final int SHADER_WAVES = 0;
    private static final int SHADER_PLASMA = 1;
    private static final int SHADER_WATER = 2;
    private static final int SHADER_DARK = 3;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/block_shader"))
        .withVertexShader(Identifier.of("fluxvisuals", "core/block_shader"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/block_shader"))
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
    private static final RenderPipeline PIPELINE_3D = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/block_shader_3d"))
        .withVertexShader(Identifier.of("fluxvisuals", "core/block_shader"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/block_shader"))
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
    private static final long startedAt = System.currentTimeMillis();
    private static final int SHADER_STAR = 4;
    private static final int SHADER_SUN = 21;
    private static final int SHADER_COMET = 22;
    private static final String[] SKY_MODES = new String[]{
        "WebShader",
        "Aurora",
        "Pulse",
        "Neon",
        "Smoke",
        "Ripple",
        "Crystal",
        "Plasma2",
        "Nebula",
        "Galaxy",
        "Ocean",
        "Sunset",
        "Cloud",
        "Shimmer",
        "Prism",
        "Radiance"
    };

    private BlockOutlineShaderUtil() {
    }

    public static void render(float[] vertices, int drawMode, Matrix4f modelView, float opacity, float lineWidth, String shaderName, float speed) {
        render(vertices, drawMode, modelView, opacity, lineWidth, shaderName, speed, Float.NaN, 16777215);
    }

    public static void render(float[] vertices, int drawMode, Matrix4f modelView, float opacity, float lineWidth, String shaderName, float speed, float seed) {
        render(vertices, drawMode, modelView, opacity, lineWidth, shaderName, speed, seed, 16777215);
    }

    public static void render(
        float[] vertices, int drawMode, Matrix4f modelView, float opacity, float lineWidth, String shaderName, float speed, float seed, int tintRgb
    ) {
        render(vertices, drawMode, modelView, opacity, lineWidth, shaderName, speed, seed, tintRgb, false);
    }

    public static void render(
        float[] vertices,
        int drawMode,
        Matrix4f modelView,
        float opacity,
        float lineWidth,
        String shaderName,
        float speed,
        float seed,
        int tintRgb,
        boolean threeD
    ) {
        renderInternal(vertices, drawMode, modelView, opacity, lineWidth, shaderName, speed, seed, tintRgb, threeD, false);
    }

    public static void renderRing(float[] vertices, int drawMode, Matrix4f modelView, float opacity, String shaderName, float speed, float seed, int tintRgb) {
        renderInternal(vertices, drawMode, modelView, opacity, 1.0F, shaderName, speed, seed, tintRgb, false, true);
    }

    private static void renderInternal(
        float[] vertices,
        int drawMode,
        Matrix4f modelView,
        float opacity,
        float lineWidth,
        String shaderName,
        float speed,
        float seed,
        int tintRgb,
        boolean threeD,
        boolean ringMask
    ) {
        if (vertices != null && vertices.length >= 18 && modelView != null && RenderSystem.tryGetDevice() != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer != null) {
                GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : framebuffer.getColorAttachmentView();
                if (colorView != null) {
                    GpuTextureView depthView = framebuffer.useDepthAttachment
                        ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthAttachmentView())
                        : null;
                    int type = resolveShader(shaderName);
                    int vertexCount = vertices.length / 6;
                    BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
                    Vector3f tmp = new Vector3f();

                    for (int i = 0; i < vertexCount; i++) {
                        int o = i * 6;
                        tmp.set(vertices[o], vertices[o + 1], vertices[o + 2]);
                        modelView.transformPosition(tmp);
                        float lx = vertices[o + 3];
                        float ly = vertices[o + 4];
                        float lz = vertices[o + 5];
                        bb.vertex(tmp.x, tmp.y, tmp.z).texture(lx + ly * 0.37F, lz + ly * 0.29F);
                    }

                    BuiltBuffer built = bb.endNullable();
                    if (built != null) {
                        float seedVal = Float.isNaN(seed) ? computeSeed(vertices) : normalizeSeed(seed);
                        float time = (float)(System.currentTimeMillis() - startedAt) / 1000.0F * Math.max(0.01F, speed);
                        float resW = Math.max(1.0F, mc.getWindow().getFramebufferWidth());
                        float resH = Math.max(1.0F, mc.getWindow().getFramebufferHeight());
                        float op = Math.max(0.0F, Math.min(1.0F, opacity));
                        float tr = (tintRgb >> 16 & 0xFF) / 255.0F;
                        float tg = (tintRgb >> 8 & 0xFF) / 255.0F;
                        float tb = (tintRgb & 0xFF) / 255.0F;
                        Matrix4f tintMat = new Matrix4f().m00(tr).m01(tg).m02(tb);
                        GpuBufferSlice uniforms = RenderSystem.getDynamicUniforms()
                            .write(
                                RenderSystem.getModelViewMatrix(),
                                new Vector4f(time, op, seedVal, type),
                                new Vector3f(resW, resH, ringMask ? 1.0F : 0.0F),
                                tintMat
                            );
                        BuiltBuffer var30 = built;

                        try {
                            RenderPipeline pipeline = threeD ? PIPELINE_3D : PIPELINE;
                            GpuBuffer vertexBuffer = pipeline.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
                            ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(built.getDrawParameters().comp_752());
                            GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(built.getDrawParameters().comp_751());
                            IndexType indexType = shapeIndexBuffer.getIndexType();

                            try (RenderPass pass = RenderSystem.getDevice()
                                    .createCommandEncoder()
                                    .createRenderPass(() -> "FluxBlockShader", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                                pass.setPipeline(pipeline);
                                RenderSystem.bindDefaultUniforms(pass);
                                pass.setUniform("DynamicTransforms", uniforms);
                                pass.setVertexBuffer(0, vertexBuffer);
                                pass.setIndexBuffer(indexBuffer, indexType);
                                pass.drawIndexed(0, 0, built.getDrawParameters().comp_751(), 1);
                            }
                        } catch (Throwable var42) {
                            if (built != null) {
                                try {
                                    var30.close();
                                } catch (Throwable var39) {
                                    var42.addSuppressed(var39);
                                }
                            }

                            throw var42;
                        }

                        if (built != null) {
                            built.close();
                        }
                    }
                }
            }
        }
    }

    private static int resolveShader(String shaderName) {
        if ("Плазма".equalsIgnoreCase(shaderName)) {
            return 1;
        }

        if ("Вода".equalsIgnoreCase(shaderName)) {
            return 2;
        }

        if ("Тень".equalsIgnoreCase(shaderName)) {
            return 3;
        }

        if ("Звёзды".equalsIgnoreCase(shaderName)) {
            return 4;
        }

        if ("Солнце".equalsIgnoreCase(shaderName)) {
            return 21;
        }

        if ("Комета".equalsIgnoreCase(shaderName)) {
            return 22;
        }

        for (int i = 0; i < SKY_MODES.length; i++) {
            if (SKY_MODES[i].equalsIgnoreCase(shaderName)) {
                return 5 + i;
            }
        }

        return 0;
    }

    private static float computeSeed(float[] vertices) {
        if (vertices != null && vertices.length >= 6) {
            float x = vertices[0];
            float y = vertices[1];
            float z = vertices[2];
            float h = (float)Math.sin(x * 12.9898F + y * 78.233F + z * 37.719F + vertices.length * 0.001F) * 43758.547F;
            return normalizeSeed(h);
        } else {
            return 0.0F;
        }
    }

    private static float normalizeSeed(float value) {
        return !Float.isFinite(value) ? 0.0F : value - (float)Math.floor(value);
    }
}
