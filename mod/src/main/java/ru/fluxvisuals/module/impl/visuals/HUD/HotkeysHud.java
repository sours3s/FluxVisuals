package ru.fluxvisuals.module.impl.visuals.HUD;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.keyboard.Keyboard;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Список включённых модулей с назначенными клавишами.
 */
@Environment(EnvType.CLIENT)
public final class HotkeysHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static final String DRAG_ID = "hotkeysHud";

   // Размеры в scaled-единицах.
   private static final float ROW_HEIGHT = 13.0F;
   private static final float MIN_WIDTH = 110.0F;
   private static final float PADDING = 7.0F;

   private HotkeysHud() {
   }

   public static boolean hasContent() {
      return !collectModules().isEmpty();
   }

   public static void render(Renderer2D r2) {
      if (r2 == null || mc.player == null) {
         return;
      }
      List<Module> modules = collectModules();
      if (modules.isEmpty()) {
         return;
      }

      float gs = guiScale();
      float fontSize = 7.0F * gs;
      float keySize = 6.5F * gs;
      float pad = PADDING * gs;
      float rowH = ROW_HEIGHT * gs;

      float nameW = 0.0F;
      float keyW = 0.0F;
      for (Module module : modules) {
         nameW = Math.max(nameW, r2.measureText(FontRegistry.INTER_MEDIUM, module.name, fontSize).width);
         String key = Keyboard.keyName(module.bind);
         keyW = Math.max(keyW, r2.measureText(FontRegistry.INTER_MEDIUM, key, keySize).width);
      }
      float panelW = Math.max(MIN_WIDTH * gs, pad * 2.0F + nameW + keyW + 18.0F * gs);
      float panelH = modules.size() * rowH + pad * 2.0F;

      // Предпочтительная позиция: левый край, чуть выше центра (scaled-координаты).
      float preferredX = 20.0F * gs;
      float preferredY = (mc.getWindow().getScaledHeight() * 0.5F - panelH / gs * 0.5F - 20.0F) * gs;

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag(DRAG_ID, preferredX, preferredY, panelW, panelH);
      try {
         float x = session.positionX();
         float y = session.positionY();
         Hud.drawClientRect(r2, x, y, panelW, panelH, 7.0F * gs, 1.0F, 1.0F);

         float rowY = y + pad;
         int accent = Renderer2D.ColorUtil.getMainColor(1, 1);
         for (Module module : modules) {
            Hud.drawClientRectLight(r2, x + pad * 0.5F, rowY,
                  panelW - pad, rowH - gs, 3.0F * gs, 0.7F, 0.0F);

            r2.text(FontRegistry.INTER_MEDIUM, x + pad, rowY + rowH * 0.6F,
                  fontSize, module.name, Renderer2D.ColorUtil.getTextColor(1, 1));

            String key = Keyboard.keyName(module.bind);
            float keyTextW = r2.measureText(FontRegistry.INTER_MEDIUM, key, keySize).width;
            float badgeW = keyTextW + 6.0F * gs;
            float badgeX = x + panelW - pad - badgeW;
            float badgeY = rowY + 1.5F * gs;
            r2.rect(badgeX, badgeY, badgeW, rowH - 4.0F * gs, 3.0F * gs,
                  ColorUtil.replAlpha(accent, 0.18F));
            r2.text(FontRegistry.INTER_SEMIBOLD, badgeX + 3.0F * gs, badgeY + (rowH - 4.0F * gs) * 0.7F,
                  keySize, key, accent);
            rowY += rowH;
         }
      } finally {
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static List<Module> collectModules() {
      List<Module> result = new ArrayList<>();
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return result;
      }
      for (Module module : FluxVisualsClient.get.manager.getModules()) {
         if (module != null && module.enable && module.bind > 0) {
            result.add(module);
         }
      }
      return result;
   }

   private static float guiScale() {
      float scale = mc.getWindow().getScaleFactor();
      return scale > 0.0F ? scale : 1.0F;
   }
}
