package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventUpdate;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;

@IModule(
   name = "Custom World",
   category = Category.Visuals,
   description = "Allows you to change the world time and visual environment clientside",
   bind = -1
)
@Environment(EnvType.CLIENT)
public class CustomWorld extends Module {
   public static ModeSetting timeOfDay = new ModeSetting("Time Mode", "Night", "Night", "Day", "Sunset", "Sunrise", "Midnight", "Noon");

   public static BooleanSetting timeEnabled = new BooleanSetting("Time Enabled", true);

   public static long customTime = -1L;
   public static boolean isEnabled = false;

   public CustomWorld() {
      this.addSettings(new Setting[]{timeOfDay, timeEnabled});
   }

   @Override
   public void onEnable() {
      super.onEnable();
      isEnabled = true;
      this.updateTargetTime();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      isEnabled = false;
      customTime = -1L;
   }

   @EventInit
   public void onUpdate(EventUpdate e) {
      if (isEnabled && mc.world != null) {
         this.updateTargetTime();
      }
   }

   private void updateTargetTime() {
      String currentMode = timeOfDay.get();
      switch (currentMode) {
         case "Day":
            customTime = 1000L;
            break;
         case "Sunset":
            customTime = 12000L;
            break;
         case "Sunrise":
            customTime = 23000L;
            break;
         case "Midnight":
            customTime = 13000L;
            break;
         case "Night":
            customTime = 18000L;
            break;
         case "Noon":
            customTime = 6000L;
            break;
         default:
            customTime = -1L;
      }
   }
}