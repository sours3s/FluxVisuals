package ru.fluxvisuals.ui.gui.component.mouse.category;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;
import ru.fluxvisuals.util.render.math.animation.Direction;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedCategory extends GuiScreen {
   public static void mouseClickedCategory(int mouseX, int mouseY) {
      float x1 = GuiScreen.x;
      float y1 = GuiScreen.y;
      float downY = 0.0F;

      for (Category category : GuiScreen.categories) {
         if (GuiRenderMain.isHovered(mouseX, mouseY, x1, y1 + 43.365F + downY, GuiLayout.SIDEBAR, GuiLayout.CATEGORY_STEP) && GuiScreen.selectedCategories != category) {
            GuiScreen.animation15.setDirection(Direction.BACKWARDS);
            GuiScreen.activeColorPicker = null;
            GuiScreen.selectedCategories = category;
            GuiScreen.modules = FluxVisualsClient.get.manager.getType(GuiScreen.selectedCategories);
            GuiScreen.categoryAnimation.reset();
            GuiScreen.moduleAnimation.reset();
            GuiScreen.getScrollUtil().reset();
            GuiScreen.scrollToModule = null;
            FluxVisualsClient.get.guiManager.setGuiCategory(category);
         }

         downY += GuiLayout.CATEGORY_STEP;
      }
   }
}