package ru.fluxvisuals.module.impl.visuals.killeffect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KillEffectModesTest {
   @Test
   void allCustomModesAreAvailable() {
      assertEquals(8, KillEffectModes.ALL.size());
      assertTrue(KillEffectModes.ALL.contains("Beam"));
      assertTrue(KillEffectModes.ALL.contains("Sky Beam"));
      assertTrue(KillEffectModes.ALL.contains("Soul"));
      assertTrue(KillEffectModes.ALL.contains("Shatter"));
      assertTrue(KillEffectModes.ALL.contains("Darkness"));
      assertTrue(KillEffectModes.ALL.contains("Spike Abduction"));
      assertTrue(KillEffectModes.ALL.contains("Body Particles"));
      assertTrue(KillEffectModes.ALL.contains("Scan"));
   }

   @Test
   void soulAndShatterArePreserved() {
      assertEquals("Soul", KillEffectModes.normalize("Soul"));
      assertEquals("Shatter", KillEffectModes.normalize("Shatter"));
   }

   @Test
   void legacyVanillaModesMigrateToBeam() {
      for (String legacy : new String[]{"Lightning", "Blood", "Explosion", "Heart", "Smoke"}) {
         assertEquals(KillEffectModes.DEFAULT, KillEffectModes.normalize(legacy));
      }
   }

   @Test
   void unknownModeFallsBackToDefault() {
      assertEquals(KillEffectModes.DEFAULT, KillEffectModes.normalize(null));
      assertEquals(KillEffectModes.DEFAULT, KillEffectModes.normalize("Unknown"));
   }
}
