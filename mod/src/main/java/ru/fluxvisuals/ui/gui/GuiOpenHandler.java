package ru.fluxvisuals.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.event.EventInit;
import ru.fluxvisuals.event.input.KeyInputEvent;

@Environment(EnvType.CLIENT)
public class GuiOpenHandler {
   @EventInit
   public void onKeyInput(KeyInputEvent event) {
   }
}
