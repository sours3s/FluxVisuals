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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
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
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/**
 * Glass Hand — стеклянная/прозрачная рука с красивым свечением.
 * Хук в HandAnimationEvent (applySwingOffset — матрица уже стоит на позиции руки).
 */
@IModule(name = "GlassHand", description = "Стеклянная рука с эффектом свечения и преломления", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class GlassHand extends Module {
   public final ModeSetting mode = new ModeSetting("Режим", "Стекло", "Стекло", "Свечение", "Контур", "Призрак");
   public final BooleanSetting enabled = new BooleanSetting("Включено", true);
   public final SliderSetting intensity = new SliderSetting("Интенсивность", 0.8F, 0.1F, 2.0F, 0.05F, false);
   public final SliderSetting alpha = new SliderSetting("Прозрачность", 120.0F, 10.0F, 255.0F, 5.0F, false);
   public final SliderSetting glowRadius = new SliderSetting("Радиус свечения", 2.5F, 0.5F, 5.0F, 0.1F, false);
   public final SliderSetting pulseSpeed = new SliderSetting("Скорость пульсации", 1.0F, 0.1F, 3.0F, 0.1F, false);

   private static final Identifier GLOW_TEXTURE = Identifier.of("fluxvisuals", "textures/world/glow.png");

   // Pipelines
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

   private float pulseTime = 0.0F;

   public GlassHand() {
      this.addSettings(new Setting[]{enabled, mode, intensity, alpha, glowRadius, pulseSpeed});
   }

   @Override
   public void onDisable() {
      super.onDisable();
      pulseTime = 0.0F;
   }

   @EventInit
   public void onRenderHand(HandAnimationEvent event) {
      if (!enabled.get() || mc.player == null) {
         return;
      }
      // Рисуем эффект только вокруг основной руки (иначе эффект рисуется дважды).
      if (event.getHand() != net.minecraft.util.Hand.MAIN_HAND) {
         return;
      }

      MatrixStack matrices = event.getMatrices();
      float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
      pulseTime += tickDelta * pulseSpeed.get();

      float alphaF = this.alpha.get() / 255.0F * intensity.get();
      if (alphaF <= 0.01F) {
         return;
      }

      int mainCol = Renderer2D.ColorUtil.getMainColor(1, 1);
      int r = mainCol >> 16 & 0xFF;
      int g = mainCol >> 8 & 0xFF;
      int b = mainCol & 0xFF;

      String m = mode.get();
      Matrix4f mat = matrices.peek().getPositionMatrix();
      VertexConsumerProvider.Immediate imm = mc.getBufferBuilders().getEntityVertexConsumers();

      if (m.equals("Стекло") || m.equals("Призрак")) {
         // Рисуем полупрозрачный "стеклянный" куб вокруг руки/предмета
         float size = 0.35F;
         float pulse = 1.0F + 0.12F * MathHelper.sin(pulseTime * 4.0F);
         size *= pulse;

         int a = (int) (alphaF * 255);
         int glassAlpha = (int) (a * 0.4F);

         VertexConsumer vc = imm.getBuffer(GLASS_LAYER);
         drawGlassCube(vc, mat, size, r, g, b, glassAlpha);
      }

      if (m.equals("Свечение") || m.equals("Призрак")) {
         // Глоу-эффект вокруг руки (билиборды)
         float size = 0.4F * glowRadius.get();
         float pulse = 1.0F + 0.15F * MathHelper.sin(pulseTime * 3.0F);
         size *= pulse;

         int glowAlpha = (int) (alphaF * 180);
         int glowColor = Renderer2D.ColorUtil.replAlpha(mainCol, glowAlpha);

         VertexConsumer vc = imm.getBuffer(WorldRenderLayers.TEXTURED_QUADS_ADDITIVE(GLOW_TEXTURE));
         drawGlowBillboard(vc, mat, size, r, g, b, glowAlpha);
      }

      if (m.equals("Контур") || m.equals("Призрак")) {
         // Контур руки/предмета
         float size = 0.38F;
         int outlineAlpha = (int) (alphaF * 255);
         int outlineColor = Renderer2D.ColorUtil.replAlpha(mainCol, outlineAlpha);

         VertexConsumer vc = imm.getBuffer(WorldRenderLayers.LINES(2.0));
         drawOutlineCube(vc, mat, size, outlineColor);
      }

      // Сбрасываем буферы, иначе отрисованное не появится на экране.
      imm.draw();
   }

   private void drawGlassCube(VertexConsumer vc, Matrix4f mat, float size, int r, int g, int b, int a) {
      float s = size;
      // 6 граней куба с текстурой глоу
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

   private void drawGlowBillboard(VertexConsumer vc, Matrix4f mat, float size, int r, int g, int b, int a) {
      // Простой бильборд в центре руки
      vc.vertex(mat, -size, -size, 0).texture(0, 0).color(r, g, b, a);
      vc.vertex(mat, -size, size, 0).texture(0, 1).color(r, g, b, a);
      vc.vertex(mat, size, size, 0).texture(1, 1).color(r, g, b, a);
      vc.vertex(mat, size, -size, 0).texture(1, 0).color(r, g, b, a);
   }

   private void drawOutlineCube(VertexConsumer vc, Matrix4f mat, float size, int color) {
      float s = size;
      // 12 рёбер куба — LINES требует .lineWidth()
      vc.vertex(mat, -s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, -s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, -s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, -s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, -s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, -s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, -s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);

      vc.vertex(mat, -s, s, -s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      vc.vertex(mat, -s, s, s).color(color).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
   }
}