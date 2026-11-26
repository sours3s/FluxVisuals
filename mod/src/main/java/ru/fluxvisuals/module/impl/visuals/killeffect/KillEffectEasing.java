package ru.fluxvisuals.module.impl.visuals.killeffect;

/** Функции плавности для анимаций эффекта убийства. */
public final class KillEffectEasing {
   private KillEffectEasing() {}

   public static float clamp(float t) {
      if (Float.isNaN(t)) {
         return 0.0F;
      }
      return Math.max(0.0F, Math.min(1.0F, t));
   }

   public static float quintOut(float t) {
      float x = clamp(t);
      float inv = 1.0F - x;
      return 1.0F - inv * inv * inv * inv * inv;
   }

   public static float sineOut(float t) {
      return (float) Math.sin(clamp(t) * Math.PI * 0.5F);
   }

   public static float sineInOut(float t) {
      return (float) (-(Math.cos(clamp(t) * Math.PI) - 1.0) * 0.5);
   }
}
