package ru.fluxvisuals.ui.gui.clickgui.externall.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/**
 * Wet World — мокрые блоки с real-time отражениями (SSR) и рябью.
 * Порт из Redux (ez.redux): логика шейдера 1:1, адаптация под Nightix:
 * fullscreen 6 вершин, #version 150, color+depth копии, REPLACE blend.
 */
public final class WetWorldPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("client", "pipeline/wet_world");
    private static final Identifier SHADER_ID = Identifier.of("client", "core/wet_world");

    private static final BlendFunction REPLACE_BLEND = new BlendFunction(
            SourceFactor.ONE, DestFactor.ZERO,
            SourceFactor.ONE, DestFactor.ZERO
    );

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(SHADER_ID)
                    .withFragmentShader(SHADER_ID)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("Uniforms", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withSampler("Sampler1")
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0f, 0f, 0f);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();

    // 3x mat4 (192) + 6x vec4 (96) = 288
    private static final int BUFFER_SIZE = 288;

    private static final WetWorldPipeline INSTANCE = new WetWorldPipeline();

    public static WetWorldPipeline getInstance() {
        return INSTANCE;
    }

    private GpuBuffer uniformBuffer;
    private GpuBuffer dummyVertexBuffer;
    private ByteBuffer dataBuffer;

    private GpuTexture colorCopy;
    private GpuTextureView colorCopyView;
    private GpuTexture depthCopy;
    private GpuTextureView depthCopyView;
    private int lastWidth;
    private int lastHeight;
    private boolean initialized;

    private WetWorldPipeline() {
    }

    private void ensureInitialized() {
        if (initialized) return;

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummy = MemoryUtil.memAlloc(4);
        dummy.putInt(0).flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:wet_world_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummy
        );
        MemoryUtil.memFree(dummy);

        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:wet_world_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                BUFFER_SIZE
        );

        initialized = true;
    }

    private void ensureCopies(int width, int height) {
        if (colorCopy != null && width == lastWidth && height == lastHeight) return;

        cleanupCopies();

        colorCopy = RenderSystem.getDevice().createTexture(
                () -> "client:wet_world_color_copy",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        colorCopyView = RenderSystem.getDevice().createTextureView(colorCopy);

        depthCopy = RenderSystem.getDevice().createTexture(
                () -> "client:wet_world_depth_copy",
                GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
                TextureFormat.DEPTH32,
                width, height, 1, 1
        );
        depthCopyView = RenderSystem.getDevice().createTextureView(depthCopy);

        lastWidth = width;
        lastHeight = height;
    }

    private void cleanupCopies() {
        if (colorCopyView != null) {
            colorCopyView.close();
            colorCopyView = null;
        }
        if (colorCopy != null) {
            colorCopy.close();
            colorCopy = null;
        }
        if (depthCopyView != null) {
            depthCopyView.close();
            depthCopyView = null;
        }
        if (depthCopy != null) {
            depthCopy.close();
            depthCopy = null;
        }
    }

    public static void render(
            Matrix4f projection,
            Matrix4f invProjection,
            Matrix4f viewRot,
            Vec3d cameraPos,
            float fov,
            float reflectionStrength,
            float darkening,
            float time,
            int qualityIndex,
            boolean ripples,
            float rippleSpeed,
            float rainGradient,
            float fogStart,
            float fogEnd,
            float fogActive,
            Color fogColor,
            float fogBrightness
    ) {
        getInstance().draw(
                projection, invProjection, viewRot, cameraPos, fov,
                reflectionStrength, darkening, time, qualityIndex, ripples,
                rippleSpeed, rainGradient, fogStart, fogEnd, fogActive, fogColor, fogBrightness
        );
    }

    public void draw(
            Matrix4f projection,
            Matrix4f invProjection,
            Matrix4f viewRot,
            Vec3d cameraPos,
            float fov,
            float reflectionStrength,
            float darkening,
            float time,
            int qualityIndex,
            boolean ripples,
            float rippleSpeed,
            float rainGradient,
            float fogStart,
            float fogEnd,
            float fogActive,
            Color fogColor,
            float fogBrightness
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Framebuffer fb = client.getFramebuffer();
        if (fb == null) return;
        if (fb.getColorAttachment() == null || fb.getDepthAttachment() == null) return;
        if (fb.getColorAttachmentView() == null) return;

        int width = fb.textureWidth;
        int height = fb.textureHeight;
        if (width <= 0 || height <= 0) return;
        if (projection == null || invProjection == null || viewRot == null || cameraPos == null) return;

        ensureInitialized();
        ensureCopies(width, height);

        prepareUniformData(
                projection, invProjection, viewRot, cameraPos, fov,
                width, height,
                reflectionStrength, darkening, time,
                qualityIndex, ripples, rippleSpeed, rainGradient,
                fogStart, fogEnd, fogActive, fogColor
        );

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(fb.getColorAttachment(), colorCopy, 0, 0, 0, 0, 0, width, height);
        encoder.copyTextureToTexture(fb.getDepthAttachment(), depthCopy, 0, 0, 0, 0, 0, width, height);
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
                RenderSystem.getModelViewMatrix(),
                COLOR_MODULATOR,
                MODEL_OFFSET,
                TEXTURE_MATRIX
        );

        GpuSampler linearSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuSampler nearestSampler = RenderSystem.getSamplerCache().get(FilterMode.NEAREST);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "client:wet_world_pass",
                fb.getColorAttachmentView(),
                OptionalInt.empty(),
                fb.getDepthAttachmentView(),
                OptionalDouble.empty())) {

            pass.setPipeline(PIPELINE);
            pass.setVertexBuffer(0, dummyVertexBuffer);

            pass.bindTexture("Sampler0", colorCopyView, linearSampler);
            pass.bindTexture("Sampler1", depthCopyView, nearestSampler);

            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setUniform("Uniforms", uniformBuffer);

            pass.draw(0, 6);
        }
    }

    private void prepareUniformData(
            Matrix4f projection,
            Matrix4f invProjection,
            Matrix4f viewRot,
            Vec3d cameraPos,
            float fov,
            int width, int height,
            float reflectionStrength,
            float darkening,
            float time,
            int qualityIndex,
            boolean ripples,
            float rippleSpeed,
            float rainGradient,
            float fogStart,
            float fogEnd,
            float fogActive,
            Color fogColor
    ) {
        dataBuffer.clear();

        putMatrix(dataBuffer, invProjection);
        putMatrix(dataBuffer, projection);
        putMatrix(dataBuffer, viewRot);

        dataBuffer.putFloat((float) cameraPos.x);
        dataBuffer.putFloat((float) cameraPos.y);
        dataBuffer.putFloat((float) cameraPos.z);
        dataBuffer.putFloat(fov);

        dataBuffer.putFloat((float) width);
        dataBuffer.putFloat((float) height);
        dataBuffer.putFloat(0f);
        dataBuffer.putFloat(0f);

        dataBuffer.putFloat(reflectionStrength);
        dataBuffer.putFloat(darkening);
        dataBuffer.putFloat(time);
        dataBuffer.putFloat(0f);

        dataBuffer.putFloat((float) qualityIndex);
        dataBuffer.putFloat(ripples ? 1f : 0f);
        dataBuffer.putFloat(rippleSpeed);
        dataBuffer.putFloat(rainGradient);

        dataBuffer.putFloat(fogStart);
        dataBuffer.putFloat(fogEnd);
        dataBuffer.putFloat(fogActive);
        dataBuffer.putFloat(0f);

        if (fogColor != null) {
            dataBuffer.putFloat(fogColor.getRed() / 255f);
            dataBuffer.putFloat(fogColor.getGreen() / 255f);
            dataBuffer.putFloat(fogColor.getBlue() / 255f);
            dataBuffer.putFloat(fogColor.getAlpha() / 255f);
        } else {
            dataBuffer.putFloat(0f);
            dataBuffer.putFloat(0f);
            dataBuffer.putFloat(0f);
            dataBuffer.putFloat(0f);
        }

        dataBuffer.flip();
    }

    private static void putMatrix(ByteBuffer buffer, Matrix4f matrix) {
        buffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
        buffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
        buffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
        buffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
    }

    public void close() {
        cleanupCopies();
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
            dummyVertexBuffer = null;
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree(dataBuffer);
            dataBuffer = null;
        }
        lastWidth = 0;
        lastHeight = 0;
        initialized = false;
    }
}

