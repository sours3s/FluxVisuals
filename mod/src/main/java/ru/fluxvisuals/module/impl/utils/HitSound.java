package ru.fluxvisuals.module.impl.utils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.player.AttackEvent;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.ModeSetting;
import ru.fluxvisuals.module.api.setting.impl.SliderSetting;
import ru.fluxvisuals.util.render.utils.SoundUtil;

@IModule(name = "Hit Sound", description = "Проигрывает звук при ударе по противнику", category = Category.Utils, bind = -1)
@Environment(EnvType.CLIENT)
public class HitSound extends Module {
   public final SliderSetting value = new SliderSetting("Value", 15.0F, 0.0F, 30.0F, 1.0F, false);
   public final ModeSetting mode = new ModeSetting("Mode", "Type1",
         "Type1", "Type2", "Type3", "Type4", "Type5", "Type6", "Type7");

   public HitSound() {
      this.addSettings(new Setting[]{this.value, this.mode});
   }

   @EventInit
   public void onAttack(AttackEvent e) {
      // Ползунок 0..30 — теперь линейно мапится в громкость 0..1 (раньше было (65+value)/100,
      // из-за чего диапазон был всего 0.65..0.95 и громкость почти не менялась).
      SoundUtil.playHitSound(this.mode.get().toLowerCase() + ".wav",
            Math.min(1.0F, this.value.get() / 30.0F));
   }
}
