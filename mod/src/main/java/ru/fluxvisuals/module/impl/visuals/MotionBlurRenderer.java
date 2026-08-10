package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.util.render.core.Renderer2D;

/**
 * Motion Blur renderer — рисует направленный оверлей размытия при движении камеры.
 * Использует angular velocity (дельту yaw/pitch между кадрами) + скорость перемещения.
 */
@Environment(EnvType.CLIENT)
public class MotionBlurRenderer {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static MotionBlurRenderer INSTANCE;
   private float prevYaw = 0;
   private float prevPitch = 0;

   public MotionBlurRenderer() {
      INSTANCE = this;
   }

   public static MotionBlurRenderer getInstance() { return INSTANCE; }

   @EventInit
   public void onRender(EventScreen e) {
      MotionBlur module = null;
      if (ru.fluxvisuals.client.FluxVisualsClient.get != null
            && ru.fluxvisuals.client.FluxVisualsClient.get.manager != null) {
         module = ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(MotionBlur.class);
      }
      if (module == null || !module.enable) return;
      if (mc.player == null || mc.world == null) return;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      float blurAmount = module.getBlurAmount();
      if (blurAmount < 0.01F) return;

      int w = mc.getWindow().getScaledWidth();
      int h = mc.getWindow().getScaledHeight();
      float cx = w / 2.0F;
      float cy = h / 2.0F;

      // Direction of movement
      float deltaYaw = mc.player.getYaw() - prevYaw;
      float deltaPitch = mc.player.getPitch() - prevPitch;
      prevYaw = mc.player.getYaw();
      prevPitch = mc.player.getPitch();

      // Velocity-based overlay: semi-transparent gradient radiating from center
      int baseAlpha = (int) (60 * blurAmount);
      int color = (baseAlpha << 24) | 0x000000; // Dark overlay

      // Draw multiple concentric circles with decreasing alpha for smooth falloff
      float maxRadius = Math.max(w, h) * 0.6F;
      int steps = 4;
      for (int i = steps; i >= 1; i--) {
         float radius = maxRadius * (i / (float) steps);
         int stepAlpha = baseAlpha * i / steps;
         int stepColor = (stepAlpha << 24) | 0x000000;
         r2.circle(cx, cy, radius, 0, 1.0F, stepColor);
      }

      // Directional streak overlay based on yaw delta
      if (Math.abs(deltaYaw) > 0.5F) {
         float streakLen = Math.min(200F, Math.abs(deltaYaw) * 15F);
         float streakW = h * 0.8F;
         float streakAlpha = Math.min(40F, Math.abs(deltaYaw) * 3F) * blurAmount;
         int streakColor = ((int) streakAlpha << 24) | 0x000000;

         if (deltaYaw > 0) {
            // Moving right — streak on left edge
            r2.rect(0, cy - streakW / 2, streakLen, streakW, 0, streakColor);
         } else {
            // Moving left — streak on right edge
            r2.rect(w - streakLen, cy - streakW / 2, streakLen, streakW, 0, streakColor);
         }
      }

      // Vertical streak based on pitch delta
      if (Math.abs(deltaPitch) > 0.5F) {
         float streakH = Math.min(150F, Math.abs(deltaPitch) * 12F);
         float streakW2 = w * 0.8F;
         float streakAlpha2 = Math.min(35F, Math.abs(deltaPitch) * 2.5F) * blurAmount;
         int streakColor2 = ((int) streakAlpha2 << 24) | 0x000000;

         if (deltaPitch > 0) {
            r2.rect(cx - streakW2 / 2, 0, streakW2, streakH, 0, streakColor2);
         } else {
            r2.rect(cx - streakW2 / 2, h - streakH, streakW2, streakH, 0, streakColor2);
         }
      }
   }
}
