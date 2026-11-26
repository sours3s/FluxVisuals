package ru.fluxvisuals.module.impl.visuals.killeffect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KillEffectModesTest {
   @Test
   void allFiveModesAreAvailable() {
      assertEquals(5, KillEffectModes.ALL.size());
      assertTrue(KillEffectModes.ALL.contains("Lightning"));
      assertTrue(KillEffectModes.ALL.contains("Blood"));
      assertTrue(KillEffectModes.ALL.contains("Explosion"));
      assertTrue(KillEffectModes.ALL.contains("Heart"));
      assertTrue(KillEffectModes.ALL.contains("Smoke"));
   }

   @Test
   void legacyModesMigrateToNewOnes() {
      assertEquals("Heart", KillEffectModes.normalize("Soul"));
      assertEquals("Explosion", KillEffectModes.normalize("Shatter"));
   }

   @Test
   void unknownModeFallsBackToDefault() {
      assertEquals(KillEffectModes.DEFAULT, KillEffectModes.normalize(null));
      assertEquals(KillEffectModes.DEFAULT, KillEffectModes.normalize("Unknown"));
   }
}
