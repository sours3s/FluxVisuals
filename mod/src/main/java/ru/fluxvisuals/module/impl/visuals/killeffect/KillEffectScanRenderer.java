package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.util.render.postfx.KillEffectScanPass;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/**
 * Scan-эффект: состояние волн и параметры эталонного fullscreen-шейдера.
 * Геометрия не создаётся — эффект рисуется шейдером `frag_effect_scan`.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectScanRenderer {
   private static final long SCAN_DURATION_MS = 5200L;
   private static final int MAX_WAVES = 4;
   private static final List<Wave> WAVES = new ArrayList<>();

   private KillEffectScanRenderer() {}

   public static void spawn(Vec3d pos, int color, float radius) {
      spawn(pos, color, radius, SCAN_DURATION_MS);
   }

   public static void spawn(Vec3d pos, int color, float radius, long durationMs) {
      if (pos == null) {
         return;
      }
      if (WAVES.size() >= MAX_WAVES) {
         WAVES.remove(0);
      }
      WAVES.add(new Wave(pos, color == 0 ? Theme.VISUAL.accent() : color,
            Math.max(0.25F, radius), durationMs));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      WAVES.removeIf(wave -> now - wave.startedAt >= wave.durationMs);
   }

   /** Обновление состояния из EventRender3D: геометрии больше нет, только истечение волн. */
   public static void render(WorldRenderer renderer) {
      tick();
   }

   /** Эталонный fullscreen-pass с чтением depth основного framebuffer. */
   public static void renderDistortion(
         Framebuffer framebuffer,
         Matrix4f positionMatrix,
         Matrix4f projectionMatrix,
         Camera camera
   ) {
      tick();
      if (framebuffer == null || WAVES.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Wave wave : WAVES) {
         long elapsed = now - wave.startedAt;
         if (elapsed >= wave.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / wave.durationMs);
         float radius = wave.radius * (0.12F + KillEffectEasing.sineInOut(progress) * 1.3F);
         float width = Math.max(0.08F, wave.radius * (0.14F - 0.05F * progress));
         float flick = KillEffectEasing.clamp(progress * 1.6F);
         int accent = wave.color;
         KillEffectScanPass.render(framebuffer, positionMatrix, projectionMatrix, camera, wave.pos,
               radius, width, lighten(accent, 0.45F), darken(accent, 0.18F), darken(accent, 0.35F),
               lerpColor(accent, 0xFFFFFFFF, 0.28F), progress, flick);
      }
   }

   public static void clear() {
      WAVES.clear();
      KillEffectScanPass.clear();
   }

   private static int lighten(int color, float amount) {
      float weight = KillEffectEasing.clamp(amount);
      int r = Math.round(((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * weight);
      int g = Math.round(((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * weight);
      int b = Math.round((color & 0xFF) + (255 - (color & 0xFF)) * weight);
      return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
   }

   private static int darken(int color, float amount) {
      float weight = 1.0F - KillEffectEasing.clamp(amount);
      int r = Math.round(((color >> 16) & 0xFF) * weight);
      int g = Math.round(((color >> 8) & 0xFF) * weight);
      int b = Math.round((color & 0xFF) * weight);
      return 0xFF000000 | (r << 16) | (g << 8) | b;
   }

   private static int lerpColor(int a, int b, float amount) {
      float t = KillEffectEasing.clamp(amount);
      int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
      int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
      int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
      return 0xFF000000 | (Math.min(255, Math.max(0, r)) << 16)
            | (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, bl));
   }

   private record Wave(Vec3d pos, int color, float radius, long durationMs, long startedAt) {
      private Wave(Vec3d pos, int color, float radius, long durationMs) {
         this(pos, color, radius, durationMs, System.currentTimeMillis());
      }
   }
}
