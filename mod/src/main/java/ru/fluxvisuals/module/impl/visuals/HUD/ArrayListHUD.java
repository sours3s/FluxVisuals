package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.TextCache;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.Translate;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Array List — список включённых модулей с сортировкой по ширине текста и плавной
 * анимацией включения/выключения (ширина + прозрачность), убирающей мигание.
 *
 * <p>Исправления относительно старой версии:
 * <ul>
 *   <li>корректный компаратор по ширине (старый всегда возвращал ±1 — ломал контракт и дёргал список);</li>
 *   <li>ширина/альфа каждого ряда интерполируются через {@code Module.mAnim} (0..1): включённый
 *       модуль раскрывается, выключенный — сворачивается и плавно исчезает;</li>
 *   <li>измерения текста кэшируются через {@link TextCache} (одна строка — одно измерение).</li>
 * </ul>
 */
@Environment(EnvType.CLIENT)
public class ArrayListHUD {
   public static final BooleanSetting sortByWidth = new BooleanSetting("Sort by width", true);
   public static final BooleanSetting smooth = new BooleanSetting("Smooth animation", true);

   private static final List<Module> visible = new ArrayList<>();

   private ArrayListHUD() {}

   public static float deltaTime() {
      return MinecraftClient.getInstance().getCurrentFps() > 0.0F
         ? 1.0F / MinecraftClient.getInstance().getCurrentFps()
         : 1.0F;
   }

   /** Есть ли что показывать: включённый модуль или ещё сворачивающийся (анимация выключения). */
   public static boolean hasContent() {
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.manager != null) {
         for (Module m : FluxVisualsClient.get.manager.getModules()) {
            if (m.enable || m.mAnim.getValue() > 0.001) {
               return true;
            }
         }
      }
      return false;
   }

   private static float cachedWidth(Renderer2D r2, Module m) {
      return TextCache.width(r2, FontRegistry.INTER_MEDIUM, m.name, 32.0F);
   }

   public static void arraylist(Renderer2D r2) {
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return;
      }

      // Обновляем анимации всех модулей и собираем видимые: включённые ИЛИ ещё анимирующиеся.
      visible.clear();
      for (Module m : FluxVisualsClient.get.manager.getModules()) {
         m.mAnim.update();
         if (m.enable || m.mAnim.getValue() > 0.001) {
            visible.add(m);
         }
      }
      if (visible.isEmpty()) {
         return;
      }

      if (sortByWidth.get()) {
         visible.sort(Comparator.comparingDouble((Module m) -> cachedWidth(r2, m)).reversed());
      }

      // Ширина списка — по самой широкой видимой строке (по полной ширине, до сжатия анимацией).
      float maxModuleWidth = 0.0F;
      for (Module m : visible) {
         float w = cachedWidth(r2, m) + 12.0F;
         if (w > maxModuleWidth) {
            maxModuleWidth = w;
         }
      }
      float listWidth = Math.max(maxModuleWidth, 40.0F);
      float listHeight = Math.max(visible.size() * 24.0F + 10.0F, 39.0F);

      float preferredX = 20.0F;
      float preferredY = 120.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
         .beginDrag("arrayList", preferredX, preferredY, listWidth, listHeight);
      int fbWidth = MinecraftClient.getInstance().getWindow().getWidth();
      int fbHeight = MinecraftClient.getInstance().getWindow().getHeight();
      float x = clamp(session.positionX(), 1.0F, Math.max(1.0F, fbWidth - listWidth - 1.0F));
      float y = clamp(session.positionY(), 1.0F, Math.max(1.0F, fbHeight - listHeight - 1.0F));

      float speed = smooth.get() ? 15.0F * deltaTime() : 1.0F;
      int count = 0;
      for (Module m : visible) {
         float offset = count * 24.0F;
         Translate translate = m.a;
         if (smooth.get()) {
            translate.interpolate(0.0F, offset, speed);
         } else {
            translate.setX(0.0F);
            translate.setY(offset);
         }
         count++;
      }

      int baseColor = Renderer2D.ColorUtil.getMainColor(16, 0);
      for (Module module : visible) {
         double anim = smooth.get() ? module.mAnim.getValue() : 1.0;
         if (anim <= 0.001) {
            continue;
         }
         float fullW = cachedWidth(r2, module) + 12.0F;
         float pillW = (float) (fullW * anim);
         if (pillW < 1.0F) {
            continue;
         }
         Translate translate = module.a;
         float pillX = clamp(translate.getX() + x, 1.0F, Math.max(1.0F, fbWidth - 1.0F));
         if (pillX + pillW > fbWidth - 1.0F) {
            pillW = Math.max(0.0F, fbWidth - 1.0F - pillX);
         }
         float pillY = translate.getY() + y + 5.0F;
         float pillH = 20.0F;

         float alpha = (float) anim;
         Hud.drawClientRectLight(r2, pillX, pillY, pillW, pillH, 6.0F, alpha, 1.0F);

         if (pillW > 6.0F) {
            float textY = pillY + pillH / 2.0F
               + TextCache.measure(r2, FontRegistry.INTER_MEDIUM, module.name, 32.0F).height * 0.5F - 1.0F;
            float textX = clamp(translate.getX() + x + 6.0F, 1.0F, Math.max(1.0F, fbWidth - 1.0F));
            r2.text(FontRegistry.INTER_MEDIUM, textX, textY, 32.0F, module.name,
               Renderer2D.ColorUtil.replAlpha(baseColor, Math.round(alpha * 255.0F)));
         }
      }

      DraggableManager.getInstance().endDrag(session);
   }

   private static float clamp(float value, float min, float max) {
      if (value < min) {
         return min;
      }
      return value > max ? max : value;
   }
}
