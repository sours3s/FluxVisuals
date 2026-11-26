package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.core.Renderer2D;

/** Экранная виньетка, сканирующие шипы и полупрозрачный серый слой. */
@Environment(EnvType.CLIENT)
public final class KillEffectOverlayRenderer {
   private static final int MAX_OVERLAYS = 12;
   private static final int SPIKES = 12;
   private static final List<Overlay> OVERLAYS = new ArrayList<>();

   private KillEffectOverlayRenderer() {}

   public static void spawn(int color, float durationSeconds, boolean desaturate, boolean scan) {
      if (OVERLAYS.size() >= MAX_OVERLAYS) {
         OVERLAYS.remove(0);
      }
      long durationMs = Math.max(300L, Math.round(durationSeconds * 1000.0F));
      OVERLAYS.add(new Overlay(color, durationMs, desaturate, scan));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      OVERLAYS.removeIf(overlay -> now - overlay.startedAt >= overlay.durationMs);
   }

   public static void render(Renderer2D r2, int width, int height) {
      if (r2 == null || OVERLAYS.isEmpty() || width <= 0 || height <= 0) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Overlay overlay : OVERLAYS) {
         long elapsed = now - overlay.startedAt;
         if (elapsed >= overlay.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / overlay.durationMs);
         float fade = 1.0F - KillEffectEasing.sineInOut(progress);

         if (overlay.desaturate) {
            int gray = Renderer2D.ColorUtil.replAlpha(0xFFB8BCC4, Math.round(70.0F * fade));
            r2.rect(0.0F, 0.0F, width, height, gray);
         }

         if (!overlay.scan) {
            continue;
         }

         int edge = Renderer2D.ColorUtil.replAlpha(0xFF05070C, Math.round(150.0F * fade));
         int clear = Renderer2D.ColorUtil.replAlpha(0xFF05070C, 0);
         float band = Math.max(24.0F, Math.min(width, height) * 0.14F);
         r2.verticalGradient(0.0F, 0.0F, width, band, clear, edge);
         r2.verticalGradient(0.0F, height - band, width, band, edge, clear);
         r2.horizontalGradient(0.0F, 0.0F, band, height, clear, edge);
         r2.horizontalGradient(width - band, 0.0F, band, height, edge, clear);

         int spikeColor = Renderer2D.ColorUtil.replAlpha(overlay.color, Math.round(200.0F * fade));
         float centerX = width * 0.5F;
         float centerY = height * 0.5F;
         float radius = Math.min(width, height) * (0.26F + 0.14F * progress);
         float spikeLength = Math.min(width, height) * 0.12F;
         float thickness = Math.max(1.5F, Math.min(width, height) * 0.004F);
         r2.pushTranslation(centerX, centerY);
         for (int i = 0; i < SPIKES; i++) {
            float angle = i * (360.0F / SPIKES) + progress * 12.0F;
            r2.pushRotation(angle);
            r2.rect(radius, -thickness * 0.5F, spikeLength, thickness, spikeColor);
            r2.popRotation();
         }
         r2.popTransform();
      }
   }

   public static void clear() {
      OVERLAYS.clear();
   }

   private record Overlay(int color, long durationMs, long startedAt, boolean desaturate, boolean scan) {
      private Overlay(int color, long durationMs, boolean desaturate, boolean scan) {
         this(color, durationMs, System.currentTimeMillis(), desaturate, scan);
      }
   }
}
