package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;

/**
 * Hit Delay Fix — клиент-сайд ускорение атаки (убирает кулдаун на клиенте).
 * Выключен по умолчанию. Пакеты не шлём.
 */
@IModule(name = "Hit Delay Fix", description = "Убирает клиент-сайд кулдаун атаки (выкл по умолчанию)", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class HitDelayFix extends Module {
   public final BooleanSetting onlyWhenHolding = new BooleanSetting("Only when holding", false);

   public HitDelayFix() {
      this.enable = false;
      this.addSettings(new Setting[]{onlyWhenHolding});
   }

   public boolean shouldBypass() {
      return this.enable;
   }
}
