package ru.fluxvisuals.util.render;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontObject;
import ru.fluxvisuals.util.render.text.TextRenderer;

/**
 * Кэш результатов {@link Renderer2D#measureText}. Измерение текста — дорогая операция
 * (обход всех глифов строки), а многие HUD-элементы меряют одни и те же строки каждый кадр.
 * Кэширование по (шрифт, размер, текст) убирает повторные измерения и сильно экономит FPS
 * в нагруженных сценах (наметаги, аррайлист, инфо-панель).
 *
 * <p>Ограничение размера с LRU-вытеснением: при превышении лимита удаляется самая старая запись.
 */
@Environment(EnvType.CLIENT)
public final class TextCache {
   private static final int MAX_ENTRIES = 2048;
   private static final Map<String, TextRenderer.TextMetrics> CACHE = new LinkedHashMap<>(128, 0.75F, true);

   private TextCache() {}

   private static String key(FontObject font, String text, float size) {
      return font.id + '|' + Math.round(size * 100.0F) + '|' + (text == null ? "" : text);
   }

   public static TextRenderer.TextMetrics measure(Renderer2D renderer, FontObject font, String text, float size) {
      String k = key(font, text, size);
      TextRenderer.TextMetrics hit = CACHE.get(k);
      if (hit != null) {
         return hit;
      }
      TextRenderer.TextMetrics m = renderer.measureText(font, text, size);
      if (CACHE.size() >= MAX_ENTRIES) {
         Iterator<String> it = CACHE.keySet().iterator();
         if (it.hasNext()) {
            it.next();
            it.remove();
         }
      }
      CACHE.put(k, m);
      return m;
   }

   public static float width(Renderer2D renderer, FontObject font, String text, float size) {
      return measure(renderer, font, text, size).width;
   }

   public static void clear() {
      CACHE.clear();
   }
}
