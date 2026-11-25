package ru.fluxvisuals.module.impl.visuals;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.WorldRenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.world.WorldRenderLayers;

/**
 * Подсветка целевого блока: ванильный контур заменяется своим, сглаженным и
 * анимированным. Поверх можно рисовать обычную заливку или один из четырёх
 * процедурных эффектов. Все эффекты считаются на вершинах, без внешних текстур.
 */
@IModule(name = "Block Outline", description = "Подсветка целевого блока: контур, заливка и анимация", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BlockOutline extends Module {
   private static BlockOutline instance;

   public final BooleanSetting outline = new BooleanSetting("Outline", true);
   public final SliderSetting lineWidth = new SliderSetting("Line Width", 2.0F, 1.0F, 5.0F, 0.5F, false);
   public final BooleanSetting fill = new BooleanSetting("Fill", true);
   public final ModeSetting fillType = new ModeSetting("Fill Type", "Normal", "Normal", "Shader");
   public final SliderSetting fillAlpha = new SliderSetting("Fill Alpha", 0.3F, 0.05F, 1.0F, 0.05F, true)
         .hidden(() -> !fill.get() || !fillType.is("Normal"));
   public final ModeSetting shader = new ModeSetting("Shader", "Nebula", "Nebula", "Stars", "Web", "Plasma")
         .hidden(() -> !fill.get() || !fillType.is("Shader"));
   public final SliderSetting shaderSpeed = new SliderSetting("Shader Speed", 1.0F, 0.1F, 3.0F, 0.1F, false)
         .hidden(() -> !fill.get() || !fillType.is("Shader"));
   public final SliderSetting shaderAlpha = new SliderSetting("Shader Alpha", 1.0F, 0.1F, 1.0F, 0.05F, true)
         .hidden(() -> !fill.get() || !fillType.is("Shader"));
   public final ModeSetting animation = new ModeSetting("Animation", "None", "None", "Pulse", "Wave");
   public final BooleanSetting clientColor = new BooleanSetting("Client Color", true);
   public final HueSetting customColor = new HueSetting("Custom Color", 0.6F, 1.0F, 1.0F)
         .hidden(() -> clientColor.get());

   private Box smoothedBox;
   private long lastRenderNanos;
   private Vec3d cameraPos = Vec3d.ZERO;
   private float shaderTime;
   private Color shaderTint = Color.WHITE;

   public BlockOutline() {
      this.addSettings(new Setting[]{outline, lineWidth, fill, fillType, fillAlpha, shader,
            shaderSpeed, shaderAlpha, animation, clientColor, customColor});
      instance = this;
   }

   public static BlockOutline getInstance() {
      return instance;
   }

   @EventInit
   public void onWorld(WorldRenderEvent event) {
      if (mc == null || mc.world == null || mc.crosshairTarget == null
            || mc.crosshairTarget.getType() != HitResult.Type.BLOCK) {
         return;
      }
      BlockPos pos = ((BlockHitResult) mc.crosshairTarget).getBlockPos();
      var state = mc.world.getBlockState(pos);
      if (state == null || state.isAir()) {
         return;
      }

      cameraPos = mc.gameRenderer.getCamera().getCameraPos();
      Box target = state.getOutlineShape(mc.world, pos).getBoundingBox().offset(
            pos.getX() - cameraPos.x, pos.getY() - cameraPos.y, pos.getZ() - cameraPos.z);
      Box box = smooth(target);
      Color color = overlayColor();

      MatrixStack matrices = event.matrixStack();
      renderOverlay(matrices, primaryAnimation(box), color);
      if (animation.is("Wave")) {
         renderWave(matrices, box, color);
      }
   }

   private Box smooth(Box target) {
      long now = System.nanoTime();
      if (smoothedBox != null && lastRenderNanos != 0L
            && smoothedBox.getCenter().squaredDistanceTo(target.getCenter()) <= 9.0) {
         double dt = Math.min(0.05, (now - lastRenderNanos) / 1.0e9);
         lastRenderNanos = now;
         double k = 1.0 - Math.exp(-dt * 13.0);
         smoothedBox = new Box(
               lerp(smoothedBox.minX, target.minX, k), lerp(smoothedBox.minY, target.minY, k),
               lerp(smoothedBox.minZ, target.minZ, k), lerp(smoothedBox.maxX, target.maxX, k),
               lerp(smoothedBox.maxY, target.maxY, k), lerp(smoothedBox.maxZ, target.maxZ, k));
         return smoothedBox;
      }
      smoothedBox = target;
      lastRenderNanos = now;
      return target;
   }

   private Box primaryAnimation(Box box) {
      if (!animation.is("Pulse")) {
         return box;
      }
      double wave = (Math.sin(System.currentTimeMillis() / 260.0) + 1.0) * 0.5;
      return scaleBox(box, 1.0F + (float) wave * 0.08F);
   }

   private void renderOverlay(MatrixStack matrices, Box box, Color color) {
      matrices.push();
      BufferAllocator allocator = new BufferAllocator(524288);
      try {
         Immediate immediate = VertexConsumerProvider.immediate(allocator);
         Matrix4f matrix = matrices.peek().getPositionMatrix();
         if (fill.get()) {
            drawFill(immediate.getBuffer(WorldRenderLayers.POSITION_COLOR_QUADS()), matrix, box, color);
         }
         if (outline.get() && !fillType.is("Shader")) {
            addBoxLines(immediate.getBuffer(WorldRenderLayers.LINES_NO_DEPTH(lineWidth.get())),
                  matrix, box, color, 1.0F, lineWidth.get());
         }
         immediate.draw();
      } finally {
         allocator.close();
         matrices.pop();
      }
   }

   private void renderWave(MatrixStack matrices, Box box, Color color) {
      float phase = (float) (System.currentTimeMillis() % 1000L) / 1000.0F;
      Box expanded = scaleBox(box, 1.0F + phase * 0.3F);
      float fade = 1.0F - phase;
      matrices.push();
      BufferAllocator allocator = new BufferAllocator(65536);
      try {
         Immediate immediate = VertexConsumerProvider.immediate(allocator);
         addBoxLines(immediate.getBuffer(WorldRenderLayers.LINES_NO_DEPTH(Math.max(1.0, lineWidth.get() - 0.5))),
               matrices.peek().getPositionMatrix(), expanded, color, fade, Math.max(1.0F, lineWidth.get() - 0.5F));
         immediate.draw();
      } finally {
         allocator.close();
         matrices.pop();
      }
   }

   private void drawFill(VertexConsumer buffer, Matrix4f matrix, Box box, Color color) {
      if (fillType.is("Shader")) {
         addShaderBoxQuads(buffer, matrix, box, shaderAlpha.get());
      } else {
         addBoxQuads(buffer, matrix, box, color, fillAlpha.get());
      }
   }

   private Color overlayColor() {
      if (!clientColor.get()) {
         return customColor.getColor();
      }
      int argb = Renderer2D.ColorUtil.getMainColor(1, 1);
      return new Color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF);
   }

   // --- Геометрия -------------------------------------------------------

   private static void addBoxLines(VertexConsumer buf, Matrix4f m, Box b, Color color, float alpha, float width) {
      float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
      float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
      line(buf, m, x0, y0, z0, x1, y0, z0, color, alpha, width);
      line(buf, m, x0, y0, z0, x0, y1, z0, color, alpha, width);
      line(buf, m, x0, y0, z0, x0, y0, z1, color, alpha, width);
      line(buf, m, x1, y1, z1, x0, y1, z1, color, alpha, width);
      line(buf, m, x1, y1, z1, x1, y0, z1, color, alpha, width);
      line(buf, m, x1, y1, z1, x1, y1, z0, color, alpha, width);
      line(buf, m, x0, y1, z0, x1, y1, z0, color, alpha, width);
      line(buf, m, x1, y0, z0, x1, y0, z1, color, alpha, width);
      line(buf, m, x0, y0, z1, x1, y0, z1, color, alpha, width);
      line(buf, m, x0, y1, z1, x0, y1, z0, color, alpha, width);
      line(buf, m, x0, y1, z1, x0, y0, z1, color, alpha, width);
      line(buf, m, x1, y1, z0, x1, y0, z0, color, alpha, width);
   }

   private static void addBoxQuads(VertexConsumer buf, Matrix4f m, Box b, Color color, float alpha) {
      float x0 = (float) b.minX, y0 = (float) b.minY, z0 = (float) b.minZ;
      float x1 = (float) b.maxX, y1 = (float) b.maxY, z1 = (float) b.maxZ;
      quad(buf, m, color, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      quad(buf, m, color, alpha, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
      quad(buf, m, color, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      quad(buf, m, color, alpha, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0);
      quad(buf, m, color, alpha, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      quad(buf, m, color, alpha, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0);
   }

   private static Box scaleBox(Box box, float factor) {
      Vec3d center = box.getCenter();
      double hx = (box.maxX - box.minX) * 0.5 * factor;
      double hy = (box.maxY - box.minY) * 0.5 * factor;
      double hz = (box.maxZ - box.minZ) * 0.5 * factor;
      return new Box(center.x - hx, center.y - hy, center.z - hz,
            center.x + hx, center.y + hy, center.z + hz);
   }

   private static void line(VertexConsumer buf, Matrix4f m, float ax, float ay, float az,
         float bx, float by, float bz, Color color, float alpha, float width) {
      int a = (int) (color.getAlpha() * alpha);
      lineVertex(buf, m, ax, ay, az, color, a, width);
      lineVertex(buf, m, bx, by, bz, color, a, width);
   }

   private static void lineVertex(VertexConsumer buf, Matrix4f m, float x, float y, float z,
         Color color, int alpha, float width) {
      buf.vertex(m, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha)
            .normal(0.0F, 0.0F, 1.0F).lineWidth(width);
   }

   private static void quad(VertexConsumer buf, Matrix4f m, Color color, float alpha,
         float ax, float ay, float az, float bx, float by, float bz,
         float cx, float cy, float cz, float dx, float dy, float dz) {
      int a = (int) (color.getAlpha() * alpha);
      vertex(buf, m, ax, ay, az, color, a);
      vertex(buf, m, bx, by, bz, color, a);
      vertex(buf, m, cx, cy, cz, color, a);
      vertex(buf, m, dx, dy, dz, color, a);
   }

   private static void vertex(VertexConsumer buf, Matrix4f m, float x, float y, float z, Color color, int alpha) {
      buf.vertex(m, x, y, z).color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
   }

   // --- Процедурные заливки ---------------------------------------------

   private void addShaderBoxQuads(VertexConsumer buf, Matrix4f m, Box box, float alpha) {
      shaderTime = (float) (System.currentTimeMillis() % 100000L) / 1000.0F * shaderSpeed.get();
      shaderTint = overlayColor();
      float x0 = (float) box.minX, y0 = (float) box.minY, z0 = (float) box.minZ;
      float x1 = (float) box.maxX, y1 = (float) box.maxY, z1 = (float) box.maxZ;
      tessellateFace(buf, m, alpha, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);
      tessellateFace(buf, m, alpha, x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0);
      tessellateFace(buf, m, alpha, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);
      tessellateFace(buf, m, alpha, x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0);
      tessellateFace(buf, m, alpha, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
      tessellateFace(buf, m, alpha, x1, y0, z0, x1, y0, z1, x0, y0, z1, x0, y0, z0);
   }

   private void tessellateFace(VertexConsumer buf, Matrix4f m, float alpha,
         float ax, float ay, float az, float bx, float by, float bz,
         float dx, float dy, float dz, float ex, float ey, float ez) {
      final int res = 16;
      final int stride = res + 1;
      float[] xs = new float[stride * stride];
      float[] ys = new float[stride * stride];
      float[] zs = new float[stride * stride];
      Color[] colors = new Color[stride * stride];
      float seed = ax * 17.0F + ay * 31.0F + az * 47.0F + bx * 7.0F + by * 11.0F + bz * 13.0F;

      for (int row = 0; row <= res; row++) {
         float v = (float) row / res;
         for (int col = 0; col <= res; col++) {
            float u = (float) col / res;
            int i = row * stride + col;
            float px = bilerp(ax, bx, dx, ex, u, v);
            float py = bilerp(ay, by, dy, ey, u, v);
            float pz = bilerp(az, bz, dz, ez, u, v);
            xs[i] = px;
            ys[i] = py;
            zs[i] = pz;
            colors[i] = shaderVertex(px, py, pz, u, v, seed, alpha);
         }
      }

      for (int row = 0; row < res; row++) {
         for (int col = 0; col < res; col++) {
            int i0 = row * stride + col;
            int i1 = i0 + 1;
            int i3 = (row + 1) * stride + col;
            int i2 = i3 + 1;
            vertex(buf, m, xs[i0], ys[i0], zs[i0], colors[i0], colors[i0].getAlpha());
            vertex(buf, m, xs[i1], ys[i1], zs[i1], colors[i1], colors[i1].getAlpha());
            vertex(buf, m, xs[i2], ys[i2], zs[i2], colors[i2], colors[i2].getAlpha());
            vertex(buf, m, xs[i3], ys[i3], zs[i3], colors[i3], colors[i3].getAlpha());
         }
      }
   }

   private Color shaderVertex(float x, float y, float z, float u, float v, float seed, float alpha) {
      Color base = shader.is("Stars")
            ? stars(u, v, seed, shaderTime, alpha)
            : shaderColor(x + (float) cameraPos.x, y + (float) cameraPos.y, z + (float) cameraPos.z, alpha);
      float edge = Math.min(Math.min(u, 1.0F - u), Math.min(v, 1.0F - v));
      int a = Math.round(base.getAlpha() * clamp(edge * 28.0F, 0.0F, 1.0F));
      return new Color(base.getRed(), base.getGreen(), base.getBlue(), a);
   }

   private Color shaderColor(float x, float y, float z, float alpha) {
      if (shader.is("Nebula")) return nebula(x, y, z, alpha);
      if (shader.is("Web")) return web(x, y, z, alpha);
      if (shader.is("Plasma")) return plasma(x, y, z, alpha);
      return stars(x, y, z, shaderTime, alpha);
   }

   private static float bilerp(float a, float b, float c, float d, float u, float v) {
      return a * (1.0F - u) * (1.0F - v) + b * u * (1.0F - v) + c * u * v + d * (1.0F - u) * v;
   }

   private static double lerp(double a, double b, double t) {
      return a + (b - a) * t;
   }

   private static float clamp(float value, float min, float max) {
      return Math.max(min, Math.min(max, value));
   }

   private static float hash(float x, float y) {
      float n = (float) Math.sin(x * 12.9898F + y * 78.233F) * 43758.547F;
      return n - (float) Math.floor(n);
   }

   private static float smoothNoise(float x, float y) {
      float ix = (float) Math.floor(x);
      float iy = (float) Math.floor(y);
      float fx = x - ix;
      float fy = y - iy;
      fx = fx * fx * (3.0F - 2.0F * fx);
      fy = fy * fy * (3.0F - 2.0F * fy);
      float a = hash(ix, iy);
      float b = hash(ix + 1.0F, iy);
      float c = hash(ix, iy + 1.0F);
      float d = hash(ix + 1.0F, iy + 1.0F);
      return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
   }

   private static float fbm(float x, float y, int octaves) {
      float value = 0.0F;
      float amplitude = 0.5F;
      for (int i = 0; i < octaves; i++) {
         value += amplitude * smoothNoise(x, y);
         x *= 2.0F;
         y *= 2.0F;
         amplitude *= 0.5F;
      }
      return value;
   }

   private Color nebula(float x, float y, float z, float alpha) {
      float u = x * 2.15F + z * 1.35F;
      float v = y * 2.25F - z * 0.85F;
      float t = shaderTime * 0.22F;
      float warpX = fbm(u * 0.55F + t * 0.24F, v * 0.55F - t * 0.13F, 5);
      float warpY = fbm(u * 0.55F - t * 0.16F + 17.3F, v * 0.55F + t * 0.2F + 9.1F, 5);
      float clouds = fbm(u * 0.72F + warpX * 3.1F + t * 0.12F, v * 0.72F + warpY * 3.1F - t * 0.09F, 5);
      float wisps = fbm(u * 1.28F - warpY * 1.8F - t * 0.18F + 4.7F, v * 1.28F + warpX * 1.8F + t * 0.14F + 12.4F, 4);
      float violet = clamp((clouds - 0.24F) * 1.85F, 0.0F, 1.0F);
      float mint = clamp((warpX + wisps - 0.67F) * 1.55F, 0.0F, 1.0F);
      float pearl = clamp((wisps - 0.48F) * 2.5F, 0.0F, 1.0F);
      float r = 0.31F + violet * 0.37F + pearl * 0.24F;
      float g = 0.12F + violet * 0.18F + mint * 0.2F + pearl * 0.27F;
      float b = 0.58F + violet * 0.34F + mint * 0.1F + pearl * 0.18F;
      if (!clientColor.get()) {
         r *= 0.65F + shaderTint.getRed() / 255.0F * 0.35F;
         g *= 0.65F + shaderTint.getGreen() / 255.0F * 0.35F;
         b *= 0.65F + shaderTint.getBlue() / 255.0F * 0.35F;
      }
      float filament = clamp((clouds + wisps - 0.82F) * 1.65F, 0.0F, 1.0F);
      filament *= filament;
      float cloudAlpha = 0.135F + filament * 0.22F + pearl * 0.1F + mint * 0.06F;
      return rgb(r, g, b, clamp(alpha * cloudAlpha, 0.09F, 0.48F));
   }

   private Color stars(float u, float v, float faceSeed, float t, float alpha) {
      float slow = t * 0.035F;
      float wave = (float) Math.sin((u * 0.78F - v * 0.46F) * Math.PI + slow);
      float wave2 = (float) Math.sin((u + v) * 1.35F * Math.PI - slow * 0.57F + faceSeed * 0.03F);
      float purple = clamp(wave * 0.5F + 0.5F, 0.0F, 1.0F);
      purple *= purple * (0.48F + (wave2 * 0.5F + 0.5F) * 0.52F);
      float gx = (float) Math.floor(u * 32.0F + 0.5F);
      float gy = (float) Math.floor(v * 32.0F + 0.5F);
      float h = hash(gx + faceSeed * 2.17F, gy - faceSeed * 1.31F);
      float h2 = hash(gx * 2.73F - faceSeed, gy * 3.19F + faceSeed);
      float fine = clamp((h - 0.7F) / 0.3F, 0.0F, 1.0F);
      fine *= fine * 0.58F;
      float bright = clamp((h2 - 0.935F) / 0.065F, 0.0F, 1.0F);
      bright *= bright * (0.86F + 0.14F * (float) Math.sin(slow * 2.1F + h2 * 24.0F));
      float r = 0.03F + purple * 0.42F + bright * 0.3F + fine * 0.12F;
      float g = 0.035F + purple * 0.075F + bright * 0.28F + fine * 0.09F;
      float b = 0.055F + purple * 0.46F + bright * 0.35F + fine * 0.16F;
      return rgb(r, g, b, clamp(alpha * (0.15F + purple * 0.075F), 0.025F, 0.64F));
   }

   private Color web(float x, float y, float z, float alpha) {
      float u = (x + z * 0.72F) * 1.75F;
      float v = (y - z * 0.38F) * 1.75F;
      float slow = shaderTime * 0.075F;
      float warpX = fbm(u * 0.45F + slow, v * 0.45F - slow * 0.7F, 4) - 0.5F;
      float warpY = fbm(u * 0.45F - slow * 0.6F + 8.3F, v * 0.45F + slow + 3.7F, 4) - 0.5F;
      float pu = u + warpX * 1.05F;
      float pv = v + warpY * 1.05F;
      float d1 = (float) Math.sqrt((pu - 0.35F) * (pu - 0.35F) + (pv + 0.15F) * (pv + 0.15F));
      float d2 = (float) Math.sqrt((pu + 1.15F) * (pu + 1.15F) + (pv - 0.65F) * (pv - 0.65F));
      float ring1 = 1.0F - clamp(Math.abs((float) Math.sin(d1 * 4.1F - slow * 0.8F)) * 3.2F, 0.0F, 1.0F);
      float ring2 = 1.0F - clamp(Math.abs((float) Math.sin(d2 * 3.5F + slow * 0.55F)) * 3.5F, 0.0F, 1.0F);
      ring1 *= ring1;
      ring2 *= ring2;
      float strand = clamp(ring1 * 0.82F + ring2 * 0.62F, 0.0F, 1.0F);
      float haze = fbm(pu * 0.52F + 4.0F, pv * 0.52F - 7.0F, 5);
      float glow = clamp((haze - 0.38F) * 1.55F, 0.0F, 1.0F);
      float warm = clamp((fbm(pu * 0.31F + slow * 0.25F + 13.0F, pv * 0.31F - slow * 0.18F + 5.0F, 5) - 0.34F) * 2.0F, 0.0F, 1.0F);
      float cool = clamp((fbm(pu * 0.38F - 9.0F, pv * 0.38F + 16.0F, 4) - 0.4F) * 1.8F, 0.0F, 1.0F);
      float r = 0.16F + warm * 0.52F + glow * 0.16F + strand * 0.16F;
      float g = 0.14F + warm * 0.29F + cool * 0.13F + strand * 0.13F;
      float b = 0.17F + warm * 0.25F + cool * 0.22F + strand * 0.17F;
      return rgb(r, g, b, clamp(alpha * (0.34F + glow * 0.22F + strand * 0.27F), 0.26F, 0.82F));
   }

   private Color plasma(float x, float y, float z, float alpha) {
      float u = (x + z * 0.68F) * 1.85F;
      float v = (y - z * 0.32F) * 1.85F;
      float slow = shaderTime * 0.085F;
      float fold1 = (float) Math.sin(v * 2.35F + slow + Math.sin(u * 1.15F - slow * 0.45F) * 0.72F);
      float fold2 = (float) Math.sin(v * 4.1F - slow * 0.62F + Math.cos(u * 0.82F + slow * 0.3F) * 0.48F);
      float fold3 = (float) Math.cos((u + v * 0.42F) * 1.34F + slow * 0.38F);
      float noise = fbm(u * 0.42F + slow * 0.18F, v * 0.42F - slow * 0.13F, 4);
      float energy = clamp(0.48F + fold1 * 0.2F + fold2 * 0.1F + fold3 * 0.07F + (noise - 0.5F) * 0.2F, 0.0F, 1.0F);
      float ribbon = clamp(1.0F - Math.abs(fold1) * 1.65F, 0.0F, 1.0F);
      ribbon *= ribbon;
      float hot = clamp((energy - 0.64F) * 2.75F, 0.0F, 1.0F);
      float cycle = (float) Math.sin(slow * 0.72F + u * 0.24F) * 0.5F + 0.5F;
      float core = clamp(1.0F - energy * 1.32F, 0.0F, 1.0F);
      float edge = clamp((energy - 0.46F) * 2.35F, 0.0F, 1.0F);
      float shine = clamp(1.0F - Math.abs((float) Math.sin(v * 3.15F + slow * 0.9F + Math.sin(u * 0.9F) * 0.62F)) * 4.2F, 0.0F, 1.0F);
      shine *= shine;
      float r = 0.3F + edge * 0.62F + cycle * 0.18F + shine * 0.34F;
      float g = 0.075F + edge * (0.08F + cycle * 0.2F) + shine * 0.26F;
      float b = 0.24F + core * 0.34F + ribbon * 0.22F + shine * 0.3F + hot * 0.05F;
      return rgb(r, g, b, clamp(alpha * (0.36F + edge * 0.23F + ribbon * 0.1F + shine * 0.08F), 0.3F, 0.77F));
   }

   private static Color rgb(float r, float g, float b, float a) {
      return new Color(Math.round(clamp(r, 0.0F, 1.0F) * 255.0F),
            Math.round(clamp(g, 0.0F, 1.0F) * 255.0F),
            Math.round(clamp(b, 0.0F, 1.0F) * 255.0F),
            Math.round(clamp(a, 0.0F, 1.0F) * 255.0F));
   }
}
