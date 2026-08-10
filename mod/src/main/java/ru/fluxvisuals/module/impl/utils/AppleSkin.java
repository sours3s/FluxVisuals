package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;

/**
 * AppleSkin — отображение сытости/насыщения и предпросмотр восстановления здоровья на еде.
 * Миксин AppleSkinMixin рисует оверлей в InGameHud.renderFood.
 */
@IModule(name = "AppleSkin", description = "Показывает насыщение и предпросмотр восстановления на еде", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class AppleSkin extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static AppleSkin INSTANCE;

   public final BooleanSetting showSaturation = new BooleanSetting("Show Saturation", true);
   public final BooleanSetting showExhaustion = new BooleanSetting("Show Exhaustion", false);

   public AppleSkin() {
      this.addSettings(new Setting[]{showSaturation, showExhaustion});
      INSTANCE = this;
   }

   public static AppleSkin getInstance() { return INSTANCE; }

   public boolean isSaturationEnabled() { return this.enable && showSaturation.get(); }
   public boolean isExhaustionEnabled() { return this.enable && showExhaustion.get(); }
}
