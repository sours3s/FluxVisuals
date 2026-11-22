package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Мини-инвентарь: сетка 9x3 слотов (9..35) в стеклянной панели.
 */
@Environment(EnvType.CLIENT)
public final class InventoryHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static final String DRAG_ID = "inventoryHud";
   private static final int COLS = 9;
   private static final int ROWS = 3;
   private static final int FIRST_SLOT = 9;

   // Размеры в scaled-единицах.
   private static final float CELL = 16.0F;
   private static final float GAP = 2.0F;
   private static final float PADDING = 6.0F;
   private static final float HEADER = 11.0F;

   private InventoryHud() {
   }

   public static boolean hasContent() {
      if (mc.player == null) {
         return false;
      }
      for (int slot = FIRST_SLOT; slot < FIRST_SLOT + COLS * ROWS; slot++) {
         if (!mc.player.getInventory().getStack(slot).isEmpty()) {
            return true;
         }
      }
      return false;
   }

   public static void render(Renderer2D r2, DrawContext ctx) {
      if (r2 == null || mc.player == null || mc.world == null) {
         return;
      }

      float gs = guiScale();
      float cell = CELL * gs;
      float gap = GAP * gs;
      float pad = PADDING * gs;
      float panelW = COLS * cell + (COLS - 1) * gap + pad * 2.0F;
      float panelH = ROWS * cell + (ROWS - 1) * gap + pad * 2.0F + HEADER * gs;

      // Предпочтительная позиция: справа, ниже центра экрана (scaled-координаты).
      float preferredX = (mc.getWindow().getScaledWidth() - panelW / gs - 20.0F) * gs;
      float preferredY = (mc.getWindow().getScaledHeight() - panelH / gs - 60.0F) * gs;

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag(DRAG_ID, preferredX, preferredY, panelW, panelH);
      try {
         float x = session.positionX();
         float y = session.positionY();

         Hud.drawClientRect(r2, x, y, panelW, panelH, 7.0F * gs, 1.0F, 1.0F);
         r2.text(FontRegistry.INTER_MEDIUM, x + pad, y + pad + 5.0F * gs, 7.0F * gs,
               "Inventory", Renderer2D.ColorUtil.getTextColor(1, 1));

         float gridX = x + pad;
         float gridY = y + pad + HEADER * gs;
         for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
               int slot = FIRST_SLOT + row * COLS + col;
               ItemStack stack = mc.player.getInventory().getStack(slot);
               float sx = gridX + col * (cell + gap);
               float sy = gridY + row * (cell + gap);

               r2.rect(sx, sy, cell, cell, 3.0F * gs,
                     ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 0.8F));
               r2.rectOutline(sx, sy, cell, cell, 3.0F * gs,
                     ColorUtil.replAlpha(0xFFFFFFFF, 0.08F), 0.6F * gs);

               if (!stack.isEmpty() && ctx != null) {
                  float ix = sx / gs + 1.0F;
                  float iy = sy / gs + 1.0F;
                  ctx.drawItem(stack, (int) ix, (int) iy);
                  ctx.drawStackOverlay(mc.textRenderer, stack, (int) ix, (int) iy);
               }
            }
         }
      } finally {
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static float guiScale() {
      float scale = mc.getWindow().getScaleFactor();
      return scale > 0.0F ? scale : 1.0F;
   }
}
