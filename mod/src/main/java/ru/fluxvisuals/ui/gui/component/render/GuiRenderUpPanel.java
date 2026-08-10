package ru.fluxvisuals.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.fluxvisuals.config.StyleConfig;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.setting.GuiRenderSetting;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.backends.gl.StencilHelper;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;
import ru.fluxvisuals.util.render.texture.TextureLoader;

@Environment(EnvType.CLIENT)
public class GuiRenderUpPanel extends GuiScreen {
   public static void renderUpPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundTwoColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundTwoColor(1, 1), (int)(178.5F * mainAlpha));
      int backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(80.0F * mainAlpha));
      int blur = Renderer2D.ColorUtil.replAlpha(-1, (int)(255.0F * mainAlpha));
      Color mainColorGlow = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(35.0F * mainAlpha)));
      // Top panel background is drawn (transparent + blurred) by GuiRenderBackground.
      // Do not draw an opaque background pass here, it would cover the frosted-glass blur.
      // Лого FluxVisuals (реальная текстура, как в лоадере)
      float nameX = GuiScreen.x + 36.0F;
      if (StyleConfig.clickGuiLogo) {
         int logoId = TextureLoader.load("assets/fluxvisuals/textures/gui/logo.png");
         renderer2D.drawRgbaTexture(logoId, GuiScreen.x + 10.0F, GuiScreen.y + 11.0F, 22.0F, 22.0F, -1, true);
         nameX = GuiScreen.x + 36.0F;
      } else {
         nameX = GuiScreen.x + 18.0F;
      }
      renderer2D.shadow(GuiScreen.x + 9.92F + 5.5F, GuiScreen.y + 11.355F + 5.5F, 0.1F, 0.1F, 6.0F, 10.5F, 0.1F, mainColorGlow.getRGB());
      renderer2D.text(FontRegistry.INTER_MEDIUM, nameX, GuiScreen.y + 12.595F + 7.0F, 14.0F, "Flux Visuals", textColor);

      float searchX = GuiLayout.searchX();
      float searchBoxY = GuiLayout.searchY();
      float searchWidth = GuiLayout.searchWidth();
      renderer2D.rectOutline(searchX, searchBoxY, searchWidth, GuiLayout.searchHeight(), 5.5F, outlineColor, 0.1F);
      renderer2D.rect(searchX, searchBoxY, searchWidth, GuiLayout.searchHeight(), 5.5F, backGroundThreeColor);
      renderer2D.text(FontRegistry.ICONS, searchX + 7.985F, GuiScreen.y + 12.335F - 1.0F + 9.75F, 17.5F, "C", mainColor);
      float searchTextX = searchX + 22.89F;
      float searchTextY = GuiScreen.y + 12.595F + 7.0F - 0.4F;
      String searchDisplayText;
      if (GuiScreen.activeSearch) {
         searchDisplayText = GuiScreen.searchText.isEmpty() ? "" : GuiScreen.searchText;
      } else {
         searchDisplayText = "Search";
      }

      renderer2D.text(FontRegistry.INTER_MEDIUM, searchTextX, searchTextY, 14.0F, searchDisplayText, textTwoColor40);
      if (GuiScreen.activeSearch) {
         long currentTime = System.currentTimeMillis();
         boolean showCursor = currentTime / 500L % 2L == 0L;
         if (showCursor) {
            float textWidth = renderer2D.measureText(FontRegistry.INTER_MEDIUM, searchDisplayText, 14.0F).width;
            float cursorX = searchTextX + textWidth;
            float cursorY = searchTextY - 0.5F;
            renderer2D.rect(cursorX, cursorY - 5.0F, 1.0F, 7.0F, 0.5F, mainColor);
         }
      }
   }

   private static void renderClientSettingsPopup(Renderer2D renderer2D, float mainAlpha) {
      float popupWidth = GuiLayout.popupWidth();
      float popupHeight = GuiLayout.popupHeight();
      float popupX = GuiLayout.popupX();
      float popupY = GuiLayout.popupY();
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      drawClientRect(renderer2D, popupX, popupY, popupWidth, popupHeight, 5.0F, mainAlpha, 1.0F);
      float settingY = popupY + 10.0F;
      float settingX = popupX + 10.0F;
      float settingWidth = popupWidth - 20.0F;
      GuiRenderSetting.renderSetting(
         renderer2D,
         ru.fluxvisuals.module.impl.visuals.Hud.blur,
         settingX,
         settingY,
         settingWidth,
         GuiScreen.currentMouseX,
         GuiScreen.currentMouseY,
         outlineColor,
         mainColor,
         Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha)),
         mainColor40,
         textColor,
         mainAlpha
      );
   }

   public static void drawClientRect(Renderer2D r2, float x, float y, float w, float h, float radius, float alpha, float thickness) {
      if (ru.fluxvisuals.module.impl.visuals.Hud.blur.get("GUI")) {
         r2.prepareBlur(23.0F);
         r2.blur(x, y, w, h, radius, alpha);
      }

      r2.rectOutline(x - 1.0F, y - 1.0F, w + 2.0F, h + 2.0F, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), alpha * 0.1F), thickness);
      r2.rect(x, y, w, h, radius, ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), alpha * 0.7F));
   }
}
