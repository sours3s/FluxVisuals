package ru.fluxvisuals.ui.gui.component.main;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.ui.gui.GuiScreen;

@Environment(EnvType.CLIENT)
public class GuiCharTyped extends GuiScreen {
   public static boolean charTyped(char codePoint, int modifiers) {
      if (GuiScreen.friendsSearchEditing) {
         if (codePoint == '\b') {
            if (!GuiScreen.friendsSearchText.isEmpty()) {
               GuiScreen.friendsSearchText = GuiScreen.friendsSearchText.substring(0, GuiScreen.friendsSearchText.length() - 1);
            }
            return true;
         }
         if (codePoint >= ' ' && codePoint != 127 && GuiScreen.friendsSearchText.length() < 24) {
            GuiScreen.friendsSearchText = GuiScreen.friendsSearchText + codePoint;
            return true;
         }
         return true;
      }

      if (GuiScreen.configNameEditing) {
         if (codePoint == '\b') {
            if (!GuiScreen.configNameText.isEmpty()) {
               GuiScreen.configNameText = GuiScreen.configNameText.substring(0, GuiScreen.configNameText.length() - 1);
            }
            return true;
         }
         if (codePoint >= ' ' && codePoint != 127 && GuiScreen.configNameText.length() < 24) {
            GuiScreen.configNameText = GuiScreen.configNameText + codePoint;
            return true;
         }
         return true;
      }

      if (GuiScreen.activeStringSetting != null) {
         if (codePoint == '\b') {
            if (!GuiScreen.activeStringSetting.input.isEmpty()) {
               GuiScreen.activeStringSetting.input = GuiScreen.activeStringSetting.input.substring(0, GuiScreen.activeStringSetting.input.length() - 1);
               if (FluxVisualsClient.get.configManager != null) {
                  FluxVisualsClient.get.configManager.autoSave();
               }
            }

            return true;
         }

         if (codePoint >= ' ' && codePoint != 127) {
            if (GuiScreen.activeStringSetting.input.length() < 16) {
               GuiScreen.activeStringSetting.input = GuiScreen.activeStringSetting.input + codePoint;
               if (FluxVisualsClient.get.configManager != null) {
                  FluxVisualsClient.get.configManager.autoSave();
               }
            }

            return true;
         }
      }

      if (GuiScreen.activeSearch) {
         if (codePoint == '\b') {
            return true;
         }

         if (codePoint >= ' '
            && codePoint != 127
            && (codePoint >= 'a' && codePoint <= 'z' || codePoint >= 'A' && codePoint <= 'Z' || codePoint >= '0' && codePoint <= '9' || codePoint == ' ')) {
            if (GuiScreen.searchText.length() < 50) {
               GuiScreen.searchText = GuiScreen.searchText + codePoint;
            }

            return true;
         }
      }

      return false;
   }
}
