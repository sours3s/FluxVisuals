package ru.fluxvisuals.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Keyboard;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.event.EventManager;
import ru.fluxvisuals.event.input.KeyInputEvent;
import ru.fluxvisuals.module.impl.visuals.MenuSettingsModule;
import ru.fluxvisuals.ui.clickgui.FluxClickGuiScreen;

@Environment(EnvType.CLIENT)
@Mixin({Keyboard.class})
public class KeyboardMixin {
   @Inject(
      method = {"onKey"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void handleMenuKeyEvent(long window, int action, KeyInput input, CallbackInfo ci) {
      if (FluxVisualsClient.isModInitialized() && FluxVisualsClient.get != null) {
         MinecraftClient client = MinecraftClient.getInstance();
         if (client == null || client.getWindow() == null) {
            return;
         }

         boolean clickGuiOpen = client.currentScreen instanceof FluxClickGuiScreen
            || client.currentScreen instanceof ru.fluxvisuals.ui.gui.clickgui.ClickGuiScreen;

         MenuSettingsModule module = MenuSettingsModule.getInstanceIfAvailable();
         int menuKey = module != null && module.bind != -1 ? module.bind : GLFW.GLFW_KEY_RIGHT_SHIFT;
         if (input.comp_4795() == menuKey) {
            // Переключаем только на нажатии (action 1), не на autorepeat (action 2).
            if (action == 1) {
               if (clickGuiOpen) {
                  // Закрываем через сам экран, чтобы не оборвать анимацию закрытия.
                  client.currentScreen.close();
               } else if (client.currentScreen == null) {
                  client.setScreen(new FluxClickGuiScreen());
                  if (client.mouse != null) {
                     client.mouse.unlockCursor();
                  }
               }
            }

            // Подавляем и нажатие, и повторы: иначе они доходят до хендлеров
            // исходныйского MenuScreen и вызывают мерцание/самозакрытие GUI.
            ci.cancel();
            return;
         }

         // Раньше событие уходило в модули только при отсутствии экрана. При открытом
         // ClickGUI оставляем клавиши самой обёртке (поиск, бинды, Esc) — иначе бинды
         // срабатывают во время ввода в GUI.
         if (client.currentScreen != null) {
            return;
         }

         KeyInputEvent event = new KeyInputEvent(window, input.comp_4795(), input.comp_4796(), action, input.comp_4797());
         EventManager.call(event);
         if (event.isCancelled()) {
            ci.cancel();
         }
      }
   }
}
