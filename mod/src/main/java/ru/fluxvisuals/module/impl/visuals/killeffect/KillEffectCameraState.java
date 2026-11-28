package ru.fluxvisuals.module.impl.visuals.killeffect;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Камера эталонной реализации: 650 мс, зум множителем FOV и поворот по Z.
 * Источник: эталонная реализация Kill Effect, зум и поворот камеры.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectCameraState {
   private static final long CAMERA_MS = 650L;
   private static long startedAt;
   private static boolean zoom;
   private static boolean shake;

   private KillEffectCameraState() {}

   public static void spawn(boolean enableZoom, boolean enableShake) {
      if (!enableZoom && !enableShake) {
         return;
      }
      startedAt = System.currentTimeMillis();
      zoom = enableZoom;
      shake = enableShake;
   }

   public static void tick() {
      if (progress() >= 1.0F) {
         clear();
      }
   }

   public static float fovScale() {
      if (!zoom || progress() >= 1.0F) {
         return 1.0F;
      }
      float envelope = (float) Math.sin(progress() * Math.PI);
      return Math.max(0.5F, Math.min(1.0F, 1.0F - 15.0F * envelope / 70.0F));
   }

   public static float shakeDegrees() {
      if (!shake || progress() >= 1.0F) {
         return 0.0F;
      }
      float progress = progress();
      float seconds = (float) (System.currentTimeMillis() - startedAt) / 1000.0F;
      float wave = (float) (Math.sin(seconds * 26.0) * 0.65 + Math.sin(seconds * 16.5) * 0.35);
      return wave * 0.8F * 2.5F * (1.0F - progress) * (1.0F - progress);
   }

   public static void clear() {
      startedAt = 0L;
      zoom = false;
      shake = false;
   }

   private static float progress() {
      if (startedAt <= 0L) {
         return 1.0F;
      }
      return KillEffectEasing.clamp((float) (System.currentTimeMillis() - startedAt) / CAMERA_MS);
   }
}
