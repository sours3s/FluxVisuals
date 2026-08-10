package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderer;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/**
 * Косметика для игроков: китайская шляпа (конус или нимб),
 * анимированные крылья и след за игроком. Джампсёрклы НЕ входят сюда — у нас свой модуль JumpCircle.
 *
 * <p>Все фичи рисуются в WorldRenderEvent через {@link WorldRenderer}. Стек WorldRenderer уже содержит
 * камеру, поэтому координаты — АБСОЛЮТНЫЕ мировые (как у BlockOutline), а локальные повороты
 * собираются в отдельный MatrixStack и композируются в итоговую матрицу: base * local.
 */
@IModule(name = "Cosmetic", description = "Косметика: шляпа, крылья, след за игроком", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Cosmetic extends Module {
   public final MultiBooleanSetting features = new MultiBooleanSetting("Features",
         new BooleanSetting("China Hat", true),
         new BooleanSetting("Wings", true),
         new BooleanSetting("Trail", true));

   // ChinaHat
   public final ModeSetting hatMode = new ModeSetting("Hat Mode", "Шляпа", "Шляпа", "Нимб");
   public final BooleanSetting hatSelf = new BooleanSetting("Hat Self", true);
   public final BooleanSetting hatFriends = new BooleanSetting("Hat Friends", true);
   public final BooleanSetting hatOthers = new BooleanSetting("Hat Others", false);

   // Wings
   public final BooleanSetting wingSelf = new BooleanSetting("Wings Self", true);
   public final BooleanSetting wingFriends = new BooleanSetting("Wings Friends", true);
   public final BooleanSetting wingOthers = new BooleanSetting("Wings Others", false);
   public final SliderSetting wingSize = new SliderSetting("Wings Size", 1.0F, 0.5F, 2.0F, 0.05F, false);
   public final SliderSetting wingAlpha = new SliderSetting("Wings Alpha", 100.0F, 5.0F, 255.0F, 1.0F, false);

   // Trail
   public final BooleanSetting trailSelf = new BooleanSetting("Trail Self", true);
   public final BooleanSetting trailFriends = new BooleanSetting("Trail Friends", true);
   public final SliderSetting trailDuration = new SliderSetting("Trail Duration", 2.0F, 0.5F, 5.0F, 0.1F, false);
   public final SliderSetting trailOpacity = new SliderSetting("Trail Opacity", 0.4F, 0.1F, 1.0F, 0.05F, false);

   // ---- Рендер-пайплайны ----
   private static final RenderPipeline FAN_PIPELINE = RenderPipelines.register(
         RenderPipeline.builder(new Snippet[] { RenderPipelines.POSITION_COLOR_SNIPPET })
               .withLocation(Identifier.of("fluxvisuals", "pipeline/world/cosmetic_fan"))
               .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.TRIANGLES)
               .withCull(false)
               .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
               .withDepthWrite(false)
               .withBlend(BlendFunction.TRANSLUCENT)
               .build());
   private static final RenderLayer FAN_LAYER = RenderLayer.of("fluxvisuals_cosmetic_fan",
         RenderSetup.builder(FAN_PIPELINE).expectedBufferSize(4096).translucent().build());

   private static final Identifier BLOOM_TEXTURE = Identifier.of("fluxvisuals", "textures/world/glow.png");

   // ---- ChinaHat геометрия ----
   private static final float HAT_SEGMENTS = 60.0F;
   private static final float HAT_WIDTH = 0.62F;
   private static final float CONE_HEIGHT = 0.3F;
   private static final float NIMB_RADIUS = 0.32F;
   private static final float NIMB_GLOW = 0.09F;
   private static final int NIMB_SEGS = 50;

   // ---- Wings геометрия ----
   private static final WingPoint[] WING_SHAPE = {
         new WingPoint(0.08f, 0.10f, 0.88f), new WingPoint(0.28f, 0.34f, 0.78f),
         new WingPoint(0.56f, 0.82f, 0.62f), new WingPoint(0.86f, 0.30f, 0.52f),
         new WingPoint(1.14f, 0.46f, 0.40f), new WingPoint(1.24f, 0.04f, 0.30f),
         new WingPoint(1.02f, -0.18f, 0.28f), new WingPoint(1.18f, -0.64f, 0.22f),
         new WingPoint(0.86f, -0.46f, 0.20f), new WingPoint(0.80f, -0.98f, 0.14f),
         new WingPoint(0.54f, -0.74f, 0.16f), new WingPoint(0.30f, -1.16f, 0.12f),
         new WingPoint(0.10f, -0.54f, 0.18f)
   };

   private final Map<UUID, WingState> wingStates = new HashMap<>();
   private final Map<UUID, List<TrailPoint>> trails = new HashMap<>();
   private float nimbAngle = 0.0F;
   private long lastTime = System.currentTimeMillis();

   public Cosmetic() {
      this.addSettings(new Setting[]{features, hatMode, hatSelf, hatFriends, hatOthers,
            wingSelf, wingFriends, wingOthers, wingSize, wingAlpha,
            trailSelf, trailFriends, trailDuration, trailOpacity});
   }

   @Override
   public void onDisable() {
      super.onDisable();
      wingStates.clear();
      trails.clear();
   }

   @EventInit
   public void onRender(WorldRenderEvent e) {
      if (mc.world == null || mc.player == null) {
         return;
      }
      WorldRenderer wr = e.worldRenderer();
      float tickDelta = wr.tickDelta();
      VertexConsumerProvider.Immediate imm = wr.bufferSource();
      // positionMatrix = вращение камеры + проекция (без трансляции), поэтому все мировые
      // координаты надо передавать camera-relative (минус позиция камеры).
      Matrix4f base = wr.matrixStack().peek().getPositionMatrix();
      Vec3d cam = mc.gameRenderer.getCamera().getCameraPos();

      if (features.get("China Hat")) {
         this.renderChinaHat(imm, base, tickDelta, cam);
      }
      if (features.get("Wings")) {
         this.renderWings(imm, base, tickDelta, cam);
      }
      if (features.get("Trail")) {
         this.renderTrail(imm, base, tickDelta, cam);
      }
   }

   // ==================== China Hat ====================

   private void renderChinaHat(VertexConsumerProvider.Immediate imm, Matrix4f base, float tickDelta, Vec3d cam) {
      long now = System.currentTimeMillis();
      float dt = Math.min((now - lastTime) / 1000.0F, 0.1F);
      lastTime = now;
      nimbAngle = (nimbAngle + dt * 55.0F) % 360.0F;
      boolean nimb = hatMode.get().equals("Нимб");

      for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
         if (!this.shouldRenderHat(p)) {
            continue;
         }
         if (p == mc.player && mc.options.getPerspective().isFirstPerson()) {
            continue;
         }
         Vec3d pos = new Vec3d(MathHelper.lerp(tickDelta, p.lastX, p.getX()),
               MathHelper.lerp(tickDelta, p.lastY, p.getY()),
               MathHelper.lerp(tickDelta, p.lastZ, p.getZ()));
         if (nimb) {
            this.renderNimb(imm, base, p, pos, now, cam);
         } else {
            this.renderCone(imm, base, p, pos, cam);
         }
      }
   }

   private boolean shouldRenderHat(PlayerEntity p) {
      if (p == mc.player) {
         return hatSelf.get();
      }
      return isFriend(p) ? hatFriends.get() : hatOthers.get();
   }

   private void renderCone(VertexConsumerProvider.Immediate imm, Matrix4f base, PlayerEntity p, Vec3d pos, Vec3d cam) {
      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int centerArgb = Renderer2D.ColorUtil.replAlpha(main, 200);
      int edgeArgb = Renderer2D.ColorUtil.replAlpha(main, 80);
      int outlineArgb = Renderer2D.ColorUtil.replAlpha(main, 255);

      MatrixStack local = new MatrixStack();
      local.translate(pos.x - cam.x, pos.y - cam.y + p.getHeight() + 0.05, pos.z - cam.z);
      local.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-p.getHeadYaw()));
      Matrix4f m = new Matrix4f(base).mul(local.peek().getPositionMatrix());

      VertexConsumer fill1 = imm.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS());
      for (int i = 0; i < HAT_SEGMENTS; i++) {
         float a1 = i * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float a2 = (i + 1) * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
         float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
         fill1.vertex(m, 0, CONE_HEIGHT, 0).color(centerArgb);
         fill1.vertex(m, x2, 0, z2).color(edgeArgb);
         fill1.vertex(m, x1, 0, z1).color(edgeArgb);
         fill1.vertex(m, 0, CONE_HEIGHT, 0).color(centerArgb);
      }

      VertexConsumer fill2 = imm.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS());
      for (int i = 0; i < HAT_SEGMENTS; i++) {
         float a1 = i * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float a2 = (i + 1) * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
         float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
         fill2.vertex(m, 0, CONE_HEIGHT, 0).color(centerArgb);
         fill2.vertex(m, x1, 0, z1).color(edgeArgb);
         fill2.vertex(m, x2, 0, z2).color(edgeArgb);
         fill2.vertex(m, 0, CONE_HEIGHT, 0).color(centerArgb);
      }

      VertexConsumer lines = imm.getBuffer(WorldRenderLayers.LINES(2.0));
      for (int i = 0; i < HAT_SEGMENTS; i++) {
         float a1 = i * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float a2 = (i + 1) * (float)Math.PI * 2.0F / HAT_SEGMENTS;
         float x1 = -MathHelper.sin(a1) * HAT_WIDTH, z1 = MathHelper.cos(a1) * HAT_WIDTH;
         float x2 = -MathHelper.sin(a2) * HAT_WIDTH, z2 = MathHelper.cos(a2) * HAT_WIDTH;
         // LINES требует формат POSITION_COLOR_NORMAL_LINE_WIDTH — без .normal() и .lineWidth()
         // краш («Missing elements in vertex») при рендере в 3-м лице (F5).
         lines.vertex(m, x1, 0, z1).color(outlineArgb).normal(0.0F, 1.0F, 0.0F).lineWidth(2.0F);
         lines.vertex(m, x2, 0, z2).color(outlineArgb).normal(0.0F, 1.0F, 0.0F).lineWidth(2.0F);
      }
   }

   private void renderNimb(VertexConsumerProvider.Immediate imm, Matrix4f base, PlayerEntity p, Vec3d pos, long now, Vec3d cam) {
      float elapsed = now / 1000.0F;
      float pulse = 1.0F + 0.05F * (float)Math.sin(elapsed * 4.5F);
      float r = NIMB_RADIUS * pulse;
      double hx = pos.x, hy = pos.y + p.getHeight() + 0.22, hz = pos.z;
      Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int r0 = main >> 16 & 0xFF, g0 = main >> 8 & 0xFF, b0 = main & 0xFF;

      // Три слоя свечения вокруг головы (ядро, средний, внешний).
      for (int layer = 0; layer < 3; layer++) {
         float size = NIMB_GLOW * (layer == 0 ? 1.0F : (layer == 1 ? 2.2F : 4.0F));
         int alpha = layer == 0 ? 180 : (layer == 1 ? 55 : 20);
         VertexConsumer buf = imm.getBuffer(WorldRenderLayers.TEXTURED_QUADS_ADDITIVE(BLOOM_TEXTURE));
         for (int i = 0; i < NIMB_SEGS; i++) {
            float angle = (float) i / NIMB_SEGS * (float) Math.PI * 2.0F + (float) Math.toRadians(nimbAngle);
            float px = (float) Math.cos(angle) * r;
            float pz = (float) Math.sin(angle) * r;
            this.drawBillboard(buf, base, camRot, hx - cam.x + px, hy - cam.y, hz - cam.z + pz, size, r0, g0, b0, alpha);
         }
      }
   }

   /** Бильборд (квад, всегда повёрнутый к камере) в camera-space точке с глоу-текстурой. */
   private void drawBillboard(VertexConsumer buf, Matrix4f base, Quaternionf camRot,
                              double x, double y, double z, float size, int r, int g, int b, int alpha) {
      Vector3f upLeft = new Vector3f(-size, -size, 0).rotate(camRot);
      Vector3f upRight = new Vector3f(-size, size, 0).rotate(camRot);
      Vector3f lowRight = new Vector3f(size, size, 0).rotate(camRot);
      Vector3f lowLeft = new Vector3f(size, -size, 0).rotate(camRot);
      buf.vertex(base, (float) x + upLeft.x, (float) y + upLeft.y, (float) z + upLeft.z).texture(0, 0).color(r, g, b, alpha);
      buf.vertex(base, (float) x + upRight.x, (float) y + upRight.y, (float) z + upRight.z).texture(0, 1).color(r, g, b, alpha);
      buf.vertex(base, (float) x + lowRight.x, (float) y + lowRight.y, (float) z + lowRight.z).texture(1, 1).color(r, g, b, alpha);
      buf.vertex(base, (float) x + lowLeft.x, (float) y + lowLeft.y, (float) z + lowLeft.z).texture(1, 0).color(r, g, b, alpha);
   }

   // ==================== Wings ====================

   private void renderWings(VertexConsumerProvider.Immediate imm, Matrix4f base, float tickDelta, Vec3d cam) {
      for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
         if (!this.shouldRenderWings(p)) {
            continue;
         }
         if (p == mc.player && mc.options.getPerspective().isFirstPerson()) {
            continue;
         }
         WingState state = wingStates.computeIfAbsent(p.getUuid(), u -> new WingState());
         double ix = MathHelper.lerp(tickDelta, p.lastX, p.getX());
         double iy = MathHelper.lerp(tickDelta, p.lastY, p.getY());
         double iz = MathHelper.lerp(tickDelta, p.lastZ, p.getZ());

         float smoothYaw = smoothYaw(p, tickDelta, state);
         float move = MathHelper.clamp(p.limbAnimator.getSpeed(), 0.0F, 1.0F);
         float targetWater = p.isTouchingWater() ? 1.0F : 0.0F;
         state.waterAnim += (targetWater - state.waterAnim) * 0.08F;

         float bodyYawRad = (float) Math.toRadians(-smoothYaw);
         float motionX = (float) (p.getX() - p.lastX);
         float motionZ = (float) (p.getZ() - p.lastZ);
         float targetForward = (float) ((motionX * Math.sin(bodyYawRad)) + (motionZ * Math.cos(bodyYawRad)));
         state.forwardAnim += (MathHelper.clamp(targetForward * 22.0F, -1.0F, 1.0F) - state.forwardAnim) * 0.08F;

         WingPose pose = wingPose(p);
         pose.pitchRotation += (25.0F - pose.pitchRotation) * state.waterAnim;
         pose.scaleFactor += (0.9F - pose.scaleFactor) * state.waterAnim;
         pose.opennessMultiplier += (0.85F - pose.opennessMultiplier) * state.waterAnim;

         state.flapAnim += (pose.flapStrength + (move * 6.0F) - state.flapAnim) * 0.08F;
         float flapAngle = (float) Math.sin((p.age + tickDelta) * pose.flapFrequency) * state.flapAnim;
         float spread = (8.0F + flapAngle + move * pose.motionSpreadBonus * 1.8F) * pose.opennessMultiplier;
         float dynamicSpread = spread + state.forwardAnim * 16.0F;

         int main = Renderer2D.ColorUtil.getMainColor(1, 1);
         int fillArgb = Renderer2D.ColorUtil.replAlpha(main, (int) this.wingAlpha.get());
         int lineArgb = Renderer2D.ColorUtil.replAlpha(main, 255);

         // Локальная матрица: положение игрока (camera-relative) + повороты.
         MatrixStack local = new MatrixStack();
         local.translate(ix - cam.x, iy - cam.y + p.getHeight() * 0.65F, iz - cam.z);
         local.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-smoothYaw));

         float leaning = p.getLeaningPitch(tickDelta);
         if (p.isGliding()) {
            leaning = 1.0F;
         }
         if (leaning > 0.01F) {
            float pitch = p.getPitch(tickDelta);
            local.multiply(RotationAxis.POSITIVE_X.rotationDegrees(leaning * (pitch - 90.0F)));
            local.translate(0.0F, 0.18F * leaning, -0.08F * leaning);
         } else if (p.isSneaking()) {
            local.translate(0.0F, -0.15F, 0.08F);
            local.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.0F));
         }

         local.translate(0.0F, 0.0F, -0.15F);
         float fScale = this.wingSize.get() * pose.scaleFactor;
         local.scale(fScale, fScale, fScale);
         if (pose.pitchRotation != 0.0F) {
            local.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.pitchRotation));
         }
         if (pose.rollRotation != 0.0F) {
            local.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(pose.rollRotation));
         }

         drawWingSide(imm, base, local, -1.0F, dynamicSpread, fillArgb, lineArgb, pose, state);
         drawWingSide(imm, base, local, 1.0F, dynamicSpread, fillArgb, lineArgb, pose, state);
      }
   }

   private boolean shouldRenderWings(PlayerEntity p) {
      if (p == mc.player) {
         return wingSelf.get();
      }
      return isFriend(p) ? wingFriends.get() : wingOthers.get();
   }

   private void drawWingSide(VertexConsumerProvider.Immediate imm, Matrix4f base, MatrixStack local, float dir,
                             float spread, int fillCol, int lineCol, WingPose pose, WingState state) {
      local.push();
      local.translate(dir * pose.sideOffset, 0, pose.sideDepthOffset);
      local.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(dir * spread));
      local.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(dir * pose.sideRollAngle));
      local.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pose.sidePitchAngle + (state.forwardAnim * 10.0F)));

      Matrix4f m = new Matrix4f(base).mul(local.peek().getPositionMatrix());
      VertexConsumer vcFill = imm.getBuffer(FAN_LAYER);
      float centerX = 0.0F, centerY = 0.0F;
      for (WingPoint wp : WING_SHAPE) {
         centerX += wp.x;
         centerY += wp.y;
      }
      centerX /= WING_SHAPE.length;
      centerY /= WING_SHAPE.length;

      for (int i = 0; i < WING_SHAPE.length; i++) {
         WingPoint p1 = WING_SHAPE[i];
         WingPoint p2 = WING_SHAPE[(i + 1) % WING_SHAPE.length];
         vcFill.vertex(m, dir * centerX, centerY, 0).color(fillCol);
         vcFill.vertex(m, dir * p1.x, p1.y, 0).color(fillCol);
         vcFill.vertex(m, dir * p2.x, p2.y, 0).color(fillCol);
      }

      VertexConsumer vcOutline = imm.getBuffer(WorldRenderLayers.LINES(2.0));
      for (int i = 0; i <= WING_SHAPE.length; i++) {
         WingPoint p = WING_SHAPE[i % WING_SHAPE.length];
         // LINES требует формат POSITION_COLOR_NORMAL_LINE_WIDTH — без .normal() и .lineWidth()
         // краш («Missing elements in vertex») при рендере в 3-м лице (F5).
         vcOutline.vertex(m, dir * p.x, p.y, 0).color(lineCol).normal(0.0F, 0.0F, 1.0F).lineWidth(2.0F);
      }
      local.pop();
   }

   private float smoothYaw(PlayerEntity p, float delta, WingState state) {
      float target = MathHelper.lerpAngleDegrees(delta, p.lastBodyYaw, p.bodyYaw);
      if (!state.yawInitialized) {
         state.smoothYaw = target;
         state.yawInitialized = true;
      }
      float diff = MathHelper.wrapDegrees(target - state.smoothYaw);
      state.smoothYaw += MathHelper.clamp(diff, -14.0F, 14.0F);
      return state.smoothYaw;
   }

   private WingPose wingPose(PlayerEntity p) {
      if (p.isGliding()) {
         return new WingPose(0.0F, 0.0F, 0.75F, 0.85F, 0.1F, 1.5F, 0.05F, 0.06F, -45.0F, 0.0F, 0.06F);
      }
      return new WingPose(0.0F, 0.0F, 1.0F, 1.0F, 0.18F, 4.5F, 0.06F, 0.02F, -11.0F, -4.0F, 0.12F);
   }

   // ==================== Trail ====================

   private void renderTrail(VertexConsumerProvider.Immediate imm, Matrix4f base, float tickDelta, Vec3d cam) {
      long now = System.currentTimeMillis();
      float maxAge = this.trailDuration.get() * 1000.0F;

      for (AbstractClientPlayerEntity p : mc.world.getPlayers()) {
         boolean isLocal = p == mc.player;
         if (isLocal && !this.trailSelf.get()) {
            continue;
         }
         if (!isLocal && !this.trailFriends.get()) {
            continue;
         }
         if (isLocal && mc.options.getPerspective().isFirstPerson()) {
            continue;
         }
         List<TrailPoint> points = trails.computeIfAbsent(p.getUuid(), u -> new ArrayList<>());
         Vec3d cur = new Vec3d(MathHelper.lerp(tickDelta, p.lastX, p.getX()),
               MathHelper.lerp(tickDelta, p.lastY, p.getY()) + 0.1,
               MathHelper.lerp(tickDelta, p.lastZ, p.getZ()));
         if (points.isEmpty() || points.get(points.size() - 1).pos.distanceTo(cur) > 0.01) {
            points.add(new TrailPoint(cur, now));
         }
         points.removeIf(pt -> (now - pt.time) > maxAge);
      }
      trails.entrySet().removeIf(e -> e.getValue().isEmpty());
      if (trails.isEmpty()) {
         return;
      }

      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int r0 = main >> 16 & 0xFF, g0 = main >> 8 & 0xFF, b0 = main & 0xFF;
      int maxA = (int) (255.0F * this.trailOpacity.get());

      VertexConsumer buf = imm.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS());
      for (List<TrailPoint> points : trails.values()) {
         if (points.size() < 2) {
            continue;
         }
         for (int i = 0; i < points.size() - 1; i++) {
            TrailPoint p1 = points.get(i);
            TrailPoint p2 = points.get(i + 1);
            float age1 = (float) (now - p1.time) / maxAge;
            float age2 = (float) (now - p2.time) / maxAge;
            int a1 = Math.max(0, (int) (maxA * (1.0F - age1)));
            int a2 = Math.max(0, (int) (maxA * (1.0F - age2)));
            float x1 = (float) (p1.pos.x - cam.x), y1 = (float) (p1.pos.y - cam.y), z1 = (float) (p1.pos.z - cam.z);
            float x2 = (float) (p2.pos.x - cam.x), y2 = (float) (p2.pos.y - cam.y), z2 = (float) (p2.pos.z - cam.z);
            buf.vertex(base, x1, y1, z1).color(r0, g0, b0, a1);
            buf.vertex(base, x2, y2, z2).color(r0, g0, b0, a2);
            buf.vertex(base, x2, y2 + 1.7F, z2).color(r0, g0, b0, a2);
            buf.vertex(base, x1, y1 + 1.7F, z1).color(r0, g0, b0, a1);
         }
      }
   }

   private boolean isFriend(PlayerEntity p) {
      try {
         return FluxVisualsClient.get != null
               && FluxVisualsClient.get.friendManager != null
               && FluxVisualsClient.get.friendManager.isFriend(p.getName().getString());
      } catch (Exception ignored) {
         return false;
      }
   }

   // ==================== Helpers ====================

   private record WingPoint(float x, float y, float alphaMul) {
   }

   private static class WingState {
      float smoothYaw, forwardAnim, flapAnim, waterAnim;
      boolean yawInitialized;
   }

   private static class WingPose {
      float pitchRotation, rollRotation, opennessMultiplier, scaleFactor, motionSpreadBonus, flapStrength,
            sideOffset, sideDepthOffset, sideRollAngle, sidePitchAngle, flapFrequency;

      WingPose(float pitch, float roll, float open, float scale, float mSpread, float flapS,
               float sOff, float sDOff, float sRoll, float sPitch, float flapFreq) {
         this.pitchRotation = pitch;
         this.rollRotation = roll;
         this.opennessMultiplier = open;
         this.scaleFactor = scale;
         this.motionSpreadBonus = mSpread;
         this.flapStrength = flapS;
         this.sideOffset = sOff;
         this.sideDepthOffset = sDOff;
         this.sideRollAngle = sRoll;
         this.sidePitchAngle = sPitch;
         this.flapFrequency = flapFreq;
      }
   }

   private static class TrailPoint {
      final Vec3d pos;
      final long time;

      TrailPoint(Vec3d pos, long time) {
         this.pos = pos;
         this.time = time;
      }
   }
}
