package ru.fluxvisuals.module.impl.visuals;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.render.RenderEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@IModule(name = "Inventory HUD", description = "Мини-инвентарь на экране + счётчики тотемов и стрел.", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class InventoryHUD extends Module {
   public final BooleanSetting renderInventory = new BooleanSetting("Render Inventory", true);
   public final BooleanSetting renderTotems = new BooleanSetting("Render Totems", true);
   public final BooleanSetting renderArrows = new BooleanSetting("Render Arrows", true);
   public final BooleanSetting compact = new BooleanSetting("Compact Mode", false);

   private int cachedTotems = -1;
   private String cachedArrowInfo = "";
   private long lastRefresh = 0L;

   @EventInit
   public void onRender(RenderEvent e) {
      if (!enable || mc.player == null || mc.currentScreen instanceof ChatScreen) return;
      long now = System.currentTimeMillis();
      if (now - lastRefresh > 250L) { // 4×/s
         refreshCounters();
         lastRefresh = now;
      }

      Renderer2D r2 = e.renderer();
      int fbW = mc.getWindow().getFramebufferWidth();
      int fbH = mc.getWindow().getFramebufferHeight();

      // Панель справа-сверху
      float panelW = compact.get() ? 120.0F : 170.0F;
      float panelH = 120.0F;
      DraggableManager.DragSession session = DraggableManager.getInstance()
         .beginDrag("inventoryHUD", fbW - panelW - 20.0F, 20.0F, panelW, panelH);
      float x = session.positionX();
      float y = session.positionY();

      int bg = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 200);
      r2.rect(x, y, panelW, panelH, 8.0F, bg);
      int outline = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), 60);
      r2.rectOutline(x, y, panelW, panelH, 8.0F, outline, 1.0F);

      float tx = x + 8.0F;
      float ty = y + 8.0F;
      int textCol = Renderer2D.ColorUtil.getTextColor(1, 1);
      int mainCol = Renderer2D.ColorUtil.getMainColor(1, 1);

      // Totems
      if (renderTotems.get()) {
         r2.text(FontRegistry.INTER_MEDIUM, tx, ty, 16.0F, "Тотемы: " + cachedTotems, textCol);
         ty += 20.0F;
      }

      // Arrows
      if (renderArrows.get()) {
         r2.text(FontRegistry.INTER_MEDIUM, tx, ty, 16.0F, cachedArrowInfo, textCol);
         ty += 20.0F;
      }

      // Мини-инвентарь (сверху)
      if (renderInventory.get()) {
         ty += 4.0F;
         int cell = compact.get() ? 18 : 22;
         int gap = 2;
         int startX = (int) (x + 8);
         int startY = (int) ty;
         // Hotbar (9) + 3 ряда по 9 = 36 слотов (для компакта 2 ряда)
         int rows = compact.get() ? 2 : 4;
         for (int row = 0; row < rows; row++) {
            for (int col = 0; col < 9; col++) {
               int slot = row * 9 + col;
               if (slot >= 36) break; // player inventory size
               int cx = startX + col * (cell + gap);
               int cy = startY + row * (cell + gap);
               r2.rect(cx, cy, cell, cell, 3.0F, Renderer2D.ColorUtil.replAlpha(0xFF1A1A1A, 200));
               r2.rectOutline(cx, cy, cell, cell, 3.0F, Renderer2D.ColorUtil.replAlpha(outline, 100), 0.5F);
               ItemStack stack = mc.player.getInventory().getStack(slot);
               if (!stack.isEmpty()) {
                  // Рендер итема — упрощённо через DrawContext недоступен здесь,
                  // но можно нарисовать иконку через рендерेर если есть текстура.
                  // Placeholder: просто имя.
                  r2.text(FontRegistry.INTER_MEDIUM, cx + 2, cy + cell - 4, 10.0F,
                     stack.getCount() > 1 ? String.valueOf(stack.getCount()) : "", mainCol);
               }
            }
         }
      }

      DraggableManager.getInstance().endDrag(session);
   }

   private void refreshCounters() {
      if (mc.player == null) return;
      // Totems
      int totems = 0;
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(Items.TOTEM_OF_UNDYING)) totems += s.getCount();
      }
      cachedTotems = totems;

      // Arrows
      int normal = 0, spectral = 0, tipped = 0;
      for (int i = 0; i < mc.player.getInventory().size(); i++) {
         ItemStack s = mc.player.getInventory().getStack(i);
         if (s.isOf(Items.ARROW)) normal += s.getCount();
         else if (s.isOf(Items.SPECTRAL_ARROW)) spectral += s.getCount();
         else if (s.getItem() == Items.TIPPED_ARROW) tipped += s.getCount();
      }

      // Определить заряженный снаряд в арбалете
      ItemStack main = mc.player.getMainHandStack();
      String loaded = "";
      if (!main.isEmpty() && main.getItem() == Items.CROSSBOW) {
         ChargedProjectilesComponent charged = main.get(DataComponentTypes.CHARGED_PROJECTILES);
         if (charged != null) {
            var projectiles = charged.getProjectiles();
            if (!projectiles.isEmpty()) {
               ItemStack proj = projectiles.get(0);
               if (proj.isOf(Items.ARROW)) loaded = "Обычная";
               else if (proj.isOf(Items.SPECTRAL_ARROW)) loaded = "Призрачная";
               else if (proj.getItem() == Items.TIPPED_ARROW) loaded = "Наложенная";
               else loaded = proj.getName().getString();
            } else {
               loaded = "Пусто";
            }
         }
      } else if (!main.isEmpty() && main.getItem() == Items.BOW) {
         // Лук автовыбирает стрелы из инвентаря
         if (normal > 0) loaded = "Обычная (Лук)";
         else if (spectral > 0) loaded = "Призрачная (Лук)";
         else if (tipped > 0) loaded = "Наложенная (Лук)";
         else loaded = "Без стрел";
      }

      StringBuilder sb = new StringBuilder("Стрелы: ");
      boolean first = true;
      if (normal > 0) { sb.append(first ? "" : ", ").append("Обычные: ").append(normal); first = false; }
      if (spectral > 0) { sb.append(first ? "" : ", ").append("Призрачные: ").append(spectral); first = false; }
      if (tipped > 0) { sb.append(first ? "" : ", ").append("Наложенные: ").append(tipped); first = false; }
      if (!loaded.isEmpty()) sb.append("  ▶ ").append(loaded);
      cachedArrowInfo = sb.toString();
   }
}