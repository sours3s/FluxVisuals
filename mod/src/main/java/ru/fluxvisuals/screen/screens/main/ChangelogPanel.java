package ru.fluxvisuals.screen.screens.main;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.api.render.ClientRenderer;
import ru.fluxvisuals.api.render.system.TextureUse;

/**
 * Панель ченджлога в главном меню — справа от кнопок.
 * Показывает: добавлено / исправлено.
 */
public final class ChangelogPanel {

   private static final float PANEL_WIDTH = 250f;
   private static final float LINE_HEIGHT = 11f;
   private static final float SECTION_GAP = 6f;
   private static final float PADDING_X = 12f;
   private static final float PADDING_Y = 10f;
   private static final float HEADER_SIZE = 8f;
   private static final float ITEM_SIZE = 7f;
   private static final float HEADER_HEIGHT = 20f;

   private static final String[][] SECTIONS = {
      {"v1.0.13", null},
      {"added", "Shader Fog — 5 режимов неба (Caustic, Drain, Nebula, Plasma, Bloom)"},
      {"added", "Better Minecraft — анимации инвентаря, таба, чата и хотбара"},
      {"added", "Обфускация jar (ProGuard) — защита от копирования"},
      {"fixed", "TargetHUD: масштаб 3.0, ctrl+колесо, ник в рамке, скрытие при смерти цели"},
      {"fixed", "BPS — исправлен расчёт (не зависает на нуле)"},
      {"fixed", "Координаты и BPS отцентрированы в рамке"},
      {"fixed", "ArrayList — текст по центру строки"},
      {"fixed", "Хотбар пропадал — исправлено"},
      {"fixed", "ClickGUI — только правый Shift (по умолчанию)"},
      {"fixed", "Логотип больше не перевёрнут"},
      {"fixed", "Убрана Мастерская прицелов из меню"},
      {"fixed", "Motion Blur — исправлен рендер эффекта"},
      {"fixed", "Dynamic Lights — свет факелов и горящих сущностей"},
      {"fixed", "Бинды модулей — смена клавиши применяется сразу"},
      {"fixed", "Друзья: вкладки, скролл, удаление, сохранение"},
      {"v1.0.12", null},
      {"added", "Друзья: вкладки Друзья/Сервер, скролл, удаление"},
      {"added", "Dynamic Lights — реально работает (свет факелов/горящих)"},
      {"added", "Motion Blur — исправлен рендер"},
      {"fixed", "Бинды модулей — смена клавиши теперь применяется"},
      {"fixed", "Discord RPC — вкл/выкл через ClickGUI"},
      {"fixed", "Lag Detector — корректный расчёт TPS и пинга"},
      {"fixed", "Better World / Custom World — восстановление при выключении"},
      {"fixed", "TargetHUD: размер, центрирование, фикс BPS"},
      {"fixed", "PotionsHUD — единый стиль с Binds"},
      {"fixed", "Better F3 — полная замена ванильного"},
      {"fixed", "Убраны ванильные иконки эффектов (верхний правый угол)"},
      {"fixed", "Конфиги: полный reset, не плодит default, кнопка папки"},
      {"v1.0.11", null},
      {"added", "Mace Helper"},
      {"added", "Reach Circle"},
      {"added", "Keystrokes"},
      {"added", "Armor Status"},
      {"added", "Better Ping"},
      {"added", "Zoom (bind)"},
      {"added", "Hitboxes"},
      {"added", "Dynamic Lights"},
      {"added", "Entity Culling"},
      {"added", "Camera Overhaul"},
      {"added", "Crosshair (6 стилей)"},
      {"added", "Motion Blur"},
      {"added", "First Person Model"},
      {"added", "HitDelayFix"},
      {"added", "AppleSkin"},
      {"added", "Better F3"},
      {"added", "Pearl Clutch Helper"},
      {"added", "TargetHUD Minimal + комбо"},
      {"fixed", "PotionsHUD scale applied"},
      {"fixed", "BPS overflow"},
      {"fixed", "DiscordRPC default on"},
   };

   private static final Color HEADER_COLOR = new Color(19, 255, 174, 255);
   private static final Color ADDED_COLOR = new Color(85, 255, 85, 220);
   private static final Color FIXED_COLOR = new Color(255, 218, 45, 220);
   private static final Color TEXT_COLOR = new Color(200, 200, 210, 200);
   private static final Color BG_COLOR = new Color(10, 10, 12, 180);
   private static final Color OUTLINE_COLOR = new Color(255, 255, 255, 5);

   /** Прокрутка ченджлога (колёсиком мыши), в пикселях. */
   private static float scrollOffset = 0.0F;

   private ChangelogPanel() {}

   public static void scroll(double verticalDelta) {
      scrollOffset = Math.max(0.0F, scrollOffset + (float) verticalDelta * 8.0F);
   }

   public static void render(ClientRenderer renderer, int screenWidth, int screenHeight, float alpha) {
      if (renderer == null || alpha <= 0.01f) return;

      float panelX = screenWidth - PANEL_WIDTH - 15f;
      float panelY = 45f;

      // Calculate total height
      float totalH = HEADER_HEIGHT + PADDING_Y * 2;
      for (String[] entry : SECTIONS) {
         if (entry[1] == null) {
            totalH += LINE_HEIGHT + SECTION_GAP; // version header
         } else {
            totalH += LINE_HEIGHT;
         }
      }

      // Панель не выше экрана; при переполнении — скролл.
      float maxPanelH = screenHeight - panelY - 20f;
      float panelH = Math.min(totalH, maxPanelH);
      float maxScroll = Math.max(0.0f, totalH - panelH);
      scrollOffset = Math.max(0.0f, Math.min(scrollOffset, maxScroll));

      // Draw background
      renderer.blur(panelX, panelY, PANEL_WIDTH, panelH, new Vector4f(8), 15f, alpha * 0.8f);
      renderer.rect(panelX, panelY, PANEL_WIDTH, panelH, new Vector4f(8), 1f,
         BG_COLOR, BG_COLOR, BG_COLOR, BG_COLOR);
      renderer.outline(panelX, panelY, PANEL_WIDTH, panelH, 0.5f, new Vector4f(8),
         new Vector2f(1), OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR, OUTLINE_COLOR);

      // Title
      renderer.text("CHANGELOG", panelX + PADDING_X, panelY + 8f,
         TextureUse.SFMEDIUM, HEADER_SIZE, new Color(19, 255, 174, (int)(255 * alpha)));

      float topBound = panelY + HEADER_HEIGHT;
      float bottomBound = panelY + panelH;
      float y = topBound - scrollOffset;

      for (String[] entry : SECTIONS) {
         if (entry[1] == null) {
            // Version header
            float h = LINE_HEIGHT + SECTION_GAP;
            if (y + h >= topBound && y <= bottomBound) {
               renderer.text(entry[0], panelX + PADDING_X, y + 2f,
                  TextureUse.SFMEDIUM, 7.5f, new Color(255, 255, 255, (int)(220 * alpha)));
            }
            y += h;
         } else {
            // Item
            if (y + LINE_HEIGHT >= topBound && y <= bottomBound) {
               String type = entry[0];
               String text = entry[1];
               Color typeColor = "added".equals(type) ? ADDED_COLOR : FIXED_COLOR;
               String prefix = "added".equals(type) ? "+ " : "* ";

               float tx = panelX + PADDING_X;
               renderer.text(prefix + text, tx, y,
                  TextureUse.SFMEDIUM, ITEM_SIZE,
                  new Color(typeColor.getRed(), typeColor.getGreen(), typeColor.getBlue(),
                     (int)(typeColor.getAlpha() * alpha)));
            }
            y += LINE_HEIGHT;
         }
      }
   }
}
