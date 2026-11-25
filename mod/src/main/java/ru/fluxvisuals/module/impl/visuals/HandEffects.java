package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.HandAnimationEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.capture.EntityFramebufferCaptureManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/**
 * Hand Effects — единый модуль для эффектов руки от первого лица.
 * Режимы Glass/Glow/Frame/Ghost — геометрия вокруг руки, остальные — анимированные эффекты.
 */
@IModule(
   name = "Hand Effects",
   description = "Стекло, свечение, контуры и анимированные эффекты руки от первого лица",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class HandEffects extends Module {
   public final BooleanSetting enabled = new BooleanSetting("Enabled", true);
   public final ModeSetting mode = new ModeSetting(
         "Mode", "Glass", "Glass", "Glow", "Frame", "Ghost", "Wave", "Glitch", "Chromatic", "Outline", "Pulse");
   public final SliderSetting intensity = new SliderSetting("Intensity", 1.0F, 0.1F, 3.0F, 0.1F, false);
   public final SliderSetting alpha = new SliderSetting("Alpha", 120.0F, 10.0F, 255.0F, 5.0F, false);
   public final SliderSetting glowRadius = new SliderSetting("Glow Radius", 2.5F, 0.5F, 5.0F, 0.1F, false);
   public final SliderSetting speed = new SliderSetting("Speed", 1.0F, 0.1F, 5.0F, 0.1F, false);
   public final SliderSetting scale = new SliderSetting("Scale", 1.0F, 0.5F, 2.0F, 0.05F, false);

   private static final Identifier GLOW_TEXTURE = Identifier.of("fluxvisuals", "textures/world/glow.png");

   private static final RenderPipeline GLASS_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/glass_hand"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.TRANSLUCENT)
               .build());
   private static final RenderLayer GLASS_LAYER = RenderLayer.of("fluxvisuals_glass_hand",
         RenderSetup.builder(GLASS_PIPELINE).expectedBufferSize(4096).translucent().texture("Sampler0", GLOW_TEXTURE).build());

   private static final RenderPipeline SHADER_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/shader_hand"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.TRANSLUCENT)
               .build());
   private static final RenderLayer SHADER_LAYER = RenderLayer.of("fluxvisuals_shader_hand",
         RenderSetup.builder(SHADER_PIPELINE).expectedBufferSize(4096).translucent().texture("Sampler0", GLOW_TEXTURE).build());

   private float effectTime = 0.0F;
   private boolean shaderCapture = false;

   public HandEffects() {
      this.addSettings(new Setting[]{enabled, mode, intensity, alpha, glowRadius, speed, scale});
   }

   @Override
   public void onEnable() {
      super.onEnable();
      effectTime = 0.0F;
      updateCapture(isShaderMode());
   }

   @Override
   public void onDisable() {
      super.onDisable();
      effectTime = 0.0F;
      updateCapture(false);
   }

   @EventInit
   public void onRenderHand(HandAnimationEvent event) {
      if (!enabled.get() || mc.player == null || event.getHand() != Hand.MAIN_HAND) {
         return;
      }

      float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
      effectTime += tickDelta * speed.get();

      String m = mode.get();
      updateCapture(isShaderMode());

      MatrixStack matrices = event.getMatrices();
      Matrix4f mat = matrices.peek().getPositionMatrix();
      VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();

      int mainCol = Renderer2D.ColorUtil.getMainColor(1, 1);
      int r = mainCol >> 16 & 0xFF;
      int g = mainCol >> 8 & 0xFF;
      int b = mainCol & 0xFF;

      switch (m) {
         case "Glass", "Glow", "Frame", "Ghost" -> renderGeometry(imm, mat, m, mainCol, r, g, b);
         case "Wave", "Glitch", "Chromatic", "Outline", "Pulse" -> renderShaderEffect(imm, mat, m, r, g, b);
         default -> {
         }
      }

      imm.draw();
   }

   private boolean isShaderMode() {
      String m = mode.get();
      return m.equals("Wave") || m.equals("Glitch") || m.equals("Chromatic")
            || m.equals("Outline") || m.equals("Pulse");
   }

   private void updateCapture(boolean needed) {
      if (shaderCapture != needed) {
         shaderCapture = needed;
         EntityFramebufferCaptureManager.getInstance().setEnabled(needed);
      }
   }

   private void renderGeometry(VertexConsumerProvider.Immediate imm, Matrix4f mat, String m,
                               int mainCol, int r, int g, int b) {
      float alphaF = alpha.get() / 255.0F * intensity.get();
      if (alphaF <= 0.01F) {
         return;
      }

      if (m.equals("Glass") || m.equals("Ghost")) {
         float pulse = 1.0F + 0.12F * MathHelper.sin(effectTime * 4.0F);
         float size = 0.35F * pulse;
         int glassAlpha = MathHelper.clamp((int) (alphaF * 255 * 0.4F), 0, 255);
         drawGlassCube(imm.getBuffer(GLASS_LAYER), mat, size, r, g, b, glassAlpha);
      }

      if (m.equals("Glow") || m.equals("Ghost")) {
         float pulse = 1.0F + 0.15F * MathHelper.sin(effectTime * 3.0F);
         float size = 0.4F * glowRadius.get() * pulse;
         int glowAlpha = MathHelper.clamp((int) (alphaF * 180), 0, 255);
         drawQuad(imm.getBuffer(WorldRenderLayers.TEXTURED_QUADS_ADDITIVE(GLOW_TEXTURE)), mat,
               -size, -size, size, size, r, g, b, glowAlpha);
      }

      if (m.equals("Frame") || m.equals("Ghost")) {
         int outlineAlpha = MathHelper.clamp((int) (alphaF * 255), 0, 255);
         drawOutlineCube(imm.getBuffer(WorldRenderLayers.LINES(2.0)), mat, 0.38F,
               Renderer2D.ColorUtil.replAlpha(mainCol, outlineAlpha));
      }
   }

   private void renderShaderEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, String m,
                                   int r, int g, int b) {
      int a = MathHelper.clamp((int) (255 * intensity.get()), 0, 255);
      float size = 0.5F * scale.get();
      VertexConsumer vc = imm.getBuffer(SHADER_LAYER);

      switch (m) {
         case "Wave" -> {
            float wave = MathHelper.sin(effectTime * 3.0F) * 0.15F;
            drawQuad(vc, mat, -size + wave, -size, size + wave, size, r, g, b, a);
         }
         case "Glitch" -> {
            float offset = MathHelper.sin(effectTime * 10.0F) * 0.1F;
            drawQuad(vc, mat, -size + offset, -size, size + offset, size, r, g, b, a);
         }
         case "Chromatic" -> {
            float offset = MathHelper.sin(effectTime * 5.0F) * 0.05F;
            drawQuad(vc, mat, -size + offset, -size, size + offset, size, r, 0, 0, a);
            drawQuad(vc, mat, -size - offset, -size, size - offset, size, 0, g, 0, a);
            drawQuad(vc, mat, -size, -size, size, size, 0, 0, b, a);
         }
         case "Pulse" -> {
            float s = size * (1.0F + 0.3F * MathHelper.sin(effectTime * 4.0F));
            drawQuad(vc, mat, -s, -s, s, s, r, g, b, a);
         }
         case "Outline" -> drawOutlineQuad(imm.getBuffer(WorldRenderLayers.LINES(3.0)), mat, size,
               Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), a));
         default -> {
         }
      }
   }

   private static void drawQuad(VertexConsumer vc, Matrix4f mat, float x1, float y1, float x2, float y2,
                                int r, int g, int b, int a) {
      vc.vertex(mat, x1, y1, 0).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, x2, y1, 0).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, x2, y2, 0).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, x1, y2, 0).texture(0, 1).color(r, g, b, a);
   }

   private static void drawGlassCube(VertexConsumer vc, Matrix4f mat, float s, int r, int g, int b, int a) {
      // Front
      vc.vertex(mat, -s, -s, s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, s, -s, s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, s, s, s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, -s, s, s).texture(0, 1).color(r, g, b, a);
      // Back
      vc.vertex(mat, s, -s, -s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, -s, -s, -s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, -s, s, -s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, s, s, -s).texture(0, 1).color(r, g, b, a);
      // Top
      vc.vertex(mat, -s, -s, -s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, s, -s, -s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, s, -s, s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, -s, -s, s).texture(0, 1).color(r, g, b, a);
      // Bottom
      vc.vertex(mat, -s, s, s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, s, s, s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, s, s, -s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, -s, s, -s).texture(0, 1).color(r, g, b, a);
      // Left
      vc.vertex(mat, -s, -s, -s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, -s, -s, s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, -s, s, s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, -s, s, -s).texture(0, 1).color(r, g, b, a);
      // Right
      vc.vertex(mat, s, -s, s).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, s, -s, -s).texture(1, 0).color(r, g, b, a);
      vc.vertex(mat, s, s, -s).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, s, s, s).texture(0, 1).color(r, g, b, a);
   }

   private static void drawOutlineCube(VertexConsumer vc, Matrix4f mat, float s, int color) {
      // 12 рёбер куба — LINES требует .lineWidth()
      line(vc, mat, -s, -s, -s, s, -s, -s, color, 2.0F);
      line(vc, mat, s, -s, -s, s, s, -s, color, 2.0F);
      line(vc, mat, s, s, -s, -s, s, -s, color, 2.0F);
      line(vc, mat, -s, s, -s, -s, -s, -s, color, 2.0F);

      line(vc, mat, -s, -s, s, s, -s, s, color, 2.0F);
      line(vc, mat, s, -s, s, s, s, s, color, 2.0F);
      line(vc, mat, s, s, s, -s, s, s, color, 2.0F);
      line(vc, mat, -s, s, s, -s, -s, s, color, 2.0F);

      line(vc, mat, -s, -s, -s, -s, -s, s, color, 2.0F);
      line(vc, mat, s, -s, -s, s, -s, s, color, 2.0F);
      line(vc, mat, s, s, -s, s, s, s, color, 2.0F);
      line(vc, mat, -s, s, -s, -s, s, s, color, 2.0F);
   }

   private static void drawOutlineQuad(VertexConsumer vc, Matrix4f mat, float s, int color) {
      line(vc, mat, -s, -s, 0, s, -s, 0, color, 3.0F);
      line(vc, mat, s, -s, 0, s, s, 0, color, 3.0F);
      line(vc, mat, s, s, 0, -s, s, 0, color, 3.0F);
      line(vc, mat, -s, s, 0, -s, -s, 0, color, 3.0F);
   }

   private static void line(VertexConsumer vc, Matrix4f mat,
                            float x1, float y1, float z1, float x2, float y2, float z2,
                            int color, float width) {
      vc.vertex(mat, x1, y1, z1).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(width);
      vc.vertex(mat, x2, y2, z2).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(width);
   }
}
