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
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;

@IModule(
   name = "Gamma",
   description = " ",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class Gamma extends Module {
   public ModeSetting mode = new ModeSetting("Type", "Gamma", "Gamma", "Effect");
   private double prevGamma = -1.0;

   public Gamma() {
      this.addSettings(new Setting[]{this.mode});
   }

   @Override
   public void onEnable() {
      super.onEnable();
      if (this.mode.is("Gamma")) {
         this.prevGamma = mc.options.getGamma().getValue();
         mc.options.getGamma().setValue(15.0);
      }
   }

   @Override
   public void onDisable() {
      super.onDisable();
      if (this.mode.is("Gamma")) {
         if (this.prevGamma >= 0.0) {
            mc.options.getGamma().setValue(this.prevGamma);
         } else {
            mc.options.getGamma().setValue(1.0);
         }
      }

      if (this.mode.is("Effect")) {
         mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
      }
   }

   @EventInit
   public void onUpdate(ClientTickEvent e) {
      if (mc.player != null) {
         if (this.mode.is("Gamma")) {
            mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
         }

         if (this.mode.is("Effect")) {
            mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 300, 0, false, false));
         }
      }
   }
}
