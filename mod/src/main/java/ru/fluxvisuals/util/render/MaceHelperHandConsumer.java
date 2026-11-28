package ru.fluxvisuals.util.render;

import java.awt.Color;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack.Entry;

public class MaceHelperHandConsumer implements VertexConsumer {
   private final VertexConsumer delegate;
   private final Color color;

   public MaceHelperHandConsumer(VertexConsumer delegate, Color color) {
      this.delegate = delegate;
      this.color = color;
   }

   public VertexConsumer vertex(float x, float y, float z) {
      return this.delegate.vertex(x, y, z);
   }

   public VertexConsumer color(int red, int green, int blue, int alpha) {
      float factor = 0.95F;
      int nr = (int)(red * (1.0F - factor) + this.color.getRed() * factor);
      int ng = (int)(green * (1.0F - factor) + this.color.getGreen() * factor);
      int nb = (int)(blue * (1.0F - factor) + this.color.getBlue() * factor);
      return this.delegate.color(nr, ng, nb, alpha);
   }

   public VertexConsumer color(int argb) {
      int r = argb >> 16 & 0xFF;
      int g = argb >> 8 & 0xFF;
      int b = argb & 0xFF;
      int a = argb >> 24 & 0xFF;
      return this.color(r, g, b, a);
   }

   public VertexConsumer texture(float u, float v) {
      return this.delegate.texture(u, v);
   }

   public VertexConsumer overlay(int u, int v) {
      return this.delegate.overlay(u, v);
   }

   public VertexConsumer light(int u, int v) {
      return this.delegate.light(u, v);
   }

   public VertexConsumer normal(float x, float y, float z) {
      return this.delegate.normal(x, y, z);
   }

   public VertexConsumer lineWidth(float width) {
      return this.delegate.lineWidth(width);
   }

   public void vertex(float x, float y, float z, int colorInt, float u, float v, int overlay, int light, float nx, float ny, float nz) {
      float factor = 0.95F;
      int origA = colorInt >> 24 & 0xFF;
      int origR = colorInt >> 16 & 0xFF;
      int origG = colorInt >> 8 & 0xFF;
      int origB = colorInt & 0xFF;
      int sr = this.color.getRed();
      int sg = this.color.getGreen();
      int sb = this.color.getBlue();
      int nr = (int)(origR * (1.0F - factor) + sr * factor);
      int ng = (int)(origG * (1.0F - factor) + sg * factor);
      int nb = (int)(origB * (1.0F - factor) + sb * factor);
      int newColor = origA << 24 | nr << 16 | ng << 8 | nb;
      this.delegate.vertex(x, y, z, newColor, u, v, overlay, light, nx, ny, nz);
   }

   public void quad(Entry matrixEntry, BakedQuad quad, float red, float green, float blue, float alpha, int light, int overlay) {
   }

   public void quad(
      Entry matrixEntry, BakedQuad quad, float[] brightnesses, float red, float green, float blue, float alpha, int[] lights, int overlay
   ) {
   }
}
