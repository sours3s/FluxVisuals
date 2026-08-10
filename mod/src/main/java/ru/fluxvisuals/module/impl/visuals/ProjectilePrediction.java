package ru.fluxvisuals.module.impl.visuals;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EggEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.event.render.EventRender3D;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.other.Mathf;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(
   name = "Projectile Prediction",
   description = "Predicts projectile trajectories and renders their landing spots",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class ProjectilePrediction extends Module {

   private final MultiBooleanSetting targets = new MultiBooleanSetting("Targets",
      new BooleanSetting("Ender Pearl", true),
      new BooleanSetting("Arrows", true),
      new BooleanSetting("Trident", true),
      new BooleanSetting("Items", true),
      new BooleanSetting("Other", true),
      new BooleanSetting("Potion radius", true)
   );
   private final SliderSetting fontSize = new SliderSetting("Size", 26.0F, 18.0F, 32.0F, 1.0F, false);

   private static final float INFO_TOP_PADDING_X = 5.0F;
   private static final float INFO_TOP_PADDING_Y = 3.0F;
   private static final float INFO_NAME_PADDING_X = 5.0F;
   private static final float INFO_NAME_PADDING_Y = 2.5F;
   private static final float INFO_GAP = 3.0F;
   private static final float INFO_ICON_SIZE = 12.0F;
   private static final float INFO_ICON_SCALE = INFO_ICON_SIZE / 32.0F;
   private static final float INFO_NAME_GAP = 1.0F;
   private static final float PILL_RADIUS = 8.0F;
   private static final float PILL_ALPHA = 0.38F;
   private static final float PILL_BLUR_ALPHA = 0.82F;
   private static final float PILL_BLUR_STRENGTH = 18.0F;
   private static final double SPLASH_POTION_RADIUS = 4.0;
   private static final double LINGERING_POTION_RADIUS = 3.0;
   private static final int POTION_CIRCLE_SEGMENTS = 96;

   private static final RenderPipeline LINE_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("fluxvisuals", "prediction_lines"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.DEBUG_LINES)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .build()
   );
   private static final RenderLayer LINE_LAYER = RenderLayer.of(
      "fluxvisuals_prediction_line",
      RenderSetup.builder(LINE_PIPELINE).expectedBufferSize(1024).translucent().build()
   );

   private static final RenderPipeline QUAD_PIPELINE = RenderPipelines.register(
      RenderPipeline.builder(new Snippet[]{RenderPipelines.POSITION_COLOR_SNIPPET})
         .withLocation(Identifier.of("fluxvisuals", "prediction_quads"))
         .withVertexFormat(VertexFormats.POSITION_COLOR, DrawMode.QUADS)
         .withCull(false)
         .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
         .withDepthWrite(false)
         .withBlend(BlendFunction.LIGHTNING)
         .build()
   );
   private static final RenderLayer QUAD_LAYER = RenderLayer.of(
      "fluxvisuals_prediction_quad",
      RenderSetup.builder(QUAD_PIPELINE).expectedBufferSize(1024).translucent().build()
   );

   private final BufferAllocator allocator = new BufferAllocator(262144);
   private final VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate(allocator);

   private final List<ImpactPoint> impactPoints = new ArrayList<>();

   public ProjectilePrediction() {
      this.addSettings(new Setting[]{this.targets, this.fontSize});
   }

   @EventInit
   public void onRender3D(EventRender3D event) {
      if (mc.player == null || mc.world == null) return;
      impactPoints.clear();

      predictInHand();

      Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      MatrixStack matrices = event.getMatrixStack();
      VertexConsumer lineConsumer = immediate.getBuffer(LINE_LAYER);
      VertexConsumer quadConsumer = immediate.getBuffer(QUAD_LAYER);
      Matrix4f matrix = matrices.peek().getPositionMatrix();

      for (Entity entity : mc.world.getEntities()) {
         if (!isTrackedProjectile(entity)) continue;
         if (isStationary(entity)) continue;

         Vec3d motion = entity.getVelocity();
         Vec3d pos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
         List<Vec3d> rawPoints = new ArrayList<>();
         rawPoints.add(getLerpedPos(entity, event.getTickDelta()));

         for (int i = 0; i < 300; i++) {
            Vec3d prevPos = pos;
            Vec3d nextPos = pos.add(motion);
            HitResult result = getCollision(entity, prevPos, nextPos);

            if (result != null && !result.getType().equals(HitResult.Type.MISS)) {
               pos = result.getPos();
               rawPoints.add(pos);
               if (entity instanceof PotionEntity potion) {
                  renderPotionImpact3D(lineConsumer, matrix, cameraPos, pos, getPotionRadius(potion));
               }
               recordImpactPoint(entity, pos, i + 1);
               break;
            }
            pos = nextPos;
            rawPoints.add(pos);
            if (pos.y < -128) break;
            motion = calculateMotion(entity, pos, motion);
         }

         if (rawPoints.size() >= 2) {
            List<Vec3d> smoothed = smoothRenderPath(rawPoints);
            for (int i = 1; i < smoothed.size(); i++) {
               float alpha = MathHelper.clamp(0.4f + (i / 40.0f), 0.4f, 1.0f);
               int color = ColorUtil.multAlpha(ColorUtil.fade(i), alpha);
               drawLine3D(lineConsumer, matrix, cameraPos, smoothed.get(i - 1), smoothed.get(i), color);
            }
         }
      }

      for (PredictionResult pr : handPredictions) {
         Direction dir = getDirection(pr.result);
         int baseColor = ColorUtil.fade();
         int color = pr.result.getType().equals(HitResult.Type.ENTITY) ? ColorUtil.RED : baseColor;
         renderImpactCircle(quadConsumer, matrix, cameraPos, pr.result.getPos(), dir, color);
         if (pr.isPotionStack) {
            renderPotionImpact3D(lineConsumer, matrix, cameraPos, pr.result.getPos(), pr.potionRadius);
         }
      }

      immediate.draw();
   }

   @EventInit
   public void onRender2D(EventScreen event) {
      if (mc.player == null || mc.world == null) return;
      Renderer2D renderer = event.renderer();
      DrawContext drawContext = event.drawContext();

      for (ImpactPoint point : impactPoints) {
         Vec3d screenPos = Mathf.worldSpaceToScreenSpace(point.pos);
         if (screenPos == null || screenPos.z < 0.0 || screenPos.z > 1.0) continue;

         float sx = (float) screenPos.x * 2.0F;
         float sy = (float) screenPos.y * 2.0F;

         double time = point.ticks * 50 / 1000.0;
         String text = String.format("%.1f", time) + " сек";
         var timeBounds = renderer.measureText(FontRegistry.INTER_MEDIUM, text, fontSize.get());
         float textWidth = timeBounds.width;
         float textHeight = timeBounds.height;

         float topRowHeight = Math.max(INFO_ICON_SIZE, textHeight);
         float topRowWidth = INFO_ICON_SIZE + INFO_GAP + textWidth;

         boolean hasOwner = point.ownerName != null && !point.ownerName.isBlank();
         float ownerFontSize = Math.max(18.0F, fontSize.get() - 5.0F);
         var ownerBounds = hasOwner ? renderer.measureText(FontRegistry.INTER_MEDIUM, point.ownerName, ownerFontSize) : null;
         float ownerTextWidth = ownerBounds != null ? ownerBounds.width : 0.0F;
         float ownerTextHeight = ownerBounds != null ? ownerBounds.height : 0.0F;

         float topCapsuleWidth = topRowWidth + INFO_TOP_PADDING_X * 2.0F;
         float topCapsuleHeight = topRowHeight + INFO_TOP_PADDING_Y * 2.0F;
         float nameCapsuleWidth = hasOwner ? ownerTextWidth + INFO_NAME_PADDING_X * 2.0F : 0.0F;
         float nameCapsuleHeight = hasOwner ? ownerTextHeight + INFO_NAME_PADDING_Y * 2.0F : 0.0F;
         float totalWidth = Math.max(topCapsuleWidth, nameCapsuleWidth);
         float totalHeight = hasOwner ? topCapsuleHeight + INFO_NAME_GAP + nameCapsuleHeight : topCapsuleHeight;
         float blockX = sx - totalWidth / 2.0F;
         float blockY = sy + 4.0F;
         float topX = blockX + (totalWidth - topCapsuleWidth) / 2.0F;
         float topY = blockY;

         drawPredictionPanel(renderer, topX, topY, topCapsuleWidth, topCapsuleHeight);

         float topRowX = topX + INFO_TOP_PADDING_X;
         float topRowY = topY + INFO_TOP_PADDING_Y;

         float iconX = topRowX;
         float iconY = topRowY + (topRowHeight - INFO_ICON_SIZE) / 2.0F;
         drawPredictionIcon(drawContext, point.stack, iconX, iconY);

         float textX = iconX + INFO_ICON_SIZE + INFO_GAP;
         float textY = centeredTextBaseline(text, topRowY, topRowHeight, fontSize.get(), timeBounds);
         renderer.text(FontRegistry.INTER_MEDIUM, textX, textY, fontSize.get(), text, Renderer2D.ColorUtil.getTextColor(1, 1));

         if (hasOwner) {
            float nameX = blockX + (totalWidth - nameCapsuleWidth) / 2.0F;
            float nameY = topY + topCapsuleHeight + INFO_NAME_GAP;
            drawPredictionPanel(renderer, nameX, nameY, nameCapsuleWidth, nameCapsuleHeight);
            float ownerTextX = nameX + INFO_NAME_PADDING_X;
            float ownerTextY = centeredTextBaseline(point.ownerName, nameY + INFO_NAME_PADDING_Y, ownerTextHeight, ownerFontSize, ownerBounds);
            renderer.text(FontRegistry.INTER_MEDIUM, ownerTextX, ownerTextY, ownerFontSize, point.ownerName, Renderer2D.ColorUtil.getTextColor(1, 1));
         }
      }
   }

   private void drawPredictionIcon(DrawContext drawContext, ItemStack stack, float x, float y) {
      drawContext.getMatrices().pushMatrix();
      drawContext.getMatrices().translate(x / 2.0F, y / 2.0F);
      drawContext.getMatrices().scale(INFO_ICON_SCALE, INFO_ICON_SCALE);
      drawContext.drawItem(stack, 0, 0);
      drawContext.getMatrices().popMatrix();
   }

   private static float centeredTextBaseline(String text, float y, float height, float size, ru.fluxvisuals.util.render.text.TextRenderer.TextMetrics metrics) {
      return y + (height - metrics.height) / 2.0F + metrics.baselineOffset;
   }

   private void drawPredictionPanel(Renderer2D renderer, float x, float y, float width, float height) {
      renderer.prepareBlur(PILL_BLUR_STRENGTH);
      renderer.blur(x, y, width, height, PILL_RADIUS, PILL_BLUR_ALPHA);
      renderer.rect(x, y, width, height, PILL_RADIUS, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), PILL_ALPHA));
   }

   private final List<PredictionResult> handPredictions = new ArrayList<>();

   private void predictInHand() {
      handPredictions.clear();
      if (mc.player == null || mc.world == null) return;

      ItemStack mainHand = mc.player.getMainHandStack();
      ItemStack offHand = mc.player.getOffHandStack();
      ItemStack[] stacks = new ItemStack[]{mainHand, offHand};
      Item activeItem = mc.player.getActiveItem().getItem();
      float yaw = mc.player.getYaw();
      float pitch = mc.player.getPitch();

      for (ItemStack stack : stacks) {
         List<HitResult> results = null;
         Item item = stack.getItem();

         if (item instanceof ExperienceBottleItem && targets.get("Other")) {
            results = checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8, yaw, pitch);
         } else if (item instanceof SplashPotionItem && targets.get("Other")) {
            results = checkTrajectory(new SimulationPotionEntity(mc.world, mc.player, stack), 0.55, yaw, pitch);
         } else if (item instanceof LingeringPotionItem && targets.get("Other")) {
            results = checkTrajectory(new SimulationPotionEntity(mc.world, mc.player, stack), 0.55, yaw, pitch);
         } else if (item instanceof TridentItem && item.equals(activeItem) && targets.get("Trident") && mc.player.getItemUseTime() >= 10) {
            results = checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5, yaw, pitch);
         } else if (item instanceof SnowballItem && targets.get("Other")) {
            results = checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5, yaw, pitch);
         } else if (item instanceof EggItem && targets.get("Other")) {
            results = checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5, yaw, pitch);
         } else if (item instanceof EnderPearlItem && targets.get("Ender Pearl")) {
            results = checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5, yaw, pitch);
         } else if (item instanceof BowItem && item.equals(activeItem) && targets.get("Arrows") && mc.player.isUsingItem()) {
            results = checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), getBowVelocity(), yaw, pitch);
         } else if (item instanceof CrossbowItem && CrossbowItem.isCharged(stack) && targets.get("Arrows")) {
            ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
            if (component != null) {
               float velocity = component.getProjectiles().get(0).isOf(Items.FIREWORK_ROCKET) ? 100 : 3;
               results = new ArrayList<>();
               results.add(checkSingleTrajectory(angleToVector(yaw, pitch), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
               if (component.getProjectiles().size() > 2) {
                  float pitchAbs = pitch / 90.0F;
                  float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                  float yawSpread = MathHelper.lerp(Math.abs(delta), 10, 90);
                  float pitchSpread = MathHelper.lerp(delta, 0, 10);
                  results.add(checkSingleTrajectory(angleToVector(yaw - yawSpread, pitch - pitchSpread), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                  results.add(checkSingleTrajectory(angleToVector(yaw + yawSpread, pitch - pitchSpread), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
               }
            }
         }

         if (results != null) {
            boolean isPotion = stack.getItem() instanceof SplashPotionItem || stack.getItem() instanceof LingeringPotionItem;
            double potionRad = stack.getItem() instanceof LingeringPotionItem ? LINGERING_POTION_RADIUS : SPLASH_POTION_RADIUS;
            for (HitResult r : results) {
               if (r != null) {
                  handPredictions.add(new PredictionResult(r, isPotion, potionRad));
               }
            }
            return;
         }
      }
   }

   private List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity, float yaw, float pitch) {
      return new ArrayList<>(Collections.singleton(checkSingleTrajectory(angleToVector(yaw, pitch), entity, velocity)));
   }

   private HitResult checkSingleTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
      float sqrt = MathHelper.sqrt((float) lookVec.lengthSquared());
      Vec3d playerVel = mc.player.getVelocity();
      Vec3d motion = new Vec3d(playerVel.x, mc.player.isOnGround() ? 0.0 : playerVel.y, playerVel.z);
      float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
      return traceTrajectory(mc.player.getCameraPosVec(tickDelta), lookVec.multiply(velocity / sqrt).add(motion), entity);
   }

   private HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
      for (int i = 0; i < 300; i++) {
         Vec3d prevPos = pos;
         Vec3d nextPos = pos.add(motion);
         HitResult result = getCollision(entity, prevPos, nextPos);
         if (result != null && !result.getType().equals(HitResult.Type.MISS)) {
            return result;
         }
         pos = nextPos;
         if (pos.y < -128) break;
         motion = calculateMotion(entity, pos, motion);
      }
      return null;
   }

   private HitResult getCollision(Entity entity, Vec3d prevPos, Vec3d pos) {
      HitResult blockResult = mc.world.raycast(new RaycastContext(
         prevPos, pos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, entity
      ));
      EntityHitResult entityResult = getEntityCollision(entity, prevPos, pos);
      if (entityResult != null && (blockResult == null || blockResult.getType().equals(HitResult.Type.MISS)
            || prevPos.squaredDistanceTo(entityResult.getPos()) < prevPos.squaredDistanceTo(blockResult.getPos()))) {
         return entityResult;
      }
      return blockResult;
   }

   private EntityHitResult getEntityCollision(Entity entity, Vec3d prevPos, Vec3d pos) {
      Entity owner = entity instanceof ProjectileEntity projectile ? projectile.getOwner() : null;
      EntityHitResult closest = null;
      double closestDistance = Double.MAX_VALUE;
      for (Entity target : mc.world.getEntities()) {
         if (!(target instanceof LivingEntity living) || living == owner || living == entity || living == mc.player || !living.isAlive() || living.isSpectator()) {
            continue;
         }
         var hitPos = target.getBoundingBox().expand(0.3).raycast(prevPos, pos).orElse(null);
         if (hitPos == null) continue;
         double distance = prevPos.squaredDistanceTo(hitPos);
         if (distance < closestDistance) {
            closestDistance = distance;
            closest = new EntityHitResult(target, hitPos);
         }
      }
      return closest;
   }

   private Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
      boolean isInWater = mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
      double multiply;
      if (entity instanceof TridentEntity) {
         multiply = 0.99;
      } else if (entity instanceof PersistentProjectileEntity && isInWater) {
         multiply = 0.6;
      } else {
         multiply = isInWater ? 0.8 : 0.99;
      }
      return motion.multiply(multiply).add(0, -entity.getFinalGravity(), 0);
   }

   private double getBowVelocity() {
      float tickDelta = mc.getRenderTickCounter().getTickProgress(false);
      float pull = (mc.player.getItemUseTime() + tickDelta) / 20.0F;
      pull = (pull * pull + pull * 2.0F) / 3.0F;
      return MathHelper.clamp(pull, 0.0F, 1.0F) * 3.0F;
   }

   private void drawLine3D(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Vec3d from, Vec3d to, int color) {
      consumer.vertex(matrix,
         (float) (from.x - camera.x), (float) (from.y - camera.y), (float) (from.z - camera.z)
      ).color(color);
      consumer.vertex(matrix,
         (float) (to.x - camera.x), (float) (to.y - camera.y), (float) (to.z - camera.z)
      ).color(color);
   }

   private void renderImpactCircle(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Vec3d pos, Direction direction, int color) {
      double width = 0.3;
      int segments = 90;
      for (int i = 0; i <= segments; i++) {
         double angle1 = Math.PI * 2.0 * i / segments;
         double angle2 = Math.PI * 2.0 * (i + 1) / segments;
         Vec3d p1 = getCirclePoint(pos, direction, width, angle1);
         Vec3d p2 = getCirclePoint(pos, direction, width, angle2);
         consumer.vertex(matrix,
            (float) (p1.x - camera.x), (float) (p1.y - camera.y), (float) (p1.z - camera.z)
         ).color(color);
         consumer.vertex(matrix,
            (float) (p2.x - camera.x), (float) (p2.y - camera.y), (float) (p2.z - camera.z)
         ).color(color);
      }
   }

   private Vec3d getCirclePoint(Vec3d center, Direction direction, double radius, double angle) {
      double cos = Math.cos(angle) * radius;
      double sin = Math.sin(angle) * radius;
      return switch (direction) {
         case UP, DOWN -> new Vec3d(center.x + cos, center.y, center.z + sin);
         case NORTH, SOUTH -> new Vec3d(center.x + cos, center.y + sin, center.z);
         case EAST, WEST -> new Vec3d(center.x, center.y + sin, center.z + cos);
      };
   }

   private void renderPotionImpact3D(VertexConsumer consumer, Matrix4f matrix, Vec3d camera, Vec3d pos, double radius) {
      if (!targets.get("Potion radius")) return;

      int baseColor = ColorUtil.fade();
      List<LivingEntity> affected = getAffectedByPotion(pos, radius);
      int circleColor = affected.isEmpty() ? baseColor : ColorUtil.GREEN;
      double y = pos.y + 0.025;

      for (int i = 0; i < POTION_CIRCLE_SEGMENTS; i++) {
         double a1 = Math.PI * 2.0 * i / POTION_CIRCLE_SEGMENTS;
         double a2 = Math.PI * 2.0 * (i + 1) / POTION_CIRCLE_SEGMENTS;
         Vec3d first = new Vec3d(pos.x + Math.cos(a1) * radius, y, pos.z + Math.sin(a1) * radius);
         Vec3d second = new Vec3d(pos.x + Math.cos(a2) * radius, y, pos.z + Math.sin(a2) * radius);
         drawLine3D(consumer, matrix, camera, first, second, circleColor);
      }
   }

   private List<LivingEntity> getAffectedByPotion(Vec3d pos, double radius) {
      double radiusSq = radius * radius;
      List<LivingEntity> result = new ArrayList<>();
      for (Entity entity : mc.world.getEntities()) {
         if (entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && living != mc.player) {
            if (living.squaredDistanceTo(pos) <= radiusSq) {
               result.add(living);
            }
         }
      }
      return result;
   }

   private List<Vec3d> smoothRenderPath(List<Vec3d> rawPoints) {
      if (rawPoints.size() < 3) return rawPoints;
      ArrayList<Vec3d> smoothed = new ArrayList<>(rawPoints.size() * 4);
      smoothed.add(rawPoints.get(0));
      for (int i = 0; i < rawPoints.size() - 1; i++) {
         Vec3d p0 = rawPoints.get(Math.max(0, i - 1));
         Vec3d p1 = rawPoints.get(i);
         Vec3d p2 = rawPoints.get(i + 1);
         Vec3d p3 = rawPoints.get(Math.min(rawPoints.size() - 1, i + 2));
         double segmentLength = p1.distanceTo(p2);
         int steps = MathHelper.clamp((int) Math.ceil(segmentLength * 14.0), 4, 16);
         for (int step = 1; step <= steps; step++) {
            float t = (float) step / (float) steps;
            smoothed.add(catmullRom(p0, p1, p2, p3, t));
         }
      }
      return smoothed;
   }

   private Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, float t) {
      double t2 = t * t;
      double t3 = t2 * t;
      double x = 0.5 * ((2.0 * p1.x) + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
      double y = 0.5 * ((2.0 * p1.y) + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
      double z = 0.5 * ((2.0 * p1.z) + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
      return new Vec3d(x, y, z);
   }

   private Vec3d angleToVector(float yaw, float pitch) {
      float yawRad = (float) Math.toRadians(-yaw - 180.0F);
      float pitchRad = (float) Math.toRadians(-pitch);
      float cosP = MathHelper.cos(pitchRad);
      return new Vec3d(MathHelper.sin(yawRad) * cosP, MathHelper.sin(pitchRad), MathHelper.cos(yawRad) * cosP);
   }

   private Vec3d getLerpedPos(Entity entity, float tickDelta) {
      return new Vec3d(
         MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX()),
         MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY()),
         MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ())
      );
   }

   private Direction getDirection(HitResult result) {
      if (result instanceof BlockHitResult blockHitResult) {
         return blockHitResult.getSide();
      }
      return Direction.getFacing(result.getPos().subtract(mc.player.getEyePos()).normalize());
   }

   private double getPotionRadius(PotionEntity potion) {
      return potion instanceof net.minecraft.entity.projectile.thrown.LingeringPotionEntity ? LINGERING_POTION_RADIUS : SPLASH_POTION_RADIUS;
   }

   private void recordImpactPoint(Entity entity, Vec3d pos, int ticks) {
      if (entity instanceof EnderPearlEntity pearl) {
         PlayerEntity owner = pearl.getOwner() instanceof PlayerEntity p ? p : null;
         String name = owner != null ? owner.getNameForScoreboard() : "";
         impactPoints.add(new ImpactPoint(pearl.getStack(), pos, ticks, name));
      } else if (entity instanceof ItemEntity item) {
         impactPoints.add(new ImpactPoint(item.getStack(), pos, ticks, null));
      } else if (entity instanceof ThrownItemEntity thrown) {
         impactPoints.add(new ImpactPoint(thrown.getStack(), pos, ticks, null));
      } else if (entity instanceof PersistentProjectileEntity persistent) {
         impactPoints.add(new ImpactPoint(persistent.getItemStack(), pos, ticks, null));
      }
   }

   private boolean isTrackedProjectile(Entity entity) {
      if (entity instanceof EnderPearlEntity) return targets.get("Ender Pearl");
      if (entity instanceof TridentEntity) return targets.get("Trident");
      if (entity instanceof ItemEntity) return targets.get("Items");
      if (entity instanceof PersistentProjectileEntity) return targets.get("Arrows");
      return entity instanceof ThrownItemEntity && !(entity instanceof EnderPearlEntity) && targets.get("Other");
   }

   private boolean isStationary(Entity entity) {
      boolean noVelocity = entity.getVelocity().lengthSquared() <= 1.0E-7;
      boolean itemOnGround = entity instanceof ItemEntity && (entity.isOnGround() || isInWater(entity));
      return noVelocity || itemOnGround;
   }

   private boolean isInWater(Entity entity) {
      return mc.world.getBlockState(BlockPos.ofFloored(entity.getX(), entity.getY(), entity.getZ())).isOf(Blocks.WATER);
   }

   private record ImpactPoint(ItemStack stack, Vec3d pos, int ticks, String ownerName) {}
   private record PredictionResult(HitResult result, boolean isPotionStack, double potionRadius) {}

   private static class SimulationPotionEntity extends net.minecraft.entity.projectile.thrown.PotionEntity {
       
       @SuppressWarnings("unchecked")
       public SimulationPotionEntity(net.minecraft.world.World world, net.minecraft.entity.LivingEntity owner, net.minecraft.item.ItemStack stack) {
           super((EntityType<? extends PotionEntity>) Registries.ENTITY_TYPE.get(Identifier.of("minecraft", "potion")), world);
           this.setOwner(owner);
           this.setItem(stack);
       }

       @Override
       protected net.minecraft.item.Item getDefaultItem() {
           return net.minecraft.item.Items.SPLASH_POTION;
       }

       @Override
       protected void spawnAreaEffectCloud(net.minecraft.server.world.ServerWorld world, net.minecraft.item.ItemStack stack, net.minecraft.util.hit.HitResult hitResult) {
       }
   }
}
