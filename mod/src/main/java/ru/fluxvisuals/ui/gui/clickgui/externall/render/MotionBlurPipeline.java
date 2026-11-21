package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;

public final class MotionBlurPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("client", "pipeline/motion_blur");
    private static final Identifier VERTEX_SHADER = Identifier.of("client", "core/motion_blur");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("client", "core/motion_blur");

    private static final int BUFFER_SIZE = 16;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("MotionBlurData", UniformType.UNIFORM_BUFFER)
                    .withSampler("CurrentSampler")
                    .withSampler("HistorySampler")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuTexture currentTexture;
    private static GpuTextureView currentTextureView;
    private static GpuTexture historyTexture;
    private static GpuTextureView historyTextureView;
    private static ByteBuffer dataBuffer;
    private static int lastWidth = 0;
    private static int lastHeight = 0;
    private static boolean hasHistory = false;
    private static boolean initialized = false;

    private MotionBlurPipeline() {}

    private static void ensureInitialized() {
        if (initialized) return;

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:motion_blur_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:motion_blur_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                BUFFER_SIZE
        );

        initialized = true;
    }

    private static void ensureTextures(int width, int height) {
        if (currentTexture != null && historyTexture != null && width == lastWidth && height == lastHeight) return;

        cleanupTextures();

        currentTexture = RenderSystem.getDevice().createTexture(
                () -> "client:motion_blur_current",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        currentTextureView = RenderSystem.getDevice().createTextureView(currentTexture);

        historyTexture = RenderSystem.getDevice().createTexture(
                () -> "client:motion_blur_history",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        historyTextureView = RenderSystem.getDevice().createTextureView(historyTexture);

        lastWidth = width;
        lastHeight = height;
        hasHistory = false;
    }

    public static void draw(float blurFactor, int mode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) return;
        if (client.getFramebuffer().getColorAttachment() == null || client.getFramebuffer().getColorAttachmentView() == null) return;

        ensureInitialized();

        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        if (width <= 0 || height <= 0) return;
        ensureTextures(width, height);

        DrawBatcher.flushPending();

        dataBuffer.clear();
        dataBuffer.putFloat(blurFactor);
        dataBuffer.putFloat(hasHistory ? 1.0f : 0.0f);
        dataBuffer.putFloat((float) mode);
        dataBuffer.putFloat(0.0f);
        dataBuffer.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), currentTexture, 0, 0, 0, 0, 0, width, height);
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "client:motion_blur_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("CurrentSampler", currentTextureView, sampler);
            renderPass.bindTexture("HistorySampler", historyTextureView, sampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("MotionBlurData", uniformBuffer);

            renderPass.draw(0, 6);
        }

        encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), historyTexture, 0, 0, 0, 0, 0, width, height);
        hasHistory = true;
    }

    public static void reset() {
        hasHistory = false;
    }

    private static void cleanupTextures() {
        if (currentTextureView != null) {
            currentTextureView.close();
            currentTextureView = null;
        }
        if (currentTexture != null) {
            currentTexture.close();
            currentTexture = null;
        }
        if (historyTextureView != null) {
            historyTextureView.close();
            historyTextureView = null;
        }
        if (historyTexture != null) {
            historyTexture.close();
            historyTexture = null;
        }
        hasHistory = false;
    }

    public static void close() {
        cleanupTextures();
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
        hasHistory = false;
        initialized = false;
    }
}

