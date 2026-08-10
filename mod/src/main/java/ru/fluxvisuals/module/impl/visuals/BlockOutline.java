package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/**
 * Единый модуль подсветки целевого блока: контур (Outline) и/или плазма-оверлей
 * на верхней грани (Overlay). Оверлей рисуется ПОВЕРХ блока, не внутри.
 */
@IModule(name = "Block Outline", description = "Подсветка целевого блока: контур или плазма-оверлей", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BlockOutline extends Module {
   private static BlockOutline instance;
   private final BufferAllocator allocator = new BufferAllocator(262144);
   private final Immediate immediate = VertexConsumerProvider.immediate(allocator);

   public final ModeSetting mode = new ModeSetting("Mode", "Both", "Outline", "Overlay", "Both");
   public final BooleanSetting fill = new BooleanSetting("Fill", false);
   public final SliderSetting alpha = new SliderSetting("Alpha", 130.0F, 40.0F, 220.0F, 10.0F, false);
   public final SliderSetting speed = new SliderSetting("Speed", 1.0F, 0.2F, 3.0F, 0.1F, false);

   private static final Identifier PLASMA_TEXTURE = Identifier.of("fluxvisuals", "textures/world/jump.png");
   private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/world/block_overlay"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.LIGHTNING)
               .build());

   public BlockOutline() {
      this.addSettings(new Setting[]{this.mode, this.fill, this.alpha, this.speed});
      instance = this;
   }

   public static BlockOutline getInstance() {
      return instance;
   }

   @EventInit
   public void onWorld(WorldRenderEvent e) {
      if (mc.crosshairTarget == null || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
         return;
      }
      BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
      if (mc.world == null || mc.world.getBlockState(pos).isAir()) {
         return;
      }
      // Немного раздуваем бокс наружу, чтобы с depth-тестом не прятались задние рёбра
      // (иначе контур "уходит внутрь блока").
      double minX = pos.getX() - 0.0025;
      double minY = pos.getY() - 0.0025;
      double minZ = pos.getZ() - 0.0025;
      double maxX = pos.getX() + 1.0025;
      double maxY = pos.getY() + 1.0025;
      double maxZ = pos.getZ() + 1.0025;

      boolean outline = this.mode.is("Outline") || this.mode.is("Both");
      boolean overlay = this.mode.is("Overlay") || this.mode.is("Both");
      int lineColor = Renderer2D.ColorUtil.getMainColor(1, 1);
      WorldRenderer wr = e.worldRenderer();

      if (outline) {
         if (this.fill.get()) {
            wr.drawCube(new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ),
                  Renderer2D.ColorUtil.replAlpha(lineColor, 75), true);
         }
         // Контур: 12 рёбер куба.
         wr.drawLine(new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, minY, maxZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(minX, minY, maxZ), new Vec3d(minX, minY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, maxZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(minX, maxY, maxZ), new Vec3d(minX, maxY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), 2.0, lineColor, true);
         wr.drawLine(new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ), 2.0, lineColor, true);
      }

      if (overlay) {
         this.renderPlasma(e, minX, minY, minZ);
      }
   }

   private void renderPlasma(WorldRenderEvent e, double minX, double minY, double minZ) {
      MatrixStack pose = e.matrixStack();
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();
      double px = minX - cam.x;
      double py = minY + 1.002 - cam.y; // ПОВЕРХ верхней грани, а не внутри блока
      double pz = minZ - cam.z;
      long t = System.currentTimeMillis();
      float spd = this.speed.get();
      float size = 1.0F;
      int baseAlpha = (int) this.alpha.get();

      // Плазма: два наложенных квада с бегущей по спектру расцветкой.
      float hue1 = (t % 3600L) / 3600.0F;
      float hue2 = ((t + 900L) % 3600L) / 3600.0F;
      int c1 = hueColor(hue1, baseAlpha);
      int c2 = hueColor(hue2, (int) (baseAlpha * 0.55F));

      pose.push();
      pose.translate(px, py, pz);
      pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
      RenderLayer layer = RenderLayer.of(PLASMA_TEXTURE.toString(),
            RenderSetup.builder(TEXTURED_QUADS_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", PLASMA_TEXTURE).build());
      Entry entry = pose.peek();
      Matrix4f mat = entry.getPositionMatrix();
      Matrix3f normal = entry.getNormalMatrix();
      VertexConsumer buffer = this.immediate.getBuffer(layer);

      // Внешний квад — плазма-свечение по грани.
      float s = size;
      this.quad(buffer, mat, normal, 0.0F, 0.0F, s, s, c1);
      // Внутренний квад — пульсирующий центр.
      float inner = size * (0.5F + 0.15F * (float) Math.sin(t / 250.0 * spd));
      this.quad(buffer, mat, normal, (size - inner) / 2.0F, (size - inner) / 2.0F, inner, inner, c2);
      pose.pop();
      this.immediate.draw();
   }

   private int hueColor(float hue, int alpha) {
      int rgb = Color.HSBtoRGB(hue, 0.9F, 1.0F);
      int r = (rgb >> 16) & 0xFF;
      int g = (rgb >> 8) & 0xFF;
      int b = rgb & 0xFF;
      return (alpha << 24) | (r << 16) | (g << 8) | b;
   }

   private void quad(VertexConsumer buffer, Matrix4f mat, Matrix3f normalMatrix, float x, float y, float w, float h,
         int color) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      int a = color >>> 24 & 0xFF;
      Vector3f n = new Vector3f(0.0F, 0.0F, 1.0F);
      normalMatrix.transform(n);
      n.normalize();
      float x2 = x + w;
      float y2 = y + h;
      buffer.vertex(mat, x, y, 0.0F).color(r, g, b, a).texture(0.0F, 0.0F).normal(n.x, n.y, n.z);
      buffer.vertex(mat, x, y2, 0.0F).color(r, g, b, a).texture(0.0F, 1.0F).normal(n.x, n.y, n.z);
      buffer.vertex(mat, x2, y2, 0.0F).color(r, g, b, a).texture(1.0F, 1.0F).normal(n.x, n.y, n.z);
      buffer.vertex(mat, x2, y, 0.0F).color(r, g, b, a).texture(1.0F, 0.0F).normal(n.x, n.y, n.z);
   }
}
