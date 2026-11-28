package ru.fluxvisuals.module.impl.visuals.killeffect;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import ru.fluxvisuals.util.render.world.WorldGeometryEmitter;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/**
 * Затемнение и похищение цели шипами. Геометрия, фазы и длительности перенесены
 * из исходной реализации: цилиндры с градиентом альфы, кольца, 12 -> 6 частиц
 * по спирали и анимированная модель шипа.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectDarknessRenderer {
   private static final int MAX_EFFECTS = 20;
   private static final long BEAM_DURATION_MS = 1800L;
   private static final long DARKNESS_DURATION_MS = 1650L;
   private static final long SPIKE_DURATION_MS = 4200L;
   private static final float BEAM_HEIGHT = 64.0F;
   private static final int BEAM_SEGMENTS = 24;
   private static final float DARK_CYLINDER_HEIGHT = 2.7F;
   private static final float GLOW_CYLINDER_HEIGHT = 2.9F;
   private static final SpikeModel SPIKE_MODEL = new SpikeModel();
   private static final List<Darkness> EFFECTS = new ArrayList<>();

   private KillEffectDarknessRenderer() {}

   public static void spawn(Vec3d pos, int color, float durationSeconds, float height, float width,
         float radius, float speed, int particles, boolean spikes) {
      if (pos == null) {
         return;
      }
      if (EFFECTS.size() >= MAX_EFFECTS) {
         EFFECTS.remove(0);
      }
      // Эталон фиксирует длительности режимов: duration из настроек не растягивает фазы.
      long durationMs = spikes ? SPIKE_DURATION_MS : DARKNESS_DURATION_MS;
      double yaw = MinecraftClient.getInstance().player == null
            ? 0.0 : MinecraftClient.getInstance().player.getYaw();
      double angle = Math.toRadians(yaw);
      EFFECTS.add(new Darkness(pos, color, durationMs, Math.max(0.25F, radius),
            Math.max(0.1F, speed), Math.max(4, Math.min(12, particles)), spikes,
            -Math.sin(angle), Math.cos(angle), ThreadLocalRandom.current().nextLong()));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      EFFECTS.removeIf(effect -> now - effect.startedAt >= effect.durationMs);
      SPIKE_MODEL.tickAnimation();
   }

   public static void render(WorldRenderer renderer) {
      // Очистка выполняется и здесь: во время паузы client-tick может не приходить.
      tick();
      if (renderer == null || EFFECTS.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Darkness effect : EFFECTS) {
         long elapsed = now - effect.startedAt;
         if (elapsed >= effect.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / effect.durationMs);
         if (effect.spikeMode) {
            renderSpikeAbduction(renderer, effect, progress);
         } else {
            renderDarkness(renderer, effect, progress);
         }
      }
   }

   public static void clear() {
      EFFECTS.clear();
      SPIKE_MODEL.resetAnimation();
   }

   /** Тёмный цилиндр, фиолетовое свечение, кольцо и спираль частиц из эталона. */
   private static void renderDarkness(WorldRenderer renderer, Darkness effect, float progress) {
      float appear = Math.min(1.0F, progress / 0.1F);
      float fade = 1.0F - smoothstep(0.68F, 1.0F, progress);
      if (appear <= 0.0F || fade <= 0.0F) {
         return;
      }
      float radius = (1.45F - progress * 0.95F) * appear;
      Vec3d base = effect.pos.add(0.0, 0.05, 0.0);

      // Тёмное ядро рисуется полупрозрачным слоем; затемнение экрана делает overlay.
      drawCylinder(renderer, base, radius, DARK_CYLINDER_HEIGHT,
            0.015F, 0.005F, 0.025F, 0.82F * fade,
            WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH());
      drawCylinder(renderer, base, radius * 1.18F, GLOW_CYLINDER_HEIGHT,
            0.2F, 0.02F, 0.34F, 0.22F * fade,
            WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND());
      drawGroundRing(renderer, base, radius * 2.1F, radius * 0.22F,
            0.24F, 0.01F, 0.38F, 0.46F * fade,
            WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH());

      spawnDarknessParticles(effect, progress);
   }

   /** Ванильные частицы из alphadcp200: 12 -> 6 штук плюс particles у основания. */
   private static void spawnDarknessParticles(Darkness effect, float progress) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null) {
         return;
      }
      float pull = 1.0F - progress;
      int count = progress < 0.78F ? 12 : 6;
      for (int i = 0; i < count; i++) {
         double angle = progress * 18.0 + i * (Math.PI * 2.0 / count)
               + client.world.random.nextDouble() * 0.35;
         double radius = (0.25 + client.world.random.nextDouble() * 1.45) * (0.35 + pull * 0.65);
         double height = 0.15 + client.world.random.nextDouble() * (2.35 * pull + 0.35);
         double px = effect.pos.x + Math.cos(angle) * radius;
         double pz = effect.pos.z + Math.sin(angle) * radius;
         double vx = (effect.pos.x - px) * 0.09;
         double vz = (effect.pos.z - pz) * 0.09;
         addParticle(px, effect.pos.y + height, pz, vx, -0.16, vz,
               (i & 1) == 0 ? ParticleTypes.PORTAL : ParticleTypes.SMOKE);
      }
      addParticle(effect.pos.x, effect.pos.y + 0.2 + pull, effect.pos.z,
            0.0, -0.12, 0.0, ParticleTypes.CLOUD);
   }

   private static void addParticle(double x, double y, double z,
         double vx, double vy, double vz, net.minecraft.particle.ParticleEffect type) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null) {
         client.world.addParticleClient(type, x, y, z, vx, vy, vz);
      }
   }

   /**
    * Похищение: путь -7.5 -> 0 -> 11.5, складка/подъём/полёт и модель шипа.
    * В финальной фазе добавляется небесный луч с параметрами эталона.
    */
   private static void renderSpikeAbduction(WorldRenderer renderer, Darkness effect, float progress) {
      double path = spikePath(progress);
      Vec3d spike = effect.pos.add(effect.directionX * path, 0.0, effect.directionZ * path);
      boolean running = progress < 0.28F || progress > 0.56F;
      float fold = smoothstep(0.28F, 0.4F, progress);
      float lift = smoothstep(0.4F, 0.56F, progress);
      float escape = smoothstep(0.56F, 1.0F, progress);
      float jump = escape > 0.0F
            ? Math.max(0.0F, (float) Math.sin(escape * Math.PI * 5.0F)) * 0.38F : 0.0F;
      float runYaw = (float) Math.toDegrees(Math.atan2(-effect.directionX, effect.directionZ));

      // alphadcp200: частица-след шипа, а не линия.
      if (running) {
         addParticle(spike.x, effect.pos.y + 0.08, spike.z,
               -effect.directionX * 0.035, 0.015, -effect.directionZ * 0.035,
               ParticleTypes.CRIT);
      }

      SPIKE_MODEL.render(renderer, spike.add(0.0, jump, 0.0), runYaw,
            running ? 1.0F : 0.08F, running ? 0.105F : 0.0F, lift);

      float ringRadius = effect.radius * (0.25F + KillEffectEasing.quintOut(progress) * 1.1F);
      drawGroundRing(renderer, effect.pos.add(0.0, 0.02, 0.0), ringRadius, ringRadius * 0.22F,
            0.24F, 0.01F, 0.38F, 0.3F * (1.0F - escape),
            WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH());

      if (escape > 0.0F) {
         float beamProgress = Math.min(1.0F, (float) (System.currentTimeMillis() - effect.startedAt)
               / BEAM_DURATION_MS);
         float appear = Math.min(1.0F, beamProgress / 0.08F);
         float fade = 1.0F - smoothstep(0.62F, 1.0F, escape);
         float beamRadius = (1.28F * (1.0F - escape) + 0.035F) * appear;
         Vec3d beamBase = spike.add(0.0, jump, 0.0);
         drawCylinder(renderer, beamBase, beamRadius * 1.75F, BEAM_HEIGHT + jump,
               0.72F, 0.58F, 1.0F, 0.22F * fade,
               WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND());
         drawGroundRing(renderer, beamBase, beamRadius * 2.5F, beamRadius * 0.72F,
               0.86F, 0.75F, 1.0F, 0.3F * fade,
               WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND());
         drawCylinder(renderer, beamBase, beamRadius * 0.62F, BEAM_HEIGHT + jump,
               1.0F, 0.96F, 1.0F, 0.9F * fade,
               WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND());
      }
   }

   private static double spikePath(float progress) {
      if (progress < 0.28F) {
         return lerp(smoothstep(0.0F, 0.28F, progress), -7.5, 0.0);
      }
      return progress < 0.56F ? 0.0 : lerp(smoothstep(0.56F, 1.0F, progress), 0.0, 11.5);
   }

   private static double lerp(double delta, double start, double end) {
      return start + (end - start) * delta;
   }

   private static float smoothstep(float edge0, float edge1, float value) {
      float t = KillEffectEasing.clamp((value - edge0) / (edge1 - edge0));
      return t * t * (3.0F - 2.0F * t);
   }

   private static int rgba(float red, float green, float blue, float alpha) {
      int r = Math.round(KillEffectEasing.clamp(red) * 255.0F);
      int g = Math.round(KillEffectEasing.clamp(green) * 255.0F);
      int b = Math.round(KillEffectEasing.clamp(blue) * 255.0F);
      int a = Math.round(KillEffectEasing.clamp(alpha) * 255.0F);
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   private static WorldGeometryEmitter emitter(WorldRenderer renderer, RenderLayer layer) {
      return new WorldGeometryEmitter(renderer, renderer.matrixStack().peek(), renderer.getBuffer(layer));
   }

   /** Цилиндр с 24 сегментами и затуханием альфы к верхней кромке (как в эталоне). */
   private static void drawCylinder(WorldRenderer renderer, Vec3d center, float radius, float height,
         float red, float green, float blue, float alpha, RenderLayer layer) {
      if (radius <= 0.0F || alpha <= 0.0F) {
         return;
      }
      WorldGeometryEmitter geometry = emitter(renderer, layer);
      int bottom = rgba(red, green, blue, alpha);
      int top = rgba(red, green, blue, 0.0F);
      float base = (float) center.y;
      for (int i = 0; i < BEAM_SEGMENTS; i++) {
         double a0 = Math.PI * 2.0 * i / BEAM_SEGMENTS;
         double a1 = Math.PI * 2.0 * (i + 1) / BEAM_SEGMENTS;
         float x0 = (float) center.x + (float) Math.cos(a0) * radius;
         float z0 = (float) center.z + (float) Math.sin(a0) * radius;
         float x1 = (float) center.x + (float) Math.cos(a1) * radius;
         float z1 = (float) center.z + (float) Math.sin(a1) * radius;
         Vec3d p0 = new Vec3d(x0, base, z0);
         Vec3d p1 = new Vec3d(x1, base, z1);
         Vec3d p2 = new Vec3d(x1, base + height, z1);
         Vec3d p3 = new Vec3d(x0, base + height, z0);
         geometry.emitQuad(p0, p1, p2, p3, bottom, bottom, top, top);
      }
   }

   /** Кольцо с градиентом от внутренней кромки к внешней. */
   private static void drawGroundRing(WorldRenderer renderer, Vec3d center, float outer, float inner,
         float red, float green, float blue, float alpha, RenderLayer layer) {
      if (outer <= 0.0F || inner < 0.0F || alpha <= 0.0F) {
         return;
      }
      WorldGeometryEmitter geometry = emitter(renderer, layer);
      int solid = rgba(red, green, blue, alpha);
      int faded = rgba(red, green, blue, 0.0F);
      float y = (float) center.y + 0.025F;
      for (int i = 0; i < BEAM_SEGMENTS; i++) {
         double a0 = Math.PI * 2.0 * i / BEAM_SEGMENTS;
         double a1 = Math.PI * 2.0 * (i + 1) / BEAM_SEGMENTS;
         Vec3d i0 = new Vec3d(center.x + Math.cos(a0) * inner, y, center.z + Math.sin(a0) * inner);
         Vec3d i1 = new Vec3d(center.x + Math.cos(a1) * inner, y, center.z + Math.sin(a1) * inner);
         Vec3d o1 = new Vec3d(center.x + Math.cos(a1) * outer, y, center.z + Math.sin(a1) * outer);
         Vec3d o0 = new Vec3d(center.x + Math.cos(a0) * outer, y, center.z + Math.sin(a0) * outer);
         geometry.emitQuad(i0, i1, o1, o0, solid, solid, faded, faded);
      }
   }

   private static void drawCrossedParticle(WorldGeometryEmitter geometry, Vec3d pos, float size, int color) {
      Vec3d x0 = pos.add(-size, -size, 0.0);
      Vec3d x1 = pos.add(size, -size, 0.0);
      Vec3d x2 = pos.add(size, size, 0.0);
      Vec3d x3 = pos.add(-size, size, 0.0);
      geometry.emitQuad(x0, x1, x2, x3, color);
      Vec3d z0 = pos.add(0.0, -size, -size);
      Vec3d z1 = pos.add(0.0, -size, size);
      Vec3d z2 = pos.add(0.0, size, size);
      Vec3d z3 = pos.add(0.0, size, -size);
      geometry.emitQuad(z0, z1, z2, z3, color);
   }

   private record Darkness(Vec3d pos, int color, long durationMs, long startedAt, float radius,
                           float speed, int particles, boolean spikeMode, double directionX,
                           double directionZ, long seed) {
      private Darkness(Vec3d pos, int color, long durationMs, float radius, float speed, int particles,
            boolean spikeMode, double directionX, double directionZ, long seed) {
         this(pos, color, durationMs, System.currentTimeMillis(), radius, speed, particles, spikeMode,
               directionX, directionZ, seed);
      }
   }

   /** Загрузчик и рендер анимированной модели шипа из spike_mesh.json. */
   private static final class SpikeModel {
      private static final Identifier MODEL_LOCATION =
            Identifier.of("fluxvisuals", "models/killeffect/spike_mesh.json");
      private static final Identifier TEXTURE_LOCATION =
            Identifier.of("fluxvisuals", "textures/models/killeffect/spike.png");
      private static final RenderLayer TEXTURED_LAYER = WorldRenderLayers.TEXTURED_QUADS(TEXTURE_LOCATION);
      private static final float MODEL_SCALE = 1.18F;
      private final List<ModelNode> nodes = new ArrayList<>();
      private final List<Integer> rootNodes = new ArrayList<>();
      private final List<ModelAnimation> animations = new ArrayList<>();
      private ModelAnimation idle;
      private ModelAnimation walk;
      private boolean loaded;
      private boolean failed;
      private float modelYawOffset = 180.0F;
      private float centerX;
      private float centerZ;
      private float minY;
      private float walkBlend;
      private float cycle;
      private long lastAnimationNanos;

      void tickAnimation() {
         long now = System.nanoTime();
         if (this.lastAnimationNanos == 0L) {
            this.lastAnimationNanos = now;
         }
      }

      void resetAnimation() {
         this.walkBlend = 0.0F;
         this.cycle = 0.0F;
         this.lastAnimationNanos = 0L;
      }

      void render(WorldRenderer renderer, Vec3d position, float yaw, float walkAmount,
            float moveSpeed, float carryPose) {
         this.ensureLoaded();
         if (this.failed || this.nodes.isEmpty()) {
            return;
         }
         float delta = this.updateClock();
         float targetBlend = walkAmount > 0.025F ? Math.min(1.0F, walkAmount * 1.35F) : 0.0F;
         this.walkBlend = approach(this.walkBlend, targetBlend, delta, 3.6F);
         this.cycle += delta * (1.45F + moveSpeed * 58.0F);
         float age = System.currentTimeMillis() / 50.0F;
         float idleCycle = age * 0.06F;
         float idleBlend = 1.0F - this.walkBlend;
         float bob = ((float) Math.sin(idleCycle) * 7.0E-4F
               + (float) Math.sin(idleCycle * 0.31F + 0.9F) * 4.5E-4F) * idleBlend
               + Math.abs((float) Math.sin(this.cycle * 2.0F)) * 0.0012F * this.walkBlend;
         float bodyPitch = ((float) Math.cos(idleCycle * 0.42F) * 0.026F - 0.018F) * idleBlend
               + ((float) Math.cos(this.cycle * 2.0F) * 0.035F - 0.008F) * this.walkBlend;
         float stride = 30.0F * this.walkBlend;
         float frontLeftSwing = (float) Math.cos(this.cycle) * stride;
         float frontRightSwing = (float) Math.cos(this.cycle + Math.PI) * stride;
         float rearLeftSwing = (float) Math.cos(this.cycle + Math.PI) * stride * 0.86F;
         float rearRightSwing = (float) Math.cos(this.cycle) * stride * 0.86F;
         float frontLeftKnee = Math.max(0.0F, (float) Math.sin(this.cycle)) * stride * 0.72F;
         float frontRightKnee = Math.max(0.0F, (float) Math.sin(this.cycle + Math.PI)) * stride * 0.72F;
         float rearLeftKnee = Math.max(0.0F, (float) Math.sin(this.cycle + Math.PI)) * stride * 0.78F;
         float rearRightKnee = Math.max(0.0F, (float) Math.sin(this.cycle)) * stride * 0.78F;
         float frontLeftFoot = (frontLeftKnee / Math.max(1.0F, stride) * -0.38F
               + (float) Math.cos(this.cycle) * 0.12F) * stride;
         float frontRightFoot = (frontRightKnee / Math.max(1.0F, stride) * -0.38F
               + (float) Math.cos(this.cycle + Math.PI) * 0.12F) * stride;

         AnimationPose pose = new AnimationPose(age / 20.0F, this.cycle * 0.095F, this.walkBlend,
               frontLeftSwing, frontRightSwing, rearLeftSwing, rearRightSwing, frontLeftKnee,
               frontRightKnee, rearLeftKnee, rearRightKnee, frontLeftFoot, frontRightFoot,
               -12.0F, carryPose);

         MatrixStack stack = renderer.matrixStack();
         stack.push();
         // WorldGeometryEmitter сам переводит вершины в пространство камеры.
         stack.translate(position.x, position.y + bob, position.z);
         stack.multiply(new Quaternionf().rotateY((float) Math.toRadians(this.modelYawOffset - yaw)));
         stack.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);
         stack.translate(-this.centerX, -this.minY, -this.centerZ);
         stack.multiply(new Quaternionf().rotateX(bodyPitch));
         VertexConsumer vertices = renderer.getBuffer(TEXTURED_LAYER);
         for (int root : this.rootNodes) {
            this.renderNode(stack, renderer, vertices, root, pose);
         }
         stack.pop();
      }

      private void renderNode(MatrixStack stack, WorldRenderer renderer, VertexConsumer vertices,
            int index, AnimationPose pose) {
         if (index < 0 || index >= this.nodes.size()) {
            return;
         }
         ModelNode node = this.nodes.get(index);
         stack.push();
         stack.translate(node.translation[0], node.translation[1], node.translation[2]);
         stack.multiply(new Quaternionf(node.rotation[0], node.rotation[1], node.rotation[2], node.rotation[3]));
         stack.scale(node.scale[0], node.scale[1], node.scale[2]);
         this.applyAnimation(stack, node.name, pose);
         if (!node.triangles.isEmpty()) {
            WorldGeometryEmitter geometry = new WorldGeometryEmitter(renderer, stack.peek(), vertices);
            for (MeshTriangle triangle : node.triangles) {
               geometry.emitTexturedQuad(triangle.a, triangle.b, triangle.c, triangle.c,
                     triangle.aU, triangle.aV, triangle.bU, triangle.bV,
                     triangle.cU, triangle.cV, triangle.cU, triangle.cV, 0xFFFFFFFF);
            }
         }
         for (int child : node.children) {
            this.renderNode(stack, renderer, vertices, child, pose);
         }
         stack.pop();
      }

      private void applyAnimation(MatrixStack stack, String name, AnimationPose pose) {
         float[] idleTranslation = this.idle == null ? null : this.idle.sample(name, "translation", pose.animationTime);
         float[] walkTranslation = this.walk == null ? null : this.walk.sample(name, "translation", pose.walkAnimationTime);
         float[] translation = blend(idleTranslation, walkTranslation, pose.walkBlend);
         if (translation != null) {
            stack.translate(translation[0], translation[1], translation[2]);
         }
         float[] idleRotation = this.idle == null ? null : this.idle.sample(name, "rotation", pose.animationTime);
         float[] walkRotation = this.walk == null ? null : this.walk.sample(name, "rotation", pose.walkAnimationTime);
         float[] rotation = slerp(idleRotation, walkRotation, pose.walkBlend);
         if (rotation != null) {
            stack.multiply(new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]));
         }
      }

      private static float[] blend(float[] idle, float[] walk, float amount) {
         if (idle == null && walk == null) {
            return null;
         }
         float[] a = idle == null ? new float[]{0.0F, 0.0F, 0.0F} : idle;
         float[] b = walk == null ? a : walk;
         return new float[]{lerp(amount, a[0], b[0]), lerp(amount, a[1], b[1]), lerp(amount, a[2], b[2])};
      }

      private static float[] slerp(float[] idle, float[] walk, float amount) {
         if (idle == null && walk == null) {
            return null;
         }
         float[] a = idle == null ? new float[]{0.0F, 0.0F, 0.0F, 1.0F} : idle;
         float[] b = walk == null ? a : walk;
         float dot = a[0] * b[0] + a[1] * b[1] + a[2] * b[2] + a[3] * b[3];
         float bx = b[0];
         float by = b[1];
         float bz = b[2];
         float bw = b[3];
         if (dot < 0.0F) {
            dot = -dot;
            bx = -bx;
            by = -by;
            bz = -bz;
            bw = -bw;
         }
         float sa;
         float sb;
         if (dot > 0.9995F) {
            sa = 1.0F - amount;
            sb = amount;
         } else {
            float theta = (float) Math.acos(KillEffectEasing.clamp(dot));
            float sin = (float) Math.sin(theta);
            sa = (float) Math.sin((1.0F - amount) * theta) / sin;
            sb = (float) Math.sin(amount * theta) / sin;
         }
         float x = sa * a[0] + sb * bx;
         float y = sa * a[1] + sb * by;
         float z = sa * a[2] + sb * bz;
         float w = sa * a[3] + sb * bw;
         float length = (float) Math.sqrt(x * x + y * y + z * z + w * w);
         return length < 1.0E-5F ? new float[]{0.0F, 0.0F, 0.0F, 1.0F}
               : new float[]{x / length, y / length, z / length, w / length};
      }

      private static float lerp(float amount, float start, float end) {
         return start + (end - start) * amount;
      }

      private static float approach(float current, float target, float delta, float speed) {
         return current + (target - current) * (1.0F - (float) Math.exp(-delta * speed));
      }

      private float updateClock() {
         long now = System.nanoTime();
         if (this.lastAnimationNanos == 0L) {
            this.lastAnimationNanos = now;
            return 0.016666668F;
         }
         float delta = (float) (now - this.lastAnimationNanos) / 1.0E9F;
         this.lastAnimationNanos = now;
         return Math.min(delta, 0.05F);
      }

      private void ensureLoaded() {
         if (this.loaded || this.failed) {
            return;
         }
         try (InputStream stream = MinecraftClient.getInstance().getResourceManager().open(MODEL_LOCATION);
              InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            this.parse(JsonParser.parseReader(reader).getAsJsonObject());
            this.loaded = true;
         } catch (Exception exception) {
            this.failed = true;
            System.err.println("[FluxVisuals] Failed to load kill effect spike model: " + MODEL_LOCATION);
         }
      }

      private void parse(JsonObject root) {
         this.nodes.clear();
         this.rootNodes.clear();
         this.animations.clear();
         this.idle = null;
         this.walk = null;
         JsonObject bounds = root.getAsJsonObject("bounds");
         JsonArray min = bounds.getAsJsonArray("min");
         JsonArray max = bounds.getAsJsonArray("max");
         this.minY = min.get(1).getAsFloat();
         this.centerX = (min.get(0).getAsFloat() + max.get(0).getAsFloat()) * 0.5F;
         this.centerZ = (min.get(2).getAsFloat() + max.get(2).getAsFloat()) * 0.5F;
         for (int i = 0; i < root.getAsJsonArray("rootNodes").size(); i++) {
            this.rootNodes.add(root.getAsJsonArray("rootNodes").get(i).getAsInt());
         }
         JsonArray nodeArray = root.getAsJsonArray("nodes");
         for (int i = 0; i < nodeArray.size(); i++) {
            JsonObject object = nodeArray.get(i).getAsJsonObject();
            String name = object.has("name") ? object.get("name").getAsString() : "";
            float[] translation = floats(object.getAsJsonArray("translation"), 3,
                  new float[]{0.0F, 0.0F, 0.0F});
            float[] rotation = floats(object.getAsJsonArray("rotation"), 4,
                  new float[]{0.0F, 0.0F, 0.0F, 1.0F});
            float[] scale = floats(object.getAsJsonArray("scale"), 3,
                  new float[]{1.0F, 1.0F, 1.0F});
            int[] children = ints(object.getAsJsonArray("children"));
            List<MeshTriangle> triangles = new ArrayList<>();
            JsonArray triangleArray = object.getAsJsonArray("triangles");
            if (triangleArray != null) {
               for (int t = 0; t < triangleArray.size(); t++) {
                  JsonArray triangle = triangleArray.get(t).getAsJsonArray();
                  triangles.add(triangle(triangle.get(0).getAsJsonArray(),
                        triangle.get(1).getAsJsonArray(), triangle.get(2).getAsJsonArray()));
               }
            }
            this.nodes.add(new ModelNode(name, translation, rotation, scale, children, triangles));
         }
         int chest = this.findNode("chest");
         int bone = this.findNode("bone");
         int rootIndex = this.rootNodes.isEmpty() ? -1 : this.rootNodes.get(0);
         if (chest >= 0 && bone >= 0 && rootIndex >= 0) {
            float[] chestPos = this.nodePosition(rootIndex, chest, identity());
            float[] rootPos = this.nodePosition(rootIndex, bone, identity());
            float dx = chestPos[0] - rootPos[0];
            float dz = chestPos[2] - rootPos[2];
            this.modelYawOffset = Math.abs(dx) < 1.0E-4F && Math.abs(dz) < 1.0E-4F
                  ? 180.0F
                  : normalizeDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F);
         }
         JsonArray animationArray = root.getAsJsonArray("animations");
         if (animationArray != null) {
            for (int i = 0; i < animationArray.size(); i++) {
               JsonObject object = animationArray.get(i).getAsJsonObject();
               String name = object.has("name") ? object.get("name").getAsString() : "";
               float duration = object.has("duration") ? object.get("duration").getAsFloat() : 0.0F;
               List<AnimationChannel> channels = new ArrayList<>();
               JsonArray channelArray = object.getAsJsonArray("channels");
               if (channelArray != null) {
                  for (int c = 0; c < channelArray.size(); c++) {
                     JsonObject channelObject = channelArray.get(c).getAsJsonObject();
                     channels.add(new AnimationChannel(
                           channelObject.has("node") ? channelObject.get("node").getAsString() : "",
                           channelObject.has("path") ? channelObject.get("path").getAsString() : "",
                           floats(channelObject.getAsJsonArray("times"), -1, new float[0]),
                           matrix(channelObject.getAsJsonArray("values"))));
                  }
               }
               ModelAnimation animation = new ModelAnimation(name, duration, channels);
               this.animations.add(animation);
               if ("idle".equalsIgnoreCase(name)) {
                  this.idle = animation;
               } else if ("walk".equalsIgnoreCase(name)) {
                  this.walk = animation;
               }
            }
         }
      }

      private int findNode(String name) {
         for (int i = 0; i < this.nodes.size(); i++) {
            if (name.equals(this.nodes.get(i).name)) {
               return i;
            }
         }
         return -1;
      }

      private float[] nodePosition(int current, int target, float[] parent) {
         if (current < 0 || current >= this.nodes.size()) {
            return null;
         }
         ModelNode node = this.nodes.get(current);
         float[] matrix = multiply(parent, compose(node));
         if (current == target) {
            return new float[]{matrix[12], matrix[13], matrix[14]};
         }
         for (int child : node.children) {
            float[] found = this.nodePosition(child, target, matrix);
            if (found != null) {
               return found;
            }
         }
         return null;
      }

      private static float[] identity() {
         return new float[]{1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F, 0.0F, 0.0F,
               0.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F};
      }

      private static float[] compose(ModelNode node) {
         float x = node.rotation[0];
         float y = node.rotation[1];
         float z = node.rotation[2];
         float w = node.rotation[3];
         float x2 = x + x;
         float y2 = y + y;
         float z2 = z + z;
         float xx = x * x2;
         float xy = x * y2;
         float xz = x * z2;
         float yy = y * y2;
         float yz = y * z2;
         float zz = z * z2;
         float wx = w * x2;
         float wy = w * y2;
         float wz = w * z2;
         return new float[]{
               (1.0F - (yy + zz)) * node.scale[0], (xy + wz) * node.scale[0],
               (xz - wy) * node.scale[0], 0.0F,
               (xy - wz) * node.scale[1], (1.0F - (xx + zz)) * node.scale[1],
               (yz + wx) * node.scale[1], 0.0F,
               (xz + wy) * node.scale[2], (yz - wx) * node.scale[2],
               (1.0F - (xx + yy)) * node.scale[2], 0.0F,
               node.translation[0], node.translation[1], node.translation[2], 1.0F};
      }

      private static float[] multiply(float[] a, float[] b) {
         float[] out = new float[16];
         for (int column = 0; column < 4; column++) {
            for (int row = 0; row < 4; row++) {
               out[column * 4 + row] = a[row] * b[column * 4] + a[4 + row] * b[column * 4 + 1]
                     + a[8 + row] * b[column * 4 + 2] + a[12 + row] * b[column * 4 + 3];
            }
         }
         return out;
      }

      private static float[] floats(JsonArray array, int expected, float[] fallback) {
         if (array == null || array.size() == 0 || (expected > 0 && array.size() < expected)) {
            return fallback;
         }
         float[] values = new float[array.size()];
         for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsFloat();
         }
         return values;
      }

      private static int[] ints(JsonArray array) {
         if (array == null) {
            return new int[0];
         }
         int[] values = new int[array.size()];
         for (int i = 0; i < values.length; i++) {
            values[i] = array.get(i).getAsInt();
         }
         return values;
      }

      private static float[][] matrix(JsonArray array) {
         if (array == null) {
            return new float[0][0];
         }
         float[][] values = new float[array.size()][];
         for (int i = 0; i < values.length; i++) {
            values[i] = floats(array.get(i).getAsJsonArray(), 0, new float[0]);
         }
         return values;
      }

      private static MeshTriangle triangle(JsonArray a, JsonArray b, JsonArray c) {
         return new MeshTriangle(
               a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat(),
               a.get(3).getAsFloat(), a.get(4).getAsFloat(),
               b.get(0).getAsFloat(), b.get(1).getAsFloat(), b.get(2).getAsFloat(),
               b.get(3).getAsFloat(), b.get(4).getAsFloat(),
               c.get(0).getAsFloat(), c.get(1).getAsFloat(), c.get(2).getAsFloat(),
               c.get(3).getAsFloat(), c.get(4).getAsFloat());
      }

      private static float normalizeDegrees(float angle) {
         float fixed = angle % 360.0F;
         return fixed < 0.0F ? fixed + 360.0F : fixed;
      }
   }

   private record MeshTriangle(Vec3d a, Vec3d b, Vec3d c, float aU, float aV,
                               float bU, float bV, float cU, float cV) {
      private MeshTriangle(float ax, float ay, float az, float au, float av,
            float bx, float by, float bz, float bu, float bv,
            float cx, float cy, float cz, float cu, float cv) {
         this(new Vec3d(ax, ay, az), new Vec3d(bx, by, bz), new Vec3d(cx, cy, cz),
               au, av, bu, bv, cu, cv);
      }
   }

   private record ModelNode(String name, float[] translation, float[] rotation, float[] scale,
                            int[] children, List<MeshTriangle> triangles) {}

   private record AnimationChannel(String node, String path, float[] times, float[][] values) {
      float[] sample(float duration, float time) {
         if (this.times.length == 0 || this.values.length == 0) {
            return null;
         }
         float local = duration > 0.0F ? time % duration : time;
         if (local < 0.0F) {
            local += duration;
         }
         if (this.times.length == 1 || local <= this.times[0]) {
            return this.values[0];
         }
         int last = Math.min(this.times.length, this.values.length) - 1;
         if (last < 1 || local >= this.times[last]) {
            return this.values[last];
         }
         int frame = 0;
         while (frame + 1 < last && local > this.times[frame + 1]) {
            frame++;
         }
         float start = this.times[frame];
         float end = this.times[frame + 1];
         float amount = end <= start ? 0.0F : (local - start) / (end - start);
         if ("rotation".equals(this.path)) {
            return SpikeModel.slerp(this.values[frame], this.values[frame + 1], amount);
         }
         float[] a = this.values[frame];
         float[] b = this.values[frame + 1];
         int size = Math.min(a.length, b.length);
         float[] result = new float[size];
         for (int i = 0; i < size; i++) {
            result[i] = SpikeModel.lerp(amount, a[i], b[i]);
         }
         return result;
      }
   }

   private record ModelAnimation(String name, float duration, List<AnimationChannel> channels) {
      float[] sample(String nodeName, String path, float time) {
         for (AnimationChannel channel : this.channels) {
            if (channel.node.equals(nodeName) && channel.path.equals(path)) {
               return channel.sample(this.duration, time);
            }
         }
         return null;
      }
   }

   private record AnimationPose(float animationTime, float walkAnimationTime, float walkBlend,
                                float frontLeftSwing, float frontRightSwing, float rearLeftSwing,
                                float rearRightSwing, float frontLeftKnee, float frontRightKnee,
                                float rearLeftKnee, float rearRightKnee, float frontLeftFoot,
                                float frontRightFoot, float headPitch, float carryPose) {}
}
