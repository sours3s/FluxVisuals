package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.entity.LivingEntity;
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
 * Mace Helper — индикатор готовности булавы.
 * Красный при перезарядке, зелёный при полной готовности.
 */
@IModule(name = "Mace Helper", description = "Показывает готовность булавы (красный→зелёный)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class MaceHelper extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();

   public final ModeSetting style = new ModeSetting("Style", "Ring", "Bar", "Ring");
   public final SliderSetting size = new SliderSetting("Size", 1.0F, 0.5F, 2.0F, 0.05F, false);

   public MaceHelper() {
      this.addSettings(new Setting[]{style, size});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null || mc.world == null) return;
      if (!mc.player.getMainHandStack().isOf(Items.MACE)) return;

      float cooldown = mc.player.getAttackCooldownProgress(0);
      float s = size.get();
      float centerX = mc.getWindow().getScaledWidth() / 2.0F;
      float centerY = mc.getWindow().getScaledHeight() / 2.0F;
      float radius = 22.0F * s;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      r2.pushAlpha(0.9F);

      if (style.is("Ring")) {
         // Background ring
         int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF000000, 100);
         r2.circle(centerX, centerY, radius, 0, 1.0F, bgColor);
         // Fill arc by cooldown
         int fillColor = lerpColor(0xFFFF4444, 0xFF44FF44, cooldown);
         r2.circle(centerX, centerY, radius, -90, cooldown, fillColor);
      } else {
         // Bar style
         float barW = 60.0F * s;
         float barH = 6.0F * s;
         float barX = centerX - barW / 2.0F;
         float barY = centerY + 30.0F * s;
         int bgColor = Renderer2D.ColorUtil.replAlpha(0xFF000000, 120);
         r2.rect(barX, barY, barW, barH, 3.0F, bgColor);
         int fillColor = lerpColor(0xFFFF4444, 0xFF44FF44, cooldown);
         r2.rect(barX, barY, barW * cooldown, barH, 3.0F, fillColor);
      }

      // "Ready" label
      if (cooldown >= 0.99F) {
         int green = Renderer2D.ColorUtil.replAlpha(0xFF44FF44, (int)(200 * (0.5F + 0.5F * Math.sin(System.currentTimeMillis() / 200.0))));
         r2.text(FontRegistry.INTER_SEMIBOLD, centerX - 12.0F * s, centerY + 38.0F * s, 9.0F * s, "READY", green);
      }

      r2.popAlpha();
   }

   private static int lerpColor(int c1, int c2, float t) {
      t = Math.max(0, Math.min(1, t));
      int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
      int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
      int r = (int)(r1 + (r2 - r1) * t);
      int g = (int)(g1 + (g2 - g1) * t);
      int b = (int)(b1 + (b2 - b1) * t);
      return 0xFF000000 | (r << 16) | (g << 8) | b;
   }
}
