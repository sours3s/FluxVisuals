package ru.fluxvisuals.module.bind;

import net.minecraft.client.MinecraftClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.input.KeyInputEvent;
import ru.fluxvisuals.event.input.MouseButtonEvent;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.util.render.utils.KeyUtil;

@Environment(EnvType.CLIENT)
public class BindingManager {
   private static final BindingManager INSTANCE = new BindingManager();
   private boolean initialized = false;
   private boolean awaitingCapture = false;

   public static BindingManager getInstance() {
      return INSTANCE;
   }

   public void initialize() {
      if (!this.initialized) {
         EventManager.register(this);
         this.initialized = true;
      }
   }

   @EventInit
   public void onKeyInput(KeyInputEvent event) {
      if (event.action() == 1 && !this.awaitingCapture) {
         if (FluxVisualsClient.get.manager == null) {
            return;
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.currentScreen != null) {
            // Don't process key binds when a screen is open (chat, inventory, clickgui, etc.)
            // Also avoid processing media keys (Fn+F1/F2) that might trigger system actions
            int key = event.key();
            if (key == 290 || key == 291) { // F1=290, F2=291 (media keys on some keyboards)
               return;
            }
            return;
         }

         Module[] modules = FluxVisualsClient.get.manager.getBind(event.key());
         if (modules != null) {
            for (Module module : modules) {
               module.toggle();
            }
         }
      }
   }

   @EventInit
   public void onMouseInput(MouseButtonEvent event) {
      if (event.isPress() && !this.awaitingCapture) {
         if (FluxVisualsClient.get.manager == null) {
            return;
         }

         MinecraftClient client = MinecraftClient.getInstance();
         if (client != null && client.currentScreen != null) {
            return;
         }

         int bindCode = KeyUtil.mouseButtonToKey(event.button());
         Module[] modules = FluxVisualsClient.get.manager.getBind(bindCode);
         if (modules != null) {
            for (Module module : modules) {
               module.toggle();
            }
         }
      }
   }

   public void clearAllBindings() {
   }

   public void clearModuleBinds(String name) {
   }

   public void setAwaitingCapture(boolean awaiting) {
      this.awaitingCapture = awaiting;
   }

   public boolean isAwaitingCapture() {
      return this.awaitingCapture;
   }

   public void updateModuleBinding(Module module, int keyCode, BindingMode mode) {
      if (module != null) {
         module.bind = keyCode;
      }
   }

   public void putSettingBinding(Module module, Setting setting, BindingMode mode, int keyCode, Object targetValue) {
   }

   public void removeSettingBinding(String moduleName, String settingName) {
   }

   public Object getSettingBinding(String moduleName, String settingName) {
      return null;
   }

   public String formatKeyName(int keyCode) {
      return keyCode == -1 ? "None" : KeyUtil.getKey(keyCode);
   }
}
