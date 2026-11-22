package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Компактная панель брони: четыре слота экипировки с иконками и полосками прочности.
 */
@Environment(EnvType.CLIENT)
public final class ArmorHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static final String DRAG_ID = "armorHud";
   private static final EquipmentSlot[] ARMOR_SLOTS = {
      EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
   };

   // Размеры заданы в scaled-единицах и переводятся в framebuffer-пиксели Renderer2D.
   private static final float PANEL_WIDTH = 66.0F;
   private static final float ROW_HEIGHT = 14.0F;
   private static final float ICON_SIZE = 12.0F;

   private ArmorHud() {
   }

   public static boolean hasContent() {
      if (mc.player == null) {
         return false;
      }
      for (EquipmentSlot slot : ARMOR_SLOTS) {
         if (!mc.player.getEquippedStack(slot).isEmpty()) {
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
      float panelW = PANEL_WIDTH * gs;
      float rowH = ROW_HEIGHT * gs;
      float panelH = rowH * ARMOR_SLOTS.length + 10.0F * gs;

      // Предпочтительная позиция: левый нижний угол (scaled-координаты).
      float preferredX = 20.0F * gs;
      float preferredY = (mc.getWindow().getScaledHeight() - 100.0F) * gs;

      DraggableManager.DragSession session = DraggableManager.getInstance()
            .beginDrag(DRAG_ID, preferredX, preferredY, panelW, panelH);
      try {
         float x = session.positionX();
         float y = session.positionY();

         Hud.drawClientRect(r2, x, y, panelW, panelH, 6.0F * gs, 1.0F, 1.0F);
         r2.text(FontRegistry.INTER_MEDIUM, x + 5.0F * gs, y + 6.0F * gs, 6.0F * gs,
               "Armor", Renderer2D.ColorUtil.getTextColor(1, 1));

         float rowY = y + 12.0F * gs;
         for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = mc.player.getEquippedStack(slot);
            Hud.drawClientRectLight(r2, x + 3.0F * gs, rowY, panelW - 6.0F * gs,
                  rowH - 1.0F * gs, 3.0F * gs, 0.7F, 0.0F);

            if (!stack.isEmpty()) {
               if (ctx != null) {
                  drawItem(ctx, stack, x + 5.0F * gs, rowY + 1.0F * gs, ICON_SIZE * gs);
               }
               r2.text(FontRegistry.INTER_MEDIUM, x + 20.0F * gs, rowY + 5.0F * gs,
                     6.0F * gs, slot.getName(), Renderer2D.ColorUtil.getTextColor(1, 1));

               if (stack.isDamageable()) {
                  drawDurability(r2, stack, x + 20.0F * gs, rowY + 10.0F * gs,
                        panelW - 25.0F * gs, 2.0F * gs);
               }
            }
            rowY += rowH;
         }
      } finally {
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private static void drawDurability(Renderer2D r2, ItemStack stack, float x, float y, float w, float h) {
      int maxDamage = stack.getMaxDamage();
      if (maxDamage <= 0) {
         return;
      }
      float remaining = Math.max(0.0F, 1.0F - (float) stack.getDamage() / maxDamage);
      r2.rect(x, y, w, h, h * 0.5F, ColorUtil.replAlpha(0xFF000000, 0.55F));
      if (remaining > 0.0F) {
         int color = remaining > 0.5F ? 0xFF55DD55 : remaining > 0.25F ? 0xFFFFBB44 : 0xFFFF5555;
         r2.rect(x, y, w * remaining, h, h * 0.5F, ColorUtil.replAlpha(color, 0.95F));
      }
   }

   private static void drawItem(DrawContext ctx, ItemStack stack, float x, float y, float size) {
      float gs = guiScale();
      ctx.getMatrices().pushMatrix();
      try {
         ctx.getMatrices().translate(x / gs, y / gs);
         float scale = size / (16.0F * gs);
         ctx.getMatrices().scale(scale, scale);
         ctx.drawItem(stack, 0, 0);
      } finally {
         ctx.getMatrices().popMatrix();
      }
   }

   private static float guiScale() {
      float scale = mc.getWindow().getScaleFactor();
      return scale > 0.0F ? scale : 1.0F;
   }
}
