package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventScreen;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Reach Circle / Attack Ring — кольцо дальности атаки вокруг прицела (стиль Pulse).
 */
@IModule(name = "Reach Circle", description = "Индикатор дальности атаки вокруг прицела", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ReachCircle extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float SURVIVAL_REACH = 3.0F;

   public final SliderSetting radius = new SliderSetting("Radius", 20.0F, 10.0F, 40.0F, 1.0F, false);
   public final SliderSetting thickness = new SliderSetting("Thickness", 1.5F, 0.5F, 4.0F, 0.5F, false);
   public final SliderSetting alpha = new SliderSetting("Alpha", 180.0F, 50.0F, 255.0F, 5.0F, false);
   public final BooleanSetting onlyOnTarget = new BooleanSetting("Only on target", true);

   public ReachCircle() {
      this.addSettings(new Setting[]{radius, thickness, alpha, onlyOnTarget});
   }

   @EventInit
   public void onRender(EventScreen e) {
      if (!this.enable || mc.player == null || mc.world == null) return;
      Renderer2D r2 = e.renderer();
      if (r2 == null) return;

      LivingEntity target = getTarget();
      if (onlyOnTarget.get() && target == null) return;

      float centerX = mc.getWindow().getScaledWidth() / 2.0F;
      float centerY = mc.getWindow().getScaledHeight() / 2.0F;
      float r = radius.get();
      float t = thickness.get();
      int a = (int) alpha.get();

      // Idle ring
      int idleColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(a * 0.3F));
      r2.circle(centerX, centerY, r, 0, 1.0F, idleColor);

      // Fill arc when target is in range
      if (target != null) {
         double dist = mc.player.distanceTo(target);
         float fill = Math.min(1.0F, (float)(dist / SURVIVAL_REACH));
         int fillColor;
         if (fill < 0.5F) {
            fillColor = Renderer2D.ColorUtil.replAlpha(0xFF44FF44, a);
         } else if (fill < 0.85F) {
            fillColor = Renderer2D.ColorUtil.replAlpha(0xFFFFFF44, a);
         } else {
            fillColor = Renderer2D.ColorUtil.replAlpha(0xFFFF4444, a);
         }
         r2.circle(centerX, centerY, r, -90, fill, fillColor);
      }
   }

   private LivingEntity getTarget() {
      if (mc.crosshairTarget instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le) {
         return le;
      }
      return null;
   }
}
