package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.ui.draggable.DraggableManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Armor Status — прочность брони и предметов в руках.
 */
@IModule(name = "Armor Status", description = "Показывает прочность брони и предметов в руках", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ArmorStatus extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
   public final BooleanSetting showHeld = new BooleanSetting("Show Held", true);
   public final BooleanSetting showPercent = new BooleanSetting("Show %", true);
   public final SliderSetting scale = new SliderSetting("Scale", 1.0F, 0.5F, 2.0F, 0.05F, false);

   public ArmorStatus() {
      this.addSettings(new Setting[]{showArmor, showHeld, showPercent, scale});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null) return;
      if (mc.currentScreen instanceof ChatScreen) return;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;
      float s = scale.get();

      float x = 20.0F;
      float y = mc.getWindow().getScaledHeight() - 80.0F;
      float itemSize = 14.0F * s;
      float fontSize = 9.0F * s;
      float rowH = itemSize + 4.0F * s;
      int mainColor = Renderer2D.ColorUtil.getMainColor(1, 1);

      EquipmentSlot[] armorSlots = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

      if (showArmor.get()) {
         for (int i = 0; i < armorSlots.length; i++) {
            ItemStack stack = mc.player.getEquippedStack(armorSlots[i]);
            if (stack.isEmpty()) continue;
            float iy = y + i * rowH;
            r2.pushAlpha(0.9F);
            DrawContext ctx = e.drawContext();
            if (ctx != null) {
               ctx.getMatrices().pushMatrix();
               ctx.getMatrices().translate(x / mc.getWindow().getScaleFactor(), iy / mc.getWindow().getScaleFactor());
               ctx.getMatrices().scale(s, s);
               ctx.drawItem(stack, 0, 0);
               ctx.getMatrices().popMatrix();
            }
            if (stack.isDamageable() && showPercent.get()) {
               float pct = 1.0F - (float) stack.getDamage() / stack.getMaxDamage();
               int color = pct > 0.5F ? 0xFF55FF55 : pct > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;
               r2.text(FontRegistry.INTER_MEDIUM, x + itemSize + 3.0F * s, iy + 3.0F * s, fontSize,
                  String.valueOf((int)(pct * 100)) + "%", color);
            }
            r2.popAlpha();
         }
      }

      if (showHeld.get()) {
         float hx = x;
         float hy = y + armorSlots.length * rowH + 4.0F * s;
         for (int hand = 0; hand < 2; hand++) {
            ItemStack stack = hand == 0 ? mc.player.getMainHandStack() : mc.player.getOffHandStack();
            if (stack.isEmpty()) { hx += itemSize + 4.0F * s; continue; }
            r2.pushAlpha(0.9F);
            DrawContext ctx = e.drawContext();
            if (ctx != null) {
               ctx.getMatrices().pushMatrix();
               ctx.getMatrices().translate(hx / mc.getWindow().getScaleFactor(), hy / mc.getWindow().getScaleFactor());
               ctx.getMatrices().scale(s, s);
               ctx.drawItem(stack, 0, 0);
               ctx.getMatrices().popMatrix();
            }
            if (stack.isDamageable() && showPercent.get()) {
               float pct = 1.0F - (float) stack.getDamage() / stack.getMaxDamage();
               int color = pct > 0.5F ? 0xFF55FF55 : pct > 0.25F ? 0xFFFFFF55 : 0xFFFF5555;
               r2.text(FontRegistry.INTER_MEDIUM, hx + itemSize + 3.0F * s, hy + 3.0F * s, fontSize,
                  String.valueOf((int)(pct * 100)) + "%", color);
            }
            r2.popAlpha();
            hx += itemSize + 4.0F * s;
         }
      }
   }
}
