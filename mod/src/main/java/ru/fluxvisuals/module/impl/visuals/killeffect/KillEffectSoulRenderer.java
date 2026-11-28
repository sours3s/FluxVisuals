package ru.fluxvisuals.module.impl.visuals.killeffect;

import com.mojang.blaze3d.opengl.GlStateManager;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.BatchingRenderCommandQueue;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueueImpl;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL14;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Душа и осколки: спирали, кольца, вихрь и разлетающиеся обломки. */
@Environment(EnvType.CLIENT)
public final class KillEffectSoulRenderer {
   private static final int MAX_EFFECTS = 24;
   private static final int DNA_SEGMENTS = 60;
   private static final int RISING_RINGS = 8;
   private static final int CIRCLE_SEGMENTS = 36;
   private static final int RING_SEGMENTS = 40;
   private static final int SPIRAL_SEGMENTS = 48;
   private static final int VORTEX_RINGS = 7;
   private static final long SOUL_DURATION_MS = 4000L;
   private static final long SHATTER_DURATION_MS = 2500L;
   private static final double THIRD_TURN = 2.0943951023931953;
   private static final int WHITE = 0xFFFFFF;

   private static final List<Structured> EFFECTS = new ArrayList<>();

   private KillEffectSoulRenderer() {}

   public static void spawn(LivingEntity entity, Vec3d pos, int color, float durationSeconds,
         float height, float width, float radius, float speed, int particles, boolean soul) {
      if (pos == null) {
         return;
      }
      if (EFFECTS.size() >= MAX_EFFECTS) {
         remove(EFFECTS.remove(0));
      }
      // Длительности эталона фиксированы: 4 с для Soul и 2.5 с для Shatter.
      // Остальные параметры сигнатуры сохранены для совместимости с вызовом.
      long durationMs = soul ? SOUL_DURATION_MS : SHATTER_DURATION_MS;
      EFFECTS.add(Structured.create(entity, pos, color, durationMs, soul));
   }

   /** Призрак игрока создаётся только для Soul, как в исходной реализации. */
   private static OtherClientPlayerEntity createGhost(LivingEntity source) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (!(source instanceof PlayerEntity player) || client == null || client.world == null) {
         return null;
      }
      OtherClientPlayerEntity ghost = new OtherClientPlayerEntity(client.world, player.getGameProfile());
      ghost.setPitch(-15.0F);
      ghost.setYaw(source.getYaw());
      ghost.headYaw = source.getYaw();
      ghost.bodyYaw = source.getYaw();
      ghost.setCustomNameVisible(false);
      ghost.setCustomName(Text.literal("Ghost_" + player.getUuid()));
      client.world.addEntity(ghost);
      return ghost;
   }

   public static void tick() {
      long now = System.currentTimeMillis();
      Iterator<Structured> it = EFFECTS.iterator();
      while (it.hasNext()) {
         Structured effect = it.next();
         if (now - effect.startedAt >= effect.durationMs) {
            remove(effect);
            it.remove();
         }
      }
   }

   public static void render(WorldRenderer renderer) {
      tick();
      if (renderer == null || EFFECTS.isEmpty()) {
         return;
      }
      long now = System.currentTimeMillis();
      for (Structured effect : EFFECTS) {
         long elapsed = now - effect.startedAt;
         if (elapsed >= effect.durationMs) {
            continue;
         }
         float t = (float) elapsed / effect.durationMs;
         if (effect.soul) {
            renderSoul(renderer, effect, t, elapsed);
         } else {
            renderShatter(renderer, effect, t, elapsed);
         }
      }
   }

   public static void clear() {
      for (Structured effect : EFFECTS) {
         remove(effect);
      }
      EFFECTS.clear();
   }

   private static void remove(Structured effect) {
      if (effect == null || effect.ghost == null || effect.ghost == effect.source) {
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null) {
         client.world.removeEntity(effect.ghost.getId(), RemovalReason.DISCARDED);
      }
   }

   private static void renderSoul(WorldRenderer renderer, Structured effect, float t, long elapsed) {
      Vec3d base = effect.pos;
      int theme = effect.color;
      float rise = easeOutCubic(t) * 6.0F;
      float fadeOut = 1.0F - smoothstep(0.72F, 1.0F, t);
      float fadeIn = smoothstep(0.0F, 0.08F, t);
      float dnaH = rise + 0.8F;
      double dnaPhase = elapsed * 0.0025;

      // Три ДНК-спирали по 60 сегментов.
      for (int strand = 0; strand < 3; strand++) {
         double strandOffset = strand * THIRD_TURN;
         for (int i = 0; i < DNA_SEGMENTS - 1; i++) {
            float fA = (float) i / DNA_SEGMENTS;
            float fB = (float) (i + 1) / DNA_SEGMENTS;
            float yA = fA * dnaH;
            float yB = fB * dnaH;
            double aA = fA * Math.PI * 8.0 + dnaPhase + strandOffset;
            double aB = fB * Math.PI * 8.0 + dnaPhase + strandOffset;
            float r = 0.4F;
            Vec3d pA = base.add(Math.cos(aA) * r, yA, Math.sin(aA) * r);
            Vec3d pB = base.add(Math.cos(aB) * r, yB, Math.sin(aB) * r);
            float edgeFade = (float) Math.sin(fA * Math.PI);
            float alpha = edgeFade * fadeOut * fadeIn * 0.85F;
            alpha *= 0.7 + Math.sin(fA * Math.PI * 4.0 + elapsed * 0.004) * 0.3;
            int hShift = strand * 30 + (int) (fA * 15.0F);
            renderer.drawLine(pA, pB, 1.4, tinted(theme, hShift, (int) (210.0F * alpha)), true);
         }
      }

      // Перекладины между способами каждые 4 сегмента.
      for (int i = 0; i < DNA_SEGMENTS; i += 4) {
         float fA = (float) i / DNA_SEGMENTS;
         float yA = fA * dnaH;
         double phase0 = fA * Math.PI * 8.0 + dnaPhase;
         float r = 0.4F;
         for (int s = 0; s < 3; s++) {
            double a1 = phase0 + s * THIRD_TURN;
            double a2 = phase0 + (s + 1) * THIRD_TURN;
            Vec3d p1 = base.add(Math.cos(a1) * r, yA, Math.sin(a1) * r);
            Vec3d p2 = base.add(Math.cos(a2) * r, yA, Math.sin(a2) * r);
            float edgeFade = (float) Math.sin(fA * Math.PI);
            int ca = (int) (90.0F * edgeFade * fadeOut * fadeIn);
            if (ca > 0) {
               renderer.drawLine(p1, p2, 0.7, plain(theme, ca), true);
            }
         }
      }

      // Восемь поднимающихся колец с задержкой.
      for (int ring = 0; ring < RISING_RINGS; ring++) {
         float delay = ring * 0.055F;
         float rt = Math.max(0.0F, t - delay);
         float ringY = easeOutCubic(rt) * 6.0F;
         float ringT = 1.0F - smoothstep(0.62F, 1.0F, rt);
         float ringF = ringT * (1.0F - (float) ring / 9.0F);
         float radius = (0.45F + rt * 0.3F) * (1.0F - rt * 0.25F);
         int ra = (int) (140.0F * ringF * fadeIn);
         if (ra > 0) {
            Vec3d center = base.add(0.0, ringY, 0.0);
            int rc = tinted(theme, ring * 18, ra);
            int rc2 = tinted(theme, ring * 18 + 12, ra / 3);
            drawCircle(renderer, center, radius, CIRCLE_SEGMENTS, 0.0, rc, 1.3);
            drawCircle(renderer, center, radius * 1.4F, CIRCLE_SEGMENTS, 0.0, rc2, 0.6);
         }
      }

      // Две орбиты звёзд: 8 и 12 точек.
      for (int orbit = 0; orbit < 2; orbit++) {
         int starCount = orbit == 0 ? 8 : 12;
         float orbitR = orbit == 0 ? 0.7F : 1.1F;
         float orbitSpd = orbit == 0 ? 0.001F : -6.0E-4F;
         for (int i = 0; i < starCount; i++) {
            double angle = Math.PI * 2.0 / starCount * i + elapsed * orbitSpd;
            float starH = (float) ((Math.sin(elapsed * 0.0018 + i * 1.1 + orbit) * 0.5 + 0.5) * rise);
            Vec3d sp = base.add(Math.cos(angle) * orbitR, starH, Math.sin(angle) * orbitR);
            double tailAngle = angle - 0.25 * Math.signum(orbitSpd);
            Vec3d tail = base.add(Math.cos(tailAngle) * orbitR, starH, Math.sin(tailAngle) * orbitR);
            float sa = 0.75F * fadeOut * fadeIn
                  * (float) (Math.sin(elapsed * 0.006 + i * 1.4) * 0.25 + 0.75);
            int sc = tinted(theme, i * 15 + orbit * 40, (int) (220.0F * sa));
            renderer.drawLine(sp, tail, 2.0, sc, true);
            Vec3d tip = sp.add(0.0, 0.04, 0.0);
            renderer.drawLine(sp, tip, 1.0, white((int) (180.0F * sa)), true);
         }
      }

      Vec3d soulPos = base.add(0.0, rise, 0.0);
      float flicker = (float) (Math.sin(elapsed / 80.0) * 0.06 + 0.94);
      int soulAlpha = (int) (255.0F * flicker * fadeOut * fadeIn);
      Entity visible = effect.ghost != null ? effect.ghost : effect.source;
      if (soulAlpha > 0 && visible != null) {
         if (effect.ghost != null) {
            effect.ghost.setPos(soulPos.x, soulPos.y, soulPos.z);
         }
         drawEntity(renderer, visible, soulAlpha, renderer.tickDelta());
      }
      Vec3d auraCenter = soulPos.add(0.0, 1.0, 0.0);

      for (int a = 0; a < 3; a++) {
         double pulsePhase = Math.sin(elapsed * 0.004 + a * Math.PI * 0.66);
         float aR = 0.5F + a * 0.22F + (float) pulsePhase * 0.08F;
         float aAlpha = (0.55F - a * 0.14F) * fadeOut * fadeIn;
         int aa = (int) (170.0F * aAlpha);
         if (aa > 0) {
            drawCircle(renderer, auraCenter, aR, RING_SEGMENTS, 0.0,
                  tinted(theme, a * 22, aa), 2.2F - a * 0.5F);
         }
      }

      if (t < 0.4F) {
         float bt = t / 0.4F;
         float beamAlpha = (float) Math.sin(bt * Math.PI) * fadeIn;
         int ba = (int) (120.0F * beamAlpha);
         float[] widths = {4.0F, 2.0F, 0.8F};
         float[] alphaMs = {0.25F, 0.55F, 1.0F};
         Vec3d from = base.add(0.0, -0.3, 0.0);
         Vec3d to = base.add(0.0, rise + 2.5, 0.0);
         for (int l = 0; l < 3; l++) {
            renderer.drawLine(from, to, widths[l], plain(theme, (int) (ba * alphaMs[l])), true);
         }
      }
   }

   private static void renderShatter(WorldRenderer renderer, Structured effect, float t, long elapsed) {
      Vec3d pos = effect.pos;
      int theme = effect.color;
      float fadeIn = smoothstep(0.0F, 0.06F, t);

      if (t < 0.25F) {
         float bt = t / 0.25F;
         float ease = easeOutExpo(bt);
         Vec3d center = pos.add(0.0, 0.9, 0.0);
         for (int ring = 0; ring < 4; ring++) {
            float rr = ease * (0.8F + ring * 0.5F);
            float ra = (1.0F - bt) * (1.0F - ring * 0.2F);
            float lw = 3.5F - ring * 0.6F;
            int rc = tinted(theme, ring * 22, (int) (240.0F * ra));
            drawCircle(renderer, center, rr, RING_SEGMENTS, 0.0, rc, lw);
         }

         float vba = 1.0F - bt;
         Vec3d from = pos.add(0.0, -0.5, 0.0);
         Vec3d to = pos.add(0.0, 3.5, 0.0);
         renderer.drawLine(from, to, 3.0, plain(theme, (int) (180.0F * vba)), true);
         renderer.drawLine(from, to, 1.0, white((int) (120.0F * vba)), true);
      }

      float gravity = t * t * 4.5F;
      float vortexPull = smoothstep(0.35F, 0.9F, t) * 2.2F;

      for (int i = 0; i < effect.shards.size(); i++) {
         ShardLine s = effect.shards.get(i);
         float ease = easeOutCubic(Math.min(t * 1.6F, 1.0F));
         double vx = s.dir.x * s.speed * ease * (1.0F - vortexPull * 0.45);
         double vz = s.dir.z * s.speed * ease * (1.0F - vortexPull * 0.45);
         double vy = s.dir.y * s.speed * ease - gravity * s.gravMult + vortexPull * 3.0;
         Vec3d shardPos = s.origin.add(vx, vy, vz);
         double rot = Math.toRadians(s.rotAngle + s.rotSpeed * elapsed * 0.06F);
         Vec3d perp = new Vec3d(-s.dir.z, 0.2, s.dir.x).normalize();
         double ex = Math.cos(rot) * s.dir.x + Math.sin(rot) * perp.x;
         double ez = Math.cos(rot) * s.dir.z + Math.sin(rot) * perp.z;
         Vec3d shardEnd = shardPos.add(ex * s.length, Math.sin(rot * 0.7) * s.length * 0.35,
               ez * s.length);
         float alphaF = (float) Math.pow(1.0 - t, 2.0);
         alphaF *= 0.65 + Math.sin(elapsed * 0.009 + i * 0.8) * 0.35;
         alphaF *= fadeIn;
         int a = (int) (235.0F * alphaF);
         if (a > 0) {
            float lw = 1.0F + (1.0F - t) * 2.2F;
            int col = tinted(theme, s.hueShift + i % 3 * 15, a);
            renderer.drawLine(shardPos, shardEnd, lw, col, true);
            Vec3d velocity = new Vec3d(vx, vy, vz);
            Vec3d dir = velocity.lengthSquared() > 1.0E-6 ? velocity.normalize() : Vec3d.ZERO;
            Vec3d tail1 = shardPos.subtract(dir.multiply(0.25 * ease));
            Vec3d tail2 = shardPos.subtract(dir.multiply(0.12 * ease));
            renderer.drawLine(shardPos, tail1, lw * 0.4F,
                  tinted(theme, s.hueShift + 20, a / 3), true);
            renderer.drawLine(shardPos, tail2, lw * 0.2F, white(a / 6), true);
         }
      }

      float vortexT = smoothstep(0.28F, 0.88F, t);
      if (vortexT > 0.01F) {
         float spiralH = 4.5F;
         double spiralPhase = elapsed * 0.006;
         for (int i = 0; i < SPIRAL_SEGMENTS - 1; i++) {
            float fA = (float) i / SPIRAL_SEGMENTS;
            float fB = (float) (i + 1) / SPIRAL_SEGMENTS;
            for (int strand = 0; strand < 2; strand++) {
               double stOff = strand * Math.PI;
               double aA = fA * Math.PI * 10.0 + spiralPhase + stOff;
               double aB = fB * Math.PI * 10.0 + spiralPhase + stOff;
               float rA = (0.9F - fA * 0.82F) * vortexT;
               float rB = (0.9F - fB * 0.82F) * vortexT;
               Vec3d pA = pos.add(Math.cos(aA) * rA, fA * spiralH, Math.sin(aA) * rA);
               Vec3d pB = pos.add(Math.cos(aB) * rB, fB * spiralH, Math.sin(aB) * rB);
               float edgeFade = (float) Math.sin(fA * Math.PI);
               int va = (int) (160.0F * vortexT * edgeFade * (1.0F - fA * 0.5F));
               if (va > 0) {
                  int vc = tinted(theme, strand * 30 + (int) (fA * 25.0F), va);
                  renderer.drawLine(pA, pB, 1.1, vc, true);
               }
            }
         }

         for (int v = 0; v < VORTEX_RINGS; v++) {
            float vf = (float) v / VORTEX_RINGS;
            float vY = vf * spiralH;
            float vR = (0.9F - vf * 0.82F) * vortexT;
            double ph = elapsed * 0.005 + v * 0.6;
            int va = (int) (80.0F * vortexT * (1.0F - vf * 0.6F));
            if (va > 0) {
               drawCircle(renderer, pos.add(0.0, vY, 0.0), vR, 24, ph,
                     tinted(theme, v * 20, va), 0.9);
            }
         }
      }

      float sphereAlpha = (float) (Math.sin(elapsed * 0.008) * 0.3 + 0.5)
            * (1.0F - t) * fadeIn;
      if (sphereAlpha > 0.02F) {
         float sr = 0.18F + (float) (Math.sin(elapsed * 0.01) * 0.06);
         Vec3d center = pos.add(0.0, 0.9, 0.0);
         for (int axis = 0; axis < 3; axis++) {
            double phaseOff = elapsed * 0.003 + axis * Math.PI / 3.0;
            int sa = (int) (180.0F * sphereAlpha);
            drawCircle(renderer, center, sr, 24, phaseOff,
                  tinted(theme, axis * 30, sa), 1.5);
         }
      }

      if (t > 0.85F) {
         float ft = (t - 0.85F) / 0.15F;
         float ease = easeOutExpo(ft);
         Vec3d center = pos.add(0.0, 0.9, 0.0);
         for (int ring = 0; ring < 3; ring++) {
            float fr = ease * (1.0F + ring * 0.6F);
            float fa = (1.0F - ft) * (1.0F - ring * 0.3F);
            int fc = tinted(theme, ring * 25, (int) (200.0F * fa));
            drawCircle(renderer, center, fr, RING_SEGMENTS, 0.0, fc, 2.5F - ring * 0.5F);
         }
      }
   }

   /** Рендер сущности с прозрачностью, соответствующий исходной реализации. */
   @SuppressWarnings({"rawtypes", "unchecked"})
   private static void drawEntity(WorldRenderer renderer, Entity entity, int alpha, float tickDelta) {
      if (!(entity instanceof LivingEntity living)) {
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null || client.player == null) {
         return;
      }
      Vec3d cam = renderer.camera().getCameraPos();
      MatrixStack matrices = renderer.matrixStack();
      matrices.push();
      try {
         GlStateManager._enableBlend();
         GL14.glBlendColor(1.0F, 1.0F, 1.0F, alpha / 255.0F);
         GlStateManager._blendFuncSeparate(GL14.GL_CONSTANT_ALPHA, GL14.GL_ONE_MINUS_CONSTANT_ALPHA,
               1, 0);
         GlStateManager._enableDepthTest();

         EntityRenderManager dispatcher = client.getEntityRenderDispatcher();
         EntityRenderer entityRenderer = dispatcher.getRenderer(living);
         EntityRenderState state = entityRenderer == null
               ? null : entityRenderer.getAndUpdateRenderState(living, tickDelta);
         if (state != null) {
            CameraRenderState cameraState = new CameraRenderState();
            cameraState.blockPos = renderer.camera().getBlockPos();
            cameraState.pos = cam;
            cameraState.entityPos = client.player.getEntityPos();
            cameraState.orientation = renderer.camera().getRotation();
            cameraState.initialized = true;

            OrderedRenderCommandQueueImpl queue = new OrderedRenderCommandQueueImpl();
            dispatcher.render(state, cameraState,
                  living.getX() - cam.x, living.getY() - cam.y, living.getZ() - cam.z,
                  matrices, queue);

            BatchingRenderCommandQueue batching = (BatchingRenderCommandQueue) queue.getBatchingQueue(0);
            if (batching != null) {
               VertexConsumerProvider.Immediate entities = client.getBufferBuilders().getEntityVertexConsumers();
               VertexConsumerProvider.Immediate effects = client.getBufferBuilders().getEffectVertexConsumers();
               new ModelCommandRenderer().render(batching, entities,
                     client.getBufferBuilders().getOutlineVertexConsumers(), effects);
               entities.draw();
               effects.draw();
            }
         }

         GlStateManager._blendFuncSeparate(770, 771, 1, 0);
         GL14.glBlendColor(1.0F, 1.0F, 1.0F, 1.0F);
         GlStateManager._disableBlend();
      } finally {
         matrices.pop();
      }
   }

   private static void drawCircle(WorldRenderer renderer, Vec3d center, float radius, int segments,
         double phase, int color, double width) {
      for (int i = 0; i < segments; i++) {
         double a0 = Math.PI * 2.0 * i / segments + phase;
         double a1 = Math.PI * 2.0 * (i + 1) / segments + phase;
         renderer.drawLine(circlePoint(center, a0, radius), circlePoint(center, a1, radius),
               width, color, true);
      }
   }

   private static Vec3d circlePoint(Vec3d center, double angle, float radius) {
      return center.add(Math.cos(angle) * radius, 0.0, Math.sin(angle) * radius);
   }

   private static float easeOutCubic(float t) {
      float x = Math.min(t, 1.0F);
      return 1.0F - (float) Math.pow(1.0 - x, 3.0);
   }

   private static float easeOutExpo(float t) {
      float x = Math.min(t, 1.0F);
      return x == 1.0F ? 1.0F : 1.0F - (float) Math.pow(2.0, -10.0 * x);
   }

   private static float smoothstep(float edge0, float edge1, float x) {
      float v = Math.max(0.0F, Math.min(1.0F, (x - edge0) / (edge1 - edge0)));
      return v * v * (3.0F - 2.0F * v);
   }

   private static int tinted(int base, float hueShift, int alpha) {
      return Renderer2D.ColorUtil.replAlpha(shiftHue(base, hueShift), clampAlpha(alpha));
   }

   private static int plain(int base, int alpha) {
      return Renderer2D.ColorUtil.replAlpha(base, clampAlpha(alpha));
   }

   private static int white(int alpha) {
      return Renderer2D.ColorUtil.replAlpha(WHITE, clampAlpha(alpha));
   }

   private static int clampAlpha(int alpha) {
      return Math.max(0, Math.min(255, alpha));
   }

   private static int shiftHue(int color, float degrees) {
      float[] hsb = Color.RGBtoHSB(Renderer2D.ColorUtil.red(color), Renderer2D.ColorUtil.green(color),
            Renderer2D.ColorUtil.blue(color), null);
      float hue = (hsb[0] + degrees / 360.0F) % 1.0F;
      if (hue < 0.0F) {
         hue += 1.0F;
      }
      return Color.HSBtoRGB(hue, hsb[1], hsb[2]);
   }

   private static List<ShardLine> buildShards(Vec3d pos, int seed) {
      Random rng = new Random(seed);
      int[] counts = {10, 16, 14, 8};
      float[] heights = {0.2F, 0.7F, 1.3F, 1.9F};
      int[] hues = {0, 20, -20, 40};
      List<ShardLine> shards = new ArrayList<>(48);
      for (int r = 0; r < counts.length; r++) {
         for (int i = 0; i < counts[r]; i++) {
            double angle = Math.PI * 2.0 / counts[r] * i + rng.nextDouble() * 0.5;
            float spd = 1.2F + (float) rng.nextDouble() * 2.0F;
            double yVel = -0.1 + rng.nextDouble() * 0.8;
            Vec3d dir = new Vec3d(Math.cos(angle), yVel, Math.sin(angle)).normalize();
            float rotSpd = (float) (rng.nextDouble() * 8.0 - 4.0);
            float len = 0.2F + (float) rng.nextDouble() * 0.6F;
            float grav = 0.5F + (float) rng.nextDouble();
            Vec3d origin = pos.add(0.0, heights[r], 0.0);
            shards.add(new ShardLine(origin, dir, spd, rotSpd, len, grav, hues[r]));
         }
      }
      return shards;
   }

   private static final class ShardLine {
      final Vec3d origin;
      final Vec3d dir;
      final float speed;
      final float rotSpeed;
      final float length;
      final float gravMult;
      final int hueShift;
      float rotAngle;

      ShardLine(Vec3d origin, Vec3d dir, float speed, float rotSpeed, float length,
            float gravMult, int hueShift) {
         this.origin = origin;
         this.dir = dir;
         this.speed = speed;
         this.rotSpeed = rotSpeed;
         this.length = length;
         this.gravMult = gravMult;
         this.hueShift = hueShift;
         this.rotAngle = 0.0F;
      }
   }

   private record Structured(Vec3d pos, int color, long durationMs, long startedAt, int seed,
                             boolean soul, List<ShardLine> shards, LivingEntity source,
                             OtherClientPlayerEntity ghost) {
      private static Structured create(LivingEntity entity, Vec3d pos, int color, long durationMs,
            boolean soul) {
         long startedAt = System.currentTimeMillis();
         int seed = (int) System.nanoTime();
         List<ShardLine> shards = soul ? List.of() : buildShards(pos, seed);
         OtherClientPlayerEntity ghost = soul ? createGhost(entity) : null;
         return new Structured(pos, color, durationMs, startedAt, seed, soul, shards, entity, ghost);
      }
   }
}
