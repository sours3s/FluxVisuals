package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

@IModule(name = "Client Sound", description = "Проигрывает звук при переключении функции", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class ClientSound extends Module {
   private static ClientSound instance;

   public final SliderSetting value = new SliderSetting("Value", 50.0F, 0.0F, 100.0F, 1.0F, true);

   public ClientSound() {
      this.addSettings(new Setting[]{this.value});
      instance = this;
   }

   public static ClientSound getInstance() {
      return instance;
   }

   /** Громкость звуков переключения в диапазоне 0..1 (0 = тишина, 1 = максимум). */
   public float getVolume() {
      return Math.min(1.0F, Math.max(0.0F, this.value.get() / 100.0F));
   }
}
