package ru.fluxvisuals.ui.fluxgui.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class FramebufferBlitter {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder()
        .withLocation(Identifier.of("fluxvisuals", "pipeline/core/framebuffer_blit"))
        .withVertexShader(Identifier.of("fluxvisuals", "core/framebuffer_blit"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/framebuffer_blit"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withSampler("Sampler0")
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static GpuBuffer dummyVertexBuffer;

    private FramebufferBlitter() {
    }

    public static void blit(GpuTextureView source, GpuTextureView target) {
        if (source != null && target != null && RenderSystem.tryGetDevice() != null) {
            blit(RenderSystem.getDevice().createCommandEncoder(), source, target);
        }
    }

    public static void blit(CommandEncoder encoder, GpuTextureView source, GpuTextureView target) {
        if (encoder != null && source != null && target != null && RenderSystem.tryGetDevice() != null) {
            init();
            GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);

            try (RenderPass pass = encoder.createRenderPass(() -> "FluxVisuals:framebuffer_blit", target, OptionalInt.empty(), null, OptionalDouble.empty())) {
                pass.setPipeline(PIPELINE);
                pass.setVertexBuffer(0, dummyVertexBuffer);
                pass.bindTexture("Sampler0", source, sampler);
                pass.draw(0, 6);
            }
        }
    }

    private static void init() {
        if (dummyVertexBuffer == null) {
            ByteBuffer dummy = MemoryUtil.memAlloc(4);
            dummy.putInt(0).flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "FluxVisuals:framebuffer_blit_dummy", 40, dummy);
            MemoryUtil.memFree(dummy);
        }
    }
}
