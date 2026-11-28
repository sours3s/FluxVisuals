package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.texture.TextureLoader;

/**
 * Экранные эффекты убийства: виньетка TEXTURE + шипы, десатурация с огибающей
 * 260/760 мс и расходящаяся скан-волна. Параметры и фазы взяты из эталонной
 * реализации; framebuffer-pass искажения заменён эквивалентным overlay-слоем.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectOverlayRenderer {
   private static final int MAX_OVERLAYS = 12;
   private static final int SPIKES = 12;
   private static final long BASE_EFFECT_DURATION_MS = 5200L;
   private static final long VIGNETTE_MS = 600L;
   private static final long CAMERA_MS = 650L;
   private static final long DISTORTION_MS = 900L;
   private static final long SATURATION_MS = 1950L;
   private static final long SAT_RAMP_MS = 260L;
   private static final long SAT_FADE_MS = 760L;
   private static final String VIGNETTE_PATH =
         "assets/fluxvisuals/textures/effects/frag/lightarroundscreen_alpha.png";
   private static final List<Overlay> OVERLAYS = new ArrayList<>();
   private static int vignetteTexture = -2;

   private KillEffectOverlayRenderer() {}

   public static void spawn(int color, float durationSeconds, boolean desaturate, boolean scan) {
      if (OVERLAYS.size() >= MAX_OVERLAYS) {
         OVERLAYS.remove(0);
      }
      long durationMs = Math.min(BASE_EFFECT_DURATION_MS,
            Math.max(300L, Math.round(durationSeconds * 1000.0F)));
      OVERLAYS.add(new Overlay(color, durationMs, desaturate, scan));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      OVERLAYS.removeIf(overlay -> now - overlay.startedAt >= overlay.durationMs);
   }

   public static void render(Renderer2D r2, int width, int height) {
      // Пауза останавливает client-tick, поэтому очистка выполняется и здесь.
      tick();
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
         if (overlay.desaturate) {
            float saturation = envelope(elapsed, overlay.durationMs, SAT_RAMP_MS, SAT_FADE_MS);
            int tint = overlay.scan ? 0xFFB8BCC4 : 0xFF10131A;
            int alpha = Math.round((overlay.scan ? 70 : 75) * saturation);
            r2.rect(0.0F, 0.0F, width, height, withAlpha(tint, alpha));
         }
         if (!overlay.scan) {
            continue;
         }
         renderScan(r2, width, height, overlay, elapsed, progress);
      }
   }

   public static void clear() {
      OVERLAYS.clear();
   }

   private static void renderScan(Renderer2D r2, int width, int height, Overlay overlay,
         long elapsed, float progress) {
      float vignetteProgress = KillEffectEasing.clamp((float) elapsed / VIGNETTE_MS);
      float pulse = (float) Math.sin(vignetteProgress * Math.PI);
      float eased = KillEffectEasing.quintOut(pulse);
      float fade = 1.0F - eased;
      float spreadX = width * 0.75F * fade;
      float spreadY = height * 0.75F * fade;

      // Эталонная виньетка: текстура расширяется за экран и тает по sin(pi * t).
      if (elapsed < VIGNETTE_MS) {
         int texture = vignetteTexture();
         if (texture > 0) {
            int tint = withAlpha(lerpColor(lighten(overlay.color, 0.22F), 0xFFFFFF,
                  vignetteProgress), Math.round(150.0F * pulse));
            r2.drawRgbaTexture(texture, -spreadX, -spreadY,
                  width + spreadX * 2.0F, height + spreadY * 2.0F, tint);
         }
      }

      int edge = withAlpha(0xFF05070C, Math.round(150.0F * fade));
      int clear = withAlpha(0xFF05070C, 0);
      float band = Math.max(24.0F, Math.min(width, height) * 0.14F);
      r2.verticalGradient(0.0F, 0.0F, width, band, clear, edge);
      r2.verticalGradient(0.0F, height - band, width, band, edge, clear);
      r2.horizontalGradient(0.0F, 0.0F, band, height, clear, edge);
      r2.horizontalGradient(width - band, 0.0F, band, height, edge, clear);

      if (elapsed < CAMERA_MS) {
         drawInvadingSpikes(r2, width, height, overlay, vignetteProgress, pulse);
      }
      drawScanRing(r2, width, height, overlay, elapsed, progress);
   }

   /** Шипы врываются от краёв экрана к центру на пике виньетки. */
   private static void drawInvadingSpikes(Renderer2D r2, int width, int height, Overlay overlay,
         float progress, float pulse) {
      float minSide = Math.min(width, height);
      float radius = minSide * (0.62F - 0.34F * KillEffectEasing.quintOut(progress));
      float length = minSide * (0.09F + 0.08F * pulse);
      float thickness = Math.max(1.5F, minSide * 0.004F);
      int color = withAlpha(lerpColor(lighten(overlay.color, 0.45F), 0xFFFFFFFF, 0.18F),
            Math.round(210.0F * pulse));
      r2.pushTranslation(width * 0.5F, height * 0.5F);
      for (int i = 0; i < SPIKES; i++) {
         float angle = i * (360.0F / SPIKES) + progress * 12.0F;
         r2.pushRotation(angle);
         r2.rect(radius, -thickness * 0.5F, length, thickness, color);
         r2.popRotation();
      }
      r2.popTransform();
   }

   /**
    * Искажение представлено экранным кольцом с теми же 900 мс и синусоидальной
    * огибающей: полный framebuffer-pass в текущем overlay-хуке недоступен.
    */
   private static void drawScanRing(Renderer2D r2, int width, int height, Overlay overlay,
         long elapsed, float progress) {
      float distortion = envelope(elapsed, DISTORTION_MS, 120L, 260L);
      if (distortion <= 0.0F) {
         return;
      }
      float minSide = Math.min(width, height);
      float radius = minSide * (0.08F + 0.52F * KillEffectEasing.quintOut(progress));
      float cx = width * 0.5F;
      float cy = height * 0.5F;
      int mid = lighten(overlay.color, 0.45F);
      int outer = lighten(overlay.color, 0.7F);
      r2.circle(cx, cy, radius, 0.0F, 1.0F,
            withAlpha(mid, Math.round(160.0F * distortion)));
      r2.circle(cx, cy, radius * 0.88F, 0.0F, 1.0F,
            withAlpha(outer, Math.round(90.0F * distortion)));
      int line = withAlpha(outer, Math.round(42.0F * distortion));
      for (int i = 0; i < 18; i++) {
         float y = cy - radius + i * (radius / 9.0F);
         r2.rect(cx - radius, y, radius * 2.0F, 1.5F, line);
      }
   }

   /** Вход, удержание и затухание с общей огибающей. */
   private static float envelope(long elapsed, long durationMs, long rampMs, long fadeMs) {
      if (elapsed <= 0L || durationMs <= 0L) {
         return 0.0F;
      }
      long ramp = Math.max(1L, Math.min(rampMs, durationMs));
      long fade = Math.max(1L, Math.min(fadeMs, durationMs - ramp));
      long fadeStart = Math.max(ramp, durationMs - fade);
      if (elapsed < ramp) {
         return KillEffectEasing.sineOut((float) elapsed / ramp);
      }
      if (elapsed >= fadeStart) {
         return 1.0F - KillEffectEasing.sineInOut(
               (float) (elapsed - fadeStart) / fade);
      }
      return 1.0F;
   }

   private static int vignetteTexture() {
      if (vignetteTexture == -2) {
         vignetteTexture = TextureLoader.load(VIGNETTE_PATH);
      }
      return vignetteTexture;
   }

   /** Renderer2D ожидает premultiplied alpha. */
   private static int withAlpha(int color, int alpha) {
      int a = Math.max(0, Math.min(255, alpha));
      int r = ((color >> 16) & 0xFF) * a / 255;
      int g = ((color >> 8) & 0xFF) * a / 255;
      int b = (color & 0xFF) * a / 255;
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   private static int lighten(int color, float amount) {
      float weight = KillEffectEasing.clamp(amount);
      int r = Math.round(((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * weight);
      int g = Math.round(((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * weight);
      int b = Math.round((color & 0xFF) + (255 - (color & 0xFF)) * weight);
      return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
   }

   private static int lerpColor(int a, int b, float amount) {
      float t = KillEffectEasing.clamp(amount);
      int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
      int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
      int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
      return 0xFF000000 | (Math.min(255, Math.max(0, r)) << 16)
            | (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, bl));
   }

   private record Overlay(int color, long durationMs, long startedAt, boolean desaturate, boolean scan) {
      private Overlay(int color, long durationMs, boolean desaturate, boolean scan) {
         this(color, durationMs, System.currentTimeMillis(), desaturate, scan);
      }
   }
}
