package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;

/**
 * First Person Model — отображение тела игрока от первого лица.
 * Базовая реализация: рендер собственного тела при приседании (viewmodel смещение).
 */
@IModule(name = "First Person Model", description = "Показывает тело игрока от первого лица", category = Category.Visuals, bind = -1)
@Environment(EnvType.CLIENT)
public class FirstPersonModel extends Module {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private static FirstPersonModel INSTANCE;

   public final BooleanSetting showBody = new BooleanSetting("Show Body", true);
   public final BooleanSetting showArmor = new BooleanSetting("Show Armor", true);
   public final BooleanSetting onlySneaking = new BooleanSetting("Only Sneaking", false);

   public FirstPersonModel() {
      this.addSettings(new Setting[]{showBody, showArmor, onlySneaking});
      INSTANCE = this;
   }

   public static FirstPersonModel getInstance() { return INSTANCE; }

   public boolean shouldRenderBody() {
      if (!this.enable || mc.player == null || mc.options == null) return false;
      if (!mc.options.getPerspective().isFirstPerson()) return false;
      if (onlySneaking.get() && !mc.player.isSneaking()) return false;
      return showBody.get();
   }
}
