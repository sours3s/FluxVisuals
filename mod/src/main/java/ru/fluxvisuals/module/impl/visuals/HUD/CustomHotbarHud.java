package ru.fluxvisuals.module.impl.visuals.HUD;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.AnimationMath;

/**
 * Кастомный хотбар: девять слотов по центру внизу с плавным выделением активного.
 * Позиция ванильная, перетаскивание не предусмотрено.
 */
@Environment(EnvType.CLIENT)
public final class CustomHotbarHud {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   private static final int SLOT_COUNT = 9;

   // Размеры в scaled-единицах.
   private static final float SLOT_SIZE = 20.0F;
   private static final float SLOT_GAP = 2.0F;

   private static float selectedAnim = 0.0F;

   private CustomHotbarHud() {
   }

   /** Настройка включения будет добавлена в Hud позже; пока элемент доступен всегда. */
   public static boolean isActive() {
      return true;
   }

   public static boolean hasContent() {
      if (mc.player == null) {
         return false;
      }
      for (int slot = 0; slot < SLOT_COUNT; slot++) {
         if (!mc.player.getInventory().getStack(slot).isEmpty()) {
            return true;
         }
      }
      return false;
   }

   public static void render(Renderer2D r2, DrawContext ctx) {
      if (r2 == null || ctx == null || mc.player == null || mc.world == null) {
         return;
      }

      float gs = guiScale();
      float slot = SLOT_SIZE * gs;
      float gap = SLOT_GAP * gs;
      float step = slot + gap;
      float panelW = SLOT_COUNT * slot + (SLOT_COUNT - 1) * gap;
      float panelH = slot;
      float x = (mc.getWindow().getWidth() - panelW * gs) * 0.5F;
      float y = (mc.getWindow().getScaledHeight() - SLOT_SIZE - 2.0F) * gs;

      selectedAnim = AnimationMath.animation(selectedAnim, mc.player.getInventory().getSelectedSlot(), 0.41F);

      Hud.drawClientRect(r2, x - 3.0F * gs, y - 3.0F * gs,
            panelW + 6.0F * gs, panelH + 6.0F * gs, 6.0F * gs, 1.0F, 1.0F);

      int accent = Renderer2D.ColorUtil.getMainColor(1, 1);
      for (int i = 0; i < SLOT_COUNT; i++) {
         float sx = x + i * step;
         r2.rect(sx, y, slot, slot, 4.0F * gs,
               ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 0.75F));
         r2.rectOutline(sx, y, slot, slot, 4.0F * gs,
               ColorUtil.replAlpha(0xFFFFFFFF, 0.08F), 0.6F * gs);

         ItemStack stack = mc.player.getInventory().getStack(i);
         if (!stack.isEmpty()) {
            float ix = sx / gs + 2.0F;
            float iy = y / gs + 2.0F;
            ctx.drawItem(stack, (int) ix, (int) iy);
            ctx.drawStackOverlay(mc.textRenderer, stack, (int) ix, (int) iy);
         }
      }

      // Плавный акцент на выбранном слоте.
      float selectedX = x + step * selectedAnim;
      r2.rectOutline(selectedX - 1.0F * gs, y - 1.0F * gs,
            slot + 2.0F * gs, slot + 2.0F * gs, 5.0F * gs,
            ColorUtil.replAlpha(accent, 0.85F), 1.0F * gs);
      r2.rect(selectedX + 2.0F * gs, y + slot - 2.0F * gs,
            slot - 4.0F * gs, 2.0F * gs, 1.0F * gs, accent);
   }

   private static float guiScale() {
      float scale = mc.getWindow().getScaleFactor();
      return scale > 0.0F ? scale : 1.0F;
   }
}
