package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Душа и осколки: спирали, кольца, вихрь и разлетающиеся обломки. */
@Environment(EnvType.CLIENT)
public final class KillEffectSoulRenderer {
   private static final int MAX_EFFECTS = 24;
   private static final int RING_SEGMENTS = 20;
   private static final double GOLDEN_ANGLE = 2.399963229728653;

   private static final List<Structured> EFFECTS = new ArrayList<>();

   private KillEffectSoulRenderer() {}

   public static void spawn(Vec3d pos, int color, float durationSeconds, float height, float width,
         float radius, float speed, int particles, boolean soul) {
      if (pos == null) {
         return;
      }
      if (EFFECTS.size() >= MAX_EFFECTS) {
         EFFECTS.remove(0);
      }
      long durationMs = Math.max(300L, Math.round(durationSeconds * 1000.0F));
      EFFECTS.add(new Structured(pos, color, durationMs, Math.max(0.5F, height), Math.max(0.05F, width),
            Math.max(0.25F, radius), Math.max(0.1F, speed), Math.max(4, Math.min(120, particles)),
            (int) System.currentTimeMillis(), soul));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      EFFECTS.removeIf(effect -> now - effect.startedAt >= effect.durationMs);
   }

   public static void render(WorldRenderer renderer) {
      if (renderer == null || EFFECTS.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Structured effect : EFFECTS) {
         long elapsed = now - effect.startedAt;
         if (elapsed >= effect.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / effect.durationMs);
         int alpha = Math.round(220.0F * (1.0F - progress * progress));
         int solid = Renderer2D.ColorUtil.replAlpha(effect.color, alpha);
         int glow = Renderer2D.ColorUtil.replAlpha(effect.color, Math.max(0, alpha / 3));
         Vec3d base = effect.pos.add(0.0, 0.2, 0.0);

         if (effect.soul) {
            renderSoul(renderer, effect, base, progress, solid, glow);
         } else {
            renderShatter(renderer, effect, base, progress, solid, glow);
         }
      }
   }

   public static void clear() {
      EFFECTS.clear();
   }

   private static void renderSoul(WorldRenderer renderer, Structured effect, Vec3d base, float progress,
         int solid, int glow) {
      Vec3d top = base.add(0.0, effect.height * KillEffectEasing.quintOut(Math.min(1.0F, progress * 1.6F)), 0.0);
      renderer.drawLine(base, top, Math.max(0.6, effect.width), glow, false);

      int strands = Math.max(3, Math.min(12, effect.particles / 12));
      for (int i = 0; i < strands; i++) {
         double phase = Math.PI * 2.0 * i / strands + progress * effect.speed * 2.0;
         Vec3d previous = base;
         for (int step = 1; step <= 5; step++) {
            float stepProgress = progress + step * 0.08F;
            double angle = phase + step * 0.9 + stepProgress * effect.speed;
            double ring = effect.radius * (0.35F + 0.65F * Math.sin(stepProgress * Math.PI));
            Vec3d next = base.add(Math.cos(angle) * ring,
                  effect.height * step / 5.0F * KillEffectEasing.quintOut(Math.min(1.0F, stepProgress * 1.6F)),
                  Math.sin(angle) * ring);
            renderer.drawLine(previous, next, Math.max(0.4, effect.width * 0.7), solid, false);
            previous = next;
         }
      }

      int rings = Math.max(2, Math.min(6, effect.particles / 24));
      for (int ringIndex = 0; ringIndex < rings; ringIndex++) {
         float ringProgress = KillEffectEasing.clamp(progress * 1.4F - ringIndex * 0.12F);
         if (ringProgress <= 0.0F) {
            continue;
         }
         float y = effect.height * ringProgress;
         float ringRadius = effect.radius * (0.5F + 0.5F * ringProgress);
         drawRing(renderer, base.add(0.0, y, 0.0), ringRadius, RING_SEGMENTS, glow);
      }
   }

   private static void renderShatter(WorldRenderer renderer, Structured effect, Vec3d base, float progress,
         int solid, int glow) {
      float expansion = KillEffectEasing.quintOut(progress);
      float sphereRadius = effect.radius * (0.2F + expansion * 1.4F);
      drawRing(renderer, base, sphereRadius, RING_SEGMENTS, solid);
      drawRing(renderer, base, sphereRadius, RING_SEGMENTS, glow, true);
      drawRing(renderer, base, sphereRadius, RING_SEGMENTS, glow, false, true);

      int shards = Math.max(6, Math.min(64, effect.particles / 3));
      float shardSize = Math.max(0.04F, effect.width * 0.45F);
      for (int i = 0; i < shards; i++) {
         double angle = i * GOLDEN_ANGLE + effect.seed * 0.001;
         double heightFactor = hash(i + effect.seed) * 2.0 - 1.0;
         double distance = sphereRadius * (0.35 + hash(i * 3 + effect.seed) * 0.75);
         double vertical = (heightFactor * sphereRadius * 0.7 + expansion * effect.radius * 0.5);
         Vec3d shard = base.add(Math.cos(angle) * distance, vertical, Math.sin(angle) * distance);
         renderer.drawCube(shard.add(-shardSize, -shardSize, -shardSize),
               shard.add(shardSize, shardSize, shardSize), i % 3 == 0 ? solid : glow, false);
      }

      int vortexLines = Math.max(3, Math.min(10, effect.particles / 16));
      for (int i = 0; i < vortexLines; i++) {
         double phase = Math.PI * 2.0 * i / vortexLines + progress * effect.speed;
         Vec3d previous = base;
         for (int step = 1; step <= 4; step++) {
            double angle = phase + step * 0.7;
            double distance = effect.radius * (0.15F + 0.25F * step) * (1.0F - progress * 0.4F);
            Vec3d next = base.add(Math.cos(angle) * distance,
                  effect.height * step / 4.0F, Math.sin(angle) * distance);
            renderer.drawLine(previous, next, Math.max(0.4, effect.width * 0.5), glow, false);
            previous = next;
         }
      }
   }

   private static void drawRing(WorldRenderer renderer, Vec3d center, float radius, int segments, int color) {
      drawRing(renderer, center, radius, segments, color, false, false);
   }

   private static void drawRing(WorldRenderer renderer, Vec3d center, float radius, int segments, int color,
         boolean verticalX) {
      drawRing(renderer, center, radius, segments, color, verticalX, false);
   }

   private static void drawRing(WorldRenderer renderer, Vec3d center, float radius, int segments, int color,
         boolean verticalX, boolean verticalZ) {
      for (int i = 0; i < segments; i++) {
         double a0 = Math.PI * 2.0 * i / segments;
         double a1 = Math.PI * 2.0 * (i + 1) / segments;
         Vec3d from;
         Vec3d to;
         if (verticalX) {
            from = center.add(Math.cos(a0) * radius, Math.sin(a0) * radius, 0.0);
            to = center.add(Math.cos(a1) * radius, Math.sin(a1) * radius, 0.0);
         } else if (verticalZ) {
            from = center.add(0.0, Math.sin(a0) * radius, Math.cos(a0) * radius);
            to = center.add(0.0, Math.sin(a1) * radius, Math.cos(a1) * radius);
         } else {
            from = center.add(Math.cos(a0) * radius, 0.0, Math.sin(a0) * radius);
            to = center.add(Math.cos(a1) * radius, 0.0, Math.sin(a1) * radius);
         }
         renderer.drawLine(from, to, 0.8, color, false);
      }
   }

   private static double hash(int value) {
      double x = Math.sin(value * 12.9898) * 43758.5453;
      return x - Math.floor(x);
   }

   private record Structured(Vec3d pos, int color, long durationMs, long startedAt, float height, float width,
                             float radius, float speed, int particles, int seed, boolean soul) {
      private Structured(Vec3d pos, int color, long durationMs, float height, float width, float radius,
                         float speed, int particles, int seed, boolean soul) {
         this(pos, color, durationMs, System.currentTimeMillis(), height, width, radius, speed,
               particles, seed, soul);
      }
   }
}
