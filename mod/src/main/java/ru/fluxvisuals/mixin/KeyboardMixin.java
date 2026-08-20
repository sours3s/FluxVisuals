package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.input.KeyInputEvent;
import ru.fluxvisuals.module.impl.visuals.MenuSettingsModule;
import ru.fluxvisuals.ui.gui.GuiClient;

@Environment(EnvType.CLIENT)
@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleMenuKeyEvent(long window, int action, KeyInput input, CallbackInfo ci) {
      if (!FluxVisualsClient.isModInitialized() || FluxVisualsClient.get == null) return;

      MinecraftClient client = MinecraftClient.getInstance();
      if (client == null || client.getWindow() == null) return;

      int keyCode = input.comp_4795();

      // Ignore all function keys F1-F12 (290-301) and media keys that can trigger system actions
      // This prevents system actions like opening file explorer, brightness control, etc.
      if (keyCode >= 290 && keyCode <= 301) {
         ci.cancel(); // Prevent vanilla handling too
         return;
      }

      // Check for menu key FIRST before firing event to other handlers
      if (action == 1 && client.currentScreen == null) {
         MenuSettingsModule module = MenuSettingsModule.getInstanceIfAvailable();
         int menuKey = module != null && module.bind != -1 ? module.bind : 344; // DEFAULT: Right Shift
         if (menuKey != -1 && keyCode == menuKey) {
            GuiClient gui = FluxVisualsClient.get.guiClient;
            if (gui != null) {
               client.setScreen(gui);
               if (client.mouse != null) {
                  client.mouse.unlockCursor();
               }
               ci.cancel(); // Cancel vanilla processing AND prevent event propagation
               return;
            }
         }
      }

      // Fire event for other handlers (BindingManager, etc.)
      if (client.currentScreen == null) {
         KeyInputEvent event = new KeyInputEvent(window, keyCode, input.comp_4796(), action, input.comp_4797());
         EventManager.call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
