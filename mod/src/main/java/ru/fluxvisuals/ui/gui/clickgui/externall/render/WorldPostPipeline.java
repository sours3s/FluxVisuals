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

public final class WorldPostPipeline {

    private static final Identifier PIPELINE_ID = Identifier.of("client", "pipeline/world_post");
    private static final Identifier VERTEX_SHADER = Identifier.of("client", "core/world_post");
    private static final Identifier FRAGMENT_SHADER = Identifier.of("client", "core/world_post");

    private static final int BUFFER_SIZE = 32;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(PIPELINE_ID)
                    .withVertexShader(VERTEX_SHADER)
                    .withFragmentShader(FRAGMENT_SHADER)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("WorldPostData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static GpuTexture sceneTexture;
    private static GpuTextureView sceneTextureView;
    private static ByteBuffer dataBuffer;
    private static int lastWidth;
    private static int lastHeight;
    private static boolean initialized;

    private WorldPostPipeline() {}

    private static void ensureInitialized() {
        if (initialized) return;

        dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);

        ByteBuffer dummyData = MemoryUtil.memAlloc(4);
        dummyData.putInt(0);
        dummyData.flip();
        dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:world_post_dummy_vertex",
                GpuBuffer.USAGE_VERTEX,
                dummyData
        );
        MemoryUtil.memFree(dummyData);

        uniformBuffer = RenderSystem.getDevice().createBuffer(
                () -> "client:world_post_uniform",
                GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                BUFFER_SIZE
        );

        initialized = true;
    }

    private static void ensureSceneTexture(int width, int height) {
        if (sceneTexture != null && width == lastWidth && height == lastHeight) return;

        if (sceneTextureView != null) {
            sceneTextureView.close();
            sceneTextureView = null;
        }
        if (sceneTexture != null) {
            sceneTexture.close();
            sceneTexture = null;
        }

        sceneTexture = RenderSystem.getDevice().createTexture(
                () -> "client:world_post_scene",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
        lastWidth = width;
        lastHeight = height;
    }

    public static void draw(float saturation, boolean enableWorldColor, int worldColor) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.getFramebuffer() == null) return;
        if (client.getFramebuffer().getColorAttachment() == null || client.getFramebuffer().getColorAttachmentView() == null) return;

        ensureInitialized();

        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        if (width <= 0 || height <= 0) return;
        ensureSceneTexture(width, height);

        DrawBatcher.flushPending();

        dataBuffer.clear();
        dataBuffer.putFloat(saturation);
        dataBuffer.putFloat(enableWorldColor ? 1.0f : 0.0f);
        dataBuffer.putFloat(0.0f);
        dataBuffer.putFloat(0.0f);

        float r = ((worldColor >> 16) & 0xFF) / 255.0f;
        float g = ((worldColor >> 8) & 0xFF) / 255.0f;
        float b = (worldColor & 0xFF) / 255.0f;
        float a = ((worldColor >> 24) & 0xFF) / 255.0f;
        if (a <= 0.001f && worldColor != 0) a = 1.0f;
        dataBuffer.putFloat(r);
        dataBuffer.putFloat(g);
        dataBuffer.putFloat(b);
        dataBuffer.putFloat(a);
        dataBuffer.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(client.getFramebuffer().getColorAttachment(), sceneTexture, 0, 0, 0, 0, 0, width, height);
        encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);

        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

        try (RenderPass renderPass = encoder.createRenderPass(
                () -> "client:world_post_pass",
                client.getFramebuffer().getColorAttachmentView(),
                OptionalInt.empty(),
                client.getFramebuffer().getDepthAttachmentView(),
                OptionalDouble.empty())) {

            renderPass.setPipeline(PIPELINE);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("Sampler0", sceneTextureView, sampler);

            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("WorldPostData", uniformBuffer);

            renderPass.draw(0, 6);
        }
    }

    public static void close() {
        if (sceneTextureView != null) {
            sceneTextureView.close();
            sceneTextureView = null;
        }
        if (sceneTexture != null) {
            sceneTexture.close();
            sceneTexture = null;
        }
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

