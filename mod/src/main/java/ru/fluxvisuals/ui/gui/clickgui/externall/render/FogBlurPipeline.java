package ru.fluxvisuals.ui.gui.clickgui.externall.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.OptionalInt;

public final class FogBlurPipeline {

    private static final int UNIFORM_SIZE = 256;
    private static final float NEAR = 0.05F;

    private static final RenderPipeline PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation(Identifier.of("client", "pipeline/fog_blur"))
                    .withVertexShader(Identifier.of("client", "core/fog_blur"))
                    .withFragmentShader(Identifier.of("client", "core/fog_blur"))
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("FogData", UniformType.UNIFORM_BUFFER)
                    .withSampler("DepthTex")
                    .withSampler("MinecraftTex")
                    .withSampler("BlurTex")
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .build()
    );

    private static GpuBuffer dummyVertexBuffer;
    private static GpuBuffer uniformBuffer;
    private static ByteBuffer uniformData;

    private static GpuTexture colorCopy;
    private static GpuTextureView colorCopyView;
    private static SimpleFramebuffer depthCopy;
    private static KawaseBlurPipeline kawaseBlur;

    private static int lastWidth = -1;
    private static int lastHeight = -1;

    private FogBlurPipeline() {}

    public static boolean draw(float distance, float saturation, boolean clientColor,
                               int color1, int color2, int color3, int color4) {
        if (!ensureBuffers()) return false;

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getFramebuffer() == null || mc.getFramebuffer().getColorAttachment() == null) {
            return false;
        }

        Framebuffer fb = mc.getFramebuffer();
        int fbW = fb.textureWidth;
        int fbH = fb.textureHeight;
        ensureTextures(fbW, fbH);

        if (colorCopy == null || depthCopy == null || fb.getDepthAttachment() == null
                || depthCopy.getDepthAttachment() == null) {
            return false;
        }

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(fb.getColorAttachment(), colorCopy, 0, 0, 0, 0, 0, fbW, fbH);
        encoder.copyTextureToTexture(fb.getDepthAttachment(), depthCopy.getDepthAttachment(), 0, 0, 0, 0, 0, fbW, fbH);

        GpuTextureView depthView = depthCopy.getDepthAttachmentView();
        if (depthView == null) return false;

        GpuTextureView blurredView = kawaseBlur.blur(colorCopy, colorCopyView, fbW, fbH, 3, 2.5f);
        if (blurredView == null) blurredView = colorCopyView;

        float far = mc.options.getViewDistance().getValue() * 16.0F;
        writeUniforms(NEAR, far, distance, saturation, clientColor, color1, color2, color3, color4);

        GpuBufferSlice slice = uniformBuffer.slice(0L, UNIFORM_SIZE);
        encoder.writeToBuffer(slice, uniformData);

        try (RenderPass pass = encoder.createRenderPass(
                () -> "client:fog_blur",
                fb.getColorAttachmentView(),
                OptionalInt.empty()
        )) {
            pass.setPipeline(PIPELINE);
            pass.setVertexBuffer(0, dummyVertexBuffer);
            pass.bindTexture("DepthTex", depthView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            pass.bindTexture("MinecraftTex", colorCopyView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.bindTexture("BlurTex", blurredView, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            pass.setUniform("FogData", slice);
            pass.draw(0, 6);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean ensureBuffers() {
        if (dummyVertexBuffer != null && uniformBuffer != null && uniformData != null) {
            if (kawaseBlur == null) kawaseBlur = new KawaseBlurPipeline();
            return true;
        }
        try {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0).flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "client:fog_blur_dummy_vertex",
                    GpuBuffer.USAGE_VERTEX,
                    dummy
            );
            MemoryUtil.memFree(dummy);

            uniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "client:fog_blur_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    UNIFORM_SIZE
            );
            uniformData = MemoryUtil.memAlloc(UNIFORM_SIZE);
            kawaseBlur = new KawaseBlurPipeline();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static void ensureTextures(int width, int height) {
        if (colorCopy != null && width == lastWidth && height == lastHeight) return;

        if (colorCopyView != null) { colorCopyView.close(); colorCopyView = null; }
        if (colorCopy != null) { colorCopy.close(); colorCopy = null; }
        depthCopy = null;

        colorCopy = RenderSystem.getDevice().createTexture(
                () -> "client:fog_blur_color",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, 1
        );
        colorCopyView = RenderSystem.getDevice().createTextureView(colorCopy);
        depthCopy = new SimpleFramebuffer("client_fog_blur_depth", width, height, true);
        lastWidth = width;
        lastHeight = height;
    }

    private static void writeUniforms(float near, float far, float distance, float saturation,
                                      boolean clientColor, int color1, int color2, int color3, int color4) {
        uniformData.clear();
        uniformData.putFloat(near).putFloat(far).putFloat(distance).putFloat(saturation);
        uniformData.putFloat(clientColor ? 1.0F : 0.0F).putFloat(0.0F).putFloat(0.0F).putFloat(0.0F);
        putColor(uniformData, color1);
        putColor(uniformData, color2);
        putColor(uniformData, color3);
        putColor(uniformData, color4);
        while (uniformData.position() < UNIFORM_SIZE) {
            uniformData.put((byte) 0);
        }
        uniformData.flip();
    }

    private static void putColor(ByteBuffer buf, int argb) {
        buf.putFloat(((argb >> 16) & 0xFF) / 255f);
        buf.putFloat(((argb >> 8) & 0xFF) / 255f);
        buf.putFloat((argb & 0xFF) / 255f);
        buf.putFloat(((argb >>> 24) & 0xFF) / 255f);
    }
}

