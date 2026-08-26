package ru.fluxvisuals.ui.gui.widget.settings;

import java.util.HashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Интерактивные тултипы ClickGUI: при наведении на модуль или настройку рисуется аккуратная
 * панель с кратким описанием на русском. Описания берутся из реестра {@link #moduleDescriptions}
 * и {@link #settingDescriptions}; если описание не зарегистрировано, используется описание
 * модуля из аннотации (fallback).
 *
 * <p>Один тултип на кадр: рендер-цикл ClickGUI выставляет {@link #hovered} и рисует панель
 * после {@code popClipRect()}, чтобы текст не обрезался клип-областью.
 */
@Environment(EnvType.CLIENT)
public final class TooltipManager {
   private static final float MAX_WIDTH = 220.0F;
   private static final float PADDING = 10.0F;
   private static final float TITLE_SIZE = 15.0F;
   private static final float DESC_SIZE = 12.0F;

   private static final Map<String, String> moduleDescriptions = new HashMap<>();
   private static final Map<String, String> settingDescriptions = new HashMap<>();

   private static String hovered;       // текст тултипа на текущий кадр
   private static String hoveredTitle;

   private TooltipManager() {}

   public static void registerModule(String name, String ru) {
      moduleDescriptions.put(name, ru);
   }

   public static void registerSetting(String moduleName, String settingName, String ru) {
      settingDescriptions.put(moduleName + "::" + settingName, ru);
   }

   /** Устанавливает тултип модуля (заголовок — имя модуля). */
   public static void setModule(Module module) {
      String desc = moduleDescriptions.get(module.name);
      if (desc == null) {
         desc = module.description;
      }
      if (desc == null || desc.isEmpty()) {
         return;
      }
      hoveredTitle = module.name;
      hovered = desc;
   }

   /** Устанавливает тултип настройки (заголовок — имя настройки). */
   public static void setSetting(Module module, Setting setting) {
      String desc = settingDescriptions.get(module.name + "::" + setting.name);
      if (desc == null) {
         return;
      }
      hoveredTitle = setting.name;
      hovered = desc;
   }

   /** Сбрасывает тултип на начало кадра. */
   public static void reset() {
      hovered = null;
      hoveredTitle = null;
   }

   public static boolean isActive() {
      return hovered != null;
   }

   /**
    * Рисует панель тултипа возле курсора (после popClipRect). Переносит описание по ширине.
    */
   public static void render(Renderer2D r2, int mouseX, int mouseY, int viewportWidth, int viewportHeight) {
      if (hovered == null) {
         return;
      }
      String[] lines = wrap(r2, hovered, DESC_SIZE);
      float titleW = TextCache.width(r2, FontRegistry.INTER_SEMIBOLD, hoveredTitle, TITLE_SIZE);
      float bodyW = 0.0F;
      for (String line : lines) {
         bodyW = Math.max(bodyW, TextCache.width(r2, FontRegistry.INTER_MEDIUM, line, DESC_SIZE));
      }
      float boxW = Math.max(titleW, bodyW) + PADDING * 2.0F;
      float titleH = TextCache.measure(r2, FontRegistry.INTER_SEMIBOLD, hoveredTitle, TITLE_SIZE).height;
      float lineH = TextCache.measure(r2, FontRegistry.INTER_MEDIUM, "W", DESC_SIZE).height;
      float boxH = PADDING + titleH + 8.0F + lineH * lines.length + PADDING;

      float x = mouseX + 14.0F;
      float y = mouseY + 12.0F;
      if (x + boxW > viewportWidth - 4.0F) {
         x = mouseX - boxW - 8.0F;
      }
      if (y + boxH > viewportHeight - 4.0F) {
         y = mouseY - boxH - 6.0F;
      }
      x = Math.max(4.0F, x);
      y = Math.max(4.0F, y);

      int panel = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 230);
      r2.rect(x, y, boxW, boxH, 6.0F, panel);
      r2.rectOutline(x, y, boxW, boxH, 6.0F,
         Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), 40), 1.0F);

      int main = Renderer2D.ColorUtil.getMainColor(1, 1);
      int text = Renderer2D.ColorUtil.getTextColor(1, 1);
      float ty = y + PADDING + titleH;
      r2.text(FontRegistry.INTER_SEMIBOLD, x + PADDING, ty, TITLE_SIZE, hoveredTitle, main);
      ty += 8.0F;
      for (String line : lines) {
         r2.text(FontRegistry.INTER_MEDIUM, x + PADDING, ty + lineH, DESC_SIZE, line, text);
         ty += lineH;
      }
   }

   private static String[] wrap(Renderer2D r2, String text, float size) {
      String[] words = text.split("\\s+");
      StringBuilder current = new StringBuilder();
      java.util.List<String> lines = new java.util.ArrayList<>();
      for (String word : words) {
         String test = current.length() == 0 ? word : current + " " + word;
         if (TextCache.width(r2, FontRegistry.INTER_MEDIUM, test, size) > MAX_WIDTH - PADDING * 2.0F
            && current.length() > 0) {
            lines.add(current.toString());
            current = new StringBuilder(word);
         } else {
            current = new StringBuilder(test);
         }
      }
      if (current.length() > 0) {
         lines.add(current.toString());
      }
      return lines.toArray(new String[0]);
   }

   /** Заполняет реестр русских описаний для всех модулей и ключевых настроек. */
   public static void registerAll() {
      // ===== HUD =====
      registerModule("Hud", "Мастер HUD-оверлея: водяной знак, список модулей, инфо-панель, уведомления и другие элементы экрана. Здесь же включаются сами элементы.");
      registerSetting("Hud", "Interface Elements", "Какие HUD-элементы отрисовывать: водяной знак, аррайлист, инфо-панель, TargetHUD и другие.");
      registerSetting("Hud", "Notification Settings", "Типы уведомлений: включение модулей, предупреждения о низком здоровье и износе брони.");
      registerSetting("Hud", "Health Threshold", "Уровень здоровья (в HP), при котором появится предупреждение «Низкое здоровье».");
      registerSetting("Hud", "Durability %", "Процент прочности брони, при достижении которого появится предупреждение об износе.");
      registerSetting("Hud", "Blur", "Включает размытие фона под панелями HUD и интерфейса для современного вида.");
      registerSetting("Hud", "Status Line Metrics", "Какие метрики показывать в единой статус-строке: координаты, время, скорость и пинг.");

      // ===== Визуальные модули =====
      registerModule("Aspect Ratio", "Меняет соотношение сторон картинки и/или FOV камеры для кинематографичного вида.");
      registerModule("Camera Customer", "Настройка камеры: плавный зум (удержание или переключение), FOV, аспект и эффекты камеры.");
      registerModule("Custom World", "Изменяет внешний вид мира: небо, солнце, луна, облака и туман на свой вкус.");
      registerModule("ESP", "Подсвечивает сущностей и игроков рамкой с учётом глубины (не показывает сквозь стены).");
      registerModule("Gamma", "Увеличивает яркость мира до указанного уровня, позволяя видеть в темноте.");
      registerModule("Cosmetic", "Косметика клиента: отключает/изменяет некоторые навязчивые эффекты отрисовки.");
      registerModule("Item ESP", "Подсвечивает выброшенные предметы на земле рамкой, чтобы их было легче найти.");
      registerModule("Jump Circle", "Рисует красивое кольцо в точке приземления при прыжке.");
      registerModule("Name Tags", "Показывает над сущностями аккуратные таблички: имя, HP и другую информацию по линии видимости.");
      registerModule("No Render", "Скрывает раздражающие элементы: частицы, облака, анимации и т.д.");
      registerModule("Glow Cubes", "Добавляет свечение вокруг сущностей (глоу-эффект) в указанном радиусе.");
      registerModule("Swing Animation", "Анимирует взмах руки при атаке/использовании предмета плавным движением.");
      registerModule("Target ESP", "Подсвечивает текущую цель (сущность под прицелом) специальной рамкой.");
      registerModule("Particles", "Добавляет декоративные частицы вокруг игрока для атмосферы.");
      registerModule("Projectile Prediction", "Прогнозирует траекторию снарядов (стрел, снежков) линией-пунктиром.");
      registerModule("Skin Manager", "Управление отрисовкой скинов: масштаб, ориентация и другие визуальные параметры.");
      registerModule("Menu Settings", "Настройки главного меню и ClickGUI: анимации, цвета и поведение интерфейса.");
      registerModule("Smooth Camera", "Плавно сглаживает повороты камеры за спиной игрока (убирает рывки).");
      registerModule("Better World", "Улучшенный рендер мира: отключение дождя, контроль времени суток и погоды.");
      registerModule("Block Outline", "Красивая подсветка граней блока, на который смотрит игрок.");
      registerModule("Hit Effect", "Визуальный эффект при попадании по сущности: вспышка или частицы.");
      registerModule("Item Physics", "Заставляет выброшенные предметы плавно вращаться в воздухе.");
      registerModule("Kill Effect", "Эффектный визуальный всплеск при убийстве сущности.");
      registerModule("Shader Hand", "Применяет шейдерный эффект к руке игрока.");
      registerModule("Glass Hand", "Делает руку игрока полупрозрачной («стеклянной»).");

      // ===== Утилиты =====
      registerModule("Client Sound", "Звук переключения функций: тихий щелчок при включении/выключении модуля.");
      registerSetting("Client Sound", "Value", "Громкость звука переключения модулей.");
      registerModule("Hit Sound", "Звук удара по противнику при атаке (несколько вариантов на выбор).");
      registerSetting("Hit Sound", "Mode", "Вариант звука удара (типы 1-7).");
      registerSetting("Hit Sound", "Value", "Громкость звука удара.");
      registerModule("Discord RPC", "Показывает Rich Presence в Discord: ник, сервер, состояние игры.");
   }
}
