package ru.fluxvisuals.ui.gui.component.mouse;

import java.io.File;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.cfg.ConfigManager;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderSpecial;

@Environment(EnvType.CLIENT)
public class GuiMouseClickedSpecial extends GuiScreen {
   public static boolean mouseClickedSpecial(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      }
      if (GuiScreen.selectedCategories == null) {
         return false;
      }
      if (GuiScreen.selectedCategories == Category.Configs) {
         return clickConfigs(mouseX, mouseY);
      }
      return false;
   }

   private static boolean clickThemes(int mouseX, int mouseY) {
      Theme[] themes = Theme.values();
      float scroll = GuiScreen.getScrollUtil().getScroll();
      for (int i = 0; i < themes.length; i++) {
         float y = GuiRenderSpecial.rowY(i, scroll);
         if (y < GuiLayout.clipY() - GuiLayout.ROW_STEP || y > GuiLayout.clipY() + GuiLayout.clipHeight()) {
            continue;
         }
         if (GuiRenderMain.isHovered(mouseX, mouseY, GuiRenderSpecial.rowX(), y, GuiRenderSpecial.rowW(),
               GuiRenderSpecial.rowH())) {
            Theme theme = themes[i];
            GuiScreen.selectedTheme = theme;
            GuiScreen.preSelectedTheme = theme;
            if (FluxVisualsClient.get != null && FluxVisualsClient.get.guiManager != null) {
               FluxVisualsClient.get.guiManager.setGuiTheme(theme);
            }
            return true;
         }
      }
      return false;
   }

   private static boolean clickStyles(int mouseX, int mouseY) {
      List<GuiRenderSpecial.StyleRow> rows = GuiRenderSpecial.styleRows();
      float scroll = GuiScreen.getScrollUtil().getScroll();
      for (int i = 0; i < rows.size(); i++) {
         float y = GuiRenderSpecial.rowY(i, scroll);
         if (y < GuiLayout.clipY() - GuiLayout.ROW_STEP || y > GuiLayout.clipY() + GuiLayout.clipHeight()) {
            continue;
         }
         if (GuiRenderMain.isHovered(mouseX, mouseY, GuiRenderSpecial.rowX(), y, GuiRenderSpecial.rowW(),
               GuiRenderSpecial.rowH())) {
            rows.get(i).toggle.run();
            if (FluxVisualsClient.get != null && FluxVisualsClient.get.guiManager != null) {
               FluxVisualsClient.get.guiManager.saveSettings();
            }
            return true;
         }
      }
      return false;
   }

   private static boolean clickConfigs(int mouseX, int mouseY) {
      float x = GuiRenderSpecial.rowX();
      float w = GuiLayout.clipWidth() - 8.0F;
      float inputH = 24.0F;
      float inputY = GuiLayout.clipY() + 2.0F;
      float btnY = inputY + inputH + 6.0F;
      float btnH = 20.0F;
      float gap = 4.0F;
      float btnW = (w - gap * 2.0F) / 3.0F;
      float listY0 = btnY + btnH + 8.0F;
      float listHeaderH = 16.0F;
      float rowH = 20.0F;
      float gapR = 3.0F;

      // Клик по полю имени — включаем ввод.
      if (GuiRenderMain.isHovered(mouseX, mouseY, x, inputY, w, inputH)) {
         GuiScreen.configNameEditing = true;
         return true;
      }

      // Кнопки действий (Save / Load / Reset / Dir).
      for (int i = 0; i < 4; i++) {
         float bx = x + i * (btnW + gap);
         if (!GuiRenderMain.isHovered(mouseX, mouseY, bx, btnY, btnW, btnH)) {
            continue;
         }
         if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
            String name = GuiScreen.configNameText.trim().isEmpty() ? "default" : GuiScreen.configNameText.trim();
            if (i == 0) {
               FluxVisualsClient.get.configManager.saveConfig(name);
            } else if (i == 1) {
               FluxVisualsClient.get.configManager.loadConfig(name);
            } else if (i == 2) {
               // Reset: полный сброс всего клиента
               FluxVisualsClient.get.configManager.resetAll();
            } else if (i == 3) {
               // Dir: открыть папку конфигов в проводнике
               File configDir = ConfigManager.getConfigDirectoryPath();
               if (configDir.exists()) {
                  try {
                     java.awt.Desktop.getDesktop().open(configDir);
                  } catch (Exception ignored) {}
               }
            }
         }
         return true;
      }

      // Список конфигов: клик по строке — загрузка, клик по крестику — удаление.
      float scroll = GuiScreen.getScrollUtil().getScroll();
      List<String> configs = GuiRenderSpecial.configNames();
      for (int i = 0; i < configs.size(); i++) {
         float y = listY0 + listHeaderH + i * (rowH + gapR) - scroll;
         if (y < listY0 + listHeaderH - rowH || y > listY0 + listHeaderH + GuiLayout.clipHeight()) {
            continue;
         }
         if (GuiRenderMain.isHovered(mouseX, mouseY, x + w - 46.0F, y, 44.0F, rowH)) {
            if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
               FluxVisualsClient.get.configManager.deleteConfig(configs.get(i));
            }
            return true;
         }
         if (GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, rowH)) {
            if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
               FluxVisualsClient.get.configManager.loadConfig(configs.get(i));
            }
            return true;
         }
      }
      return false;
   }
}
