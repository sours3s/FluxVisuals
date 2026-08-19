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
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Attack Distance Indicator — показывает дальность атаки в цифрах под прицелом.
 */
@IModule(name = "Attack Distance Indicator", description = "Показывает расстояние до цели в цифрах под прицелом", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class ReachCircle extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static final float SURVIVAL_REACH = 3.0F;

   public final ModeSetting displayMode = new ModeSetting("Display Mode", "Numeric", "Numeric", "Circle");
   public final SliderSetting textSize = new SliderSetting("Text Size", 12.0F, 8.0F, 20.0F, 1.0F, false);
   public final SliderSetting yOffset = new SliderSetting("Y Offset", 18.0F, 0.0F, 50.0F, 1.0F, false);
   public final BooleanSetting onlyOnTarget = new BooleanSetting("Only on target", true);
   public final BooleanSetting showMaxReach = new BooleanSetting("Show Max Reach", false);

   public ReachCircle() {
      this.addSettings(new Setting[]{displayMode, textSize, yOffset, onlyOnTarget, showMaxReach});
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

      if (displayMode.is("Circle")) {
         // Legacy circle mode (kept for backwards compatibility)
         renderCircle(r2, centerX, centerY, target);
      } else {
         // Numeric mode - show distance as text under crosshair
         renderNumeric(r2, centerX, centerY, target);
      }
   }

   private void renderNumeric(Renderer2D r2, float centerX, float centerY, LivingEntity target) {
      float size = textSize.get();
      float offset = yOffset.get();

      if (target != null) {
         double dist = mc.player.distanceTo(target);
         String distStr = String.format("%.1f", dist);

         // Color based on distance
         int color;
         if (dist <= SURVIVAL_REACH) {
            color = 0xFF44FF44; // Green - in range
         } else if (dist <= SURVIVAL_REACH + 1.0) {
            color = 0xFFFFFF44; // Yellow - close
         } else {
            color = 0xFFFF4444; // Red - out of range
         }

         float textWidth = r2.measureText(FontRegistry.INTER_MEDIUM, distStr, size).width;
         r2.text(FontRegistry.INTER_MEDIUM, centerX - textWidth / 2.0F, centerY + offset, size, distStr, color);

         if (showMaxReach.get()) {
            String maxStr = " / " + (int)SURVIVAL_REACH;
            float maxWidth = r2.measureText(FontRegistry.INTER_MEDIUM, maxStr, size * 0.7F).width;
            r2.text(FontRegistry.INTER_MEDIUM, centerX - textWidth / 2.0F + textWidth, centerY + offset, size * 0.7F, maxStr, 0xFFAAAAAA);
         }
      } else if (!onlyOnTarget.get()) {
         // Show max reach when no target
         String str = String.format("%.1f / %.0f", SURVIVAL_REACH, SURVIVAL_REACH);
         float textWidth = r2.measureText(FontRegistry.INTER_MEDIUM, str, size).width;
         r2.text(FontRegistry.INTER_MEDIUM, centerX - textWidth / 2.0F, centerY + offset, size, str, 0xFF888888);
      }
   }

   private void renderCircle(Renderer2D r2, float centerX, float centerY, LivingEntity target) {
      float r = 20.0F;
      float t = 1.5F;
      int a = 180;

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