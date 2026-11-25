package ru.fluxvisuals.util.render.shader;

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
 *
 * Шейдер считается в текстуру уменьшенного разрешения без смешивания, а на экран
 * его переносит один проход с исходными EQUAL_DEPTH_TEST и TRANSLUCENT-смешиванием.
 */
public final class ClientPipelines {

   private ClientPipelines() {}

   public static final RenderPipeline SKY_CAUSTIC_OFFSCREEN_PIPELINE = sky("caustic_offscreen", "caustic");
   public static final RenderPipeline SKY_DRAIN_OFFSCREEN_PIPELINE = sky("drain_offscreen", "drain");
   public static final RenderPipeline SKY_NEBULA_OFFSCREEN_PIPELINE = sky("nebula_offscreen", "nebula");
   public static final RenderPipeline SKY_PLASMA_OFFSCREEN_PIPELINE = sky("plasma_offscreen", "plasma");
   public static final RenderPipeline SKY_BLOOM_OFFSCREEN_PIPELINE = sky("bloom_offscreen", "bloom");

   private static RenderPipeline sky(String location, String fragment) {
      return RenderPipelines.register(RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
            .withLocation("pipeline/" + location)
            .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
            .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
            .withFragmentShader(ResourceProvider.getShaderIdentifier(fragment))
            // Полноэкранный треугольник целиком покрывает офскрин-цель, depth-буфера нет.
            // Смешивание и depth-test применяются позже, при апскейле в кадр.
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .withDepthWrite(false)
            .withCull(false)
            .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
            .build());
   }
}
