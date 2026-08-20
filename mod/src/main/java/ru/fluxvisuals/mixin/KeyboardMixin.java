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

      // Only ignore Fn+F1, Fn+F2, Fn+F3 (290-292) which trigger system actions on some laptops
      // Allow F4-F12 to work normally for keybinds
      if (keyCode >= 290 && keyCode <= 292) {
         ci.cancel(); // Prevent vanilla handling for these specific keys
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
