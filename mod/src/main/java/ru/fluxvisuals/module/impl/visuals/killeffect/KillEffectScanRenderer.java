package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Расширяющееся кольцо у места гибели сущности. */
@Environment(EnvType.CLIENT)
public final class KillEffectScanRenderer {
   private static final long DURATION_MS = 850L;
   private static final int SEGMENTS = 48;
   private static final int MAX_WAVES = 32;
   private static final List<Wave> WAVES = new ArrayList<>();

   private KillEffectScanRenderer() {}

   public static void spawn(Vec3d pos, int color, float radius) {
      if (pos == null) {
         return;
      }
      if (WAVES.size() >= MAX_WAVES) {
         WAVES.remove(0);
      }
      WAVES.add(new Wave(pos, color == 0 ? Theme.VISUAL.accent() : color, Math.max(0.25F, radius)));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      WAVES.removeIf(wave -> now - wave.startedAt >= DURATION_MS);
   }

   public static void render(WorldRenderer renderer) {
      if (renderer == null || WAVES.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Wave wave : WAVES) {
         long elapsed = now - wave.startedAt;
         if (elapsed >= DURATION_MS) {
            continue;
         }
         float progress = Math.max(0.0F, (float) elapsed / DURATION_MS);
         float eased = KillEffectEasing.quintOut(progress);
         float radius = wave.radius * (0.12F + eased * 1.3F);
         int alpha = Math.round(200.0F * (1.0F - KillEffectEasing.sineOut(progress)));
         int color = Renderer2D.ColorUtil.replAlpha(wave.color, Math.max(0, Math.min(255, alpha)));
         for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2.0 * i / SEGMENTS;
            double a1 = Math.PI * 2.0 * (i + 1) / SEGMENTS;
            Vec3d from = new Vec3d(wave.pos.x + Math.cos(a0) * radius, wave.pos.y, wave.pos.z + Math.sin(a0) * radius);
            Vec3d to = new Vec3d(wave.pos.x + Math.cos(a1) * radius, wave.pos.y, wave.pos.z + Math.sin(a1) * radius);
            renderer.drawLine(from, to, 1.4, color, false);
         }
      }
   }

   public static void clear() {
      WAVES.clear();
   }

   private record Wave(Vec3d pos, int color, float radius, long startedAt) {
      private Wave(Vec3d pos, int color, float radius) {
         this(pos, color, radius, System.currentTimeMillis());
      }
   }
}
