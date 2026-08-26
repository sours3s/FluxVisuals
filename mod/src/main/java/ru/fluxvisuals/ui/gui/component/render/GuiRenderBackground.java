package ru.fluxvisuals.ui.gui.component.render;

import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.core.Renderer2D;

@Environment(EnvType.CLIENT)
public class GuiRenderBackground extends GuiScreen {
   // ----- Tunables (both plates are theme-independent pure black) -----
   private static final float LEFT_PLATE_OPACITY = 170.0F;  // left sidebar
   private static final float LEFT_BLUR = 16.0F;
   private static final float RIGHT_PLATE_OPACITY = 170.0F; // right content: same black as the left
   private static final float RIGHT_BLUR = 45.0F;           // strong blur
   private static final float DIVIDER_ALPHA = 34.0F;        // faint seam divider so the halves don't merge

   public static void renderBackground(Renderer2D renderer2D, MatrixStack pose, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));

      float x = GuiScreen.x;
      float y = GuiScreen.y;
      float w = GuiScreen.width;
      float h = GuiScreen.height;
      float sidebar = GuiLayout.SIDEBAR;
      float seam = x + sidebar;
      float rightW = w - sidebar;

      int leftBlack = (((int)(LEFT_PLATE_OPACITY * mainAlpha)) & 0xFF) << 24;
      int rightBlack = (((int)(RIGHT_PLATE_OPACITY * mainAlpha)) & 0xFF) << 24;

      boolean blur = mainAlpha > 0.1F && ru.fluxvisuals.module.impl.visuals.Hud.blur.get("GUI");
      if (blur) {
         // Right side: stronger blur, rounded only on the outer (right) corners, square at the seam.
         renderer2D.prepareBlur(RIGHT_BLUR);
         renderer2D.blur(seam, y, rightW, h, 0.0F, 6.5F, 6.5F, 0.0F, mainAlpha);
         renderer2D.flush();
         // Left side: weaker blur, rounded only on the outer (left) corners, square at the seam.
         renderer2D.prepareBlur(LEFT_BLUR);
         renderer2D.blur(x, y, sidebar, h, 6.5F, 0.0F, 0.0F, 6.5F, mainAlpha);
         renderer2D.flush();
      }

      // Clear split: left sidebar plate and right content plate, both pure black.
      renderer2D.rect(x, y, sidebar, h, 6.5F, 0.0F, 0.0F, 6.5F, leftBlack);
      renderer2D.rect(seam, y, rightW, h, 0.0F, 6.5F, 6.5F, 0.0F, rightBlack);
      // Faint divider at the seam so the two (same-colour) halves stay visually separated.
      int dividerColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(DIVIDER_ALPHA * mainAlpha));
      renderer2D.rect(seam - 0.4F, y + 1.0F, 0.8F, h - 2.0F, dividerColor);

      renderer2D.rectOutline(x, y, w, h, 6.5F, outlineColor, 0.5F);
      // Commit so later prepareBlur() calls can't overwrite the shared blur texture first.
      renderer2D.flush();
   }
}
