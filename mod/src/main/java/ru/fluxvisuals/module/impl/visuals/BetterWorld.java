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
 * Better World — меняет время/погоду/яркость.
 * При отключении восстанавливает оригинальные значения.
 */
@IModule(name = "Better World", description = "Изменяет отображение мира", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class BetterWorld extends Module {
   private static BetterWorld instance;

   public final BooleanSetting changeTime = new BooleanSetting("Time Changer", false);
   public final SliderSetting time = new SliderSetting("Time", 1.0F, 1.0F, 40.0F, 1.0F, false);
   public final BooleanSetting fullBright = new BooleanSetting("FullBright", false);
   public final BooleanSetting weather = new BooleanSetting("Weather", false);
   public final ModeSetting weatherMode = new ModeSetting("WeatherMode", "Clear", "Clear", "Rain", "Storm");

   // Для восстановления при отключении
   private long savedTime = -1L;
   private float savedRain = -1F;
   private float savedThunder = -1F;
   private boolean wasActive = false;

   public BetterWorld() {
      this.addSettings(new Setting[]{this.changeTime, this.time, this.fullBright, this.weather, this.weatherMode});
      instance = this;
   }

   public static BetterWorld getInstance() {
      return instance;
   }

   @EventInit
   public void onTick(ClientTickEvent e) {
      if (mc.player == null || mc.world == null) {
         return;
      }

      boolean timeActive = this.changeTime.get();
      boolean weatherActive = this.weather.get();
      boolean brightActive = this.fullBright.get();

      // Сохраняем оригинальные значения при первом включении
      if ((timeActive || weatherActive) && !wasActive) {
         savedTime = mc.world.getTimeOfDay();
         savedRain = mc.world.getRainGradient(1.0F);
         savedThunder = mc.world.getThunderGradient(1.0F);
         wasActive = true;
      }

      if (timeActive) {
         mc.world.setTime(0L, (long) this.time.get() * 500L, false);
      }

      if (brightActive) {
         mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 2), mc.player);
      }

      if (weatherActive) {
         String mode = this.weatherMode.get();
         boolean raining = mode.equals("Rain") || mode.equals("Storm");
         boolean thundering = mode.equals("Storm");
         mc.world.setRainGradient(raining ? 1.0F : 0.0F);
         mc.world.setThunderGradient(thundering ? 1.0F : 0.0F);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (mc.player != null && mc.world != null && wasActive) {
         // Восстанавливаем время
         if (savedTime >= 0) {
            mc.world.setTime(savedTime, 0L, false);
         }
         // Восстанавливаем погоду
         if (savedRain >= 0) {
            mc.world.setRainGradient(savedRain);
         }
         if (savedThunder >= 0) {
            mc.world.setThunderGradient(savedThunder);
         }
      }
      wasActive = false;
      savedTime = -1L;
      savedRain = -1F;
      savedThunder = -1F;
   }
}