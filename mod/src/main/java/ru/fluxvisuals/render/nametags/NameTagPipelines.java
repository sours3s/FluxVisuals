package ru.fluxvisuals.render.nametags;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;

/** Render pipelines for the ported NameTags shader stack (GLSL 330 + UBO). */
@Environment(EnvType.CLIENT)
public final class NameTagPipelines {
    private static final BlendFunction PREMULTIPLIED = new BlendFunction(
          SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA,
          SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA
    );

    public static final RenderPipeline TEXT = create("text", "nametag_text", "nametag_text", true, PREMULTIPLIED);
    public static final RenderPipeline TEXT_BLUR = create("text_blur", "nametag_text", "nametag_text_blur", true, PREMULTIPLIED);
    public static final RenderPipeline RECT = create("rect", "nametag_rect", "nametag_rect", false, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline LIQUIDGLASS = create("liquidglass_rect", "nametag_liquidglass_rect", "nametag_liquidglass_rect", true, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline SVG = create("svg", "nametag_svg", "nametag_svg", false, BlendFunction.TRANSLUCENT);
    public static final RenderPipeline TEXTURE = create("texture", "nametag_texture", "nametag_texture", true, BlendFunction.TRANSLUCENT);

    private NameTagPipelines() {
    }

    private static RenderPipeline create(String id, String vertex, String fragment, boolean sampler, BlendFunction blend) {
        Identifier location = Identifier.of("fluxvisuals", "pipeline/nametag/" + id);
        Identifier vertexShader = Identifier.of("fluxvisuals", "core/" + vertex);
        Identifier fragmentShader = Identifier.of("fluxvisuals", "core/" + fragment);
        RenderPipeline.Builder builder = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
              .withLocation(location)
              .withVertexShader(vertexShader)
              .withFragmentShader(fragmentShader)
              .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
              .withUniform(NameTagData.UNIFORM_NAME, UniformType.UNIFORM_BUFFER)
              .withBlend(blend)
              .withCull(false)
              .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
              .withDepthWrite(false);
        if (sampler) {
            builder.withSampler("Sampler0");
        }
        return builder.build();
    }
}
