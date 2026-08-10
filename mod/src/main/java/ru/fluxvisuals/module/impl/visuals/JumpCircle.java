package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.player.EventJump;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.util.render.world.WorldRenderer;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.anim.util.Easings;

@IModule(name = "Jump Circle", description = "Красивое кольцо при прыжке (Pulse / Spiral / Nova)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class JumpCircle extends Module {
   private final List<Circle> circles = new ArrayList<>();
   private final BufferAllocator allocator = new BufferAllocator(262144);
   private final Immediate immediate = VertexConsumerProvider.immediate(allocator);
   private static final String PIPELINE_NAMESPACE = "fluxvisuals";

   public final ModeSetting mode = new ModeSetting("Mode", "Pulse", "Pulse", "Spiral", "Nova", "Blocks");
   public final SliderSetting radius = new SliderSetting("Radius", 0.5F, 0.3F, 1.0F, 0.05F, false);
   public final SliderSetting blockRadius = new SliderSetting("Block Radius", 4.0F, 1.0F, 8.0F, 1.0F, false);
   public final SliderSetting time = new SliderSetting("Time", 1000.0F, 200.0F, 2000.0F, 100.0F, false);
   public final SliderSetting animSpeed = new SliderSetting("Anim", 500.0F, 100.0F, 1000.0F, 50.0F, false);
   public final MultiBooleanSetting animations = new MultiBooleanSetting("Animations",
         new BooleanSetting("Size", true), new BooleanSetting("Alpha", true));

   private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/world/textured_quads"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.LIGHTNING)
               .build());

   public JumpCircle() {
      this.addSettings(new Setting[]{this.mode, this.radius, this.blockRadius, this.time, this.animSpeed, this.animations});
   }

   @EventInit
   public void onJump(EventJump e) {
      this.circles.add(new Circle(new Vec3d(mc.player.getX(), mc.player.getY(), mc.player.getZ()).add(0.0, 0.05, 0.0),
            System.currentTimeMillis()));
   }

   @EventInit
   public void onRender(EventRender3D e) {
      // Режим "Blocks" рисуется в onWorld (контуры блоков, расширяющихся от игрока).
      if (this.mode.is("Blocks")) {
         return;
      }
      if (this.circles.isEmpty()) {
         return;
      }
      this.circles.removeIf(c -> System.currentTimeMillis() - c.startTime > this.time.get());
      if (this.circles.isEmpty()) {
         return;
      }
      MatrixStack pose = e.getMatrixStack();
      Identifier texture = Identifier.of("fluxvisuals", "textures/world/jump.png");
      int color = Renderer2D.ColorUtil.getMainColor(1, 1);
      float dt = this.time.get();

      for (Circle c : this.circles) {
         float progress = (float) (System.currentTimeMillis() - c.startTime) / dt;
         if (progress > 1.0F) {
            continue;
         }
         float ease = 1.0F - (float) Math.pow(1.0F - progress, 3.0F);
         boolean sizeAnim = this.animations.get("Size");
         boolean alphaAnim = this.animations.get("Alpha");
         float size = this.radius.get() * (sizeAnim ? (0.3F + 0.7F * ease) : 1.0F);
         int alpha = alphaAnim ? (int) (255.0F * (1.0F - progress)) : 255;

         // positionMatrix = вращение камеры + проекция (без трансляции) → координаты camera-relative.
         double posX = c.vector3d.x - mc.gameRenderer.getCamera().getCameraPos().x;
         double posY = c.vector3d.y - mc.gameRenderer.getCamera().getCameraPos().y;
         double posZ = c.vector3d.z - mc.gameRenderer.getCamera().getCameraPos().z;
         pose.push();
         pose.translate(posX, posY, posZ);
         pose.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
         String m = this.mode.get();
         if (m.equals("Spiral")) {
            pose.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(progress * 360.0F * (this.animSpeed.get() / 500.0F)));
         }
         RenderLayer renderLayer = RenderLayer.of(texture.toString(),
               RenderSetup.builder(TEXTURED_QUADS_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
         Entry entry = pose.peek();
         Matrix4f matrix4f = entry.getPositionMatrix();
         Matrix3f normalMatrix = entry.getNormalMatrix();
         VertexConsumer buffer = this.immediate.getBuffer(renderLayer);
         this.drawTexturedQuad(buffer, matrix4f, normalMatrix, -size / 2.0F, -size / 2.0F, size, size, color, alpha);
         if (m.equals("Nova")) {
            // Второе кольцо, расширяется в обратную сторону для эффекта вспышки.
            float size2 = size * 0.6F;
            this.drawTexturedQuad(buffer, matrix4f, normalMatrix, -size2 / 2.0F, -size2 / 2.0F, size2, size2, color,
                  (int) (alpha * 0.5F));
         }
         pose.pop();
      }
      this.immediate.draw();
   }

   /**
    * Режим "Blocks": после прыжка вокруг игрока подсвечиваются блоки, которые расширяются
    * от центра наружу и плавно затухают — пульсирующая волна.
    */
   @EventInit
   public void onWorld(WorldRenderEvent e) {
      if (mc.world == null || !this.mode.is("Blocks")) {
         return;
      }
      this.circles.removeIf(c -> System.currentTimeMillis() - c.startTime > this.time.get());
      if (this.circles.isEmpty()) {
         return;
      }
      WorldRenderer wr = e.worldRenderer();
      int mainColor = Renderer2D.ColorUtil.getMainColor(1, 1);

      for (Circle c : this.circles) {
         float progress = (float) (System.currentTimeMillis() - c.startTime) / this.time.get();
         if (progress > 1.0F) {
            continue;
         }
         // Волна: радиус растёт от 0.5 до blockRadius, альфа затухает (1 - progress^2).
         float alpha = Math.max(0.0F, 1.0F - progress * progress);
         float rad = Math.max(0.5F, this.blockRadius.get() * progress);
         int color = Renderer2D.ColorUtil.replAlpha(mainColor, (int) (alpha * 255.0F));
         BlockPos center = BlockPos.ofFloored(c.vector3d.x, c.vector3d.y, c.vector3d.z);
         for (BlockPos pos : this.getBlocksInRadius(center, rad)) {
            BlockState state = mc.world.getBlockState(pos);
            if (state.isAir()) {
               continue;
            }
            this.drawBlockOutline(wr, pos, color);
         }
      }
   }

   private List<BlockPos> getBlocksInRadius(BlockPos center, float radius) {
      List<BlockPos> blocks = new ArrayList<>();
      int r = (int) Math.ceil(radius);
      for (int x = -r; x <= r; x++) {
         for (int z = -r; z <= r; z++) {
            double dist = Math.sqrt(x * x + z * z);
            if (dist <= radius) {
               blocks.add(new BlockPos(center.getX() + x, center.getY(), center.getZ() + z));
               blocks.add(new BlockPos(center.getX() + x, center.getY() - 1, center.getZ() + z));
            }
         }
      }
      return blocks;
   }

   private void drawBlockOutline(WorldRenderer wr, BlockPos pos, int color) {
      double minX = pos.getX();
      double minY = pos.getY();
      double minZ = pos.getZ();
      double maxX = minX + 1.0;
      double maxY = minY + 1.0;
      double maxZ = minZ + 1.0;
      wr.drawLine(new Vec3d(minX, minY, minZ), new Vec3d(maxX, minY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, minY, maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, minY, maxZ), new Vec3d(minX, minY, maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(minX, minY, maxZ), new Vec3d(minX, minY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(minX, maxY, minZ), new Vec3d(maxX, maxY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, maxY, minZ), new Vec3d(maxX, maxY, maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, maxY, maxZ), new Vec3d(minX, maxY, maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(minX, maxY, maxZ), new Vec3d(minX, maxY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(minX, minY, minZ), new Vec3d(minX, maxY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, minY, minZ), new Vec3d(maxX, maxY, minZ), 2.0, color, true);
      wr.drawLine(new Vec3d(maxX, minY, maxZ), new Vec3d(maxX, maxY, maxZ), 2.0, color, true);
      wr.drawLine(new Vec3d(minX, minY, maxZ), new Vec3d(minX, maxY, maxZ), 2.0, color, true);
   }

   private void drawTexturedQuad(VertexConsumer buffer, Matrix4f matrix, Matrix3f normalMatrix, float x, float y,
         float width, float height, int color, int alpha) {
      int r = color >> 16 & 0xFF;
      int g = color >> 8 & 0xFF;
      int b = color & 0xFF;
      Vector3f normal = new Vector3f(0.0F, 0.0F, 1.0F);
      normalMatrix.transform(normal);
      normal.normalize();
      float x2 = x + width;
      float y2 = y + height;
      buffer.vertex(matrix, x, y, 0.0F)
            .color(r, g, b, alpha)
            .texture(0.0F, 0.0F)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x, y2, 0.0F)
            .color(r, g, b, alpha)
            .texture(0.0F, 1.0F)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y2, 0.0F)
            .color(r, g, b, alpha)
            .texture(1.0F, 1.0F)
            .normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y, 0.0F)
            .color(r, g, b, alpha)
            .texture(1.0F, 0.0F)
            .normal(normal.x, normal.y, normal.z);
   }

   @Environment(EnvType.CLIENT)
   private static final class Circle {
      private final Vec3d vector3d;
      private final long startTime;

      Circle(Vec3d vector3d, long startTime) {
         this.vector3d = vector3d;
         this.startTime = startTime;
      }
   }
}
