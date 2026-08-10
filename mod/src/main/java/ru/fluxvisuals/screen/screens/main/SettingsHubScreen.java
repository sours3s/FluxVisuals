package ru.fluxvisuals.screen.screens.main;

import java.awt.Color;
import java.util.ArrayList;
import org.joml.Vector2f;
import org.joml.Vector4f;
import ru.fluxvisuals.Client;
import ru.fluxvisuals.api.render.system.TextureUse;
import ru.fluxvisuals.screen.screens.main.widgets.CustomButton;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Экран настроек/лобби — открывается из TitleScreen.
 * Показывает: ClickGUI, Мастерская прицелов, список всех модулей с тумблерами.
 */
public class SettingsHubScreen extends Screen {

   private final Screen parent;
   private final ArrayList<CustomButton> buttons = new ArrayList<>();
   private final ArrayList<String> buttonLabels = new ArrayList<>();
   private float fadeProgress = 0f;

   private static final float BTN_W = 200f;
   private static final float BTN_H = 24f;
   private static final float SPACING = 5f;
   private static final float LEFT_PAD = 30f;

   public SettingsHubScreen(Screen parent) {
      super(Text.empty());
      this.parent = parent;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      fadeProgress = Math.min(1f, fadeProgress + 0.06f * delta);
      int alpha = (int)(fadeProgress * 255);

      context.fill(0, 0, width, height, (0xCC111111 & 0xFF000000) | (0xCC << 24));

      Client.RENDERER.setDrawContext(context);

      // Title
      drawSafeText("Настройки", LEFT_PAD, 50f, TextureUse.SFMEDIUM, 14f, new Color(19, 255, 174, alpha));
      drawSafeText("FluxVisuals — конфигурация клиента", LEFT_PAD, 72f, TextureUse.SFMEDIUM, 7f, new Color(150, 150, 160, alpha));

      // Buttons
      for (int i = 0; i < buttons.size(); i++) {
         CustomButton btn = buttons.get(i);
         String label = buttonLabels.get(i);

         float delay = i * 0.04f;
         float progress = Math.max(0f, Math.min(1f, (fadeProgress - delay) / Math.max(0.01f, 1f - delay)));
         float slide = (1f - easeOutCubic(progress)) * 20f;
         btn.setY(btn.getBaseY() + slide);
         btn.setAlpha(easeOutCubic(progress));

         float bx = btn.getX();
         float by = btn.getY();
         float bw = btn.getWidth();
         float bh = btn.getHeight();
         float a = btn.getAlpha();

         if (a > 0.01f) {
            boolean hovered = mouseX >= bx && mouseX <= bx + bw && mouseY >= by && mouseY <= by + bh;
            Vector4f round = new Vector4f(6f, 6f, 6f, 6f);

            Color bg = hovered
               ? new Color(19, 255, 174, (int)(a * 20))
               : new Color(30, 30, 35, (int)(a * 200));
            Color textCol = hovered
               ? new Color(19, 255, 174, (int)(a * 255))
               : new Color(220, 220, 230, (int)(a * 220));

            Client.RENDERER.rect(bx, by, bw, bh, round, 1f, bg, bg, bg, bg);
            Client.RENDERER.outline(bx, by, bw, bh, 0.3f, round, new Vector2f(1),
               new Color(255, 255, 255, (int)(a * 6)), new Color(255, 255, 255, (int)(a * 6)),
               new Color(255, 255, 255, (int)(a * 6)), new Color(255, 255, 255, (int)(a * 6)));

            drawSafeTextCentered(label, bx + bw / 2f, by + bh / 2f, TextureUse.SFMEDIUM, 7.5f, textCol);
         }
      }

      // Back button
      drawSafeText("ESC — назад", LEFT_PAD, height - 30f, TextureUse.SFMEDIUM, 6.5f, new Color(120, 120, 130, alpha));
   }

   @Override
   protected void init() {
      super.init();
      fadeProgress = 0f;
      buttons.clear();
      buttonLabels.clear();

      float y = 100f;
      float margin = BTN_H + SPACING;

      // Open ClickGUI
      addButton(LEFT_PAD, y, BTN_W, BTN_H, "ClickGUI (все модули)",
         CustomButton.CustomButtonBuilder.ButtonType.MAIN,
         () -> client.setScreen(null)); // closes this screen, user opens ClickGUI via keybind

      // Мастерская прицелов (доступна и отдельной кнопкой в главном меню)
      addButton(LEFT_PAD, y + margin, BTN_W, BTN_H, "Мастерская прицелов",
         CustomButton.CustomButtonBuilder.ButtonType.ALT,
         () -> client.setScreen(new CrosshairCatalogScreen(this)));
   }

   private void addButton(float x, float y, float w, float h, String label,
                           CustomButton.CustomButtonBuilder.ButtonType type, Runnable action) {
      buttons.add(CustomButton.CustomButtonBuilder.build(x, y, w, h, "", type, action));
      buttonLabels.add(label);
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mx = click.comp_4798();
      double my = click.comp_4799();
      buttons.forEach(b -> b.click((int) mx, (int) my, click.button()));
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean shouldCloseOnEsc() { return true; }

   @Override
   public void close() {
      client.setScreen(parent);
   }

   private void drawSafeText(String text, float x, float y, TextureUse font, float size, Color color) {
      if (Client.FONTS.get(font) != null) {
         Client.RENDERER.text(text, x, y, font, size, color);
      }
   }

   private void drawSafeTextCentered(String text, float x, float y, TextureUse font, float size, Color color) {
      if (Client.FONTS.get(font) != null) {
         Client.RENDERER.textCenteredStrict(text, x, y - 1f, font, size, color);
      }
   }

   private float easeOutCubic(float x) { return 1f - (float) Math.pow(1f - x, 3); }
}
