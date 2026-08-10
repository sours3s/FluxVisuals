package ru.fluxvisuals.ui.gui.component.mouse;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.impl.BindSettings;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.mouse.category.GuiMouseClickedCategory;
import ru.fluxvisuals.ui.gui.component.mouse.colorpicker.GuiMouseClickedColorPicker;
import ru.fluxvisuals.ui.gui.component.mouse.module.GuiMouseClickedModule;
import ru.fluxvisuals.ui.gui.component.mouse.setting.GuiMouseClickedSetting;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;
import ru.fluxvisuals.ui.gui.theme.ThemeScreen;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.MathHelper;
import ru.fluxvisuals.util.render.math.ScaleHelper;
import ru.fluxvisuals.util.render.math.ScaledResolution;

@Environment(EnvType.CLIENT)
public class GuiMouseClicked extends GuiScreen {
   public static boolean mouseClicked(Renderer2D renderer2D, double pMouseX, double pMouseY, int pButton) {
      int mouseX = (int)ScaleHelper.calc((float)pMouseX, (float)pMouseY)[0];
      int mouseY = (int)ScaleHelper.calc((float)pMouseX, (float)pMouseY)[1];
      if (captureActiveMouseBind(pButton)) {
         return true;
      }

      ScaledResolution sr = new ScaledResolution(GuiScreen.mc);
      GuiScreen.x = (int)MathHelper.clamp(GuiScreen.x, 0.0F, ScaleHelper.calc(sr.getWidth()) - GuiScreen.width);
      GuiScreen.y = (int)MathHelper.clamp(GuiScreen.y, 0.0F, ScaleHelper.calc(sr.getHeight()) - GuiScreen.height);
      if (!GuiScreen.exit) {
         float searchX = GuiLayout.searchX();
         float searchY = GuiLayout.searchY();
         float searchWidth = GuiLayout.searchWidth();
         float searchHeight = GuiLayout.searchHeight();
         if (pButton == 0 && GuiRenderMain.isHovered(mouseX, mouseY, searchX, searchY, searchWidth, searchHeight)) {
            GuiScreen.activeSearch = true;
            return true;
         }

         GuiMouseClickedCategory.mouseClickedCategory(mouseX, mouseY);
         if (GuiScreen.selectedCategories == Category.Configs) {
            if (GuiMouseClickedSpecial.mouseClickedSpecial(mouseX, mouseY, pButton)) {
               return true;
            }
         }
         if (GuiScreen.selectedCategories == Category.Friends) {
            if (GuiMouseClickedFriends.mouseClickedFriends(mouseX, mouseY, pButton)) {
               return true;
            }
         }

         if (GuiMouseClickedColorPicker.mouseClickedColorPicker(mouseX, mouseY, pButton)) {
            return true;
         }

         if (GuiMouseClickedModule.mouseClickedModule(renderer2D, mouseX, mouseY, pButton)) {
            return true;
         }

         ThemeScreen.mouseClickedTheme(pMouseX, pMouseY, pButton);
      }

      return false;
   }

   private static boolean captureActiveMouseBind(int button) {
      if (button < 0) {
         return false;
      } else if (GuiScreen.activeModuleBind != null) {
         Module module = GuiScreen.activeModuleBind;
         module.bind = -100 - button;
         module.binding = false;
         GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, ru.fluxvisuals.util.render.math.animation.anim.util.Easings.SINE_OUT);
         GuiScreen.activeModuleBind = null;
         if (FluxVisualsClient.get.configManager != null) {
            FluxVisualsClient.get.configManager.autoSave();
         }

         return true;
      } else if (GuiScreen.activeBindSetting != null) {
         BindSettings bindSetting = GuiScreen.activeBindSetting;
         bindSetting.key = -100 - button;
         bindSetting.active = false;
         GuiScreen.activeBindSetting = null;
         if (FluxVisualsClient.get.configManager != null) {
            FluxVisualsClient.get.configManager.autoSave();
         }

         return true;
      } else {
         return false;
      }
   }
}
