package ru.fluxvisuals.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;
import ru.fluxvisuals.util.render.math.ScaleHelper;

@Environment(EnvType.CLIENT)
public class GuiMouseScrolled extends GuiScreen {
   public static boolean mouseScrolled(double pMouseX, double pMouseY, double pScrollX, double pScrollY) {
      float[] mouseCoords = ScaleHelper.calc((float)pMouseX, (float)pMouseY);
      float mouseX = mouseCoords[0];
      float mouseY = mouseCoords[1];
      float x1 = GuiScreen.x + 104.735F + 5.0F;
      float y1 = GuiScreen.y + 34.025F + 5.0F;
      float rectWidth = 251.5F;
      float rectHeight = 199.5F;
      if (!GuiScreen.exit && GuiRenderMain.isHovered(mouseX, mouseY, x1, y1, rectWidth, rectHeight)) {
         GuiScreen.getScrollUtil().handleScroll(pScrollY);
         return true;
      } else {
         return false;
      }
   }
}
