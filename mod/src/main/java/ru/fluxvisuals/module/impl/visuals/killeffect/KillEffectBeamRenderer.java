package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import ru.fluxvisuals.util.render.world.WorldGeometryEmitter;
import ru.fluxvisuals.util.render.world.WorldRenderer;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/**
 * Прямой порт двух эталонных реализаций:
 *  - Beam: auravisual KillEffect$LaserEffect (40 срезов, 3 glow на срез, 10 нижних слоёв);
 *  - Sky Beam: alphadcp200 KillEffect (цилиндр 64 блока, 24 сегмента, ground ring).
 */
@Environment(EnvType.CLIENT)
public final class KillEffectBeamRenderer {
   private static final int MAX_BEAMS = 24;
   private static final long LASER_DURATION_MS = 1500L;
   private static final long SKY_DURATION_MS = 1800L;
   private static final int LASER_SLICES = 40;
   private static final int BOTTOM_LAYERS = 10;
   private static final float LASER_HEIGHT = 4.0F;
   private static final float LASER_MAX_RADIUS = 1.0F;
   private static final float BEAM_HEIGHT = 64.0F;
   private static final int BEAM_SEGMENTS = 24;
   private static final List<Beam> BEAMS = new ArrayList<>();

   private KillEffectBeamRenderer() {}

   public static void spawn(Vec3d pos, int color, float durationSeconds, float height, float width,
         float radius, float speed, int particles, boolean sky) {
      if (pos == null) {
         return;
      }
      if (BEAMS.size() >= MAX_BEAMS) {
         BEAMS.remove(0);
      }
      BEAMS.add(new Beam(pos, color, sky ? SKY_DURATION_MS : LASER_DURATION_MS, sky));
      if (sky) {
         spawnSkyParticles(pos);
      }
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      BEAMS.removeIf(beam -> now - beam.startedAt >= beam.durationMs);
   }

   public static void render(WorldRenderer renderer) {
      tick();
      if (renderer == null || BEAMS.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      VertexConsumer glowBuffer = KillEffectGlow.buffer(renderer);

      for (Beam beam : BEAMS) {
         long elapsed = now - beam.startedAt;
         if (elapsed >= beam.durationMs) {
            continue;
         }
         float progress = (float) elapsed / beam.durationMs;
         if (beam.sky) {
            renderSkyBeam(renderer, beam, progress);
         } else {
            renderLaser(renderer, glowBuffer, beam, progress);
         }
      }
   }

   public static void clear() {
      BEAMS.clear();
   }

   /** auravisual: 40 срезов по 3 glow-квада и 10 нижних разворачивающихся слоёв. */
   private static void renderLaser(WorldRenderer renderer, VertexConsumer glow, Beam beam, float progress) {
      int r = (beam.color >> 16) & 0xFF;
      int g = (beam.color >> 8) & 0xFF;
      int b = beam.color & 0xFF;
      float globalAlpha = progress < 0.15F
            ? progress / 0.15F
            : (progress > 0.75F ? (1.0F - progress) / 0.25F : 1.0F);

      for (int i = 0; i < LASER_SLICES; i++) {
         float t = (float) i / LASER_SLICES;
         float y = t * LASER_HEIGHT;
         float radius = LASER_MAX_RADIUS * (float) Math.sin(Math.PI * t);
         float sliceAlpha = (1.0F - Math.abs(2.0F * t - 1.0F) * 0.25F) * globalAlpha;
         Vec3d pos = beam.pos.add(0.0, y, 0.0);
         KillEffectGlow.appendGlow(renderer, glow, pos, radius * 2.1F, r, g, b, sliceAlpha * 0.22F);
         KillEffectGlow.appendGlow(renderer, glow, pos, radius * 1.15F, r, g, b, sliceAlpha * 0.48F);
         KillEffectGlow.appendGlow(renderer, glow, pos, radius * 0.55F, r, g, b, sliceAlpha * 0.85F);
      }

      for (int i = 0; i < BOTTOM_LAYERS; i++) {
         float t = (float) i / BOTTOM_LAYERS;
         float spread = 1.0F - t;
         float bottomRadius = 3.6F * spread;
         float bottomAlpha = spread * spread * globalAlpha * 0.38F;
         Vec3d pos = beam.pos.add(0.0, t * 0.45F, 0.0);
         KillEffectGlow.appendGlow(renderer, glow, pos, bottomRadius, r, g, b, bottomAlpha);
         KillEffectGlow.appendGlow(renderer, glow, pos, bottomRadius * 0.35F,
               r, g, b, bottomAlpha * 1.7F);
      }
   }

   /** alphadcp200: цилиндры и кольцо с теми же параметрами и длительностью. */
   private static void renderSkyBeam(WorldRenderer renderer, Beam beam, float progress) {
      float appear = Math.min(1.0F, progress / 0.08F);
      float fade = 1.0F - smoothstep(0.62F, 1.0F, progress);
      float radius = (1.28F * (1.0F - progress) + 0.035F) * appear;
      if (radius <= 0.0F || fade <= 0.0F) {
         return;
      }
      RenderLayer layer = WorldRenderLayers.POSITION_COLOR_QUADS_NO_DEPTH_BLEND();
      drawCylinder(renderer, beam.pos, radius * 1.75F, BEAM_HEIGHT,
            0.72F, 0.58F, 1.0F, 0.22F * fade, layer);
      drawGroundRing(renderer, beam.pos, radius * 2.5F, radius * 0.72F,
            0.86F, 0.75F, 1.0F, 0.3F * fade, layer);
      drawCylinder(renderer, beam.pos, radius * 0.62F, BEAM_HEIGHT,
            1.0F, 0.96F, 1.0F, 0.9F * fade, layer);
   }

   /** 28 пылинок вокруг точки удара из эталонной реализации. */
   private static void spawnSkyParticles(Vec3d pos) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null) {
         return;
      }
      for (int i = 0; i < 28; i++) {
         double angle = client.world.random.nextDouble() * Math.PI * 2.0;
         double distance = 0.35 + client.world.random.nextDouble() * 1.15;
         client.world.addParticleClient(ParticleTypes.END_ROD,
               pos.x + Math.cos(angle) * distance,
               pos.y + client.world.random.nextDouble() * 2.2,
               pos.z + Math.sin(angle) * distance,
               -Math.cos(angle) * 0.035, 0.08, -Math.sin(angle) * 0.035);
      }
   }

   private static void drawCylinder(WorldRenderer renderer, Vec3d center, float radius, float height,
         float red, float green, float blue, float alpha, RenderLayer layer) {
      WorldGeometryEmitter geometry = new WorldGeometryEmitter(
            renderer, renderer.matrixStack().peek(), renderer.getBuffer(layer));
      int bottom = rgba(red, green, blue, alpha);
      int top = rgba(red, green, blue, 0.0F);
      for (int i = 0; i < BEAM_SEGMENTS; i++) {
         double a0 = Math.PI * 2.0 * i / BEAM_SEGMENTS;
         double a1 = Math.PI * 2.0 * (i + 1) / BEAM_SEGMENTS;
         Vec3d p0 = new Vec3d(center.x + Math.cos(a0) * radius, center.y, center.z + Math.sin(a0) * radius);
         Vec3d p1 = new Vec3d(center.x + Math.cos(a1) * radius, center.y, center.z + Math.sin(a1) * radius);
         Vec3d p2 = p1.add(0.0, height, 0.0);
         Vec3d p3 = p0.add(0.0, height, 0.0);
         geometry.emitQuad(p0, p1, p2, p3, bottom, bottom, top, top);
      }
   }

   private static void drawGroundRing(WorldRenderer renderer, Vec3d center, float outer, float inner,
         float red, float green, float blue, float alpha, RenderLayer layer) {
      WorldGeometryEmitter geometry = new WorldGeometryEmitter(
            renderer, renderer.matrixStack().peek(), renderer.getBuffer(layer));
      int solid = rgba(red, green, blue, alpha);
      int faded = rgba(red, green, blue, 0.0F);
      float y = (float) center.y + 0.025F;
      for (int i = 0; i < BEAM_SEGMENTS; i++) {
         double a0 = Math.PI * 2.0 * i / BEAM_SEGMENTS;
         double a1 = Math.PI * 2.0 * (i + 1) / BEAM_SEGMENTS;
         Vec3d i0 = new Vec3d(center.x + Math.cos(a0) * inner, y, center.z + Math.sin(a0) * inner);
         Vec3d i1 = new Vec3d(center.x + Math.cos(a1) * inner, y, center.z + Math.sin(a1) * inner);
         Vec3d o1 = new Vec3d(center.x + Math.cos(a1) * outer, y, center.z + Math.sin(a1) * outer);
         Vec3d o0 = new Vec3d(center.x + Math.cos(a0) * outer, y, center.z + Math.sin(a0) * outer);
         geometry.emitQuad(i0, i1, o1, o0, solid, solid, faded, faded);
      }
   }

   private static int rgba(float red, float green, float blue, float alpha) {
      int r = Math.round(Math.max(0.0F, Math.min(1.0F, red)) * 255.0F);
      int g = Math.round(Math.max(0.0F, Math.min(1.0F, green)) * 255.0F);
      int b = Math.round(Math.max(0.0F, Math.min(1.0F, blue)) * 255.0F);
      int a = Math.round(Math.max(0.0F, Math.min(1.0F, alpha)) * 255.0F);
      return (a << 24) | (r << 16) | (g << 8) | b;
   }

   private static float smoothstep(float edge0, float edge1, float value) {
      float t = Math.max(0.0F, Math.min(1.0F, (value - edge0) / (edge1 - edge0)));
      return t * t * (3.0F - 2.0F * t);
   }

   private record Beam(Vec3d pos, int color, long durationMs, long startedAt, boolean sky) {
      private Beam(Vec3d pos, int color, long durationMs, boolean sky) {
         this(pos, color, durationMs, System.currentTimeMillis(), sky);
      }
   }
}
