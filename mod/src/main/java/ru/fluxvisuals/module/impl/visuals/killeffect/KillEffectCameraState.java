package ru.fluxvisuals.module.impl.visuals.killeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;

/** Кратковременное состояние камеры для зума и тряски в режиме Scan. */
@Environment(EnvType.CLIENT)
public final class KillEffectCameraState {
   private static long startedAt;
   private static long durationMs;
   private static boolean zoom;
   private static boolean shake;

   private KillEffectCameraState() {}

   public static void spawn(float durationSeconds, boolean enableZoom, boolean enableShake) {
      if (!enableZoom && !enableShake) {
         return;
      }
      startedAt = System.currentTimeMillis();
      durationMs = Math.max(300L, Math.round(durationSeconds * 1000.0F));
      zoom = enableZoom;
      shake = enableShake;
   }

   public static void tick() {
      if (isExpired()) {
         clear();
      }
   }

   public static float fovOffset() {
      if (!active() || !zoom || isExpired()) {
         return 0.0F;
      }
      float progress = KillEffectEasing.clamp((float) (System.currentTimeMillis() - startedAt) / durationMs);
      float pulse = (float) Math.sin(progress * Math.PI);
      return -9.0F * pulse * (1.0F - progress * 0.35F);
   }

   public static Vec3d shakeOffset() {
      if (!active() || !shake || isExpired()) {
         return Vec3d.ZERO;
      }
      float progress = KillEffectEasing.clamp((float) (System.currentTimeMillis() - startedAt) / durationMs);
      float strength = 0.09F * (1.0F - progress);
      long time = System.currentTimeMillis();
      double x = Math.sin(time * 0.071) * strength;
      double y = Math.sin(time * 0.093 + 1.7) * strength * 0.6;
      double z = Math.cos(time * 0.083 + 0.9) * strength;
      return new Vec3d(x, y, z);
   }

   public static void clear() {
      startedAt = 0L;
      durationMs = 0L;
      zoom = false;
      shake = false;
   }

   private static boolean active() {
      return durationMs > 0L;
   }

   private static boolean isExpired() {
      return !active() || System.currentTimeMillis() - startedAt >= durationMs;
   }
}
