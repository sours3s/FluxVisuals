package ru.fluxvisuals.module.impl.visuals;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.impl.EventUpdate;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.IModule;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.BooleanSetting;
import ru.fluxvisuals.module.api.setting.impl.MultiBooleanSetting;

@IModule(
   name = "No Render",
   description = "Отключает рендеринг различных элементов игры",
   category = Category.Visuals,
   bind = -1
)
@Environment(EnvType.CLIENT)
public class NoRender extends Module {
    public static NoRender getInstance() {
        return ru.fluxvisuals.client.FluxVisualsClient.get.manager.get(NoRender.class);
    }

    public static MultiBooleanSetting elements = new MultiBooleanSetting(
         "Элементы",
         new BooleanSetting("Fire", false),
         new BooleanSetting("Bad Effects", false),
         new BooleanSetting("Block Overlay", false),
         new BooleanSetting("Rain", false),
         new BooleanSetting("Warden Darkness", false),
         new BooleanSetting("Armor Stands", false)
    );

    private int lastSelectionHash;

    public NoRender() {
       this.addSettings(new Setting[]{elements});
    }

    @Override
    public void onEnable() {
       super.onEnable();
       this.lastSelectionHash = this.getSettingsHash();
       this.reloadChunks();
    }

    @Override
    public void onDisable() {
       super.onDisable();
       this.reloadChunks();
    }

    @EventInit
    public void onUpdate(EventUpdate e) {
       int currentHash = this.getSettingsHash();
       if (currentHash != this.lastSelectionHash) {
          this.lastSelectionHash = currentHash;
          if (this.enable) {
             this.reloadChunks();
          }
       }
    }

    private int getSettingsHash() {
        int hash = 0;
        for (BooleanSetting setting : elements.settings) {
            hash = hash * 31 + (setting.get() ? 1 : 0);
        }
        return hash;
    }

    private void reloadChunks() {
        try {
            if (mc != null && mc.world != null && mc.worldRenderer != null) {
                mc.worldRenderer.reload();
            }
        } catch (Exception ignored) {}
    }
}

