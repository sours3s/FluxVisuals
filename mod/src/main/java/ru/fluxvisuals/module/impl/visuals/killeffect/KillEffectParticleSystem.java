package ru.fluxvisuals.module.impl.visuals.killeffect;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;
import ru.fluxvisuals.util.render.world.WorldRenderUtil;
import ru.fluxvisuals.util.render.world.WorldRenderer;

/** Осколки тела: палитра по силуэту, физика, стеклянные грани и свечение кромок. */
@Environment(EnvType.CLIENT)
public final class KillEffectParticleSystem {
   private static final int MAX_PARTICLES = 520;
   private static final float STEP_MS = 50.0F;
   private static final int MAX_STEPS_PER_FRAME = 4;
   private static final float GRAVITY_PER_STEP = 0.0135F;
   private static final float DRAG_PER_STEP = 0.986F;
   private static final float FADE_TICKS = 28.0F;
   private static final double MAX_RENDER_DISTANCE_SQ = 2304.0;

   private static final Identifier BLOOM_TEXTURE = Identifier.of("fluxvisuals", "textures/world/glow.png");
   private static final RenderLayer BLOOM_LAYER = WorldRenderLayers.TEXTURED_QUADS_ADDITIVE(BLOOM_TEXTURE);

   private final List<Particle> particles = new ArrayList<>();
   private final Random random = new Random();
   private int color = 0xFFFFFF;
   private long lastTick = System.currentTimeMillis();
   private double stepRemainder;

   public void setColor(int color) {
      this.color = color & 0x00FFFFFF;
   }

   public void spawnBurst(Vec3d pos, int count, float speed, float spread, int life, float size) {
      this.spawnBurst(pos, count, speed, spread, life, size, this.color);
   }

   public void spawnBurst(Vec3d pos, int count, float speed, float spread, int life, float size, int color) {
      if (pos == null || count <= 0) {
         return;
      }
      float force = Math.max(0.05F, speed * (1.0F + Math.max(0.0F, spread) * 0.08F));
      this.spawnBody(pos, count, force, size, life, color, false);
   }

   public void spawnShards(Vec3d pos, int count, float force, float size, int life, int color) {
      if (pos == null || count <= 0) {
         return;
      }
      this.spawnBody(pos, count, Math.max(0.05F, force), size, life, color, true);
   }

   private void spawnBody(Vec3d pos, int count, float force, float size, int lifeMs, int rgb, boolean glass) {
      int spawnCount = Math.min(count, MAX_PARTICLES);
      this.ensureCapacity(spawnCount);
      BodyProfile profile = this.profileAt(pos, rgb);
      double centerY = pos.y + profile.height() * 0.52;
      float power = Math.max(0.05F, force);
      float sizeMultiplier = MathHelper.clamp(size / 0.043F, 0.35F, 6.0F);
      float baseLifeTicks = Math.max(1.0F, Math.max(50, lifeMs) / STEP_MS);
      long now = System.currentTimeMillis();

      for (int i = 0; i < spawnCount; i++) {
         float vertical = this.random.nextFloat();
         float silhouette = bodySilhouette(profile.entity(), vertical);
         double localX = (this.random.nextDouble() * 2.0 - 1.0) * profile.width() * 0.48 * silhouette;
         double localZ = (this.random.nextDouble() * 2.0 - 1.0) * profile.width() * 0.4 * silhouette;
         double x = pos.x + localX;
         double y = pos.y + 0.05 + vertical * Math.max(0.2F, profile.height() - 0.1F);
         double z = pos.z + localZ;
         double radialLength = Math.max(0.08, Math.sqrt(localX * localX + localZ * localZ));
         double outward = (0.014 + this.random.nextDouble() * 0.022) * power;
         double velocityNoise = 0.016 * power;
         double vx = localX / radialLength * outward + (this.random.nextDouble() - 0.5) * velocityNoise;
         double verticalForce = Math.sqrt(power);
         double vy = (0.018 + this.random.nextDouble() * 0.052) * verticalForce
               + Math.max(0.0, y - centerY) * 0.004;
         double vz = localZ / radialLength * outward + (this.random.nextDouble() - 0.5) * velocityNoise;

         float jitter = 0.92F + this.random.nextFloat() * 0.16F;
         float shardSize = 0.043F * sizeMultiplier * jitter;
         if (glass) {
            shardSize *= 1.35F;
         }
         int baseColor = profile.palette()[this.random.nextInt(profile.palette().length)];
         int shardColor = varyColor(baseColor, 0.82F + this.random.nextFloat() * 0.3F);
         int shardLife = Math.max(1, Math.round(baseLifeTicks * jitter));
         Kind kind = this.random.nextBoolean() ? Kind.TRIANGLE : Kind.QUAD;
         float[][] vertices = null;
         if (kind == Kind.TRIANGLE) {
            vertices = triangleVertices(shardSize);
         } else if (kind == Kind.QUAD) {
            vertices = quadVertices(shardSize);
         }

         this.particles.add(new Particle(
               x, y, z, vx, vy, vz, shardSize, shardColor, now + Math.round(lifeMs * jitter), shardLife,
               kind, vertices,
               this.random.nextFloat() * 360.0F, this.random.nextFloat() * 360.0F,
               this.random.nextFloat() * 360.0F,
               (this.random.nextFloat() - 0.5F) * 16.0F,
               (this.random.nextFloat() - 0.5F) * 16.0F,
               (this.random.nextFloat() - 0.5F) * 16.0F,
               this.random.nextFloat() * 6.2831853F
         ));
      }
   }

   private void ensureCapacity(int incoming) {
      int overflow = this.particles.size() + incoming - MAX_PARTICLES;
      if (overflow > 0) {
         this.particles.subList(0, Math.min(overflow, this.particles.size())).clear();
      }
   }

   private BodyProfile profileAt(Vec3d pos, int fallbackColor) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client != null && client.world != null) {
         Box search = new Box(pos.x - 1.5, pos.y - 0.5, pos.z - 1.5,
               pos.x + 1.5, pos.y + 2.5, pos.z + 1.5);
         LivingEntity best = null;
         double bestDistance = Double.MAX_VALUE;
         for (LivingEntity entity : client.world.getEntitiesByClass(LivingEntity.class, search,
               candidate -> client.player == null || candidate != client.player)) {
            double distance = entity.getEntityPos().distanceTo(pos);
            if (distance < bestDistance) {
               best = entity;
               bestDistance = distance;
            }
         }
         if (best != null && bestDistance <= 1.6) {
            return new BodyProfile(Math.max(0.45F, best.getWidth()), Math.max(0.65F, best.getHeight()),
                  best, bodyPalette(best, fallbackColor));
         }
      }
      return new BodyProfile(0.6F, 1.8F, null, bodyPalette(null, fallbackColor));
   }

   private static float[][] triangleVertices(float size) {
      float s = size;
      return new float[][] {
            {randomBetween(-s, -s * 0.2F), randomBetween(s * 0.3F, s), 0.0F},
            {randomBetween(-s, -s * 0.1F), randomBetween(-s, -s * 0.2F), 0.0F},
            {randomBetween(s * 0.2F, s), randomBetween(-s * 0.5F, s * 0.5F), 0.0F}
      };
   }

   private static float[][] quadVertices(float size) {
      float s = size;
      return new float[][] {
            {randomBetween(-s, -s * 0.2F), randomBetween(s * 0.2F, s), 0.0F},
            {randomBetween(-s, -s * 0.1F), randomBetween(-s, -s * 0.2F), 0.0F},
            {randomBetween(s * 0.1F, s), randomBetween(-s, -s * 0.1F), 0.0F},
            {randomBetween(s * 0.2F, s), randomBetween(s * 0.1F, s), 0.0F}
      };
   }

   private static float randomBetween(float min, float max) {
      return min + (float) Math.random() * (max - min);
   }

   public void tick() {
      long now = System.currentTimeMillis();
      this.cleanup(now);
      long elapsed = Math.max(0L, now - this.lastTick);
      this.lastTick = now;
      this.stepRemainder = Math.min(MAX_STEPS_PER_FRAME,
            this.stepRemainder + elapsed / (double) STEP_MS);
      if (this.stepRemainder < 1.0) {
         return;
      }
      MinecraftClient client = MinecraftClient.getInstance();
      Box playerBox = client != null && client.player != null ? client.player.getBoundingBox() : null;
      Vec3d playerVelocity = client != null && client.player != null
            ? client.player.getVelocity() : Vec3d.ZERO;
      while (this.stepRemainder >= 1.0) {
         this.updateStep(now, playerBox, playerVelocity);
         this.stepRemainder -= 1.0;
      }
   }

   private void updateStep(long now, Box playerBox, Vec3d playerVelocity) {
      for (int i = this.particles.size() - 1; i >= 0; i--) {
         Particle shard = this.particles.get(i);
         shard.prevX = shard.x;
         shard.prevY = shard.y;
         shard.prevZ = shard.z;
         shard.prevRotX = shard.rotX;
         shard.prevRotY = shard.rotY;
         shard.prevRotZ = shard.rotZ;
         shard.life--;
         if (shard.life <= 0 || shard.isExpired(now)) {
            this.particles.remove(i);
            continue;
         }
         if (playerBox != null) {
            pushShardFromPlayer(shard, playerBox, playerVelocity);
         }
         if (!shard.grounded) {
            shard.vy -= GRAVITY_PER_STEP;
            shard.vx *= DRAG_PER_STEP;
            shard.vz *= DRAG_PER_STEP;
            double nextX = shard.x + shard.vx;
            double nextY = shard.y + shard.vy;
            double nextZ = shard.z + shard.vz;
            double floor = collisionTop(nextX, nextY - shard.size, nextZ);
            if (Double.isFinite(floor)
                  && shard.y - shard.size >= floor - 0.12
                  && nextY - shard.size <= floor + 0.035) {
               nextY = floor + shard.size;
               shard.vy = -shard.vy * 0.24;
               shard.vx *= 0.62;
               shard.vz *= 0.62;
               if (Math.abs(shard.vy) < 0.018) {
                  shard.vy = 0.0;
                  shard.grounded = true;
               }
            }
            shard.x = nextX;
            shard.y = nextY;
            shard.z = nextZ;
         } else {
            shard.vx *= 0.72;
            shard.vz *= 0.72;
            shard.x += shard.vx;
            shard.z += shard.vz;
            double floor = collisionTop(shard.x, shard.y - shard.size - 0.05, shard.z);
            if (Double.isFinite(floor)) {
               shard.y = floor + shard.size;
            } else {
               shard.grounded = false;
            }
         }
         float spin = shard.grounded ? 0.28F : 1.0F;
         shard.rotX += shard.spinX * spin;
         shard.rotY += shard.spinY * spin;
         shard.rotZ += shard.spinZ * spin;
         if (shard.grounded) {
            shard.spinX *= 0.82F;
            shard.spinY *= 0.82F;
            shard.spinZ *= 0.82F;
         }
      }
   }

   private static void pushShardFromPlayer(Particle shard, Box playerBox, Vec3d playerVelocity) {
      double margin = shard.size + 0.1;
      if (shard.x < playerBox.minX - margin || shard.x > playerBox.maxX + margin
            || shard.z < playerBox.minZ - margin || shard.z > playerBox.maxZ + margin
            || shard.y + shard.size < playerBox.minY - 0.08
            || shard.y - shard.size > playerBox.maxY + 0.08) {
         return;
      }
      double centerX = (playerBox.minX + playerBox.maxX) * 0.5;
      double dx = shard.x - centerX;
      double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5;
      double dz = shard.z - centerZ;
      double length = Math.sqrt(dx * dx + dz * dz);
      if (length < 1.0E-4) {
         double movement = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);
         if (movement > 1.0E-4) {
            dx = playerVelocity.x / movement;
            dz = playerVelocity.z / movement;
         } else {
            double angle = Math.toRadians(shard.rotY);
            dx = Math.cos(angle);
            dz = Math.sin(angle);
         }
         length = 1.0;
      }
      double movementSpeed = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);
      double push = 0.024 + Math.min(0.075, movementSpeed * 0.55);
      double nx = dx / length;
      double nz = dz / length;
      shard.vx = MathHelper.clamp(shard.vx * 0.58 + nx * push + playerVelocity.x * 0.28, -0.16, 0.16);
      shard.vz = MathHelper.clamp(shard.vz * 0.58 + nz * push + playerVelocity.z * 0.28, -0.16, 0.16);
      shard.spinX += (float) (-nz * 1.8);
      shard.spinZ += (float) (nx * 1.8);
   }

   private static double collisionTop(double x, double bottomY, double z) {
      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.world == null) {
         return Double.NaN;
      }
      BlockPos pos = BlockPos.ofFloored(x, bottomY, z);
      BlockState state = client.world.getBlockState(pos);
      if (state.isAir()) {
         return Double.NaN;
      }
      VoxelShape shape = state.getCollisionShape(client.world, pos);
      return shape.isEmpty() ? Double.NaN : pos.getY() + shape.getMax(Axis.Y);
   }

   public void render(WorldRenderer renderer) {
      if (renderer == null) {
         return;
      }
      long now = System.currentTimeMillis();
      this.cleanup(now);
      if (this.particles.isEmpty()) {
         return;
      }
      MatrixStack matrices = renderer.matrixStack();
      VertexConsumer solid = renderer.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS());
      VertexConsumer bloom = renderer.getBuffer(BLOOM_LAYER);
      Vec3d camera = renderer.camera().getCameraPos();
      Camera view = renderer.camera();
      float tickDelta = renderer.tickDelta();

      for (Particle shard : this.particles) {
         if (shard.isExpired(now)) {
            continue;
         }
         double posX = lerp(shard.prevX, shard.x, tickDelta) - camera.x;
         double posY = lerp(shard.prevY, shard.y, tickDelta) - camera.y;
         double posZ = lerp(shard.prevZ, shard.z, tickDelta) - camera.z;
         if (posX * posX + posY * posY + posZ * posZ > MAX_RENDER_DISTANCE_SQ) {
            continue;
         }
         float fade = MathHelper.clamp(shard.life / FADE_TICKS, 0.0F, 1.0F);
         int alpha = MathHelper.clamp(Math.round(235.0F * smoothStep(fade)), 0, 235);
         if (alpha <= 0) {
            continue;
         }
         int rgb = shiftHue(shard.color, now, shard.huePhase);
         float rotX = (float) lerp(shard.prevRotX, shard.rotX, tickDelta);
         float rotY = (float) lerp(shard.prevRotY, shard.rotY, tickDelta);
         float rotZ = (float) lerp(shard.prevRotZ, shard.rotZ, tickDelta);

         matrices.push();
         matrices.translate(posX, posY, posZ);
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         appendGlassFace(solid, matrix, shard, rgb, alpha);
         appendVertexGlow(matrices, bloom, shard, view, rgb, alpha, rotX, rotY, rotZ);
         matrices.pop();

         appendShardBloom(matrices, bloom, shard, posX, posY, posZ, view, rgb, alpha);
      }
   }

   private static void appendGlassFace(VertexConsumer buffer, Matrix4f m, Particle shard, int rgb, int alpha) {
      float[][] v = shard.vertices;
      int back = varyColor(rgb, 0.68F);
      if (shard.kind == Kind.TRIANGLE) {
         writeTriangle(buffer, m,
               v[0][0], v[0][1], v[0][2],
               v[1][0], v[1][1], v[1][2],
               v[2][0], v[2][1], v[2][2], rgb, alpha);
         writeTriangle(buffer, m,
               v[2][0], v[2][1], v[2][2],
               v[1][0], v[1][1], v[1][2],
               v[0][0], v[0][1], v[0][2], back, alpha);
      } else {
         writeQuad(buffer, m,
               v[0][0], v[0][1], v[0][2],
               v[1][0], v[1][1], v[1][2],
               v[2][0], v[2][1], v[2][2],
               v[3][0], v[3][1], v[3][2], rgb, alpha);
         writeQuad(buffer, m,
               v[3][0], v[3][1], v[3][2],
               v[2][0], v[2][1], v[2][2],
               v[1][0], v[1][1], v[1][2],
               v[0][0], v[0][1], v[0][2], back, alpha);
      }
   }

   private static void appendShardBloom(MatrixStack matrices, VertexConsumer bloom, Particle shard,
         double posX, double posY, double posZ, Camera view, int rgb, int alpha) {
      matrices.push();
      matrices.translate(posX, posY, posZ);
      matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-view.getYaw()));
      matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(view.getPitch()));
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      float baseSize = shard.size * 2.4F;
      int bloomAlpha = Math.max(1, Math.round(alpha * 0.35F));
      for (int i = 0; i < 2; i++) {
         float size = baseSize * (0.5F + i * 0.6F);
         WorldRenderUtil.drawGlow(bloom, matrix, rgb, Math.max(1, bloomAlpha / (i + 1)), size);
      }
      matrices.pop();
   }

   private static void appendVertexGlow(MatrixStack matrices, VertexConsumer bloom, Particle shard,
         Camera view, int rgb, int alpha, float rotX, float rotY, float rotZ) {
      int dotAlpha = Math.round(alpha * 0.25F);
      if (dotAlpha <= 0 || shard.vertices == null) {
         return;
      }
      float dotSize = shard.size * 0.2F;
      for (float[] vertex : shard.vertices) {
         matrices.push();
         matrices.translate(vertex[0], vertex[1], vertex[2]);
         matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-rotZ));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-rotX));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-rotY));
         matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-view.getYaw()));
         matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(view.getPitch()));
         WorldRenderUtil.drawGlow(bloom, matrices.peek().getPositionMatrix(), rgb, dotAlpha, dotSize * 2.0F);
         matrices.pop();
      }
   }

   private static void writeQuad(VertexConsumer buffer, Matrix4f m,
         float x1, float y1, float z1,
         float x2, float y2, float z2,
         float x3, float y3, float z3,
         float x4, float y4, float z4, int rgb, int alpha) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      buffer.vertex(m, x1, y1, z1).color(r, g, b, alpha);
      buffer.vertex(m, x2, y2, z2).color(r, g, b, alpha);
      buffer.vertex(m, x3, y3, z3).color(r, g, b, alpha);
      buffer.vertex(m, x4, y4, z4).color(r, g, b, alpha);
   }

   private static void writeTriangle(VertexConsumer buffer, Matrix4f m,
         float x1, float y1, float z1,
         float x2, float y2, float z2,
         float x3, float y3, float z3, int rgb, int alpha) {
      int r = rgb >> 16 & 0xFF;
      int g = rgb >> 8 & 0xFF;
      int b = rgb & 0xFF;
      buffer.vertex(m, x1, y1, z1).color(r, g, b, alpha);
      buffer.vertex(m, x2, y2, z2).color(r, g, b, alpha);
      buffer.vertex(m, x3, y3, z3).color(r, g, b, alpha);
      buffer.vertex(m, x3, y3, z3).color(r, g, b, alpha);
   }

   private void cleanup(long now) {
      Iterator<Particle> iterator = this.particles.iterator();
      while (iterator.hasNext()) {
         if (iterator.next().isExpired(now)) {
            iterator.remove();
         }
      }
   }

   public void clear() {
      this.particles.clear();
      this.lastTick = System.currentTimeMillis();
      this.stepRemainder = 0.0;
   }

   private static float bodySilhouette(LivingEntity entity, float vertical) {
      String id = entity == null ? "" : Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
      boolean humanoid = entity == null || entity instanceof PlayerEntity
            || id.contains("zombie") || id.contains("skeleton") || id.contains("villager")
            || id.contains("piglin") || id.contains("witch");
      if (!humanoid) {
         return vertical > 0.78F ? 0.78F : 1.0F;
      } else if (vertical < 0.42F) {
         return 0.52F;
      }
      return vertical > 0.78F ? 0.76F : 1.0F;
   }

   private static int[] bodyPalette(LivingEntity entity, int fallbackColor) {
      String id = entity == null ? "" : Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
      if (id.contains("drowned")) {
         return new int[] {5077100, 6198661, 3244177, 3631746, 7952954};
      } else if (id.contains("zombie") || id.contains("husk")) {
         return new int[] {6061634, 7575117, 3232605, 4353400, 2505264};
      } else if (id.contains("skeleton") || id.contains("stray")) {
         return new int[] {14013119, 11184534, 7830904, 14934222, 5593952};
      } else if (id.contains("creeper")) {
         return new int[] {5020230, 6468955, 3042610, 7980650, 1522722};
      } else if (id.contains("enderman")) {
         return new int[] {1512219, 2629678, 3679805, 10172854};
      } else if (id.contains("spider")) {
         return new int[] {2892834, 4995122, 1512983, 10167592};
      } else if (id.contains("pig")) {
         return new int[] {14981786, 13203833, 15904936, 9262416};
      } else if (id.contains("cow")) {
         return new int[] {4796453, 6833978, 15130320, 2892061};
      } else if (id.contains("sheep")) {
         return new int[] {15000024, 13157562, 9210243, 4933186};
      } else if (id.contains("chicken")) {
         return new int[] {15658469, 13619144, 12135979, 14989371};
      } else if (id.contains("villager") || id.contains("witch")) {
         return new int[] {10118992, 7029560, 7755082, 12159600};
      } else if (id.contains("blaze") || id.contains("magma")) {
         return new int[] {15901995, 14248470, 16763978, 8205333};
      } else if (id.contains("slime")) {
         return new int[] {7980138, 5086800, 10739336, 3235383};
      } else if (entity instanceof PlayerEntity player) {
         int seed = player.getUuid().hashCode();
         int cloth = Color.HSBtoRGB(Math.floorMod(seed, 360) / 360.0F, 0.48F, 0.72F) & 0xFFFFFF;
         return new int[] {12946024, 11103826, 14723715, cloth, varyColor(cloth, 0.68F), 3420728};
      }
      int fallback = fallbackColor & 0xFFFFFF;
      return new int[] {fallback, varyColor(fallback, 0.72F), varyColor(fallback, 1.18F), 0x777777};
   }

   private static int shiftHue(int rgb, long now, float phase) {
      float shift = (float) Math.sin(now * 0.003 + phase) * 0.1F;
      float[] hsb = Color.RGBtoHSB(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, null);
      return Color.HSBtoRGB(hsb[0] + shift, hsb[1], hsb[2]) & 0xFFFFFF;
   }

   private static int varyColor(int rgb, float factor) {
      int r = MathHelper.clamp(Math.round((rgb >> 16 & 0xFF) * factor), 0, 255);
      int g = MathHelper.clamp(Math.round((rgb >> 8 & 0xFF) * factor), 0, 255);
      int b = MathHelper.clamp(Math.round((rgb & 0xFF) * factor), 0, 255);
      return r << 16 | g << 8 | b;
   }

   private static float smoothStep(float value) {
      float t = MathHelper.clamp(value, 0.0F, 1.0F);
      return t * t * (3.0F - 2.0F * t);
   }

   private static double lerp(double start, double end, float delta) {
      return start + (end - start) * delta;
   }

   private enum Kind {
      TRIANGLE,
      QUAD
   }

   private record BodyProfile(float width, float height, LivingEntity entity, int[] palette) {}

   private static final class Particle {
      private double x;
      private double y;
      private double z;
      private double prevX;
      private double prevY;
      private double prevZ;
      private double vx;
      private double vy;
      private double vz;
      private final float size;
      private final int color;
      private final long expiresAt;
      private int life;
      private float rotX;
      private float rotY;
      private float rotZ;
      private float prevRotX;
      private float prevRotY;
      private float prevRotZ;
      private float spinX;
      private float spinY;
      private float spinZ;
      private final float huePhase;
      private final Kind kind;
      private final float[][] vertices;
      private boolean grounded;

      private Particle(double x, double y, double z, double vx, double vy, double vz,
            float size, int color, long expiresAt, int life, Kind kind, float[][] vertices,
            float rotX, float rotY, float rotZ, float spinX, float spinY, float spinZ, float huePhase) {
         this.x = this.prevX = x;
         this.y = this.prevY = y;
         this.z = this.prevZ = z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.size = size;
         this.color = color;
         this.expiresAt = expiresAt;
         this.life = life;
         this.kind = kind;
         this.vertices = vertices;
         this.rotX = this.prevRotX = rotX;
         this.rotY = this.prevRotY = rotY;
         this.rotZ = this.prevRotZ = rotZ;
         this.spinX = spinX;
         this.spinY = spinY;
         this.spinZ = spinZ;
         this.huePhase = huePhase;
      }

      private boolean isExpired(long now) {
         return now >= this.expiresAt;
      }
   }
}
