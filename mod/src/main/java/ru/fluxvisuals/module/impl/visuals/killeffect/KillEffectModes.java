package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.List;

/** Каноничный список кастомных режимов эффекта и миграция старых значений. */
public final class KillEffectModes {
   public static final List<String> ALL = List.of(
         "Beam", "Sky Beam", "Soul", "Shatter", "Darkness", "Spike Abduction", "Body Particles", "Scan");
   public static final String DEFAULT = "Beam";

   private KillEffectModes() {}

   public static String normalize(String value) {
      if (value != null && ALL.contains(value)) {
         return value;
      }
      // Старые ванильные режимы больше не показываются в GUI.
      if ("Lightning".equals(value) || "Blood".equals(value) || "Explosion".equals(value)
            || "Heart".equals(value) || "Smoke".equals(value)) {
         return DEFAULT;
      }
      return DEFAULT;
   }
}
