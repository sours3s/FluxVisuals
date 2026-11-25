package ru.fluxvisuals.util.render.shader;

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
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

/**
 * Рендер шейдерного неба поверх цветового буфера.
 *
 * Процедурные шейдеры неба очень дорогие на полном разрешении (несколько октав
 * 3D-шума и тригонометрии на каждый пиксель). Поэтому сложная часть рисуется в
 * офскрин-текстуру в 1/3 разрешения, а на экран переносится одним дешёвым blit-ом.
 * Небо — низкочастотная картинка, поэтому LINEAR-апскейл выглядит так же, а
 * стоимость фрагментного шейдера падает примерно в 9 раз.
 *
 * Итоговый blit сохраняет исходное поведение: EQUAL_DEPTH_TEST против depth-буфера
 * кадра и то же TRANSLUCENT-смешивание с альфой шейдера.
 */
public class ShaderFogPipeline {
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
   private static final Vector3f MODEL_OFFSET = new Vector3f(0f, 0f, 0f);
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int BUFFER_SIZE = 64;

   private static final RenderPipeline BLIT_PIPELINE = RenderPipeline.builder()
         .withLocation(Identifier.of("fluxvisuals", "pipeline/shader_fog_blit"))
         .withVertexShader(Identifier.of("fluxvisuals", "core/framebuffer_blit"))
         .withFragmentShader(Identifier.of("fluxvisuals", "core/framebuffer_blit"))
         .withVertexFormat(VertexFormats.EMPTY, DrawMode.TRIANGLES)
         .withSampler("Sampler0")
         .withBlend(BlendFunction.TRANSLUCENT)
         .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
         .withDepthWrite(false)
         .withCull(false)
         .build();

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private GpuTexture skyTexture;
   private GpuTextureView skyTextureView;
   private int skyWidth;
   private int skyHeight;
   private boolean initialized;

   public void render(GpuTextureView target, GpuTextureView depthTarget, ShaderFogMode mode, int w, int h,
                      float yaw, float pitch, Color color, float time, float alpha, float speed, float scale,
                      float intensity, float fov) {
      if (target == null || w <= 0 || h <= 0) return;

      int renderW = ShaderFogQuality.internalSize(w);
      int renderH = ShaderFogQuality.internalSize(h);

      ensureInitialized();
      ensureSkyTarget(renderW, renderH);
      prepareUniformData(renderW, renderH, yaw, pitch, color, time, alpha, speed, scale, intensity, fov);

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      uploadUniform(encoder);

      GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
            RenderSystem.getModelViewMatrix(),
            COLOR_MODULATOR,
            MODEL_OFFSET,
            TEXTURE_MATRIX
      );

      String passName;
      RenderPipeline offscreenPipeline;
      switch (mode) {
         case CAUSTIC:
            passName = "fluxvisuals:sky_caustic";
            offscreenPipeline = ClientPipelines.SKY_CAUSTIC_OFFSCREEN_PIPELINE;
            break;
         case DRAIN:
            passName = "fluxvisuals:sky_drain";
            offscreenPipeline = ClientPipelines.SKY_DRAIN_OFFSCREEN_PIPELINE;
            break;
         case NEBULA:
            passName = "fluxvisuals:sky_nebula";
            offscreenPipeline = ClientPipelines.SKY_NEBULA_OFFSCREEN_PIPELINE;
            break;
         case PLASMA:
            passName = "fluxvisuals:sky_plasma";
            offscreenPipeline = ClientPipelines.SKY_PLASMA_OFFSCREEN_PIPELINE;
            break;
         case BLOOM:
         default:
            passName = "fluxvisuals:sky_bloom";
            offscreenPipeline = ClientPipelines.SKY_BLOOM_OFFSCREEN_PIPELINE;
            break;
      }

      // Дорогой проход: fullscreen-треугольник в офскрин в 1/3 разрешения.
      try (RenderPass renderPass = encoder.createRenderPass(
            () -> passName, skyTextureView, OptionalInt.empty())) {
         renderPass.setPipeline(offscreenPipeline);
         renderPass.setVertexBuffer(0, dummyVertexBuffer);
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.setUniform("DynamicTransforms", dynamicTransforms);
         renderPass.setUniform("ShaderFogData", uniformBuffer);
         renderPass.draw(0, 6);
      }

      // Дешёвый проход: LINEAR-апскейл обратно в кадр с исходными depth/blend.
      GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
      try (RenderPass blitPass = encoder.createRenderPass(
            () -> passName + "_blit", target, OptionalInt.empty(), depthTarget, OptionalDouble.empty())) {
         blitPass.setPipeline(BLIT_PIPELINE);
         blitPass.setVertexBuffer(0, dummyVertexBuffer);
         blitPass.bindTexture("Sampler0", skyTextureView, sampler);
         blitPass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (initialized) return;

      dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
      dummyVertexBuffer = createDummyVertexBuffer("fluxvisuals:shader_fog_dummy_vertex");
      initialized = true;
   }

   private void ensureSkyTarget(int width, int height) {
      if (skyTexture != null && skyWidth == width && skyHeight == height) return;

      closeSkyTarget();
      skyTexture = RenderSystem.getDevice().createTexture(
            () -> "fluxvisuals:shader_fog_sky",
            GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
            TextureFormat.RGBA8, width, height, 1, 1
      );
      skyTextureView = RenderSystem.getDevice().createTextureView(skyTexture);
      skyWidth = width;
      skyHeight = height;
   }

   private void closeSkyTarget() {
      if (skyTextureView != null) {
         skyTextureView.close();
         skyTextureView = null;
      }
      if (skyTexture != null) {
         skyTexture.close();
         skyTexture = null;
      }
      skyWidth = 0;
      skyHeight = 0;
   }

   private void prepareUniformData(float w, float h, float yaw, float pitch, Color color, float time, float alpha,
                                   float speed, float scale, float intensity, float fov) {
      dataBuffer.clear();
      dataBuffer.putFloat(w);
      dataBuffer.putFloat(h);
      dataBuffer.putFloat(yaw);
      dataBuffer.putFloat(pitch);
      dataBuffer.putFloat(color.getRed() / 255.0f);
      dataBuffer.putFloat(color.getGreen() / 255.0f);
      dataBuffer.putFloat(color.getBlue() / 255.0f);
      dataBuffer.putFloat(time);
      dataBuffer.putFloat(alpha);
      dataBuffer.putFloat(speed);
      dataBuffer.putFloat(scale);
      dataBuffer.putFloat(intensity);
      dataBuffer.putFloat(fov);
      dataBuffer.putFloat(0.0f);
      dataBuffer.putFloat(0.0f);
      dataBuffer.putFloat(0.0f);
      dataBuffer.flip();
   }

   private void uploadUniform(CommandEncoder encoder) {
      int size = dataBuffer.remaining();
      if (uniformBuffer == null || uniformBuffer.size() < size) {
         if (uniformBuffer != null) uniformBuffer.close();
         uniformBuffer = RenderSystem.getDevice().createBuffer(
               () -> "fluxvisuals:shader_fog_uniform",
               GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
               size
         );
      }
      encoder.writeToBuffer(uniformBuffer.slice(), dataBuffer);
   }

   private static GpuBuffer createDummyVertexBuffer(String name) {
      ByteBuffer dummyData = MemoryUtil.memAlloc(4);
      try {
         dummyData.putInt(0);
         dummyData.flip();
         return RenderSystem.getDevice().createBuffer(() -> name, GpuBuffer.USAGE_VERTEX, dummyData);
      } finally {
         MemoryUtil.memFree(dummyData);
      }
   }

   public void close() {
      closeSkyTarget();
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
      initialized = false;
   }
}
