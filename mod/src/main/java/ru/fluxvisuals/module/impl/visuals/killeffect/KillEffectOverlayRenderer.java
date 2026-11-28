package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.texture.TextureLoader;

/**
 * Экранные эффекты эталонной реализации: виньетка с текстурой 600 мс и
 * затухание насыщенности 1950/260/760 мс. Источник: эталонная реализация Kill Effect.
 * Самодельные шипы, кольца и градиенты удалены.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectOverlayRenderer {
   private static final int MAX_OVERLAYS = 12;
   private static final long BASE_EFFECT_DURATION_MS = 5200L;
   private static final long VIGNETTE_MS = 600L;
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
      OVERLAYS.add(new Overlay(color, desaturate, scan));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      OVERLAYS.removeIf(overlay -> now - overlay.startedAt >= overlay.durationMs);
   }

   public static void render(Renderer2D r2, int width, int height) {
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
         if (overlay.desaturate) {
            float saturation = envelope(elapsed, SATURATION_MS, SAT_RAMP_MS, SAT_FADE_MS);
            int tint = overlay.scan ? 0xFFB8BCC4 : 0xFF10131A;
            int alpha = Math.round((overlay.scan ? 70 : 75) * saturation);
            r2.rect(0.0F, 0.0F, width, height, withAlpha(tint, alpha));
         }
         if (overlay.scan && elapsed < VIGNETTE_MS) {
            renderVignette(r2, width, height, overlay.color, elapsed);
         }
      }
   }

   public static void clear() {
      OVERLAYS.clear();
   }

   /** Эталонная виньетка: sin(pi * t), quintOut, spread = 0.75 * side * (1 - quintOut). */
   private static void renderVignette(Renderer2D r2, int width, int height, int color, long elapsed) {
      float progress = KillEffectEasing.clamp((float) elapsed / VIGNETTE_MS);
      if (progress <= 0.0F || progress >= 1.0F) {
         return;
      }
      int texture = vignetteTexture();
      if (texture <= 0) {
         return;
      }
      float pulse = (float) Math.sin(progress * Math.PI);
      float fade = 1.0F - KillEffectEasing.quintOut(pulse);
      float spreadX = width * 0.75F * fade;
      float spreadY = height * 0.75F * fade;
      int tint = withAlpha(lerpColor(lighten(color, 0.22F), 0xFFFFFF, progress),
            Math.round(150.0F * pulse));
      r2.drawRgbaTexture(texture, -spreadX, -spreadY,
            width + spreadX * 2.0F, height + spreadY * 2.0F, tint);
   }

   /** Вход, удержание и затухание с огибающей эталона. */
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
         return 1.0F - KillEffectEasing.sineInOut((float) (elapsed - fadeStart) / fade);
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
      private Overlay(int color, boolean desaturate, boolean scan) {
         this(color, BASE_EFFECT_DURATION_MS, System.currentTimeMillis(), desaturate, scan);
      }
   }
}
