package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.lifecycle.ClientTickEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;

/**
 * Custom World — клиентские настройки времени, погоды и яркости мира.
 * При отключении восстанавливает исходные значения.
 */
@IModule(
   name = "Custom World",
   description = "Изменяет время суток, погоду и яркость мира на клиенте",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class CustomWorld extends Module {
   public static ModeSetting timeOfDay = new ModeSetting("Time Mode", "Night", "Night", "Day", "Sunset", "Sunrise", "Midnight", "Noon", "Custom");
   public static BooleanSetting timeEnabled = new BooleanSetting("Time Enabled", true);
   public final SliderSetting time = new SliderSetting("Time", 1.0F, 1.0F, 40.0F, 1.0F, false);
   public final BooleanSetting fullBright = new BooleanSetting("FullBright", false);
   public final BooleanSetting weather = new BooleanSetting("Weather", false);
   public final ModeSetting weatherMode = new ModeSetting("WeatherMode", "Clear", "Clear", "Rain", "Storm");

   public static long customTime = -1L;
   public static boolean isEnabled = false;

   private long savedTime = -1L;
   private float savedRain = -1F;
   private float savedThunder = -1F;

   public CustomWorld() {
      this.time.hidden(() -> !timeOfDay.is("Custom"));
      this.addSettings(new Setting[]{timeOfDay, timeEnabled, this.time, this.fullBright, this.weather, this.weatherMode});
   }

   /** Перехватываем серверное время только пока активна кастомная подстановка. */
   public static boolean overridesServerTime() {
      return isEnabled && timeEnabled.get() && customTime >= 0L;
   }

   @Override
   public void onEnable() {
      super.onEnable();
      isEnabled = true;
      if (mc.world != null) {
         savedTime = mc.world.getTimeOfDay();
         savedRain = mc.world.getRainGradient(1.0F);
         savedThunder = mc.world.getThunderGradient(1.0F);
      }
      this.updateTargetTime();
   }

   @Override
   public void onDisable() {
      super.onDisable();
      isEnabled = false;
      customTime = -1L;
      if (mc.world != null) {
         if (savedTime >= 0L) {
            mc.world.setTime(savedTime, 0L, false);
         }
         if (savedRain >= 0.0F) {
            mc.world.setRainGradient(savedRain);
         }
         if (savedThunder >= 0.0F) {
            mc.world.setThunderGradient(savedThunder);
         }
      }
      savedTime = -1L;
      savedRain = -1.0F;
      savedThunder = -1.0F;
   }

   @EventInit
   public void onTick(ClientTickEvent event) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      if (timeEnabled.get()) {
         this.updateTargetTime();
      }

      if (this.fullBright.get()) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 2), mc.player);
      }

      if (this.weather.get()) {
         String mode = this.weatherMode.get();
         boolean raining = mode.equals("Rain") || mode.equals("Storm");
         boolean thundering = mode.equals("Storm");
         mc.world.setRainGradient(raining ? 1.0F : 0.0F);
         mc.world.setThunderGradient(thundering ? 1.0F : 0.0F);
      }
   }

   private void updateTargetTime() {
      switch (timeOfDay.get()) {
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
         case "Custom":
            customTime = (long) this.time.get() * 500L;
            break;
         default:
            customTime = -1L;
      }
   }
}
