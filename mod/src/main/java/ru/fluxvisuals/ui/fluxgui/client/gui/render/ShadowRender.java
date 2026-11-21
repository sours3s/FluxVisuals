package ru.fluxvisuals.ui.fluxgui.client.gui.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class ShadowRender {
    public static final float REACH = 3.0F;
    private static final int BUFFER_SIZE = 128;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/shadow"))
        .withVertexShader(Identifier.of("fluxvisuals", "core/shadow"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/shadow"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("ShadowData", UniformType.UNIFORM_BUFFER)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f();
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer vertexBuffer;
    private static ByteBuffer data;

    private ShadowRender() {
    }

    private static void init() {
        if (data == null) {
            data = MemoryUtil.memAlloc(128);
            ByteBuffer empty = MemoryUtil.memAlloc(4);
            empty.putInt(0);
            empty.flip();
            vertexBuffer = RenderSystem.getDevice().createBuffer(() -> "FluxVisuals:shadow_vertex", 32, empty);
            MemoryUtil.memFree(empty);
        }
    }

    public static void draw(DrawContext context, float x, float y, float width, float height, float radius, float sigma, int color) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null && !(sigma <= 0.0F) && !(width <= 0.0F) && !(height <= 0.0F)) {
            init();
            Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());
            Vector2f corner = matrix.transformPosition(x, y, new Vector2f());
            float scaleX = (float)Math.sqrt(matrix.m00() * matrix.m00() + matrix.m01() * matrix.m01());
            float scaleY = (float)Math.sqrt(matrix.m10() * matrix.m10() + matrix.m11() * matrix.m11());
            float scale = Math.min(scaleX, scaleY);
            float padding = 3.0F * sigma * scale;
            float quadX = corner.x - padding;
            float quadY = corner.y - padding;
            float quadWidth = width * scaleX + padding * 2.0F;
            float quadHeight = height * scaleY + padding * 2.0F;
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();
            Matrix4f projection = new Matrix4f().setOrtho(0.0F, screenWidth, screenHeight, 0.0F, -1000.0F, 1000.0F);
            data.clear();
            putMatrix(projection);
            data.putFloat(quadX).putFloat(quadY).putFloat(quadWidth).putFloat(quadHeight);
            data.putFloat(radius * scale).putFloat(sigma * scale).putFloat(padding).putFloat(0.0F);
            data.putFloat((color >> 16 & 0xFF) / 255.0F)
                .putFloat((color >> 8 & 0xFF) / 255.0F)
                .putFloat((color & 0xFF) / 255.0F)
                .putFloat((color >>> 24) / 255.0F);
            data.flip();
            ensureBuffer();
            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.writeToBuffer(uniformBuffer.slice(), data);
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms()
                .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

            try (RenderPass pass = encoder.createRenderPass(
                    () -> "FluxVisuals:shadow",
                    client.getFramebuffer().getColorAttachmentView(),
                    OptionalInt.empty(),
                    client.getFramebuffer().getDepthAttachmentView(),
                    OptionalDouble.empty()
                )) {
                pass.setPipeline(PIPELINE);
                pass.setVertexBuffer(0, vertexBuffer);
                RenderSystem.bindDefaultUniforms(pass);
                pass.setUniform("DynamicTransforms", transforms);
                pass.setUniform("ShadowData", uniformBuffer);
                pass.draw(0, 6);
            }
        }
    }

    private static void putMatrix(Matrix4f matrix) {
        data.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
        data.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
        data.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
        data.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
    }

    private static void ensureBuffer() {
        int size = data.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }

            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "FluxVisuals:shadow_uniform", 136, size);
        }
    }

    public static void shutdown() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }

        if (vertexBuffer != null) {
            vertexBuffer.close();
            vertexBuffer = null;
        }

        if (data != null) {
            MemoryUtil.memFree(data);
            data = null;
        }
    }
}
