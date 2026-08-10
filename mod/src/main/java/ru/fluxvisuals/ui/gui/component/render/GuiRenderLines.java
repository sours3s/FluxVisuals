package ru.fluxvisuals.ui.gui.component.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class GuiRenderLines extends GuiScreen {
   public static void renderLines(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int outlineColor2 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(16.0F * mainAlpha));
      renderer2D.rect(GuiScreen.x, GuiScreen.y + GuiLayout.TOP_BAR, GuiScreen.width, 0.7F, outlineColor);
      renderer2D.rect(GuiScreen.x + GuiLayout.SIDEBAR, GuiScreen.y, 0.7F, GuiScreen.height, outlineColor2);
      renderer2D.rect(GuiScreen.x, GuiScreen.y + GuiScreen.height - 35.615F, GuiLayout.SIDEBAR, 0.7F, outlineColor2);
   }
}
