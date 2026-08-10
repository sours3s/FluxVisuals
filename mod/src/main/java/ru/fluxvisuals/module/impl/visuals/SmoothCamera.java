package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

@IModule(name = "Smooth Camera", description = "Добавляет камере от первого лица плавность движения", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class SmoothCamera extends Module {
   private static SmoothCamera instance;

   public final SliderSetting delay = new SliderSetting("Delay", 5.0F, 5.0F, 50.0F, 0.5F, false);

   public SmoothCamera() {
      this.addSettings(new Setting[]{this.delay});
      instance = this;
   }

   public static SmoothCamera getInstance() {
      return instance;
   }

   public float getDelayValue() {
      return this.delay.get();
   }
}
