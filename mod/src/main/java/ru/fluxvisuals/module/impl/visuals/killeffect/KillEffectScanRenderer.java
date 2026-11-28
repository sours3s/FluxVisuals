package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.util.render.world.WorldGeometryEmitter;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/**
 * Сканирующая волна при убийстве. Вместо плоского кольца рисуется расходящаяся
 * сферическая оболочка с квадратными ячейками и скан-линиями. Длительность,
 * палитра и огибающие сохранены.
 */
@Environment(EnvType.CLIENT)
public final class KillEffectScanRenderer {
   private static final long BASE_EFFECT_DURATION_MS = 5200L;
   private static final long DISTORTION_MS = 900L;
   private static final int WAVE_SEGMENTS = 48;
   private static final int SHELL_BANDS = 8;
   private static final int MAX_WAVES = 32;
   private static final int MAX_GRID_CELLS = 28;
   private static final List<Wave> WAVES = new ArrayList<>();

   private KillEffectScanRenderer() {}

   public static void spawn(Vec3d pos, int color, float radius) {
      if (pos == null) {
         return;
      }
      if (WAVES.size() >= MAX_WAVES) {
         WAVES.remove(0);
      }
      WAVES.add(new Wave(pos, color == 0 ? Theme.VISUAL.accent() : color,
            Math.max(0.25F, radius), BASE_EFFECT_DURATION_MS));
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      WAVES.removeIf(wave -> now - wave.startedAt >= wave.durationMs);
   }

   public static void render(WorldRenderer renderer) {
      // render() очищает истёкшие волны даже когда client-tick не вызывается (пауза).
      tick();
      if (renderer == null || WAVES.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Wave wave : WAVES) {
         long elapsed = now - wave.startedAt;
         if (elapsed >= wave.durationMs) {
            continue;
         }
         float progress = KillEffectEasing.clamp((float) elapsed / wave.durationMs);
         renderWave(renderer, wave, progress);
      }
   }

   public static void clear() {
      WAVES.clear();
   }

   private static void renderWave(WorldRenderer renderer, Wave wave, float progress) {
      float eased = KillEffectEasing.quintOut(progress);
      float radius = wave.radius * (0.12F + eased * 1.3F);
      float distortionFade = 1.0F - smoothstep(0.0F,
            (float) DISTORTION_MS / BASE_EFFECT_DURATION_MS, progress);
      float width = Math.max(0.08F, wave.radius * (0.14F - 0.05F * progress)) * distortionFade;
      if (width <= 0.001F) {
         return;
      }
      float pulse = (float) Math.sin(progress * Math.PI);
      float flick = KillEffectEasing.clamp(progress * 1.6F);

      int accent = wave.color;
      int outer = lighten(accent, 0.7F);
      int mid = lighten(accent, 0.45F);
      int inner = darken(accent, 0.18F);
      int scanline = lerpColor(accent, 0xFFFFFFFF, 0.28F);

      WorldGeometryEmitter glow = new WorldGeometryEmitter(renderer, renderer.matrixStack().peek(),
            renderer.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND()));
      renderShell(glow, wave.pos, radius, width, progress, pulse, inner, mid, outer, scanline);
      renderCells(glow, wave.pos, radius, width, flick, progress, inner, mid, outer, scanline);

      WorldGeometryEmitter ring = new WorldGeometryEmitter(renderer, renderer.matrixStack().peek(),
            renderer.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH()));
      float ringAlpha = 190.0F * (1.0F - KillEffectEasing.sineOut(progress));
      float innerRadius = Math.max(0.0F, radius - width);
      float y = (float) wave.pos.y + 0.03F;
      for (int i = 0; i < WAVE_SEGMENTS; i++) {
         double a0 = Math.PI * 2.0 * i / WAVE_SEGMENTS;
         double a1 = Math.PI * 2.0 * (i + 1) / WAVE_SEGMENTS;
         Vec3d p0 = new Vec3d(wave.pos.x + Math.cos(a0) * innerRadius, y,
               wave.pos.z + Math.sin(a0) * innerRadius);
         Vec3d p1 = new Vec3d(wave.pos.x + Math.cos(a1) * innerRadius, y,
               wave.pos.z + Math.sin(a1) * innerRadius);
         Vec3d p2 = new Vec3d(wave.pos.x + Math.cos(a1) * radius, y,
               wave.pos.z + Math.sin(a1) * radius);
         Vec3d p3 = new Vec3d(wave.pos.x + Math.cos(a0) * radius, y,
               wave.pos.z + Math.sin(a0) * radius);
         ring.emitQuad(p0, p1, p2, p3, withAlpha(mid, 0), withAlpha(mid, Math.round(ringAlpha)),
               withAlpha(outer, Math.round(ringAlpha)), withAlpha(outer, 0));
      }
   }

   /** Сферическая оболочка: front-кромка, шлейф и скан-линии. */
   private static void renderShell(WorldGeometryEmitter geometry, Vec3d center, float radius,
         float width, float progress, float pulse, int inner, int mid, int outer, int scanline) {
      float alphaScale = 1.0F - KillEffectEasing.sineOut(progress);
      float innerRadius = Math.max(0.0F, radius - width);
      for (int band = 0; band < SHELL_BANDS; band++) {
         float theta0 = (float) (Math.PI * band / SHELL_BANDS);
         float theta1 = (float) (Math.PI * (band + 1) / SHELL_BANDS);
         for (int segment = 0; segment < WAVE_SEGMENTS; segment++) {
            double phi0 = Math.PI * 2.0 * segment / WAVE_SEGMENTS;
            double phi1 = Math.PI * 2.0 * (segment + 1) / WAVE_SEGMENTS;
            float noise = hash(band * 3.17F, segment * 1.71F);
            float front0 = front(innerRadius, radius, band, WAVE_SEGMENTS, noise);
            float front1 = front(innerRadius, radius, band + 1, WAVE_SEGMENTS, noise);
            int midColor = withAlpha(mid, Math.round(150.0F * front0 * alphaScale));
            int rimColor = withAlpha(outer, Math.round(210.0F * front1 * alphaScale));
            int lineColor = withAlpha(scanline, Math.round(90.0F * pulse * front1));
            Vec3d p0 = sphere(center, innerRadius, theta0, phi0);
            Vec3d p1 = sphere(center, innerRadius, theta0, phi1);
            Vec3d p2 = sphere(center, radius, theta1, phi1);
            Vec3d p3 = sphere(center, radius, theta1, phi0);
            geometry.emitQuad(p0, p1, p2, p3, withAlpha(inner, 0), midColor, rimColor, lineColor);
         }
      }
   }

   /** Квадратные ячейки коррупции на поверхностях мира. */
   private static void renderCells(WorldGeometryEmitter geometry, Vec3d center, float radius,
         float width, float flick, float progress, int inner, int mid, int outer, int scanline) {
      float cellSize = lerp(0.27F, 0.18F, flick);
      float extent = Math.min(radius + width, waveRadiusLimit(center, radius));
      int limit = Math.min(MAX_GRID_CELLS, (int) Math.ceil(extent / cellSize));
      float alphaScale = 1.0F - KillEffectEasing.sineOut(progress);
      float y = (float) center.y + 0.04F;
      for (int ix = -limit; ix <= limit; ix++) {
         for (int iz = -limit; iz <= limit; iz++) {
            float x = (float) (center.x + (ix + 0.5F) * cellSize);
            float z = (float) (center.z + (iz + 0.5F) * cellSize);
            double distance = Math.hypot(x - center.x, z - center.z);
            if (distance < radius - width || distance > radius) {
               continue;
            }
            float diff = KillEffectEasing.clamp((float) (1.0F - (radius - distance) / width));
            float noise = hash(ix, iz);
            float noise2 = hash(ix + (int) (progress * 18.0F), iz + (int) (progress * 11.0F));
            float corruption = Math.max(smoothstep(noise * 0.14F, 0.88F, diff), diff * (0.42F + noise2 * 0.28F));
            int color = lerpColor(inner, mid, corruption);
            color = lerpColor(color, outer, (float) Math.pow(diff, 1.6F));
            float half = cellSize * 0.42F;
            int square = withAlpha(color, Math.round((65.0F + 125.0F * noise2) * diff * alphaScale));
            int line = withAlpha(scanline, Math.round(50.0F * noise2 * diff));
            Vec3d p0 = new Vec3d(x - half, y, z - half);
            Vec3d p1 = new Vec3d(x + half, y, z - half);
            Vec3d p2 = new Vec3d(x + half, y, z + half);
            Vec3d p3 = new Vec3d(x - half, y, z + half);
            geometry.emitQuad(p0, p1, p2, p3, square, square, line, line);
         }
      }
   }

   private static float waveRadiusLimit(Vec3d center, float radius) {
      return radius * 1.35F;
   }

   private static Vec3d sphere(Vec3d center, float radius, float theta, double phi) {
      float sin = (float) Math.sin(theta);
      return new Vec3d(center.x + Math.cos(phi) * sin * radius,
            center.y + Math.cos(theta) * radius,
            center.z + Math.sin(phi) * sin * radius);
   }

   private static float front(float innerRadius, float radius, int band, int bands, float noise) {
      float cell = KillEffectEasing.clamp((float) band / bands);
      float edge = (float) Math.pow(cell, 1.35F);
      return KillEffectEasing.clamp(edge * (0.72F + 0.28F * noise)
            * (1.0F - smoothstep(0.92F, 1.0F, cell)));
   }

   private static float hash(float x, float y) {
      float value = (float) Math.sin(x * 127.1F + y * 311.7F) * 43758.5453F;
      return value - (float) Math.floor(value);
   }

   private static float smoothstep(float edge0, float edge1, float value) {
      float t = KillEffectEasing.clamp((value - edge0) / (edge1 - edge0));
      return t * t * (3.0F - 2.0F * t);
   }

   private static float lerp(float start, float end, float amount) {
      return start + (end - start) * KillEffectEasing.clamp(amount);
   }

   private static int withAlpha(int color, int alpha) {
      return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF);
   }

   private static int lighten(int color, float amount) {
      float weight = KillEffectEasing.clamp(amount);
      int r = Math.round(((color >> 16) & 0xFF) + (255 - ((color >> 16) & 0xFF)) * weight);
      int g = Math.round(((color >> 8) & 0xFF) + (255 - ((color >> 8) & 0xFF)) * weight);
      int b = Math.round((color & 0xFF) + (255 - (color & 0xFF)) * weight);
      return 0xFF000000 | (Math.min(255, r) << 16) | (Math.min(255, g) << 8) | Math.min(255, b);
   }

   private static int darken(int color, float amount) {
      float weight = 1.0F - KillEffectEasing.clamp(amount);
      int r = Math.round(((color >> 16) & 0xFF) * weight);
      int g = Math.round(((color >> 8) & 0xFF) * weight);
      int b = Math.round((color & 0xFF) * weight);
      return 0xFF000000 | (r << 16) | (g << 8) | b;
   }

   private static int lerpColor(int a, int b, float amount) {
      float t = KillEffectEasing.clamp(amount);
      int r = Math.round(((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
      int g = Math.round(((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
      int bl = Math.round((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
      return 0xFF000000 | (Math.min(255, Math.max(0, r)) << 16)
            | (Math.min(255, Math.max(0, g)) << 8) | Math.min(255, Math.max(0, bl));
   }

   private record Wave(Vec3d pos, int color, float radius, long durationMs, long startedAt) {
      private Wave(Vec3d pos, int color, float radius, long durationMs) {
         this(pos, color, radius, durationMs, System.currentTimeMillis());
      }
   }
}
