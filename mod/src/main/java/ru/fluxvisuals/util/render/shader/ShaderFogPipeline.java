package ru.fluxvisuals.util.render.shader;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryUtil;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.OptionalInt;

/**
 * Рендер шейдерного неба поверх цветового буфера. Перенесено из GodWeer.
 * Рисует полный экранный треугольник через кастомный RenderPipeline
 * (см. {@link ClientPipelines}) с uniform-буфером ShaderFogData.
 */
public class ShaderFogPipeline {
   private static final Vector4f COLOR_MODULATOR = new Vector4f(1f, 1f, 1f, 1f);
   private static final Vector3f MODEL_OFFSET = new Vector3f(0f, 0f, 0f);
   private static final Matrix4f TEXTURE_MATRIX = new Matrix4f();
   private static final int BUFFER_SIZE = 64;

   private GpuBuffer uniformBuffer;
   private GpuBuffer dummyVertexBuffer;
   private ByteBuffer dataBuffer;
   private boolean initialized;

   public void render(GpuTextureView target, ShaderFogMode mode, int w, int h, float yaw, float pitch, Color color,
                      float time, float alpha, float speed, float scale, float intensity, float fov) {
      if (target == null || w <= 0 || h <= 0) return;

      ensureInitialized();
      prepareUniformData(w, h, yaw, pitch, color, time, alpha, speed, scale, intensity, fov);

      CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
      uploadUniform(encoder);

      GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().write(
            RenderSystem.getModelViewMatrix(),
            COLOR_MODULATOR,
            MODEL_OFFSET,
            TEXTURE_MATRIX
      );

      String passName;
      RenderPipeline pipeline;

      switch (mode) {
         case CAUSTIC:
            passName = "fluxvisuals:sky_caustic";
            pipeline = ClientPipelines.SKY_CAUSTIC_PIPELINE;
            break;
         case DRAIN:
            passName = "fluxvisuals:sky_drain";
            pipeline = ClientPipelines.SKY_DRAIN_PIPELINE;
            break;
         case NEBULA:
            passName = "fluxvisuals:sky_nebula";
            pipeline = ClientPipelines.SKY_NEBULA_PIPELINE;
            break;
         case PLASMA:
            passName = "fluxvisuals:sky_plasma";
            pipeline = ClientPipelines.SKY_PLASMA_PIPELINE;
            break;
         case BLOOM:
         default:
            passName = "fluxvisuals:sky_bloom";
            pipeline = ClientPipelines.SKY_BLOOM_PIPELINE;
            break;
      }

      try (RenderPass renderPass = encoder.createRenderPass(() -> passName, target, OptionalInt.empty())) {
         renderPass.setPipeline(pipeline);
         renderPass.setVertexBuffer(0, dummyVertexBuffer);
         RenderSystem.bindDefaultUniforms(renderPass);
         renderPass.setUniform("DynamicTransforms", dynamicTransforms);
         renderPass.setUniform("ShaderFogData", uniformBuffer);
         renderPass.draw(0, 6);
      }
   }

   private void ensureInitialized() {
      if (initialized) return;

      dataBuffer = MemoryUtil.memAlloc(BUFFER_SIZE);
      dummyVertexBuffer = createDummyVertexBuffer("fluxvisuals:shader_fog_dummy_vertex");
      initialized = true;
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
