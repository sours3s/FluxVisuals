package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Вертикальный луч и небесный луч в точке гибели сущности. */
@Environment(EnvType.CLIENT)
public final class KillEffectBeamRenderer {
   private static final int MAX_BEAMS = 24;
   private static final int RING_SEGMENTS = 24;
   private static final List<Beam> BEAMS = new ArrayList<>();

   private KillEffectBeamRenderer() {}

   public static void spawn(Vec3d pos, int color, float durationSeconds, float height, float width,
         float radius, float speed, int particles, boolean sky) {
      if (pos == null) {
         return;
      }
      if (BEAMS.size() >= MAX_BEAMS) {
         BEAMS.remove(0);
      }
      long durationMs = Math.max(300L, Math.round(durationSeconds * 1000.0F));
      BEAMS.add(new Beam(pos, color, durationMs, Math.max(0.5F, height), Math.max(0.05F, width),
            Math.max(0.25F, radius), Math.max(0.1F, speed), Math.max(4, Math.min(32, particles / 6)), sky));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      BEAMS.removeIf(beam -> now - beam.startedAt >= beam.durationMs);
   }

   public static void render(WorldRenderer renderer) {
      if (renderer == null || BEAMS.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Beam beam : BEAMS) {
         long elapsed = now - beam.startedAt;
         if (elapsed >= beam.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / beam.durationMs);
         float reveal = KillEffectEasing.quintOut(Math.min(1.0F, progress * 2.5F));
         int alpha = Math.round(230.0F * (1.0F - progress * progress));
         int solid = Renderer2D.ColorUtil.replAlpha(beam.color, alpha);
         int glow = Renderer2D.ColorUtil.replAlpha(beam.color, Math.max(0, alpha / 4));

         float currentHeight = beam.height * reveal;
         float currentWidth = beam.width * (1.0F + progress * 0.4F);
         Vec3d base = beam.pos.add(0.0, 0.15, 0.0);
         Vec3d top = base.add(0.0, currentHeight, 0.0);

         // Светящийся столб и более яркое ядро.
         renderer.drawCube(base.add(-currentWidth, 0.0, -currentWidth),
               top.add(currentWidth, 0.0, currentWidth), glow, false);
         renderer.drawLine(base, top, currentWidth * 1.6, solid, false);

         if (beam.sky) {
            // Небесный луч приходит сверху и распадается на тонкие лучи у цели.
            Vec3d skyTop = base.add(0.0, beam.height + 36.0F, 0.0);
            renderer.drawLine(skyTop, top, currentWidth * 0.8, glow, false);
            for (int i = 0; i < beam.rays; i++) {
               double angle = Math.PI * 2.0 * i / beam.rays + progress * beam.speed;
               Vec3d edge = base.add(Math.cos(angle) * beam.radius * reveal, 0.0,
                     Math.sin(angle) * beam.radius * reveal);
               renderer.drawLine(top, edge, currentWidth * 0.5, solid, false);
            }
         } else {
            // Обычный луч окутывает цель расходящимися нитями.
            for (int i = 0; i < beam.rays; i++) {
               double angle = Math.PI * 2.0 * i / beam.rays + progress * beam.speed;
               Vec3d edge = base.add(Math.cos(angle) * beam.radius * reveal, 0.0,
                     Math.sin(angle) * beam.radius * reveal);
               Vec3d upper = base.add(Math.cos(angle) * currentWidth * 2.0, currentHeight * 0.75F,
                     Math.sin(angle) * currentWidth * 2.0);
               renderer.drawLine(upper, edge, Math.max(0.4, currentWidth), glow, false);
            }
         }

         float ringRadius = beam.radius * (0.2F + KillEffectEasing.quintOut(progress) * 1.2F);
         for (int i = 0; i < RING_SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / RING_SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / RING_SEGMENTS;
            Vec3d from = base.add(Math.cos(a0) * ringRadius, 0.0, Math.sin(a0) * ringRadius);
            Vec3d to = base.add(Math.cos(a1) * ringRadius, 0.0, Math.sin(a1) * ringRadius);
            renderer.drawLine(from, to, Math.max(0.5, currentWidth), solid, false);
         }
      }
   }

   public static void clear() {
      BEAMS.clear();
   }

   private record Beam(Vec3d pos, int color, long durationMs, long startedAt, float height, float width,
                       float radius, float speed, int rays, boolean sky) {
      private Beam(Vec3d pos, int color, long durationMs, float height, float width, float radius,
                   float speed, int rays, boolean sky) {
         this(pos, color, durationMs, System.currentTimeMillis(), height, width, radius, speed, rays, sky);
      }
   }
}
