package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Custom FOV — ручная подстановка угла обзора. Перехватывает финальное значение
 * FOV в GameRendererMixin, поэтому эффект скорости (включая высокие уровни)
 * и ваничный множитель FOV не меняют выбранное игроком значение.
 */
@IModule(
   name = "Custom FOV",
   description = "Ручная настройка FOV, которая не зависит от скорости и эффектов",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class CustomFov extends Module {
   public final SliderSetting fov = new SliderSetting("FOV", 90.0F, 30.0F, 170.0F, 1.0F, false);

   public CustomFov() {
      this.addSettings(new Setting[]{this.fov});
   }

   /**
    * Возвращает выбранный FOV или {@link Float#NaN}, если модуль выключен.
    * Значение clamp-ится на случай старого/бивого конфига.
    */
   public static float getCustomFov() {
      if (mc == null || FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return Float.NaN;
      }
      Module module = FluxVisualsClient.get.manager.getModule(CustomFov.class);
      if (module == null || !module.enable) {
         return Float.NaN;
      }
      CustomFov custom = (CustomFov) module;
      float value = custom.fov.get();
      if (!Float.isFinite(value)) {
         return Float.NaN;
      }
      return Math.max(custom.fov.minimum, Math.min(custom.fov.maximum, value));
   }
}
