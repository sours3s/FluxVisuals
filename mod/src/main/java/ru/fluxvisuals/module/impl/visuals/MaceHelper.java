package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Mace Helper — показывает зелёное свечение на слоте булавы в хотбаре, когда она полностью заряжена.
 */
@IModule(name = "Mace Helper", description = "Показывает зелёное свечение на булаве в хотбаре, когда она готова к удару", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class MaceHelper extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final BooleanSetting showGlowOnHotbar = new BooleanSetting("Glow on Hotbar", true);
   public final BooleanSetting showRing = new BooleanSetting("Show Ring", false);
   public final SliderSetting size = new SliderSetting("Ring Size", 1.0F, 0.5F, 2.0F, 0.05F, false);

   public MaceHelper() {
      this.addSettings(new Setting[]{showGlowOnHotbar, showRing, size});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null || mc.world == null) return;

      // Check if mace is in hand
      boolean mainHandMace = mc.player.getMainHandStack().isOf(Items.MACE);
      boolean offHandMace = mc.player.getOffHandStack().isOf(Items.MACE);
      if (!mainHandMace && !offHandMace) return;

      float cooldown = mc.player.getAttackCooldownProgress(0);
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      // Show ring in center of screen (optional)
      if (showRing.get()) {
         float s = size.get();
         float centerX = mc.getWindow().getScaledWidth() / 2.0F;
         float centerY = mc.getWindow().getScaledHeight() / 2.0F;
         float radius = 22.0F * s;
         int color = cooldown >= 1.0F ? 0xFF44FF44 : 0xFFFF4444;
         // r2.circleOutline(centerX, centerY, radius, 0, 1.0F, color, 2.0F);
            // Draw ring by drawing outer circle and clearing inner
            r2.circle(centerX, centerY, radius + 2, 0, 1.0F, color);
            r2.circle(centerX, centerY, radius, 0, 1.0F, 0x00000000);
      }

      // Show green glow on hotbar slot if fully charged
      if (showGlowOnHotbar.get() && cooldown >= 1.0F) {
         renderHotbarGlow(r2);
      }
   }

   private void renderHotbarGlow(Renderer2D r2) {
      // Find mace slot in hotbar
      int maceSlot = -1;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.isOf(Items.MACE)) {
            maceSlot = i;
            break;
         }
      }

      if (maceSlot == -1) return;

      float slotSize = 24.0F;
      float slotOffset = 2.0F;
      float hotbarWidth = 198.0F;
      float halfWidth = mc.getWindow().getScaledWidth() / 2.0F;
      float hotbarX = halfWidth - hotbarWidth / 2.0F;
      float hotbarY = mc.getWindow().getScaledHeight() - 22.0F;

      float slotX = hotbarX + (maceSlot * (slotSize - slotOffset)) + 4.0F;
      float slotY = hotbarY + (slotSize - 16.0F) / 2.0F - 4.0F;

      // Green glow
      int glowColor = 0x4400FF00;
      r2.rect(slotX - 2.0F, slotY - 2.0F, 20.0F, 20.0F, 3.0F, glowColor);

      // Additional outer glow for visibility
      int outerGlow = 0x2200FF00;
      r2.rectOutline(slotX - 4.0F, slotY - 4.0F, 24.0F, 24.0F, 4.0F, 0x4400FF00, 1.0F);
   }
}