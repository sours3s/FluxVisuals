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
 * Camera Overhaul — плавные покачивания камеры при ходьбе и стрейфе.
 * Обрабатывается в CameraMixin через getCameraOverhaul().
 */
@IModule(name = "Camera Overhaul", description = "Плавные покачивания камеры при ходьбе", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class CameraOverhaul extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static CameraOverhaul INSTANCE;

   public final SliderSetting intensity = new SliderSetting("Intensity", 0.6F, 0.1F, 2.0F, 0.1F, false);
   public final SliderSetting rollAmount = new SliderSetting("Roll", 0.3F, 0.0F, 1.5F, 0.05F, false);
   public final SliderSetting bobAmount = new SliderSetting("Bob", 0.4F, 0.0F, 1.5F, 0.05F, false);

   private float roll = 0;
   private float bob = 0;

   public CameraOverhaul() {
      this.addSettings(new Setting[]{intensity, rollAmount, bobAmount});
      INSTANCE = this;
   }

   public static CameraOverhaul getInstance() { return INSTANCE; }

   public float getRoll() {
      if (!this.enable || mc.player == null) return 0;
      float walkSpeed = (float) Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x + mc.player.getVelocity().z * mc.player.getVelocity().z);
      float strafe = mc.player.sidewaysSpeed;
      float targetRoll = (float) (Math.sin(System.currentTimeMillis() / 300.0) * walkSpeed * rollAmount.get() * intensity.get() * strafe);
      roll += (targetRoll - roll) * 0.15F;
      return roll;
   }

   public float getBob() {
      if (!this.enable || mc.player == null) return 0;
      float walkSpeed = (float) mc.player.getVelocity().length();
      float targetBob = (float) (Math.sin(System.currentTimeMillis() / 250.0) * walkSpeed * bobAmount.get() * intensity.get());
      bob += (targetBob - bob) * 0.15F;
      return bob;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      roll = 0;
      bob = 0;
   }
}
