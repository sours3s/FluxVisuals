package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.Calendar;
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
 * Единая статус-строка: координаты, системное/игровое время, скорость (BPS) и пинг —
 * в одной компактной строке. Каждая метрика включается/выключается отдельной настройкой
 * {@link #metrics} (отображается в ClickGUI на модуле Hud).
 *
 * <p>Строки и измерения кэшируются через {@link TextCache} (только если текст не менялся).
 */
@Environment(EnvType.CLIENT)
public class InformationHUD {
   public static MinecraftClient mc = MinecraftClient.getInstance();

   public static final MultiBooleanSetting metrics = new MultiBooleanSetting(
      "Status Line Metrics",
      new BooleanSetting("Координаты", true),
      new BooleanSetting("Время", true),
      new BooleanSetting("Скорость", true)
   );

   private static double prevX = 0.0;
   private static double prevY = 0.0;
   private static double prevZ = 0.0;
   private static long prevTime = 0L;
   private static float bps = 0.0F;

   private static final String LBL_X = "x";
   private static final String LBL_Y = "y";
   private static final String LBL_Z = "z";
   private static final String LBL_BPS = "bps";
   private static final String LBL_MS = "ms";

   private InformationHUD() {}

   public static void information(Renderer2D r2) {
      if (mc.player == null || mc.world == null) {
         return;
      }
      Hud.animC.update();
      boolean chat = mc.currentScreen instanceof ChatScreen;
      Hud.animC.run(chat ? 1.0 : 0.0, 0.8F, Easings.CIRC_OUT, false);

      updateBps();

      float fontSize = 28.0F;
      float textY = 25.0F;

      // ===== Сегменты в порядке: координаты / время / скорость =====
      boolean showCoords = metrics.get("Координаты");
      boolean showTime = metrics.get("Время");
      boolean showSpeed = metrics.get("Скорость");

      String xStr = String.valueOf((int) mc.player.getX());
      String yStr = String.valueOf((int) mc.player.getY());
      String zStr = String.valueOf((int) mc.player.getZ());
      String bpsValue = String.format("%.1f", bps);
      String systemTime = systemTime();
      String gameTime = gameTime();

      float[] segWidths = new float[3];
      boolean[] show = { showCoords, showTime, showSpeed };

      if (showCoords) {
         segWidths[0] = TextCache.width(r2, FontRegistry.INTER_MEDIUM, xStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_X, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, yStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Y, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, zStr, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Z, fontSize);
      }
      if (showTime) {
         String timeStr = systemTime + "  " + gameTime;
         segWidths[1] = TextCache.width(r2, FontRegistry.INTER_MEDIUM, timeStr, fontSize);
      }
      if (showSpeed) {
         segWidths[2] = TextCache.width(r2, FontRegistry.INTER_MEDIUM, bpsValue, fontSize)
               + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_BPS, fontSize);
      }

      // Иконки слева от каждого сегмента (ICONS-глифы «1» и «2» известны и работают).
      float ICON = 42.0F;
      float SEP = 34.0F;
      float totalWidth = ICON;
      for (int i = 0; i < 3; i++) {
         if (!show[i]) continue;
         totalWidth += segWidths[i] + (i == 0 ? SEP : SEP);
      }
      totalWidth += 16.0F;
      float totalHeight = 40.64F;

      float preferredX = 20.0F;
      float preferredY = mc.getWindow().getHeight() - 75.0F + (20.0F + -20.0F * Hud.animC.get());

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag("informationHUD", preferredX, preferredY, totalWidth, totalHeight);
      float x = session.positionX();
      float y = session.positionY();

      Hud.drawClientRect(r2, x, y, totalWidth, totalHeight, 13.0F, 1.0F, 1.0F);

      float cursorX = x + 14.0F;
      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int text = Renderer2D.ColorUtil.getTextColor(1, 1);

      if (showCoords) {
         r2.text(FontRegistry.ICONS, cursorX, y + textY, 32.0F, "1", main);
         cursorX += ICON;
         float sx = cursorX;
         drawPair(r2, sx, y, textY, fontSize, xStr, LBL_X, text, main);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, xStr, fontSize) + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_X, fontSize);
         drawPair(r2, sx, y, textY, fontSize, yStr, LBL_Y, text, main);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, yStr, fontSize) + TextCache.width(r2, FontRegistry.INTER_MEDIUM, LBL_Y, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, y + textY, fontSize, zStr, text);
         sx += TextCache.width(r2, FontRegistry.INTER_MEDIUM, zStr, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, sx, y + textY, fontSize, LBL_Z, main);
         cursorX += segWidths[0] + SEP;
      }

      if (showTime) {
         separator(r2, cursorX, y);
         cursorX += 18.0F;
         String timeStr = systemTime + "  " + gameTime;
         r2.text(FontRegistry.INTER_MEDIUM, cursorX, y + textY, fontSize, timeStr, text);
         cursorX += segWidths[1] + SEP;
      }

      if (showSpeed) {
         separator(r2, cursorX, y);
         cursorX += 18.0F;
         r2.text(FontRegistry.ICONS, cursorX, y + textY, 32.0F, "2", main);
         cursorX += ICON;
         r2.text(FontRegistry.INTER_MEDIUM, cursorX, y + textY, fontSize, bpsValue, text);
         cursorX += TextCache.width(r2, FontRegistry.INTER_MEDIUM, bpsValue, fontSize);
         r2.text(FontRegistry.INTER_MEDIUM, cursorX, y + textY, fontSize, LBL_BPS, main);
         cursorX += segWidths[2] + SEP;
      }

      DraggableManager.getInstance().endDrag(session);
   }

   private static void drawPair(Renderer2D r2, float x, float panelY, float textY, float size,
                                String value, String label, int valueColor, int labelColor) {
      r2.text(FontRegistry.INTER_MEDIUM, x, panelY + textY, size, value, valueColor);
      x += TextCache.width(r2, FontRegistry.INTER_MEDIUM, value, size);
      r2.text(FontRegistry.INTER_MEDIUM, x, panelY + textY, size, label, labelColor);
   }

   private static void separator(Renderer2D r2, float x, float panelY) {
      r2.rect(x, panelY + 15.0F, 2.34F, 11.21F, 4.0F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), 80));
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
         double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
         bps = (float) (distance * 1000.0 / deltaTime);
      }
      prevTime = currentTime;
      prevX = mc.player.getX();
      prevY = mc.player.getY();
      prevZ = mc.player.getZ();
   }

   private static String systemTime() {
      Calendar calendar = Calendar.getInstance();
      int hours = calendar.get(11);
      int minutes = calendar.get(12);
      return String.format("%d:%02d", hours, minutes);
   }

   private static String gameTime() {
      if (mc.world == null) return "";
      long timeOfDay = mc.world.getTimeOfDay();
      long ticks = (timeOfDay + 6000L) % 24000L;
      long hours = ticks / 1000L;
      long minutes = (ticks % 1000L) * 60L / 1000L;
      return String.format("%d:%02d", hours, minutes);
   }
}