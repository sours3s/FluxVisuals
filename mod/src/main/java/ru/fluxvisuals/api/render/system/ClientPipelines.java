package ru.fluxvisuals.api.render.system;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import ru.fluxvisuals.api.render.util.ResourceProvider;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.*;
import static net.minecraft.client.render.LayeringTransform.VIEW_OFFSET_Z_LAYERING;
import static net.minecraft.client.render.OutputTarget.ITEM_ENTITY_TARGET;

/**
 * Create by daun kvass
 */
public class ClientPipelines {
    public static ru.fluxvisuals.api.render.system.CustomVertexConsumerProvider customVertexConsumerProvider;

    private static GpuBuffer sharedUniformBuffer;
    private static ByteBuffer sharedDataBuffer;
    private static long startMillis = -1;

    public static GpuBuffer getOrCreateUniformBuffer() {
        if (sharedUniformBuffer == null) {
            sharedDataBuffer = MemoryUtil.memAlloc(64);
            sharedUniformBuffer = RenderSystem.getDevice().createBuffer(
                    () -> "godweer:shared_chams_shader_uniform",
                    GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST,
                    64
            );
        }
        return sharedUniformBuffer;
    }

    public static void updateShaderUniforms(float alpha, float speed, float scale, float intensity, Color color) {
        if (startMillis < 0) startMillis = System.currentTimeMillis();

        GpuBuffer buffer = getOrCreateUniformBuffer();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;

        float time = (System.currentTimeMillis() - startMillis) / 1000.0f;
        float fw = mc.getWindow().getFramebufferWidth();
        float fh = mc.getWindow().getFramebufferHeight();

        float yawRad = 0f, pitchRad = 0f, fov = 70f;
        if (mc.gameRenderer != null && mc.gameRenderer.getCamera() != null) {
            Camera cam = mc.gameRenderer.getCamera();
            yawRad = (float) Math.toRadians(-cam.getYaw());
            pitchRad = (float) Math.toRadians(cam.getPitch());
        }
        if (mc.options != null && mc.options.getFov() != null) {
            fov = (float) mc.options.getFov().getValue().intValue();
        }

        sharedDataBuffer.clear();
        sharedDataBuffer.putFloat(fw);
        sharedDataBuffer.putFloat(fh);
        sharedDataBuffer.putFloat(yawRad);
        sharedDataBuffer.putFloat(pitchRad);
        sharedDataBuffer.putFloat(color.getRed() / 255.0f);
        sharedDataBuffer.putFloat(color.getGreen() / 255.0f);
        sharedDataBuffer.putFloat(color.getBlue() / 255.0f);
        sharedDataBuffer.putFloat(time);

        sharedDataBuffer.putFloat(alpha);
        sharedDataBuffer.putFloat(speed);
        sharedDataBuffer.putFloat(scale);
        sharedDataBuffer.putFloat(intensity);

        sharedDataBuffer.putFloat(fov);
        sharedDataBuffer.putFloat(0f);
        sharedDataBuffer.putFloat(0f);
        sharedDataBuffer.putFloat(0f);
        sharedDataBuffer.flip();

        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.writeToBuffer(buffer.slice(), sharedDataBuffer);
    }

    public static void closeUniformBuffer() {
        if (sharedUniformBuffer != null) {
            sharedUniformBuffer.close();
            sharedUniformBuffer = null;
        }
        if (sharedDataBuffer != null) {
            MemoryUtil.memFree(sharedDataBuffer);
            sharedDataBuffer = null;
        }
        startMillis = -1;
    }

    private static RenderPipeline reg(RenderPipeline pipeline) {
        return RenderPipelines.register(pipeline);
    }
    private static final BlendFunction REPLACE_BLEND = new BlendFunction(
            SourceFactor.ONE, DestFactor.ZERO,
            SourceFactor.ONE, DestFactor.ZERO
    );
    public static final RenderLayer HUD = RenderLayer.of("godweer_hud",
            RenderSetup.builder(reg(RenderPipeline.builder(GUI_SNIPPET).withLocation("pipeline/gui").build())).build());

    public static final RenderPipeline OUTLINE_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/china_hat_lines")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                    .build()
    );
    public static final RenderPipeline KAWASE_DOWN_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/kawase_down")
                    .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("kawase_down"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("kawase_down"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );
    public static final RenderPipeline HAND_FIRE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/hand_fire")
                    .withUniform("HandFireData", UniformType.UNIFORM_BUFFER)
                    .withSampler("SceneSampler")
                    .withSampler("BlurSampler")
                    .withSampler("MaskSampler")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("hand_fire"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("hand_fire"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );
    public static final RenderPipeline HAND_TRAIL_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/hand_trail")
                    .withUniform("HandTrailData", UniformType.UNIFORM_BUFFER)
                    .withSampler("PrevTrailSampler")
                    .withSampler("SceneSampler")
                    .withSampler("MaskSampler")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("hand_trail"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("hand_trail"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );

    public static final RenderPipeline MASK_DIFF_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/mask_diff")
                    .withUniform("MaskData", UniformType.UNIFORM_BUFFER)
                    .withSampler("BeforeSampler")
                    .withSampler("AfterSampler")
                    .withSampler("DepthBeforeSampler")
                    .withSampler("DepthAfterSampler")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("mask_diff"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("mask_diff"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );

    public static final RenderPipeline KAWASE_UP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/kawase_up")
                    .withUniform("KawaseData", UniformType.UNIFORM_BUFFER)
                    .withSampler("Sampler0")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("kawase_up"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("kawase_up"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );
    public static final RenderPipeline SKY_WATER_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/water")
                    .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
                    .withVertexShader(ResourceProvider.getShaderIdentifier("shader_fog"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("water"))
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.EQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .build()
    );

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
    public static final RenderPipeline HAND_OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/hand_outline")
                    .withUniform("HandOutlineData", UniformType.UNIFORM_BUFFER)
                    .withSampler("SceneSampler")
                    .withSampler("MaskSampler")
                    .withVertexShader(ResourceProvider.getShaderIdentifier("hand_outline"))
                    .withFragmentShader(ResourceProvider.getShaderIdentifier("hand_outline"))
                    .withBlend(REPLACE_BLEND)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).build()
    );

    public static final RenderLayer OUTLINE = RenderLayer.of("china_hat_lines",
            RenderSetup.builder(OUTLINE_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderPipeline OUTLINE_PIPELINE_NO = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/outline_hat_lines")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                    .build()
    );
    public static final RenderLayer OUTLINE_NO = RenderLayer.of("outline_hat_lines",
            RenderSetup.builder(OUTLINE_PIPELINE_NO).expectedBufferSize(4096).build());



    public static final RenderPipeline NIMB_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/nimb")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final Function<Identifier, RenderLayer> NIMB = Util.memoize(texture -> RenderLayer.of("nimb",
            RenderSetup.builder(NIMB_PIPELINE)
                    .texture("Sampler0", texture)
                    .expectedBufferSize(4096)
                    .build()
    ));

    public static final RenderPipeline QUADS_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/quads")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final RenderLayer QUAD = RenderLayer.of(
            "quads",
            RenderSetup.builder(QUADS_PIPELINE)
                    .expectedBufferSize(1536)
                    .layeringTransform(VIEW_OFFSET_Z_LAYERING)
                    .outputTarget(ITEM_ENTITY_TARGET)
                    .build()
    );
    public static final RenderPipeline CHAMS_WH_MASK_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/chams_mask_wh")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(true)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderLayer CHAMS_MASK_WH = RenderLayer.of("chams_mask_wh",
            RenderSetup.builder(CHAMS_WH_MASK_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderPipeline FILL_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/block_esp_fill")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderLayer FILL = RenderLayer.of("block_esp_fill",
            RenderSetup.builder(FILL_PIPELINE).expectedBufferSize(4096).build());


    public static final RenderPipeline TARGET_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR,VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderPipeline PARTICLE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/particles")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthWrite(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> PARTICLES = Util.memoize(texture -> RenderLayer.of("wparticles",
            RenderSetup.builder(PARTICLE_PIPELINE)
                    .texture("Sampler0", texture)
                    .expectedBufferSize(1536)
                    .build()
    ));
    public static final Function<Identifier, RenderLayer> TARGET_ESP = Util.memoize(texture -> RenderLayer.of("wtex",
            RenderSetup.builder(TARGET_ESP_PIPELINE)
                    .texture("Sampler0", texture)
                    .expectedBufferSize(4096)
                    .build()
    ));

    public static final RenderPipeline GLOW_PIPELINE = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/chams_glow_lines")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderLayer GLOW = RenderLayer.of("chams_glow_lines",
            RenderSetup.builder(GLOW_PIPELINE).expectedBufferSize(1536).build());
    public static final RenderPipeline GLOW_PIPELINE_LRQ = reg(
            RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                    .withLocation("pipeline/sword_glow_lines")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );
    public static final RenderLayer GLOW_LRQ = RenderLayer.of("sword_glow_lines",
            RenderSetup.builder(GLOW_PIPELINE_LRQ).expectedBufferSize(1536).build());




    private static RenderPipeline createStaticChamsPipeline(String name, String shaderFile) {
        return reg(
                RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                        .withLocation("pipeline/chams_" + name)
                        .withVertexShader("core/position_color")
                        .withFragmentShader(ResourceProvider.getShaderIdentifier(shaderFile))
                        .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
                        .withBlend(BlendFunction.TRANSLUCENT)
                        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                        .withDepthWrite(true)
                        .withCull(true)
                        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                        .build()
        );
    }

    public static final RenderPipeline CHAMS_WATER_PIPELINE = createStaticChamsPipeline("water", "water");
    public static final RenderPipeline CHAMS_CAUSTIC_PIPELINE = createStaticChamsPipeline("caustic", "caustic");
    public static final RenderPipeline CHAMS_NEBULA_PIPELINE = createStaticChamsPipeline("nebula", "nebula");

    public static final RenderLayer CHAMS_WATER = RenderLayer.of("models_water", RenderSetup.builder(CHAMS_WATER_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderLayer CHAMS_CAUSTIC = RenderLayer.of("models_caustic", RenderSetup.builder(CHAMS_CAUSTIC_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderLayer CHAMS_NEBULA = RenderLayer.of("models_nebula", RenderSetup.builder(CHAMS_NEBULA_PIPELINE).expectedBufferSize(4096).build());

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

    private static RenderPipeline createItemChamsPipeline(String name, String shaderFile, BlendFunction blend, DepthTestFunction depth) {
        return reg(
                RenderPipeline.builder(TRANSFORMS_PROJECTION_FOG_SNIPPET, GLOBALS_SNIPPET)
                        .withLocation("pipeline/chams_item_" + name)
                        .withVertexShader("core/entity")
                        .withFragmentShader(ResourceProvider.getShaderIdentifier(shaderFile))
                        .withSampler("Sampler0")
                        .withUniform("ShaderFogData", UniformType.UNIFORM_BUFFER)
                        .withBlend(blend)
                        .withDepthTestFunction(depth)
                        .withDepthWrite(false)
                        .withCull(true)
                        .withVertexFormat(VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL, VertexFormat.DrawMode.QUADS)
                        .build()
        );

    }
    public static final RenderPipeline CHAMS_PLASMA_PIPELINE = createStaticChamsPipeline("plasma", "models_plasma");
    public static final RenderPipeline CHAMS_BLOOM_PIPELINE = createStaticChamsPipeline("bloom", "models_bloom");
    public static final RenderPipeline CHAMS_GLASS_PIPELINE = createStaticChamsPipeline("glass", "glass");
    public static final RenderLayer CHAMS_GLASS = RenderLayer.of("models_bloom", RenderSetup.builder(CHAMS_GLASS_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderLayer CHAMS_PLASMA = RenderLayer.of("models_bloom", RenderSetup.builder(CHAMS_PLASMA_PIPELINE).expectedBufferSize(4096).build());
    public static final RenderLayer CHAMS_BLOOM = RenderLayer.of("models_bloom", RenderSetup.builder(CHAMS_BLOOM_PIPELINE).expectedBufferSize(4096).build());


    public static final RenderPipeline CHAMS_GLASS_XRAY_TRANS_PL  = createItemChamsPipeline("glass_xray_trans", "glass", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_GLASS_XRAY_ADD_PL    = createItemChamsPipeline("glass_xray_add", "glass", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_GLASS_DEPTH_TRANS_PL = createItemChamsPipeline("glass_depth_trans", "glass", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_GLASS_DEPTH_ADD_PL   = createItemChamsPipeline("glass_depth_add", "glass", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline Models_Water_Xray_Trans_pl  = createItemChamsPipeline("water_xray_trans", "models_water", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline Models_Water_Xray_Add_pl    = createItemChamsPipeline("water_xray_add", "models_water", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline Models_Water_Depth_Trans_pl = createItemChamsPipeline("water_depth_trans", "models_water", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_WATER_DEPTH_ADD_PL   = createItemChamsPipeline("water_depth_add", "models_water", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_PLASMA_XRAY_TRANS_PL  = createItemChamsPipeline("plasma_xray_trans", "models_plasma", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_PLASMA_XRAY_ADD_PL    = createItemChamsPipeline("plasma_xray_add", "models_plasma", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_PLASMA_DEPTH_TRANS_PL = createItemChamsPipeline("plasma_depth_trans", "models_plasma", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_PLASMA_DEPTH_ADD_PL   = createItemChamsPipeline("plasma_depth_add", "models_plasma", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);

    public static final RenderPipeline CHAMS_BLOOM_XRAY_TRANS_PL  = createItemChamsPipeline("bloom_xray_trans", "models_bloom", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_BLOOM_XRAY_ADD_PL    = createItemChamsPipeline("bloom_xray_add", "models_bloom", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_BLOOM_DEPTH_TRANS_PL = createItemChamsPipeline("bloom_depth_trans", "models_bloom", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_BLOOM_DEPTH_ADD_PL   = createItemChamsPipeline("bloom_depth_add", "models_bloom", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_CAUSTIC_XRAY_TRANS_PL  = createItemChamsPipeline("caustic_xray_trans", "models_caustic", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_CAUSTIC_XRAY_ADD_PL    = createItemChamsPipeline("caustic_xray_add", "models_caustic", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_CAUSTIC_DEPTH_TRANS_PL = createItemChamsPipeline("caustic_depth_trans", "models_caustic", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_CAUSTIC_DEPTH_ADD_PL   = createItemChamsPipeline("caustic_depth_add", "models_caustic", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);

    public static final RenderPipeline CHAMS_NEBULA_XRAY_TRANS_PL  = createItemChamsPipeline("nebula_xray_trans", "models_nebula", BlendFunction.TRANSLUCENT, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_NEBULA_XRAY_ADD_PL    = createItemChamsPipeline("nebula_xray_add", "models_nebula", BlendFunction.LIGHTNING, DepthTestFunction.NO_DEPTH_TEST);
    public static final RenderPipeline CHAMS_NEBULA_DEPTH_TRANS_PL = createItemChamsPipeline("nebula_depth_trans", "models_nebula", BlendFunction.TRANSLUCENT, DepthTestFunction.LEQUAL_DEPTH_TEST);
    public static final RenderPipeline CHAMS_NEBULA_DEPTH_ADD_PL   = createItemChamsPipeline("nebula_depth_add", "models_nebula", BlendFunction.LIGHTNING, DepthTestFunction.LEQUAL_DEPTH_TEST);

    private static final Map<String, RenderLayer> DYNAMIC_CHAMS_LAYERS = new ConcurrentHashMap<>();

    public static RenderLayer getDynamicChamsLayer(String layerName, RenderPipeline pipeline, Identifier texture) {
        String key = layerName + "_" + (texture != null ? texture.toString() : "null");
        return DYNAMIC_CHAMS_LAYERS.computeIfAbsent(key, k -> {
            RenderSetup.Builder builder = RenderSetup.builder(pipeline).expectedBufferSize(4096);
            if (texture != null) {
                builder.texture("Sampler0", texture);
            }
            return RenderLayer.of("chams_" + layerName, builder.build());
        });
    }
}