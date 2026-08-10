package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.Entity;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack.Entry;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;

@IModule(name = "Particles", description = "Добавляет в мир частицы после выбранных действий", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Particles extends Module {
   private static final String[] TYPE_NAMES = {"Type 1", "Type 2", "Type 3", "Type 4", "Type 5", "Type 6", "Type 7", "Type 8", "Type 9"};
   private static final Identifier[] TEXTURES = {
         Identifier.of("fluxvisuals", "images/particles/type1.jpg"),
         Identifier.of("fluxvisuals", "images/particles/type2.jpg"),
         Identifier.of("fluxvisuals", "images/particles/type3.jpg"),
         Identifier.of("fluxvisuals", "images/particles/type4.png"),
         Identifier.of("fluxvisuals", "images/particles/type5.png"),
         Identifier.of("fluxvisuals", "images/particles/type6.png"),
         Identifier.of("fluxvisuals", "images/particles/type7.png"),
         Identifier.of("fluxvisuals", "images/particles/type8.png"),
         Identifier.of("fluxvisuals", "images/particles/type9.png")};
   private static final float BILLBOARD_SCALE = 0.6F;
   private static final long MOVE_LIFETIME = 500L;
   private static final long SKY_LIFETIME = 5000L;
   private static final long HIT_LIFETIME = 5000L;
   private static final int MAX_PARTICLES = 200;

   private final List<Particle> particles = new ArrayList<>();
   private final BufferAllocator allocator = new BufferAllocator(262144);
   private final Immediate immediate = VertexConsumerProvider.immediate(allocator);
   private final Random random = new Random();

   public final ModeSetting mode = new ModeSetting("Particle", "Type 1", TYPE_NAMES);
   public final ModeSetting trigger = new ModeSetting("Trigger", "Sky", "Sky", "Move", "Attack");

   private static final RenderPipeline TEXTURED_QUADS_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_TEX_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/world/particle_quads"))
               .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, DrawMode.QUADS)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.LIGHTNING)
               .build());

   public Particles() {
      this.addSettings(new Setting[]{this.mode, this.trigger});
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (mc.player == null || mc.world == null) {
         return;
      }
      if (this.particles.size() >= MAX_PARTICLES) {
         return;
      }
      String trig = this.trigger.get();
      if (trig.equals("Sky")) {
         for (int i = 0; i < 3; i++) {
            Vec3d p = new Vec3d(
                  mc.player.getX() + (this.random.nextDouble() - 0.5) * 16.0,
                  mc.player.getY() + this.random.nextDouble() * 10.0 + 2.0,
                  mc.player.getZ() + (this.random.nextDouble() - 0.5) * 16.0);
            this.particles.add(new Particle(p, new Vec3d(0.0, 0.02 + this.random.nextDouble() * 0.04, 0.0),
                  System.currentTimeMillis(), SKY_LIFETIME));
         }
      } else if (trig.equals("Move")) {
         double speed = Math.hypot(mc.player.getVelocity().x, mc.player.getVelocity().z);
         if (speed > 0.1) {
            Vec3d p = new Vec3d(mc.player.getX(), mc.player.getY() + 1.0 + this.random.nextDouble() * 0.5, mc.player.getZ());
            this.particles.add(new Particle(p, new Vec3d((this.random.nextDouble() - 0.5) * 0.05, this.random.nextDouble() * 0.05,
                  (this.random.nextDouble() - 0.5) * 0.05), System.currentTimeMillis(), MOVE_LIFETIME));
         }
      }
   }

   @EventInit
   public void onAttack(AttackEvent e) {
      if (this.trigger.get().equals("Attack") && e.getTarget() != null && mc.world != null) {
         Entity tgt = e.getTarget();
         Vec3d p = new Vec3d(tgt.getX(), tgt.getY() + tgt.getHeight() / 2.0, tgt.getZ());
         for (int i = 0; i < 12; i++) {
            this.particles.add(new Particle(p,
                  new Vec3d((this.random.nextDouble() - 0.5) * 0.4, this.random.nextDouble() * 0.4,
                        (this.random.nextDouble() - 0.5) * 0.4),
                  System.currentTimeMillis(), HIT_LIFETIME));
         }
      }
   }

   @EventInit
   public void onRender(EventRender3D event) {
      if (this.particles.isEmpty()) {
         return;
      }
      this.particles.removeIf(p -> System.currentTimeMillis() - p.startTime > p.lifetime);
      if (this.particles.isEmpty()) {
         return;
      }
      MatrixStack pose = event.getMatrixStack();
      Identifier texture = TEXTURES[this.mode.index];
      int color = Renderer2D.ColorUtil.getMainColor(1, 1);
      Quaternionf camRot = new Quaternionf(mc.gameRenderer.getCamera().getRotation());
      for (Particle p : this.particles) {
         p.pos = p.pos.add(p.velocity);
         long elapsed = System.currentTimeMillis() - p.startTime;
         float t = (float) elapsed / (float) p.lifetime;
         float fade = Math.min(1.0F, (1.0F - t) * 3.0F);
         if (fade <= 0.02F) {
            continue;
         }
         int alpha = (int) (fade * 200.0F);
         double dx = p.pos.x - mc.gameRenderer.getCamera().getCameraPos().x;
         double dy = p.pos.y - mc.gameRenderer.getCamera().getCameraPos().y;
         double dz = p.pos.z - mc.gameRenderer.getCamera().getCameraPos().z;
         pose.push();
         pose.translate(dx, dy, dz);
         pose.multiply(new Quaternionf(camRot));
         float size = BILLBOARD_SCALE * (0.4F + 0.6F * p.size);
         RenderLayer renderLayer = RenderLayer.of(texture.toString(),
               RenderSetup.builder(TEXTURED_QUADS_PIPELINE).expectedBufferSize(1024).translucent().texture("Sampler0", texture).build());
         Entry entry = pose.peek();
         Matrix4f matrix4f = entry.getPositionMatrix();
         Matrix3f normalMatrix = entry.getNormalMatrix();
         VertexConsumer buffer = this.immediate.getBuffer(renderLayer);
         this.drawTexturedQuad(buffer, matrix4f, normalMatrix, -size / 2.0F, -size / 2.0F, size, size, color, alpha);
         pose.pop();
      }
      this.immediate.draw();
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
      buffer.vertex(matrix, x, y, 0.0F).color(r, g, b, alpha).texture(0.0F, 0.0F).normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x, y2, 0.0F).color(r, g, b, alpha).texture(0.0F, 1.0F).normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y2, 0.0F).color(r, g, b, alpha).texture(1.0F, 1.0F).normal(normal.x, normal.y, normal.z);
      buffer.vertex(matrix, x2, y, 0.0F).color(r, g, b, alpha).texture(1.0F, 0.0F).normal(normal.x, normal.y, normal.z);
   }

   @Environment(EnvType.CLIENT)
   private static final class Particle {
      private Vec3d pos;
      private final Vec3d velocity;
      private final long startTime;
      private final long lifetime;
      private final float size;

      Particle(Vec3d pos, Vec3d velocity, long startTime, long lifetime) {
         this.pos = pos;
         this.velocity = velocity;
         this.startTime = startTime;
         this.lifetime = lifetime;
         this.size = 0.5F + new Random().nextFloat() * 0.8F;
      }
   }
}
