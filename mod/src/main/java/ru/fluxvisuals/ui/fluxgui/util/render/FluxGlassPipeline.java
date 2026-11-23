package ru.fluxvisuals.ui.fluxgui.util.render;

import ru.fluxvisuals.ui.fluxgui.module.render.Optimization;
import ru.fluxvisuals.util.render.backends.gl.GlState;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

public final class FluxGlassPipeline {
    private static final int BUFFER_SIZE = 256;
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("fluxvisuals", "pipeline/flux_glass"))
        .withVertexShader(Identifier.of("fluxvisuals", "core/flux_glass_vertex"))
        .withFragmentShader(Identifier.of("fluxvisuals", "core/flux_glass_fragment"))
        .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
        .withUniform("FluxGlassData", UniformType.UNIFORM_BUFFER)
        .withSampler("Sampler0")
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .withCull(false)
        .build();
    private static final Vector4f COLOR_MODULATOR = new Vector4f(1.0F, 1.0F, 1.0F, 1.0F);
    private static final Vector3f MODEL_OFFSET = new Vector3f(0.0F, 0.0F, 0.0F);
    private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture sourceTexture;
    private static GpuTextureView sourceTextureView;
    private static int lastWidth;
    private static int lastHeight;
    private static TextureFormat lastFormat;
    private static long lastCaptureNanos;
    private static boolean hudFrameCaptureLocked;
    private static boolean initialized;
    private static final Color DEFAULT_TINT = new Color(206, 212, 226, 30);

    private FluxGlassPipeline() {
    }

    public static void init() {
        if (!initialized) {
            dataBuffer = MemoryUtil.memAlloc(256);
            ByteBuffer dummyData = MemoryUtil.memAlloc(4);
            dummyData.putInt(0);
            dummyData.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "FluxVisuals:flux_glass_dummy_vertex", 32, dummyData);
            MemoryUtil.memFree(dummyData);
            initialized = true;
        }
    }

    public static void captureFrame() {
        if (!hudFrameCaptureLocked) {
            captureFrame(false);
        }
    }

    public static void beginRenderFrame() {
        hudFrameCaptureLocked = false;
    }

    public static void captureHudFrame() {
        hudFrameCaptureLocked = false;
        captureFrame(true);
        hudFrameCaptureLocked = true;
    }

    private static void captureFrame(boolean force) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getFramebuffer() != null && client.getFramebuffer().getColorAttachment() != null) {
            long now = System.nanoTime();
            if (force || sourceTexture == null || now - lastCaptureNanos >= Optimization.glassCaptureCoalesceNanos()) {
                init();
                int fbWidth = client.getFramebuffer().textureWidth;
                int fbHeight = client.getFramebuffer().textureHeight;
                ensureTexture(fbWidth, fbHeight, client.getFramebuffer().getColorAttachment().getFormat());
                // Blit может оставить другой FBO привязанным; это рвёт отложенный
                // GuiRenderer и делает иконки предметов нестабильными. Возвращаем
                // привязку, но не трогаем read/draw buffer (ломают GUI-проход).
                GlState.Snapshot beforeBlit = GlState.push();
                try {
                    FramebufferBlitter.blit(client.getFramebuffer().getColorAttachmentView(), sourceTextureView);
                } finally {
                    GlState.pop(beforeBlit, false);
                }
                lastCaptureNanos = now;
            }
        }
    }

    public static void draw(
        Matrix4f matrix, float x, float y, float width, float height, float radius, float blur, float distortion, float shine, Color tint, float alpha, float z
    ) {
        draw(matrix, x, y, width, height, radius, blur, distortion, shine, tint, alpha, z, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static void drawAt(DrawContext context, float x, float y, float w, float h, float radius, float shine, float alpha, Color tint) {
        Matrix3x2fStack gm = context.getMatrices();
        Vector2f tl = gm.transformPosition(x, y, new Vector2f());
        float sx = (float)Math.sqrt(gm.m00() * gm.m00() + gm.m01() * gm.m01());
        float sy = (float)Math.sqrt(gm.m10() * gm.m10() + gm.m11() * gm.m11());
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, sw, sh, 0.0F, -1000.0F, 1000.0F);
        draw(ortho, tl.x, tl.y, w * sx, h * sy, radius * sx, 3.0F, 12.0F, shine, tint != null ? tint : DEFAULT_TINT, alpha, 0.0F);
    }

    public static void drawLiquidAt(
        DrawContext context, float x, float y, float w, float h, float radius, float blur, float distortion, float shine, float alpha, Color tint
    ) {
        drawLiquidAt(context, x, y, w, h, radius, blur, distortion, shine, alpha, tint, false);
    }

    public static void drawLiquidAt(
        DrawContext context,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float blur,
        float distortion,
        float shine,
        float alpha,
        Color tint,
        boolean strongBlur
    ) {
        drawLiquidAtClipped(context, x, y, w, h, radius, blur, distortion, shine, alpha, tint, 0.0F, 0.0F, 0.0F, 0.0F, strongBlur);
    }

    public static void drawLiquidAtClipped(
        DrawContext context,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float blur,
        float distortion,
        float shine,
        float alpha,
        Color tint,
        float clipX,
        float clipY,
        float clipW,
        float clipH
    ) {
        drawLiquidAtClipped(context, x, y, w, h, radius, blur, distortion, shine, alpha, tint, clipX, clipY, clipW, clipH, false);
    }

    public static void drawLiquidAtClipped(
        DrawContext context,
        float x,
        float y,
        float w,
        float h,
        float radius,
        float blur,
        float distortion,
        float shine,
        float alpha,
        Color tint,
        float clipX,
        float clipY,
        float clipW,
        float clipH,
        boolean strongBlur
    ) {
        Matrix3x2fStack gm = context.getMatrices();
        Vector2f tl = gm.transformPosition(x, y, new Vector2f());
        float sx = (float)Math.sqrt(gm.m00() * gm.m00() + gm.m01() * gm.m01());
        float sy = (float)Math.sqrt(gm.m10() * gm.m10() + gm.m11() * gm.m11());
        Vector2f clipTl = gm.transformPosition(clipX, clipY, new Vector2f());
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, sw, sh, 0.0F, -1000.0F, 1000.0F);
        drawInternal(
            ortho,
            tl.x,
            tl.y,
            w * sx,
            h * sy,
            radius * Math.min(sx, sy),
            Math.max(0.75F, blur * Math.max(sx, sy)),
            distortion * Math.max(sx, sy),
            shine,
            tint != null ? tint : DEFAULT_TINT,
            alpha,
            0.0F,
            clipTl.x,
            clipTl.y,
            clipW * sx,
            clipH * sy,
            strongBlur ? 2.0F : 0.0F
        );
    }

    public static void drawBlurAt(DrawContext context, float x, float y, float w, float h, float radius, float blur, float alpha) {
        drawBlurAtClipped(context, x, y, w, h, radius, blur, alpha, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static void drawBlurAtClipped(
        DrawContext context, float x, float y, float w, float h, float radius, float blur, float alpha, float clipX, float clipY, float clipW, float clipH
    ) {
        Matrix3x2fStack gm = context.getMatrices();
        Vector2f tl = gm.transformPosition(x, y, new Vector2f());
        float sx = (float)Math.sqrt(gm.m00() * gm.m00() + gm.m01() * gm.m01());
        float sy = (float)Math.sqrt(gm.m10() * gm.m10() + gm.m11() * gm.m11());
        Vector2f clipTl = gm.transformPosition(clipX, clipY, new Vector2f());
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, sw, sh, 0.0F, -1000.0F, 1000.0F);
        drawInternal(
            ortho,
            tl.x,
            tl.y,
            w * sx,
            h * sy,
            radius * Math.min(sx, sy),
            Math.max(0.75F, blur),
            0.0F,
            0.0F,
            new Color(0, 0, 0, 0),
            alpha,
            0.0F,
            clipTl.x,
            clipTl.y,
            clipW * sx,
            clipH * sy,
            1.0F
        );
    }

    public static void drawBlurScreen(float x, float y, float w, float h, float radius, float blur, float alpha) {
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        Matrix4f ortho = new Matrix4f().setOrtho(0.0F, sw, sh, 0.0F, -1000.0F, 1000.0F);
        drawInternal(ortho, x, y, w, h, radius, Math.max(0.75F, blur), 0.0F, 0.0F, new Color(0, 0, 0, 0), alpha, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
    }

    public static void draw(
        Matrix4f matrix,
        float x,
        float y,
        float width,
        float height,
        float radius,
        float blur,
        float distortion,
        float shine,
        Color tint,
        float alpha,
        float z,
        float clipX,
        float clipY,
        float clipW,
        float clipH
    ) {
        drawInternal(matrix, x, y, width, height, radius, blur, distortion, shine, tint, alpha, z, clipX, clipY, clipW, clipH, 0.0F);
    }

    private static void drawInternal(
        Matrix4f matrix,
        float x,
        float y,
        float width,
        float height,
        float radius,
        float blur,
        float distortion,
        float shine,
        Color tint,
        float alpha,
        float z,
        float clipX,
        float clipY,
        float clipW,
        float clipH,
        float effectMode
    ) {
        if (!(alpha <= 0.01F) && !(width <= 0.5F) && !(height <= 0.5F)) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.getFramebuffer() != null && client.getFramebuffer().getColorAttachment() != null) {
                init();
                if (sourceTexture == null) {
                    captureFrame();
                }

                if (sourceTexture != null) {
                    int screenW = client.getWindow().getScaledWidth();
                    int screenH = client.getWindow().getScaledHeight();
                    CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
                    prepareData(matrix, x, y, width, height, screenW, screenH, radius, blur, distortion, shine, tint, alpha, z, effectMode);
                    encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
                    GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                    GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms()
                        .write(RenderSystem.getModelViewMatrix(), COLOR_MODULATOR, MODEL_OFFSET, TEXTURE_MATRIX);

                    try (RenderPass pass = encoder.createRenderPass(
                            () -> "FluxVisuals:flux_glass",
                            client.getFramebuffer().getColorAttachmentView(),
                            OptionalInt.empty(),
                            null,
                            OptionalDouble.empty()
                        )) {
                        pass.setPipeline(PIPELINE);
                        pass.setVertexBuffer(0, dummyVertexBuffer);
                        pass.bindTexture("Sampler0", sourceTextureView, sampler);
                        RenderSystem.bindDefaultUniforms(pass);
                        pass.setUniform("DynamicTransforms", dynamicTransforms);
                        pass.setUniform("FluxGlassData", uniformBuffer);
                        if (clipW > 0.5F && clipH > 0.5F) {
                            int fbH = client.getWindow().getFramebufferHeight();
                            int sf = client.getWindow().getScaleFactor();
                            int px = Math.round(clipX * sf);
                            int pw = Math.max(0, Math.round(clipW * sf));
                            int ph = Math.max(0, Math.round(clipH * sf));
                            int py = fbH - Math.round((clipY + clipH) * sf);
                            pass.enableScissor(px, py, pw, ph);
                        }

                        pass.draw(0, 6);
                    }
                }
            }
        }
    }

    private static void ensureTexture(int fbWidth, int fbHeight, TextureFormat format) {
        if (sourceTexture == null || fbWidth != lastWidth || fbHeight != lastHeight || format != lastFormat) {
            if (sourceTextureView != null) {
                sourceTextureView.close();
                sourceTextureView = null;
            }

            if (sourceTexture != null) {
                sourceTexture.close();
                sourceTexture = null;
            }

            sourceTexture = RenderSystem.getDevice().createTexture(() -> "FluxVisuals:flux_glass_source", 12, format, fbWidth, fbHeight, 1, 1);
            sourceTextureView = RenderSystem.getDevice().createTextureView(sourceTexture);
            lastWidth = fbWidth;
            lastHeight = fbHeight;
            lastFormat = format;
            lastCaptureNanos = 0L;
        }
    }

    private static void prepareData(
        Matrix4f matrix,
        float x,
        float y,
        float w,
        float h,
        int screenW,
        int screenH,
        float radius,
        float blur,
        float distortion,
        float shine,
        Color tint,
        float alpha,
        float z,
        float effectMode
    ) {
        float red = tint.getRed() / 255.0F;
        float green = tint.getGreen() / 255.0F;
        float blue = tint.getBlue() / 255.0F;
        float tintAlpha = tint.getAlpha() / 255.0F;
        float time = (float)(System.currentTimeMillis() % 100000L) / 1000.0F;
        dataBuffer.clear();
        dataBuffer.putFloat(matrix.m00()).putFloat(matrix.m01()).putFloat(matrix.m02()).putFloat(matrix.m03());
        dataBuffer.putFloat(matrix.m10()).putFloat(matrix.m11()).putFloat(matrix.m12()).putFloat(matrix.m13());
        dataBuffer.putFloat(matrix.m20()).putFloat(matrix.m21()).putFloat(matrix.m22()).putFloat(matrix.m23());
        dataBuffer.putFloat(matrix.m30()).putFloat(matrix.m31()).putFloat(matrix.m32()).putFloat(matrix.m33());
        dataBuffer.putFloat(x).putFloat(y).putFloat(w).putFloat(h);
        dataBuffer.putFloat(screenW).putFloat(screenH).putFloat(0.0F).putFloat(0.0F);
        dataBuffer.putFloat(radius).putFloat(blur).putFloat(distortion).putFloat(shine);
        dataBuffer.putFloat(red).putFloat(green).putFloat(blue).putFloat(tintAlpha);
        dataBuffer.putFloat(alpha).putFloat(z).putFloat(time).putFloat(effectMode);
        dataBuffer.flip();
        ensureBuffer();
    }

    private static void ensureBuffer() {
        int size = dataBuffer.remaining();
        if (uniformBuffer == null || uniformBuffer.size() < size) {
            if (uniformBuffer != null) {
                uniformBuffer.close();
            }

            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "FluxVisuals:flux_glass_uniform", 136, size);
        }
    }

    public static void shutdown() {
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

        if (sourceTextureView != null) {
            sourceTextureView.close();
            sourceTextureView = null;
        }

        if (sourceTexture != null) {
            sourceTexture.close();
            sourceTexture = null;
        }

        initialized = false;
        lastWidth = 0;
        lastHeight = 0;
        lastFormat = null;
        lastCaptureNanos = 0L;
        hudFrameCaptureLocked = false;
    }
}
