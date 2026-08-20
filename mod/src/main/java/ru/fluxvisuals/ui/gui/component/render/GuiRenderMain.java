package ru.fluxvisuals.ui.gui.component.render;

import java.awt.Color;
import java.util.List;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Module;
import ru.fluxvisuals.module.api.setting.Setting;
import ru.fluxvisuals.module.api.setting.impl.HueSetting;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.setting.GuiRenderSetting;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.keyboard.Keyboard;
import ru.fluxvisuals.util.player.MovementManager;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.animation.anim.util.Easings;
import ru.fluxvisuals.util.render.math.MathHelper;
import ru.fluxvisuals.util.render.math.animation.Direction;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class GuiRenderMain extends GuiScreen {
   public static void renderMain(Renderer2D renderer2D, MatrixStack pose, int mouseX, int mouseY, float mainAlpha) {
      // Специальные вкладки (Themes / Styles / Configs / Friends) вместо списка модулей.
      if (GuiScreen.selectedCategories == Category.Configs) {
         GuiRenderSpecial.render(renderer2D, mainAlpha, mouseX, mouseY);
         return;
      }
      if (GuiScreen.selectedCategories == Category.Friends) {
         GuiRenderFriends.render(renderer2D, mainAlpha, mouseX, mouseY);
         return;
      }

      ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.reset();

      if (GuiScreen.activeSearch) {
         MovementManager.getInstance().lockMovement("Search");
      } else {
         MovementManager.getInstance().unlockMovement("Search");
      }

      if (GuiScreen.activeSearch) {
         boolean backspaceDown = ru.fluxvisuals.util.render.utils.KeyUtil.isKeyDown(259);
         long currentTime = System.currentTimeMillis();
         if (backspaceDown) {
            if (!GuiScreen.backspaceHeld) {
               GuiScreen.backspaceHeld = true;
               GuiScreen.firstBackspacePressTime = currentTime;
               GuiScreen.lastBackspaceTime = currentTime;
               if (!GuiScreen.searchText.isEmpty()) {
                  GuiScreen.searchText = GuiScreen.searchText.substring(0, GuiScreen.searchText.length() - 1);
               }
            } else if (currentTime - GuiScreen.firstBackspacePressTime > 500L && currentTime - GuiScreen.lastBackspaceTime > 30L) {
               if (!GuiScreen.searchText.isEmpty()) {
                  GuiScreen.searchText = GuiScreen.searchText.substring(0, GuiScreen.searchText.length() - 1);
               }

               GuiScreen.lastBackspaceTime = currentTime;
            }
         } else {
            GuiScreen.backspaceHeld = false;
            GuiScreen.firstBackspacePressTime = 0L;
         }
      } else {
         GuiScreen.backspaceHeld = false;
         GuiScreen.firstBackspacePressTime = 0L;
      }

      int outlineColor = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundThreeColor = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int mainColor = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor6 = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha));
      int mainColor40 = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int textColor = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int backGroundOneColor = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(178.5F * mainAlpha));
      Color mainColorGlow35 = ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getColor(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.replAlpha(ru.fluxvisuals.util.render.core.Renderer2D.ColorUtil.getMainColor(1, 1), (int)(56.0F * mainAlpha)));

      float moduleWidth = GuiLayout.moduleWidth();
      float rectHeight = GuiLayout.contentRectHeight();
      renderer2D.pushRoundedClipRect(GuiLayout.clipX(), GuiLayout.clipY(), GuiLayout.clipWidth(), GuiLayout.clipHeight(), 0.0F, 0.0F, 0.0F, 0.0F);

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

      float calcDownY = 0.0F;
      float calcDownYSetting1 = 0.0F;
      float calcDownYSetting2 = 0.0F;
      float maxHeightColumn1 = 0.0F;
      float maxHeightColumn2 = 0.0F;
      int calcIndex = 1;

      for (Module module : filteredModules) {
         module.animation.update();
         module.animation.run(module.enable ? 1.0 : 0.0, 0.15F, Easings.SINE_OUT);
         module.animation1.setDirection(module.enable ? Direction.FORWARDS : Direction.BACKWARDS);
         float settingsHeight = 0.0F;
         float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
         float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
         if (GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F || settingsAlphaAnim > 0.0F) {
            float fullSettingsHeight = GuiRenderSetting.getSettingsTotalHeight(renderer2D, module.getSettingsForGUI(), GuiLayout.settingWidth());
            settingsHeight = fullSettingsHeight * settingsAnim;
         }

         if (calcIndex % 2 == 0) {
            float currentDownY = calcDownY + calcDownYSetting2 - GuiLayout.COLUMN_STAGGER;
            float moduleHeight = GuiLayout.MODULE_HEIGHT + settingsHeight;
            maxHeightColumn2 = Math.max(maxHeightColumn2, currentDownY + moduleHeight);
            calcDownYSetting2 += settingsHeight;
         } else {
            float currentDownY = calcDownY + calcDownYSetting1;
            float moduleHeight = GuiLayout.MODULE_HEIGHT + settingsHeight;
            maxHeightColumn1 = Math.max(maxHeightColumn1, currentDownY + moduleHeight);
            calcDownYSetting1 += settingsHeight;
            calcDownY += GuiLayout.ROW_STEP;
         }

         calcIndex++;
      }

      float totalHeight = Math.max(maxHeightColumn1, maxHeightColumn2);
      float upMax = totalHeight + 14.0F;
      boolean check = isHovered(mouseX, mouseY, GuiLayout.clipX(), GuiLayout.clipY(), GuiLayout.clipWidth(), GuiLayout.clipHeight());
      GuiScreen.getScrollUtil().setSpeed(6.0F);
      GuiScreen.getScrollUtil().setEnabled(check);
      GuiScreen.getScrollUtil().update();
      GuiScreen.getScrollUtil().setMax(MathHelper.clamp(upMax, rectHeight - 10.0F, 9999.0F), rectHeight - 10.0F);

      float yShar = -0.35F;
      float yZnar = -0.7F;
      int index = 1;
      float downY = GuiScreen.getScrollUtil().getScroll();
      float downYSetting1 = 0.0F;
      float downYSetting2 = 0.0F;

      for (Module module : filteredModules) {
         if (!module.getSettingsForGUI().isEmpty()) {
            GuiScreen.openSettingsModules.add(module);
            GuiScreen.getModuleSettingsAlphaAnimation(module).run(1.0, 0.34F,
                  ru.fluxvisuals.util.render.math.animation.anim.util.Easings.SINE_OUT);
            GuiScreen.getModuleSettingsAnimation(module).run(1.0, 0.5F,
                  ru.fluxvisuals.util.render.math.animation.anim.util.Easings.EXPO_OUT);
         }

         boolean rightColumn = index % 2 == 0;
         float moduleX = GuiLayout.moduleColumnX(index);
         float currentDownY = rightColumn ? (downY + downYSetting2 - GuiLayout.COLUMN_STAGGER) : (downY + downYSetting1);
         float animPC = module.animation.get();
         float headerBoxY = GuiScreen.y + 43.365F + currentDownY;
         if (isHovered(mouseX, mouseY, moduleX, headerBoxY, moduleWidth, GuiLayout.MODULE_HEIGHT)) {
            ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.setModule(module);
         }

         float settingsHeightLocal = 0.0F;
         float fullSettingsHeightLocal = 0.0F;
         float settingsAnim = GuiScreen.getModuleSettingsAnimation(module).get();
         float settingsAlphaAnim = GuiScreen.getModuleSettingsAlphaAnimation(module).get();
         if (GuiScreen.openSettingsModules.contains(module) || settingsAnim > 0.0F) {
            fullSettingsHeightLocal = GuiRenderSetting.getSettingsTotalHeight(renderer2D, module.getSettingsForGUI(), GuiLayout.settingWidth());
            settingsHeightLocal = fullSettingsHeightLocal * settingsAnim;
         }

         if (module == GuiScreen.scrollToModule) {
            float relTop = currentDownY - GuiScreen.getScrollUtil().getScroll();
            float clipBottomRel = (GuiLayout.clipY() + GuiLayout.clipHeight()) - GuiScreen.y - 43.365F;
            float desired = clipBottomRel - relTop - GuiLayout.MODULE_HEIGHT - fullSettingsHeightLocal - 4.0F;
            float clamped = Math.max(desired, GuiScreen.getScrollUtil().getMax());
            if (clamped < GuiScreen.getScrollUtil().getTarget()) {
               GuiScreen.getScrollUtil().setTarget(clamped);
            }
            if (settingsAnim > 0.985F || !GuiScreen.openSettingsModules.contains(module)) {
               GuiScreen.scrollToModule = null;
            }
         }

         float boxY = GuiScreen.y + 43.365F + currentDownY;
         if (!(settingsAnim > 0.0F) && !(settingsAlphaAnim > 0.0F)) {
            renderer2D.rectOutline(moduleX, boxY, moduleWidth, GuiLayout.MODULE_HEIGHT, 6.5F, outlineColor, 0.1F);
            renderer2D.rect(moduleX, boxY, moduleWidth, GuiLayout.MODULE_HEIGHT, 6.5F, backGroundThreeColor);
         } else {
            renderer2D.rectOutline(moduleX, boxY, moduleWidth, GuiLayout.MODULE_HEIGHT + settingsHeightLocal, 6.5F, outlineColor, 0.1F);
            renderer2D.rect(moduleX, boxY, moduleWidth, GuiLayout.MODULE_HEIGHT + settingsHeightLocal, 6.5F, backGroundThreeColor);
            if (settingsAlphaAnim > 0.01F) {
               renderer2D.rect(moduleX, GuiScreen.y + 64.69F + currentDownY, moduleWidth, 1.0F, ColorUtil.multAlpha(outlineColor, settingsAlphaAnim));
            }
         }

         float moduleNameX = GuiLayout.moduleNameX(moduleX);
         float moduleNameY = GuiScreen.y + 49.555F + currentDownY;
         renderer2D.text(FontRegistry.INTER_MEDIUM, moduleNameX, moduleNameY + 6.6F, 14.0F, module.name, ColorUtil.overCol(mainColor40, textColor, animPC));

         float bindAnim = GuiScreen.getModuleBindAnimation(module).get();
         if (module.binding || module.bind != -1 || bindAnim > 0.0F) {
            float bindHeight = 10.0F;
            String keyText = module.binding ? "..." : (module.bind != -1 ? Keyboard.keyName(module.bind) : "");
            float keyTextWidth = keyText.isEmpty() ? 0.0F : renderer2D.measureText(FontRegistry.INTER_MEDIUM, keyText, 12.0F).width;
            float buttonWidth = Math.max(6.0F, keyTextWidth + 6.0F);
            float moduleNameWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, module.name, 14.0F).width;
            float bindX = moduleNameX + moduleNameWidth + 4.0F;
            float bindY = moduleNameY - 0.35F;
            renderer2D.rectOutline(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(outlineColor, bindAnim), 0.1F);
            renderer2D.rect(bindX, bindY, buttonWidth, bindHeight, 3.0F, ColorUtil.multAlpha(mainColor6, bindAnim));
            if (!keyText.isEmpty()) {
               renderer2D.text(
                  FontRegistry.INTER_MEDIUM,
                  bindX + buttonWidth / 2.0F - keyTextWidth / 2.0F - 0.2F,
                  bindY + 2.0F + 5.25F,
                  12.0F,
                  keyText,
                  ColorUtil.multAlpha(module.binding ? mainColor : mainColor40, bindAnim)
               );
            }
         }

         float dotX = GuiLayout.moduleToggleX(moduleX);
         renderer2D.rectOutline(dotX - 1.5F, GuiScreen.y + 52.505F + currentDownY - 1.5F + yShar, 6.0F, 6.0F, 3.0F, outlineColor, 0.08F);
         renderer2D.rect(dotX - 1.5F, GuiScreen.y + 52.505F + currentDownY - 1.5F + yShar, 6.0F, 6.0F, 3.0F, mainColor6);
         renderer2D.rect(
            dotX - 0.75F + 0.855F,
            GuiScreen.y + 53.365F + currentDownY - 0.78F + yShar,
            3.0F,
            3.0F,
            1.5F,
            ColorUtil.overCol(mainColor40, mainColor, animPC)
         );
         renderer2D.shadow(
            dotX + 0.855F + 0.7F,
            GuiScreen.y + 53.365F + currentDownY + yShar,
            0.1F,
            0.1F,
            1.5F,
            2.575F,
            0.1F,
            ColorUtil.overCol(0, mainColorGlow35.getRGB(), animPC)
         );

         if (settingsAnim > 0.0F || settingsAlphaAnim > 0.0F) {
            float settingY = GuiScreen.y + 64.69F + currentDownY + 4.0F;
            float settingX = GuiLayout.settingX(moduleX);
            float settingWidth = GuiLayout.settingWidth();
            float totalSettingsHeight = 0.0F;

            for (Setting setting : module.getSettingsForGUI()) {
               float rowHeight = (GuiRenderSetting.getSettingHeight(renderer2D, setting, settingWidth) + 1.0F) * settingsAlphaAnim;
               if (isHovered(mouseX, mouseY, settingX, settingY + totalSettingsHeight, settingWidth, rowHeight)) {
                  ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.setSetting(module, setting);
               }
               totalSettingsHeight += GuiRenderSetting.renderSetting(
                     renderer2D,
                     setting,
                     settingX,
                     settingY + totalSettingsHeight,
                     settingWidth,
                     mouseX,
                     mouseY,
                     ColorUtil.multAlpha(outlineColor, settingsAlphaAnim),
                     ColorUtil.multAlpha(mainColor, settingsAlphaAnim),
                     ColorUtil.multAlpha(mainColor6, settingsAlphaAnim),
                     ColorUtil.multAlpha(mainColor40, settingsAlphaAnim),
                     ColorUtil.multAlpha(textColor, settingsAlphaAnim),
                     mainAlpha * settingsAlphaAnim
                  )
                  * settingsAlphaAnim;
            }

            if (rightColumn) {
               downYSetting2 += settingsHeightLocal;
            } else {
               downYSetting1 += settingsHeightLocal;
            }
         }

         if (!rightColumn) {
            downY += GuiLayout.ROW_STEP;
         }

         index++;
      }

      renderer2D.popClipRect();
      // Интерактивный тултип (русское описание модуля/настройки) рисуется поверх всего, без клипа.
      if (ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.isActive()) {
         net.minecraft.client.MinecraftClient tipMc = net.minecraft.client.MinecraftClient.getInstance();
         if (tipMc.getWindow() != null) {
            ru.fluxvisuals.ui.gui.widget.settings.TooltipManager.render(
               renderer2D, mouseX, mouseY,
               tipMc.getWindow().getFramebufferWidth(), tipMc.getWindow().getFramebufferHeight());
         }
      }
      if (GuiScreen.activeColorPicker != null && GuiScreen.activeColorPicker instanceof HueSetting) {
         GuiRenderColorPicker.renderColorPickerWindow(
            renderer2D,
            GuiScreen.activeColorPicker,
            mouseX,
            mouseY,
            ColorUtil.multAlpha(outlineColor, GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(backGroundOneColor, GuiScreen.animation15.getOutput()),
            ColorUtil.multAlpha(mainColor40, GuiScreen.animation15.getOutput()),
            mainAlpha * GuiScreen.animation15.getOutput()
         );
      }
   }

   public static boolean isHovered(float mouseX, float mouseY, float x, float y, float width, float height) {
      return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
   }
}
