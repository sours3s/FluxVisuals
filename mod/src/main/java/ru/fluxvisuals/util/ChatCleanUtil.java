package ru.fluxvisuals.util;

import java.util.regex.Pattern;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

/**
 * Убирает из чата маркер голов, который вставляет плагин крашенных серверов
 * в начало сообщения: «[unknown player head] Имя » текст» или «[Имя head] Имя » текст».
 * Пересобирает текст, сохраняя стили каждого куска (цвет ника и т.п.).
 */
public final class ChatCleanUtil {
   // Маркер в начале строки: [unknown player head] / [Ник head]
   private static final Pattern LEADING_MARKER = Pattern.compile("^\\s*\\[[^\\]]*head[^\\]]*\\]\\s*");

   private ChatCleanUtil() {
   }

   public static Text clean(Text text) {
      if (text == null) {
         return null;
      }
      // Если маркера нет в начале — не трогаем (защита от ложных срабатываний на «[my head] и т.п.»)
      if (!LEADING_MARKER.matcher(text.getString()).find()) {
         return text;
      }
      return rebuild(text, new boolean[]{false});
   }

   private static MutableText rebuild(Text node, boolean[] removed) {
      MutableText out = Text.empty();
      out.setStyle(node.getStyle());

      boolean isLiteral = node.getLiteralString() != null;
      if (isLiteral && !removed[0] && LEADING_MARKER.matcher(node.getString()).find()) {
         // Литерал с маркером: вырезаем маркер, остальное добавляем в том же стиле.
         String cleaned = LEADING_MARKER.matcher(node.getString()).replaceFirst("").trim();
         removed[0] = true;
         if (!cleaned.isEmpty()) {
            out.append(Text.literal(cleaned).setStyle(node.getStyle()));
         }
      } else {
         // Обычный узел: сохраняем контент и стиль, детей обрабатываем ниже.
         out.append(node.copyContentOnly());
      }

      for (Text child : node.getSiblings()) {
         out.append(rebuild(child, removed));
      }
      return out;
   }
}
