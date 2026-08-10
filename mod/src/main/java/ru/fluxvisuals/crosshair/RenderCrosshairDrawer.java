package ru.fluxvisuals.crosshair;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Рисует кастомный прицел через ванильный DrawContext.
 * Используется и в игре (InGameHudCrosshairMixin), и для превью в мастерской.
 */
@Environment(EnvType.CLIENT)
public final class RenderCrosshairDrawer {

   private RenderCrosshairDrawer() {}

   /** Рисует прицел по центру экрана. */
   public static void draw(DrawContext context, CrosshairSettings s) {
      MinecraftClient mc = MinecraftClient.getInstance();
      float cx = mc.getWindow().getScaledWidth() / 2.0F;
      float cy = mc.getWindow().getScaledHeight() / 2.0F;
      drawAt(context, s, cx, cy);
   }

   /** Рисует прицел в заданной позиции (превью). */
   public static void drawAt(DrawContext context, CrosshairSettings s, float cx, float cy) {
      float size = s.size;
      float t = s.thickness;
      float g = s.gap;
      int color = s.getColor();

      switch (s.style) {
         case "Cross" -> {
            fill(context, cx - t / 2, cy - size - g, t, size, color);
            fill(context, cx - t / 2, cy + g, t, size, color);
            fill(context, cx - size - g, cy - t / 2, size, t, color);
            fill(context, cx + g, cy - t / 2, size, t, color);
         }
         case "Dot" -> fill(context, cx - size / 2, cy - size / 2, size, size, color);
         case "GapDot" -> {
            fill(context, cx - t / 2, cy - size - g, t, size, color);
            fill(context, cx - t / 2, cy + g, t, size, color);
            fill(context, cx - size - g, cy - t / 2, size, t, color);
            fill(context, cx + g, cy - t / 2, size, t, color);
            fill(context, cx - 1, cy - 1, 2, 2, color);
         }
         case "T" -> {
            fill(context, cx - size, cy - t / 2, size * 2, t, color);
            fill(context, cx - t / 2, cy, t, size, color);
         }
         case "Corner" -> {
            float c = size * 0.7F;
            fill(context, cx - g - c, cy - t / 2, c, t, color);
            fill(context, cx - t / 2, cy - g - c, t, c, color);
            fill(context, cx + g, cy - t / 2, c, t, color);
            fill(context, cx - t / 2, cy - g - c, t, c, color);
            fill(context, cx - g - c, cy - t / 2, c, t, color);
            fill(context, cx - t / 2, cy + g, t, c, color);
            fill(context, cx + g, cy - t / 2, c, t, color);
            fill(context, cx - t / 2, cy + g, t, c, color);
         }
         case "Circle" -> {
            float r = size;
            float sq = t;
            for (int i = 0; i < 12; i++) {
               double ang = Math.toRadians(i * 30.0);
               float px = (float) (cx + Math.cos(ang) * r);
               float py = (float) (cy + Math.sin(ang) * r);
               fill(context, px - sq / 2, py - sq / 2, sq, sq, color);
            }
         }
      }
   }

   private static void fill(DrawContext context, float x, float y, float w, float h, int color) {
      context.fill((int) x, (int) y, (int) (x + w), (int) (y + h), color);
   }
}
