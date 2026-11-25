package ru.fluxvisuals.util.render.shader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShaderFogQualityTest {
   @Test
   void rendersSkyAtOneThirdResolution() {
      assertEquals(640, ShaderFogQuality.internalSize(1920));
      assertEquals(360, ShaderFogQuality.internalSize(1080));
      assertEquals(1, ShaderFogQuality.internalSize(1));
   }

   @Test
   void keepsAspectAndCutsFragmentWorkByAtLeastEightTimes() {
      int width = 2560;
      int height = 1440;
      int reducedWidth = ShaderFogQuality.internalSize(width);
      int reducedHeight = ShaderFogQuality.internalSize(height);

      assertTrue(Math.abs((float) reducedWidth / reducedHeight - (float) width / height) < 0.01F);
      assertTrue((long) width * height >= (long) reducedWidth * reducedHeight * 8L);
   }
}
