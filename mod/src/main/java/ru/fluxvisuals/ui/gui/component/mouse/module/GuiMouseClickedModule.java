package ru.fluxvisuals.ui.gui.component.mouse.module;

import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.mouse.setting.GuiMouseClickedSetting;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;
import ru.fluxvisuals.ui.gui.component.setting.GuiRenderSetting;
import ru.fluxvisuals.util.keyboard.Keyboard;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.Direction;
import ru.fluxvisuals.util.render.math.animation.anim.util.Easings;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedModule extends GuiScreen {
   public static boolean mouseClickedModule(Renderer2D renderer2D, int mouseX, int mouseY, int pButton) {
      if (!GuiRenderMain.isHovered(mouseX, mouseY, GuiLayout.clipX(), GuiLayout.clipY(), GuiLayout.clipWidth(), GuiLayout.clipHeight())) {
         return false;
      }

      List<Module> filteredModules = GuiScreen.modules;
      if (GuiScreen.activeSearch && !GuiScreen.searchText.isEmpty()) {
         String searchLower = GuiScreen.searchText.toLowerCase().trim();
         List<Module> source = ru.fluxvisuals.client.FluxVisualsClient.get != null && ru.fluxvisuals.client.FluxVisualsClient.get.manager != null
               ? ru.fluxvisuals.client.FluxVisualsClient.get.manager.getModules()
               : GuiScreen.modules;
         filteredModules = source.stream()
               .filter(modulex -> modulex.name.toLowerCase().contains(searchLower))
               .collect(Collectors.toList());
      }

      float moduleWidth = GuiLayout.moduleWidth();
      int index = 1;
      float downY = GuiScreen.getScrollUtil().getScroll();
      float downYSetting1 = 0.0F;
      float downYSetting2 = 0.0F;

      for (Module module : filteredModules) {
         boolean rightColumn = index % 2 == 0;
         float moduleX = GuiLayout.moduleColumnX(index);
         float currentDownY = rightColumn ? (downY + downYSetting2 - GuiLayout.COLUMN_STAGGER) : (downY + downYSetting1);

         float settingsHeight = 0.0F;
         if (GuiScreen.openSettingsModules.contains(module)) {
            settingsHeight = GuiRenderSetting.getSettingsTotalHeight(renderer2D, module.getSettingsForGUI(), GuiLayout.settingWidth());
         }

         float moduleY = GuiScreen.y + 43.365F + currentDownY;
         float moduleHeight = GuiLayout.MODULE_HEIGHT;

         // Settings clicks first (so they take priority over the toggle area below them)
         if (GuiScreen.openSettingsModules.contains(module) && pButton == 0) {
            float settingY = GuiScreen.y + 64.69F + currentDownY + 4.0F;
            float settingX = GuiLayout.settingX(moduleX);
            float settingWidth = GuiLayout.settingWidth();
            float totalSettingsHeight = 0.0F;

            for (Setting setting : module.getSettingsForGUI()) {
               float actualSettingY = settingY + totalSettingsHeight;
               if (GuiMouseClickedSetting.handleSettingClick(renderer2D, setting, settingX, actualSettingY, settingWidth, mouseX, mouseY, pButton)) {
                  return true;
               }

               totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting, settingWidth) + 1.0F;
            }
         }

         // Bind button hit-test
         if (module.binding || module.bind != -1) {
            float moduleNameX = GuiLayout.moduleNameX(moduleX);
            float moduleNameY = GuiScreen.y + 49.555F + currentDownY;
            float moduleNameWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, module.name, 14.0F).width;
            float bindX = moduleNameX + moduleNameWidth + 4.0F;
            float bindY = moduleNameY - 1.0F;
            String keyText = module.binding ? "..." : Keyboard.keyName(module.bind);
            float keyTextWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
            float buttonWidth = Math.max(16.0F, keyTextWidth + 8.0F);
            if (GuiRenderMain.isHovered(mouseX, mouseY, bindX, bindY, buttonWidth, 16.0F)) {
               if (pButton == 2) {
                  toggleBinding(module);
                  return true;
               }

               if (module.binding && pButton >= 0 && pButton <= 1) {
                  module.bind = -100 - pButton;
                  module.binding = false;
                  GuiScreen.activeModuleBind = null;
                  GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, Easings.SINE_OUT);
                  if (ru.fluxvisuals.client.FluxVisualsClient.get != null && ru.fluxvisuals.client.FluxVisualsClient.get.manager != null) {
                     ru.fluxvisuals.client.FluxVisualsClient.get.manager.invalidateCaches();
                  }
                  return true;
               }
            }
         }

         boolean hoveredModule = GuiRenderMain.isHovered(mouseX, mouseY, moduleX, moduleY, moduleWidth, moduleHeight);
         if (hoveredModule && pButton == 0) {
            module.toggle();
         }

         if (hoveredModule && pButton == 1 && !module.getSettingsForGUI().isEmpty()) {
            if (GuiScreen.openSettingsModules.contains(module)) {
               GuiScreen.openSettingsModules.remove(module);
               GuiScreen.scrollToModule = null;
               GuiScreen.getModuleSettingsAnimation(module).run(0.0, 0.42F, Easings.EXPO_OUT);
               GuiScreen.getModuleSettingsAlphaAnimation(module).run(0.0, 0.13F, Easings.SINE_OUT);
               if (GuiScreen.activeColorPicker != null && module.getSettingsForGUI().contains(GuiScreen.activeColorPicker)) {
                  GuiScreen.animation15.setDirection(Direction.BACKWARDS);
                  GuiScreen.activeColorPicker = null;
                  GuiScreen.colorPickerX = 0.0F;
                  GuiScreen.colorPickerY = 0.0F;
               }
            } else {
               GuiScreen.openSettingsModules.add(module);
               GuiScreen.scrollToModule = module;
               GuiScreen.getModuleSettingsAlphaAnimation(module).run(1.0, 0.34F, Easings.SINE_OUT);
               GuiScreen.getModuleSettingsAnimation(module).run(1.0, 0.5F, Easings.EXPO_OUT);
            }
         }

         if (hoveredModule && pButton == 2) {
            toggleBinding(module);
            return true;
         }

         if (GuiScreen.openSettingsModules.contains(module)) {
            if (rightColumn) {
               downYSetting2 += settingsHeight;
            } else {
               downYSetting1 += settingsHeight;
            }
         }

         if (!rightColumn) {
            downY += GuiLayout.ROW_STEP;
         }

         index++;
      }

      return false;
   }

   private static void toggleBinding(Module module) {
      if (module.binding) {
         module.binding = false;
         GuiScreen.activeModuleBind = null;
         GuiScreen.getModuleBindAnimation(module).run(0.0, 0.2F, Easings.SINE_OUT);
      } else {
         if (GuiScreen.activeModuleBind != null) {
            GuiScreen.activeModuleBind.binding = false;
            GuiScreen.getModuleBindAnimation(GuiScreen.activeModuleBind).run(0.0, 0.2F, Easings.SINE_OUT);
         }

         GuiScreen.activeModuleBind = module;
         module.binding = true;
         GuiScreen.getModuleBindAnimation(module).run(1.0, 0.2F, Easings.SINE_OUT);
      }
   }

   public static float[] findColorPickerPosition(Renderer2D renderer2D, HueSetting hueSetting) {
      if (hueSetting == null) {
         return null;
      }

      int index = 1;
      float downY = GuiScreen.getScrollUtil().getScroll();
      float downYSetting1 = 0.0F;
      float downYSetting2 = 0.0F;

      for (Module module : GuiScreen.modules) {
         boolean rightColumn = index % 2 == 0;
         float moduleX = GuiLayout.moduleColumnX(index);
         float currentDownY = rightColumn ? (downY + downYSetting2 - GuiLayout.COLUMN_STAGGER) : (downY + downYSetting1);

         float settingsHeight = 0.0F;
         if (GuiScreen.openSettingsModules.contains(module)) {
            settingsHeight = GuiRenderSetting.getSettingsTotalHeight(renderer2D, module.getSettingsForGUI(), GuiLayout.settingWidth() + 6.0F);
         }

         if (GuiScreen.openSettingsModules.contains(module)) {
            float settingY = GuiScreen.y + 64.69F + currentDownY + 4.0F;
            float settingX = GuiLayout.settingX(moduleX);
            float settingWidth = GuiLayout.settingWidth() + 6.0F;
            float totalSettingsHeight = 0.0F;

            for (Setting setting : module.getSettingsForGUI()) {
               if (setting == hueSetting) {
                  float pickerX = settingX + settingWidth - 15.0F;
                  float pickerY = settingY + totalSettingsHeight - 5.0F;
                  return new float[]{pickerX, pickerY};
               }

               totalSettingsHeight += GuiRenderSetting.getSettingHeight(renderer2D, setting, settingWidth) + 1.0F;
            }

            if (rightColumn) {
               downYSetting2 += settingsHeight;
            } else {
               downYSetting1 += settingsHeight;
            }
         }

         if (!rightColumn) {
            downY += GuiLayout.ROW_STEP;
         }

         index++;
      }

      return null;
   }
}
