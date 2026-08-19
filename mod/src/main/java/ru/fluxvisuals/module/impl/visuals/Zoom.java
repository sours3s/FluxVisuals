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
 * Zoom — плавное оптическое приближение с настраиваемой клавишей.
 * ZoomFOV обрабатывается в GameRendererMixin через this.getCurrentZoom().
 */
@IModule(name = "Zoom", description = "Плавное оптическое приближение (bind клавиши)", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class Zoom extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static Zoom INSTANCE;

   public final SliderSetting multiplier = new SliderSetting("Multiplier", 4.0F, 2.0F, 10.0F, 0.5F, false);
   public final SliderSetting smooth = new SliderSetting("Smooth", 0.15F, 0.05F, 0.5F, 0.01F, false);

   private float currentFov = 1.0F;
   private float targetFov = 1.0F;

   public Zoom() {
      this.addSettings(new Setting[]{multiplier, smooth});
      INSTANCE = this;
   }

   public static Zoom getInstance() { return INSTANCE; }

   public float getCurrentFov() {
      if (!this.enable || mc.player == null) return 1.0F;
      targetFov = 1.0F / multiplier.get();
      currentFov += (targetFov - currentFov) * smooth.get();
      return currentFov;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      currentFov = 1.0F;
   }
}
