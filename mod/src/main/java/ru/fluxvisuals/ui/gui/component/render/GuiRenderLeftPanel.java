package ru.fluxvisuals.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.backends.gl.StencilHelper;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

@Environment(EnvType.CLIENT)
public class GuiRenderLeftPanel extends GuiScreen {
   public static void renderLeftPanel(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundTwoColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundTwoColor(1, 1), (int)(178.5F * mainAlpha));
      int backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(102.0F * mainAlpha));
      int blur = Renderer2D.ColorUtil.replAlpha(-1, (int)(255.0F * mainAlpha));
      Color mainColorGlow = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(35.0F * mainAlpha)));
      Color mainColorGlow35 = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(56.0F * mainAlpha)));
      Color koronaColors = Renderer2D.ColorUtil.getColor(Renderer2D.ColorUtil.replAlpha(Color.ORANGE.getRGB(), (int)(50.0F * mainAlpha)));
      // Sidebar background is drawn (transparent + blurred) by GuiRenderBackground.
      // Do not draw an opaque background pass here, it would cover the frosted-glass blur.
      float downY = 0.0F;
      float ch = GuiLayout.CATEGORY_HEIGHT;

      for (Category category : GuiScreen.categories) {
         float yicons = 0.0F;
         category.anim33.update();
         category.anim33.run(category == selectedCategories ? 1.0 : 0.0, 1.0, Easings.QUART_OUT);
         renderer2D.rect(GuiScreen.x, GuiScreen.y + 43.365F + 0.5F + downY, GuiLayout.SIDEBAR, 0.5F, ColorUtil.overCol(0, outlineColor, category.anim33.get()));
         renderer2D.rect(GuiScreen.x, GuiScreen.y + 43.365F + downY, GuiLayout.SIDEBAR, ch, ColorUtil.overCol(0, backGroundThreeColor, category.anim33.get()));
         renderer2D.rect(GuiScreen.x, GuiScreen.y + 43.365F + ch + downY, GuiLayout.SIDEBAR, 0.5F, ColorUtil.overCol(0, outlineColor, category.anim33.get()));
         renderer2D.rect(GuiScreen.x, GuiScreen.y + 43.365F + downY, 1.0F, ch + 0.5F, ColorUtil.overCol(0, mainColor, category.anim33.get()));
         renderer2D.shadow(
            GuiScreen.x, GuiScreen.y + 43.365F + downY, 1.0F, ch + 0.5F, 0.0F, 1.0F, 0.1F, ColorUtil.overCol(0, mainColorGlow.getRGB(), category.anim33.get())
         );
         renderer2D.text(FontRegistry.ICONS, GuiScreen.x + 9.145F, GuiScreen.y + 49.285F + yicons + downY + 8.0F, 16.0F, category.getIcon(), mainColor);
         renderer2D.shadow(
            GuiScreen.x + 9.145F + 3.5F,
            GuiScreen.y + 49.285F + downY + yicons + 3.5F,
            0.1F,
            0.1F,
            8.0F,
            5.7F,
            0.1F,
            ColorUtil.overCol(0, mainColorGlow35.getRGB(), category.anim33.get())
         );
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            GuiScreen.x + 21.995F,
            GuiScreen.y + 48.87F + downY + 7.0F + 0.2F,
            14.0F,
            category.getName(),
            ColorUtil.overCol(0, textColor, category.anim33.get())
         );
         renderer2D.text(
            FontRegistry.ICONS,
            GuiScreen.x + 9.145F,
            GuiScreen.y + 49.285F + yicons + downY + 8.0F,
            16.0F,
            category.getIcon(),
            ColorUtil.overCol(mainColor40, 0, category.anim33.get())
         );
         renderer2D.text(
            FontRegistry.INTER_MEDIUM,
            GuiScreen.x + 21.995F,
            GuiScreen.y + 48.87F + downY + 7.0F + 0.2F,
            14.0F,
            category.getName(),
            ColorUtil.overCol(mainColor40, 0, category.anim33.get())
         );
         downY += GuiLayout.CATEGORY_STEP;
      }

      float profileY = GuiLayout.profileY();
      // Profile area intentionally left empty for now (avatar / username / badge removed).
      // The space itself is preserved via GuiLayout.profileY() so the layout stays the same.
   }
}
