package ru.fluxvisuals.util.render.shader;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;

/**
 * Кастомные RenderPipeline для шейдерного неба (ShaderFog). Регистрируются через
 * {@link RenderPipelines#register(RenderPipeline)} — полный экранный треугольник
 * (VertexFormats.EMPTY) + vertex/fragment шейдеры из assets/fluxvisuals/shaders/core.
 */
public final class ClientPipelines {

   private ClientPipelines() {}

   public static final RenderPipeline SKY_CAUSTIC_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
               .withLocation("pipeline/caustic")
               .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
               .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
               .withFragmentShader(ResourceProvider.getShaderIdentifier("caustic"))
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
               .build()
   );

   public static final RenderPipeline SKY_DRAIN_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
               .withLocation("pipeline/drain")
               .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
               .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
               .withFragmentShader(ResourceProvider.getShaderIdentifier("drain"))
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
               .build()
   );

   public static final RenderPipeline SKY_NEBULA_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
               .withLocation("pipeline/nebula")
               .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
               .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
               .withFragmentShader(ResourceProvider.getShaderIdentifier("nebula"))
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
               .build()
   );

   public static final RenderPipeline SKY_PLASMA_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
               .withLocation("pipeline/plasma")
               .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
               .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
               .withFragmentShader(ResourceProvider.getShaderIdentifier("plasma"))
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
               .build()
   );

   public static final RenderPipeline SKY_BLOOM_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
               .withLocation("pipeline/bloom")
               .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
               .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
               .withFragmentShader(ResourceProvider.getShaderIdentifier("bloom"))
               .withBlend(BlendFunction.TRANSLUCENT)
               .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withCull(false)
               .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
               .build()
   );
}
