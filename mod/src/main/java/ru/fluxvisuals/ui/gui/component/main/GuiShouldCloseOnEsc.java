package ru.fluxvisuals.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.math.animation.anim.util.Easings;

@Environment(EnvType.CLIENT)
public class GuiShouldCloseOnEsc extends GuiScreen {
   public static boolean shouldCloseOnEsc() {
      if (!GuiScreen.exit && GuiScreen.alphaPC.getValue() > 0.0) {
         GuiScreen.alphaPC.run(0.0, 0.36F, Easings.QUART_IN);
         GuiScreen.exit = true;
      }

      return false;
   }
}
