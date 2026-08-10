package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Motion Blur — эффект размытия движения.
 * Упрощённая реализация: overlay-based motion blur через пост-рендер пасс.
 */
@IModule(name = "Motion Blur", description = "Плавный эффект размытия движения", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class MotionBlur extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static MotionBlur INSTANCE;

   public final SliderSetting strength = new SliderSetting("Strength", 0.5F, 0.1F, 1.5F, 0.1F, false);

   private float prevYaw = 0;
   private float prevPitch = 0;
   private float blurAmount = 0;

   public MotionBlur() {
      this.addSettings(new Setting[]{strength});
      INSTANCE = this;
   }

   public static MotionBlur getInstance() { return INSTANCE; }

   /** Вызывается каждый кадр для расчёта blur. */
   public float getBlurAmount() {
      if (!this.enable || mc.player == null) return 0;
      float deltaYaw = mc.player.getYaw() - prevYaw;
      float deltaPitch = mc.player.getPitch() - prevPitch;
      float movement = (float) mc.player.getVelocity().length();
      float lookDelta = Math.abs(deltaYaw) + Math.abs(deltaPitch);
      float targetBlur = Math.min(1.0F, (lookDelta * 0.02F + movement * 0.3F) * strength.get());
      blurAmount += (targetBlur - blurAmount) * 0.2F;
      prevYaw = mc.player.getYaw();
      prevPitch = mc.player.getPitch();
      return blurAmount;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      blurAmount = 0;
   }
}
