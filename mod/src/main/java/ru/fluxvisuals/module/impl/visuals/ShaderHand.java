package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import ru.fluxvisuals.client.FluxVisualsClient;
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
 * Shader Hand — шейдерный эффект для руки от первого лица.
 * Использует захват фреймбуфера руки для пост-процессинга.
 */
@IModule(name = "Shader Hand", description = "Шейдерные эффекты для руки от первого лица", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ShaderHand extends Module {
   private static ShaderHand instance;

   public final ModeSetting mode = new ModeSetting("Mode", "Wave", "Wave", "Glitch", "Chromatic", "Outline", "Pulse");
   public final BooleanSetting enabled = new BooleanSetting("Enabled", true);
   public final SliderSetting intensity = new SliderSetting("Intensity", 1.0F, 0.1F, 3.0F, 0.1F, false);
   public final SliderSetting speed = new SliderSetting("Speed", 1.0F, 0.1F, 5.0F, 0.1F, false);
   public final SliderSetting scale = new SliderSetting("Scale", 1.0F, 0.5F, 2.0F, 0.05F, false);

   private static final Identifier HAND_TEXTURE = Identifier.of("fluxvisuals", "textures/world/glow.png");
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
         RenderSetup.builder(SHADER_PIPELINE).expectedBufferSize(4096).translucent().texture("Sampler0", HAND_TEXTURE).build());

   private float animationTime = 0.0F;

   public ShaderHand() {
      this.addSettings(new Setting[]{enabled, mode, intensity, speed, scale});
      instance = this;
   }

   public static ShaderHand getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      // Enable the entity framebuffer capture for hand rendering
      EntityFramebufferCaptureManager.getInstance().setEnabled(true);
      animationTime = 0.0F;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      EntityFramebufferCaptureManager.getInstance().setEnabled(false);
      animationTime = 0.0F;
   }

   @EventInit
   public void onRenderHand(HandAnimationEvent event) {
      if (!enabled.get() || mc.player == null) {
         return;
      }
      // Эффект рисуем только вокруг основной руки.
      if (event.getHand() != net.minecraft.util.Hand.MAIN_HAND) {
         return;
      }

      float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
      animationTime += tickDelta * speed.get();

      MatrixStack matrices = event.getMatrices();
      Matrix4f mat = matrices.peek().getPositionMatrix();
      VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();

      int mainCol = Renderer2D.ColorUtil.getMainColor(1, 1);
      int r = mainCol >> 16 & 0xFF;
      int g = mainCol >> 8 & 0xFF;
      int b = mainCol & 0xFF;
      int alpha = (int) (255 * intensity.get());

      String m = mode.get();
      float s = 0.5F * scale.get();

      switch (m) {
         case "Wave" -> renderWaveEffect(imm, mat, r, g, b, alpha, s);
         case "Glitch" -> renderGlitchEffect(imm, mat, r, g, b, alpha, s);
         case "Chromatic" -> renderChromaticEffect(imm, mat, r, g, b, alpha, s);
         case "Outline" -> renderOutlineEffect(imm, mat, r, g, b, alpha, s);
         case "Pulse" -> renderPulseEffect(imm, mat, r, g, b, alpha, s);
      }

      // Без сброса буфера нарисованное не появится на экране.
      imm.draw();
   }

   private void renderWaveEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, int r, int g, int b, int alpha, float size) {
      float wave = (float) Math.sin(animationTime * 3.0F) * 0.15F;
      float x1 = -size + wave;
      float x2 = size + wave;
      float y1 = -size;
      float y2 = size;

      VertexConsumer vc = imm.getBuffer(SHADER_LAYER);
      int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), alpha);
      vc.vertex(mat, x1, y1, 0).texture(0, 0).color(r, g, b, alpha);
      vc.vertex(mat, x2, y1, 0).texture(1, 0).color(r, g, b, alpha);
      vc.vertex(mat, x2, y2, 0).texture(1, 1).color(r, g, b, alpha);
      vc.vertex(mat, x1, y2, 0).texture(0, 1).color(r, g, b, alpha);
   }

   private void renderGlitchEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, int r, int g, int b, int alpha, float size) {
      float glitchOffset = ((float) (Math.sin(animationTime * 10.0F) * 0.1F));
      float x1 = -size + glitchOffset;
      float x2 = size + glitchOffset;

      VertexConsumer vc = imm.getBuffer(SHADER_LAYER);
      vc.vertex(mat, x1, -size, 0).texture(0, 0).color(r, g, b, alpha);
      vc.vertex(mat, x2, -size, 0).texture(1, 0).color(r, g, b, alpha);
      vc.vertex(mat, x2, size, 0).texture(1, 1).color(r, g, b, alpha);
      vc.vertex(mat, x1, size, 0).texture(0, 1).color(r, g, b, alpha);
   }

   private void renderChromaticEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, int r, int g, int b, int alpha, float size) {
      float offset = (float) Math.sin(animationTime * 5.0F) * 0.05F;

      // Red channel
      VertexConsumer vc = imm.getBuffer(SHADER_LAYER);
      vc.vertex(mat, -size + offset, -size, 0).texture(0, 0).color(r, 0, 0, alpha);
      vc.vertex(mat, size + offset, -size, 0).texture(1, 0).color(r, 0, 0, alpha);
      vc.vertex(mat, size + offset, size, 0).texture(1, 1).color(r, 0, 0, alpha);
      vc.vertex(mat, -size + offset, size, 0).texture(0, 1).color(r, 0, 0, alpha);

      // Green channel
      vc.vertex(mat, -size - offset, -size, 0).texture(0, 0).color(0, g, 0, alpha);
      vc.vertex(mat, size - offset, -size, 0).texture(1, 0).color(0, g, 0, alpha);
      vc.vertex(mat, size - offset, size, 0).texture(1, 1).color(0, g, 0, alpha);
      vc.vertex(mat, -size - offset, size, 0).texture(0, 1).color(0, g, 0, alpha);

      // Blue channel
      vc.vertex(mat, -size, -size, 0).texture(0, 0).color(0, 0, b, alpha);
      vc.vertex(mat, size, -size, 0).texture(1, 0).color(0, 0, b, alpha);
      vc.vertex(mat, size, size, 0).texture(1, 1).color(0, 0, b, alpha);
      vc.vertex(mat, -size, size, 0).texture(0, 1).color(0, 0, b, alpha);
   }

   private void renderOutlineEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, int r, int g, int b, int alpha, float size) {
      VertexConsumer vc = imm.getBuffer(WorldRenderLayers.LINES(3.0));
      int color = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), alpha);
      // Draw square outline — LINES требует .lineWidth()
      vc.vertex(mat, -size, -size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, size, -size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, size, -size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, size, size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, size, size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, -size, size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, -size, size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
      vc.vertex(mat, -size, -size, 0).color(color).normal(0, 0, 1).lineWidth(3.0F);
   }

   private void renderPulseEffect(VertexConsumerProvider.Immediate imm, Matrix4f mat, int r, int g, int b, int alpha, float size) {
      float pulse = 1.0F + 0.3F * (float) Math.sin(animationTime * 4.0F);
      float s = size * pulse;

      VertexConsumer vc = imm.getBuffer(SHADER_LAYER);
      vc.vertex(mat, -s, -s, 0).texture(0, 0).color(r, g, b, alpha);
      vc.vertex(mat, s, -s, 0).texture(1, 0).color(r, g, b, alpha);
      vc.vertex(mat, s, s, 0).texture(1, 1).color(r, g, b, alpha);
      vc.vertex(mat, -s, s, 0).texture(0, 1).color(r, g, b, alpha);
   }
}
