package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(name = "Inventory HUD", description = "Мини-инвентарь на экране + счётчики тотемов и стрел. Количество стрел показывается на слоте со стрелами.", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class InventoryHUD extends Module {
   public final BooleanSetting renderInventory = new BooleanSetting("Render Inventory", true);
   public final BooleanSetting renderTotems = new BooleanSetting("Render Totems", true);
   public final BooleanSetting renderArrows = new BooleanSetting("Render Arrows", true);
   public final ModeSetting position = new ModeSetting("Position", "Top Right", "Top Right", "Top Left", "Bottom Right", "Bottom Left");
   public final BooleanSetting showCounts = new BooleanSetting("Show Counts", true);

   private int cachedTotems = -1;
   private int cachedArrows = 0;
   private int cachedSpectralArrows = 0;
   private int cachedTippedArrows = 0;
   private long lastRefresh = 0L;

   @EventInit
   public void onRender(EventScreen e) {
      if (!enable || mc.player == null || mc.currentScreen instanceof ChatScreen) return;
      long now = System.currentTimeMillis();
      if (now - lastRefresh > 250L) { // 4×/s
         refreshCounters();
         lastRefresh = now;
      }

      Renderer2D r2 = e.renderer();
      DraggableManager.DragSession session = null;
      float x = 10.0F, y = 10.0F;

      if (renderInventory.get()) {
         int cell = 20;
         int gap = 2;
         int cols = 9;
         int rows = 3;
         float panelW = cols * (cell + gap) + 16.0F;
         float panelH = rows * (cell + gap) + 16.0F + 20.0F;

         // Position based on setting
         int fbW = mc.getWindow().getScaledWidth();
         int fbH = mc.getWindow().getScaledHeight();
         float px = switch (position.get()) {
            case "Top Left" -> 10.0F;
            case "Bottom Right" -> fbW - panelW - 10.0F;
            case "Bottom Left" -> 10.0F;
            case "Top Right" -> fbW - panelW - 10.0F;
            default -> fbW - panelW - 10.0F;
         };
         float py = switch (position.get()) {
            case "Top Left", "Top Right" -> 10.0F;
            case "Bottom Right", "Bottom Left" -> fbH - panelH - 10.0F;
            default -> 10.0F;
         };

         session = DraggableManager.getInstance().beginDrag("inventoryHUD", px, py, panelW, panelH);
         x = session.positionX();
         y = session.positionY();

         DrawContext ctx = e.drawContext();
         int bg = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 200);
         r2.rect(x, y, panelW, panelH, 8.0F, bg);
         int outline = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), 60);
         r2.rectOutline(x, y, panelW, panelH, 8.0F, outline, 1.0F);

         // Title
         r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + 6.0F, 11.0F, "Inventory", Renderer2D.ColorUtil.getTextColor(1, 1));

         // Inventory grid (3 rows, 9 cols) starting from slot 9 (skip hotbar)
         int startX = (int) (x + 8.0F);
         int startY = (int) (y + 22.0F);
         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
               int slot = 9 + row * 9 + col; // Skip slots 0-8 (hotbar)
               int cx = startX + col * (cell + gap);
               int cy = startY + row * (cell + gap);

               r2.rect(cx, cy, cell, cell, 3.0F, Renderer2D.ColorUtil.replAlpha(0xFF1A1A1A, 200));
               r2.rectOutline(cx, cy, cell, cell, 3.0F, Renderer2D.ColorUtil.replAlpha(0xFFFFFFFF, 30), 0.5F);

               ItemStack stack = mc.player.getInventory().getStack(slot);
               if (!stack.isEmpty() && ctx != null) {
                  // Draw item icon + count
                  ctx.drawItem(mc.player, stack, cx + 2, cy + 2, 0);
                  if (stack.getCount() > 1) {
                     ctx.drawStackOverlay(mc.textRenderer, stack, cx + 2, cy + 2);
                  }
                  // Also show total arrow count on arrow slot
                  if (stack.isOf(Items.ARROW) || stack.isOf(Items.SPECTRAL_ARROW) || stack.isOf(Items.TIPPED_ARROW)) {
                     int totalArrows = cachedArrows + cachedSpectralArrows + cachedTippedArrows;
                     if (showCounts.get() && totalArrows > 0) {
                        String arrowText = String.valueOf(totalArrows);
                        float textSize = 9.0F;
                        float textW = r2.measureText(FontRegistry.INTER_MEDIUM, arrowText, textSize).width;
                        r2.text(FontRegistry.INTER_MEDIUM, cx + cell - textW - 2.0F, cy + cell - textSize - 2.0F, textSize, arrowText, 0xFFFFFFFF);
                     }
                  }
               }
            }
         }
      }

      // Totems and arrows info (right side or above inventory)
      if (renderTotems.get() || renderArrows.get()) {
         float infoX = x + 8.0F;
         float infoY = y + (renderInventory.get() ? 0 : 0) + 8.0F;

         if (renderTotems.get() && cachedTotems >= 0) {
            r2.text(FontRegistry.INTER_MEDIUM, infoX, infoY, 11.0F, "Totems: " + cachedTotems, Renderer2D.ColorUtil.getTextColor(1, 1));
            infoY += 14.0F;
         }

         if (renderArrows.get()) {
            StringBuilder sb = new StringBuilder();
            if (cachedArrows > 0) sb.append("Arrows: ").append(cachedArrows);
            if (cachedSpectralArrows > 0) sb.append(sb.length() > 0 ? ", " : "Spectral: ").append(cachedSpectralArrows);
            if (cachedTippedArrows > 0) sb.append(sb.length() > 0 ? ", " : "Tipped: ").append(cachedTippedArrows);
            if (sb.length() == 0) sb.append("Arrows: 0");
            r2.text(FontRegistry.INTER_MEDIUM, infoX, infoY, 11.0F, sb.toString(), Renderer2D.ColorUtil.getTextColor(1, 1));
         }
      }

      if (session != null) {
         DraggableManager.getInstance().endDrag(session);
      }
   }

   private void refreshCounters() {
      if (mc.player == null) return;
      cachedTotems = 0;
      cachedArrows = 0;
      cachedSpectralArrows = 0;
      cachedTippedArrows = 0;

      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(Items.TOTEM_OF_UNDYING)) cachedTotems += s.getCount();
         else if (s.isOf(Items.ARROW)) cachedArrows += s.getCount();
         else if (s.isOf(Items.SPECTRAL_ARROW)) cachedSpectralArrows += s.getCount();
         else if (s.isOf(Items.TIPPED_ARROW)) cachedTippedArrows += s.getCount();
      }
   }
}