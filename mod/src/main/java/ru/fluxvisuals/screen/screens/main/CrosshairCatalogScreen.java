package ru.fluxvisuals.screen.screens.main;

import java.awt.Color;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.module.impl.visuals.CrosshairModule;
import ru.fluxvisuals.screen.screens.main.widgets.CustomButton;
import ru.fluxvisuals.util.render.core.Renderer2D;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Каталог прицелов — мастерская стилей кастомного прицела.
 * Сетка превью стилей, клик для выбора, редактор размера/цвета.
 */
public class CrosshairCatalogScreen extends Screen {
   private static final MinecraftClient mc = MinecraftClient.getInstance();
   private final Screen parent;
   private float fadeProgress = 0f;

   private static final String[] STYLES = {"Cross", "Dot", "GapDot", "T", "Corner", "Circle"};
   private static final float PREVIEW_SIZE = 32f;
   private static final float PREVIEW_GAP = 12f;
   private static final float COLS = 3f;
   private static final float LEFT_PAD = 30f;

   private final CustomButton[] styleButtons = new CustomButton[STYLES.length];

   public CrosshairCatalogScreen(Screen parent) {
      super(Text.empty());
      this.parent = parent;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      fadeProgress = Math.min(1f, fadeProgress + 0.06f * delta);
      int alpha = (int)(fadeProgress * 255);

      context.fill(0, 0, width, height, 0xCC111111);
      Client.RENDERER.setDrawContext(context);

      drawSafeText("Мастерская прицелов", LEFT_PAD, 40f, TextureUse.SFMEDIUM, 14f, new Color(19, 255, 174, alpha));
      drawSafeText("Выбери стиль прицела, настрой размер и цвет", LEFT_PAD, 60f, TextureUse.SFMEDIUM, 7f, new Color(150, 150, 160, alpha));

      float startX = LEFT_PAD;
      float startY = 85f;

      for (int i = 0; i < STYLES.length; i++) {
         int col = i % (int) COLS;
         int row = i / (int) COLS;
         float x = startX + col * (PREVIEW_SIZE + PREVIEW_GAP + 60f);
         float y = startY + row * (PREVIEW_SIZE + PREVIEW_GAP + 18f);

         boolean isSelected = CrosshairModule.getInstance() != null
            && CrosshairModule.getInstance().style.is(STYLES[i]);
         boolean hovered = mouseX >= x && mouseX <= x + PREVIEW_SIZE + 55f
            && mouseY >= y - 2 && mouseY <= y + PREVIEW_SIZE + 14f;

         // Card background
         Color bg = isSelected
            ? new Color(19, 255, 174, (int)(alpha * 0.15f))
            : hovered
               ? new Color(40, 40, 45, (int)(alpha * 0.9f))
               : new Color(25, 25, 30, (int)(alpha * 0.8f));
         Client.RENDERER.rect(x - 4, y - 4, PREVIEW_SIZE + 60f, PREVIEW_SIZE + 18f,
            new Vector4f(6f), 1f, bg, bg, bg, bg);

         if (isSelected) {
            Client.RENDERER.outline(x - 4, y - 4, PREVIEW_SIZE + 60f, PREVIEW_SIZE + 18f,
               0.5f, new Vector4f(6f), new Vector2f(1),
               new Color(19, 255, 174, (int)(alpha * 0.5f)), new Color(19, 255, 174, (int)(alpha * 0.5f)),
               new Color(19, 255, 174, (int)(alpha * 0.5f)), new Color(19, 255, 174, (int)(alpha * 0.5f)));
         }

         // Draw crosshair preview
         float cx = x + PREVIEW_SIZE / 2f;
         float cy = y + PREVIEW_SIZE / 2f;
         drawCrosshairPreview(cx, cy, STYLES[i], alpha);

         // Label
         drawSafeText(STYLES[i], x, y + PREVIEW_SIZE + 6f, TextureUse.SFMEDIUM, 7f,
            new Color(220, 220, 230, alpha));
      }

      drawSafeText("ESC — назад", LEFT_PAD, height - 30f, TextureUse.SFMEDIUM, 6.5f, new Color(120, 120, 130, alpha));
   }

   private void drawCrosshairPreview(float cx, float cy, String style, int alpha) {
      Renderer2D r2 = FluxVisualsClient.getRenderer();
      if (r2 == null) return;
      float s = 1.0F;
      float t = 1.5F;
      float g = 3F;
      int color = 0xAAFFFFFF;

      switch (style) {
         case "Cross" -> {
            r2.rect(cx - t / 2, cy - 8 - g, t, 8, 0, color);
            r2.rect(cx - t / 2, cy + g, t, 8, 0, color);
            r2.rect(cx - 8 - g, cy - t / 2, 8, t, 0, color);
            r2.rect(cx + g, cy - t / 2, 8, t, 0, color);
         }
         case "Dot" -> r2.circle(cx, cy, 3f, 0, 1f, color);
         case "GapDot" -> {
            r2.rect(cx - t / 2, cy - 8 - g, t, 8, 0, color);
            r2.rect(cx - t / 2, cy + g, t, 8, 0, color);
            r2.rect(cx - 8 - g, cy - t / 2, 8, t, 0, color);
            r2.rect(cx + g, cy - t / 2, 8, t, 0, color);
            r2.circle(cx, cy, 1.5f, 0, 1f, color);
         }
         case "T" -> {
            r2.rect(cx - 8, cy - t / 2, 16, t, 0, color);
            r2.rect(cx - t / 2, cy, t, 8, 0, color);
         }
         case "Corner" -> {
            float c = 5f;
            r2.rect(cx - g - c, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy - g - c, t, c, 0, color);
            r2.rect(cx + g, cy - t / 2, c, t, 0, color);
            r2.rect(cx - t / 2, cy + g, t, c, 0, color);
         }
         case "Circle" -> {
            r2.circle(cx, cy, 8f, 0, 1f, Renderer2D.ColorUtil.replAlpha(color, 100));
         }
      }
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mx = click.comp_4798();
      double my = click.comp_4799();

      float startX = LEFT_PAD;
      float startY = 85f;

      for (int i = 0; i < STYLES.length; i++) {
         int col = i % (int) COLS;
         int row = i / (int) COLS;
         float x = startX + col * (PREVIEW_SIZE + PREVIEW_GAP + 60f) - 4;
         float y = startY + row * (PREVIEW_SIZE + PREVIEW_GAP + 18f) - 4;
         float cardW = PREVIEW_SIZE + 60f;
         float cardH = PREVIEW_SIZE + 18f;

         if (mx >= x && mx <= x + cardW && my >= y && my <= y + cardH) {
            if (CrosshairModule.getInstance() != null) {
               CrosshairModule.getInstance().style.currentMode = STYLES[i];
            }
            return super.mouseClicked(click, doubled);
         }
      }
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean shouldCloseOnEsc() { return true; }

   @Override
   public void close() { client.setScreen(parent); }

   private void drawSafeText(String text, float x, float y, TextureUse font, float size, Color color) {
      if (Client.FONTS.get(font) != null) Client.RENDERER.text(text, x, y, font, size, color);
   }
}
