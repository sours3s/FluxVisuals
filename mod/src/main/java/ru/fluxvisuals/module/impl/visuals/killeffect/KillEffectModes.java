package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.List;

/** Каноничный список режимов эффекта и миграция старых значений. */
public final class KillEffectModes {
   public static final List<String> ALL = List.of("Lightning", "Blood", "Explosion", "Heart", "Smoke");
   public static final String DEFAULT = "Lightning";

   private KillEffectModes() {}

   public static String normalize(String value) {
      if (value != null && ALL.contains(value)) {
         return value;
      }
      if ("Soul".equals(value)) {
         return "Heart";
      }
      if ("Shatter".equals(value)) {
         return "Explosion";
      }
      return DEFAULT;
   }
}
