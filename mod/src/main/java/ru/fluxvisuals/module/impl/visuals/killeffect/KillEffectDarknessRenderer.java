package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Затемнение области и похищение цели шипами. */
@Environment(EnvType.CLIENT)
public final class KillEffectDarknessRenderer {
   private static final int MAX_EFFECTS = 20;
   private static final int RING_SEGMENTS = 20;
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
      long durationMs = Math.max(300L, Math.round(durationSeconds * 1000.0F));
      EFFECTS.add(new Darkness(pos, color, durationMs, Math.max(0.5F, height), Math.max(0.05F, width),
            Math.max(0.25F, radius), Math.max(0.1F, speed), Math.max(4, Math.min(64, particles / 4)), spikes));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      EFFECTS.removeIf(effect -> now - effect.startedAt >= effect.durationMs);
   }

   public static void render(WorldRenderer renderer) {
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
         float reveal = KillEffectEasing.sineInOut(Math.min(1.0F, progress * 1.8F));
         int outline = Renderer2D.ColorUtil.replAlpha(effect.color,
               Math.round(180.0F * (1.0F - progress * progress)));
         Vec3d base = effect.pos.add(0.0, 0.15, 0.0);
         if (effect.spikeMode) {
            renderSpikes(renderer, effect, base, progress, reveal, outline);
         } else {
            renderDarkness(renderer, effect, base, reveal, outline);
         }
      }
   }

   public static void clear() {
      EFFECTS.clear();
   }

   private static void renderDarkness(WorldRenderer renderer, Darkness effect, Vec3d base,
         float reveal, int outline) {
      float currentRadius = effect.radius * (0.35F + reveal * 0.9F);

      // Затемнение делается экранным оверлеем: доступный world-слой кубов аддитивный,
      // поэтому тёмный куб только подсвечивал бы сцену вместо затемнения.
      for (int layer = 0; layer < 3; layer++) {
         float layerRadius = currentRadius * (0.5F + layer * 0.25F);
         float y = effect.height * (layer + 1) / 4.0F;
         drawRing(renderer, base.add(0.0, y, 0.0), layerRadius, RING_SEGMENTS, outline);
      }
   }

   private static void renderSpikes(WorldRenderer renderer, Darkness effect, Vec3d base, float progress,
         float reveal, int outline) {
      float currentHeight = effect.height * reveal;
      renderer.drawLine(base, base.add(0.0, currentHeight, 0.0), Math.max(0.6, effect.width), outline, false);

      for (int i = 0; i < effect.spikes; i++) {
         double angle = Math.PI * 2.0 * i / effect.spikes + progress * effect.speed;
         double distance = effect.radius * (0.35 + 0.65 * KillEffectEasing.quintOut(progress));
         Vec3d ground = base.add(Math.cos(angle) * distance, 0.0, Math.sin(angle) * distance);
         float spikeHeight = currentHeight * (0.55F + 0.45F * (i % 3) / 2.0F);
         Vec3d tip = ground.add(0.0, spikeHeight, 0.0);
         renderer.drawLine(ground, tip, Math.max(0.5, effect.width), outline, false);
         float tipSize = Math.max(0.05F, effect.width * 0.5F);
         renderer.drawCube(tip.add(-tipSize, -tipSize, -tipSize),
               tip.add(tipSize, tipSize, tipSize), outline, false);
      }

      float ringRadius = effect.radius * (0.25F + KillEffectEasing.quintOut(progress) * 1.1F);
      drawRing(renderer, base, ringRadius, RING_SEGMENTS, outline);
   }

   private static void drawRing(WorldRenderer renderer, Vec3d center, float radius, int segments, int color) {
      for (int i = 0; i < segments; i++) {
         double a0 = Math.PI * 2.0 * i / segments;
         double a1 = Math.PI * 2.0 * (i + 1) / segments;
         Vec3d from = center.add(Math.cos(a0) * radius, 0.0, Math.sin(a0) * radius);
         Vec3d to = center.add(Math.cos(a1) * radius, 0.0, Math.sin(a1) * radius);
         renderer.drawLine(from, to, 0.8, color, false);
      }
   }

   private record Darkness(Vec3d pos, int color, long durationMs, long startedAt, float height, float width,
                           float radius, float speed, int spikes, boolean spikeMode) {
      private Darkness(Vec3d pos, int color, long durationMs, float height, float width, float radius,
                       float speed, int spikes, boolean spikeMode) {
         this(pos, color, durationMs, System.currentTimeMillis(), height, width, radius, speed, spikes, spikeMode);
      }
   }
}
