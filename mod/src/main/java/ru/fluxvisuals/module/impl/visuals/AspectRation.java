package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

@IModule(
   name = "Aspect Ratio",
   description = "Modifies the screen aspect ratio for a stretched or custom FOV effect",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class AspectRation extends Module {
   public static final ModeSetting aspect = new ModeSetting(
      "Screen Ratio", "16:9", "16:9", "4:3", "1:1", "16:10", "21:9", "32:9", "5:4", "2:1", "Custom"
   );
   public static final SliderSetting customAspect = new SliderSetting("Custom Value", 2.0F, 1.0F, 3.0F, 0.1F, false).hidden(() -> !aspect.is("Custom"));

   public AspectRation() {
      this.addSettings(new Setting[]{aspect, customAspect});
   }

   /**
    * Возвращает разницу между желаемым и текущим аспектом. Миксин прибавляет её
    * к framebuffer-аспекту при построении проекционной матрицы.
    */
   public static float getAspectRation() {
      float target = getTargetAspect();
      if (target <= 0.0F || mc == null) {
         return 0.0F;
      }
      var window = mc.getWindow();
      if (window == null || window.getFramebufferHeight() <= 0) {
         return 0.0F;
      }
      float current = (float) window.getFramebufferWidth() / (float) window.getFramebufferHeight();
      return target - current;
   }

   /** Желаемый аспект или 0, если модуль выключен. */
   public static float getTargetAspect() {
      if (mc == null || FluxVisualsClient.get == null || FluxVisualsClient.get.manager == null) {
         return 0.0F;
      }
      Module module = FluxVisualsClient.get.manager.getModule(AspectRation.class);
      if (module == null || !module.enable) {
         return 0.0F;
      }
      float target = switch (aspect.get()) {
         case "16:9" -> 16.0F / 9.0F;
         case "4:3" -> 4.0F / 3.0F;
         case "1:1" -> 1.0F;
         case "16:10" -> 16.0F / 10.0F;
         case "21:9" -> 21.0F / 9.0F;
         case "32:9" -> 32.0F / 9.0F;
         case "5:4" -> 5.0F / 4.0F;
         case "2:1" -> 2.0F;
         default -> customAspect.get();
      };
      return Float.isFinite(target) && target > 0.0F ? target : 0.0F;
   }
}
