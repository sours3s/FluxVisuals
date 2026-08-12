package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Инфо-строка: координаты + скорость (BPS) — компактно, отцентрировано.
 * Время вынесено в WaterMark (не дублируется).
 */
@Environment(EnvType.CLIENT)
public class InformationHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static final MultiBooleanSetting metrics = new MultiBooleanSetting(
      "Status Line Metrics",
      new BooleanSetting("Координаты", true),
      new BooleanSetting("Скорость", true)
   );

   private static double prevX = 0.0;
   private static double prevY = 0.0;
   private static double prevZ = 0.0;
   private static long prevTime = 0L;
   private static float bps = 0.0F;
   // Накопительный расчёт: позиция игрока обновляется раз в тик (50 мс), поэтому
   // мерить по кадрам нельзя — на высоком FPS bps будет прыгать в 0. Считаем по окну ~250 мс.
   private static float accDistance = 0.0F;
   private static long accTimeMs = 0L;
   private static final long BPS_WINDOW_MS = 250L;

   private static final String LBL_X = "x";
   private static final String LBL_Y = "y";
   private static final String LBL_Z = "z";
   private static final String LBL_BPS = "bps";

   private InformationHUD() {}

   public static void information(Renderer2D r2) {
      if (mc.player == null || mc.world == null) return;

      Hud.animC.update();
      boolean chat = mc.currentScreen instanceof ChatScreen;
      Hud.animC.run(chat ? 1.0 : 0.0, 0.8F, Easings.CIRC_OUT, false);

      updateBps();

      float fontSize = 28.0F;
      boolean showCoords = metrics.get("Координаты");
      boolean showSpeed = metrics.get("Скорость");

      String xStr = String.valueOf((int) mc.player.getX());
      String yStr = String.valueOf((int) mc.player.getY());
      String zStr = String.valueOf((int) mc.player.getZ());
      String bpsValue = String.format("%.1f", Math.min(bps, 999.9F));

      // ===== Вычисляем ширину сегментов =====
      float coordsW = 0;
      if (showCoords) {
         coordsW = TextCache.width(r2, FontRegistry.INTER_MEDIUM, xStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_X, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, yStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Y, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, zStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Z, fontSize);
      }
      float speedW = 0;
      if (showSpeed) {
         speedW = TextCache.width(r2, FontRegistry.INTER_MEDIUM, bpsValue, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_BPS, fontSize);
      }

      float ICON = 24.0F;       // уменьшен с 36 -> 24 (было слишком большим)
      float GAP = 16.0F;        // уменьшен с 24 -> 16
      float PADDING = 10.0F;    // уменьшен с 12 -> 10

      float totalWidth = PADDING;
      if (showCoords) totalWidth += coordsW;       // убрали ICON перед координатами
      if (showCoords && showSpeed) totalWidth += GAP;
      if (showSpeed) totalWidth += ICON + speedW;  // иконка только перед BPS
      totalWidth += PADDING;

      float totalHeight = 38.0F;

      float preferredX = 20.0F;
      float preferredY = mc.getWindow().getHeight() - 75.0F + (20.0F + -20.0F * Hud.animC.get());

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("informationHUD", preferredX, preferredY, totalWidth, totalHeight);
      float x = session.positionX();
      float y = session.positionY();

      Hud.drawClientRect(r2, x, y, totalWidth, totalHeight, 13.0F, 1.0F, 1.0F);

      // Вертикальное центрирование текста в рамке. r2.text принимает BASELINE,
      // поэтому baseline = y + (totalHeight + ascender) / 2 — так глифы стоят по центру,
      // а не уезжают за верхнюю границу рамки.
      float ascent = TextCache.measure(r2, FontRegistry.INTER_MEDIUM, "A", fontSize).baselineOffset;
      float textY = y + (totalHeight + ascent) / 2.0F;
      float cursorX = x + PADDING;
      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int text = Renderer2D.ColorUtil.getTextColor(1, 1);

      if (showCoords) {
         float sx = cursorX;
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, xStr, text);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, xStr, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, LBL_X, main);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_X, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, yStr, text);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, yStr, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, LBL_Y, main);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Y, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, zStr, text);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, zStr, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, textY, fontSize, LBL_Z, main);
         cursorX += coordsW + GAP;
      }

      if (showSpeed) {
         r2.text(FontRegistry.ICONS, cursorX, textY, 32.0F, "2", main);
         cursorX += ICON;
         r2.text(FontRegistry.INTER_MEDIUM, cursorX, textY, fontSize, bpsValue, text);
         cursorX += TextCache.width(r2, FontRegistry.INTER_MEDIUM, bpsValue, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, cursorX, textY, fontSize, LBL_BPS, main);
      }

      DraggableManager.getInstance().endDrag(session);
   }

   private static void updateBps() {
      long currentTime = System.currentTimeMillis();
      if (prevTime == 0L) {
         prevTime = currentTime;
         prevX = mc.player.getX();
         prevY = mc.player.getY();
         prevZ = mc.player.getZ();
         return;
      }
      long deltaTime = currentTime - prevTime;
      if (deltaTime > 0) {
         double dx = mc.player.getX() - prevX;
         double dy = mc.player.getY() - prevY;
         double dz = mc.player.getZ() - prevZ;
         accDistance += (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
         accTimeMs += deltaTime;
      }
      prevTime = currentTime;
      prevX = mc.player.getX();
      prevY = mc.player.getY();
      prevZ = mc.player.getZ();

      // Раз в окно (~250 мс) публикуем сглаженный BPS.
      if (accTimeMs >= BPS_WINDOW_MS) {
         bps = accDistance * 1000.0F / accTimeMs;
         accDistance = 0.0F;
         accTimeMs = 0L;
      }
   }
}
