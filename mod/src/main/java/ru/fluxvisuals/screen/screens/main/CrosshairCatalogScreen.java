package ru.fluxvisuals.screen.screens.main;

import java.util.function.Consumer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import ru.fluxvisuals.crosshair.CrosshairSettings;
import ru.fluxvisuals.crosshair.RenderCrosshairDrawer;

/**
 * Мастерская прицелов — экран настройки кастомного прицела.
 * Сетка стилей + слайдеры (размер, толщина, зазор, цвет) + живое превью.
 * Настройки сохраняются в crosshair.json и загружаются в игре всегда.
 */
public class CrosshairCatalogScreen extends Screen {
   private final Screen parent;
   private CrosshairSettings settings;

   private static final int PANEL_X = 20;
   private static final int PANEL_Y = 60;
   private static final int PANEL_W = 240;
   private static final float PREVIEW_SIZE = 34f;
   private static final float PREVIEW_GAP = 10f;
   private static final int COLS = 3;

   public CrosshairCatalogScreen(Screen parent) {
      super(Text.empty());
      this.parent = parent;
   }

   @Override
   public void render(DrawContext context, int mouseX, int mouseY, float delta) {
      context.fill(0, 0, width, height, 0xCC0D0D12);

      // Заголовок
      drawTextWithShadow(context, "Мастерская прицелов", 22, 18, 0xFF13FFAE, 1.6f);
      drawTextWithShadow(context, "Выбери стиль и настрой под себя — прицел применится в игре", 22, 44, 0xFF9AA0AB, 1f);

      // ===== Сетка стилей =====
      float startX = PANEL_X;
      float startY = PANEL_Y;
      for (int i = 0; i < CrosshairSettings.STYLES.length; i++) {
         int col = i % COLS;
         int row = i / COLS;
         float x = startX + col * (PREVIEW_SIZE + PREVIEW_GAP + 45f);
         float y = startY + row * (PREVIEW_SIZE + PREVIEW_GAP + 18f);

         boolean selected = settings.style.equalsIgnoreCase(CrosshairSettings.STYLES[i]);
         boolean hovered = mouseX >= x - 4 && mouseX <= x + PREVIEW_SIZE + 40f
            && mouseY >= y - 4 && mouseY <= y + PREVIEW_SIZE + 16f;

         int bg = selected ? 0xDD19FFAE : hovered ? 0xCC2E2E38 : 0xBB1B1B22;
         int border = selected ? 0x8819FFAE : 0x33FFFFFF;
         fillCard(context, x, y, PREVIEW_SIZE + 44f, PREVIEW_SIZE + 20f, bg, border);

         // Превью прицела
         CrosshairSettings preview = CrosshairSettings.createPreview(CrosshairSettings.STYLES[i]);
         preview.size = settings.size * 0.8f;
         preview.thickness = settings.thickness;
         preview.gap = settings.gap;
         preview.colorR = settings.colorR;
         preview.colorG = settings.colorG;
         preview.colorB = settings.colorB;
         RenderCrosshairDrawer.drawAt(context, preview, x + PREVIEW_SIZE / 2 + 3, y + PREVIEW_SIZE / 2);

         // Название
         drawTextWithShadow(context, CrosshairSettings.STYLES[i],
            (int) x, (int) (y + PREVIEW_SIZE + 6), selected ? 0xFF13FFAE : 0xFFE3E5E9, 1f);
      }

      // ===== Живое превью в центре экрана =====
      float px = width / 2f;
      float py = height / 2f;
      RenderCrosshairDrawer.drawAt(context, settings, px, py);

      // ===== Подписи слайдеров =====
      int sliderX = PANEL_X;
      int sliderY = PANEL_Y + 100;
      drawTextWithShadow(context, "Размер: " + fmt(settings.size), sliderX, sliderY - 6, 0xFFD9DDE3, 1f);
      drawTextWithShadow(context, "Толщина: " + fmt(settings.thickness), sliderX, sliderY + 14, 0xFFD9DDE3, 1f);
      drawTextWithShadow(context, "Зазор: " + fmt(settings.gap), sliderX, sliderY + 34, 0xFFD9DDE3, 1f);
      drawTextWithShadow(context, "Красный: " + settings.colorR, sliderX, sliderY + 54, 0xFFD9DDE3, 1f);
      drawTextWithShadow(context, "Зелёный: " + settings.colorG, sliderX, sliderY + 74, 0xFFD9DDE3, 1f);
      drawTextWithShadow(context, "Синий: " + settings.colorB, sliderX, sliderY + 94, 0xFFD9DDE3, 1f);
   }

   @Override
   protected void init() {
      super.init();
      settings = CrosshairSettings.getInstance();
      if (!settings.isStyleValid()) settings.style = "Cross";

      int sliderX = PANEL_X;
      int sliderY = PANEL_Y + 106;
      int sliderW = PANEL_W - 20;

      addDrawableChild(new CrosshairSlider(sliderX, sliderY - 4, sliderW, "Размер",
         settings.size, 2f, 15f, v -> settings.size = v));
      addDrawableChild(new CrosshairSlider(sliderX, sliderY + 16, sliderW, "Толщина",
         settings.thickness, 0.5f, 4f, v -> settings.thickness = v));
      addDrawableChild(new CrosshairSlider(sliderX, sliderY + 36, sliderW, "Зазор",
         settings.gap, 0f, 10f, v -> settings.gap = v));
      addDrawableChild(new CrosshairSlider(sliderX, sliderY + 56, sliderW, "Красный",
         settings.colorR, 0f, 255f, v -> settings.colorR = v.intValue()));
      addDrawableChild(new CrosshairSlider(sliderX, sliderY + 76, sliderW, "Зелёный",
         settings.colorG, 0f, 255f, v -> settings.colorG = v.intValue()));
      addDrawableChild(new CrosshairSlider(sliderX, sliderY + 96, sliderW, "Синий",
         settings.colorB, 0f, 255f, v -> settings.colorB = v.intValue()));

      // Сохранить
      addDrawableChild(ButtonWidget.builder(Text.of("Сохранить прицел"), b -> settings.save())
         .dimensions(sliderX, sliderY + 118, sliderW, 20)
         .tooltip(Tooltip.of(Text.of("Сохранить и применить в игре")))
         .build());

      // Вкл/выкл прицел
      addDrawableChild(ButtonWidget.builder(
         Text.of(settings.enabled ? "Прицел: ВКЛ" : "Прицел: ВЫКЛ"),
         b -> {
            settings.enabled = !settings.enabled;
            settings.save();
            b.setMessage(Text.of(settings.enabled ? "Прицел: ВКЛ" : "Прицел: ВЫКЛ"));
         }).dimensions(sliderX, sliderY + 142, sliderW - 70, 16).build());

      // Сброс
      addDrawableChild(ButtonWidget.builder(Text.of("Сбросить"), b -> {
         settings.style = "Cross";
         settings.size = 6f;
         settings.thickness = 1.5f;
         settings.gap = 3f;
         settings.colorR = 255;
         settings.colorG = 255;
         settings.colorB = 255;
         settings.save();
      }).dimensions(sliderX + sliderW - 64, sliderY + 142, 64, 16).build());
   }

   @Override
   public boolean mouseClicked(Click click, boolean doubled) {
      double mx = click.comp_4798();
      double my = click.comp_4799();

      float startX = PANEL_X;
      float startY = PANEL_Y;
      for (int i = 0; i < CrosshairSettings.STYLES.length; i++) {
         int col = i % COLS;
         int row = i / COLS;
         float x = startX + col * (PREVIEW_SIZE + PREVIEW_GAP + 45f) - 4;
         float y = startY + row * (PREVIEW_SIZE + PREVIEW_GAP + 18f) - 4;
         if (mx >= x && mx <= x + PREVIEW_SIZE + 48 && my >= y && my <= y + PREVIEW_SIZE + 24) {
            settings.style = CrosshairSettings.STYLES[i];
            settings.save();
            return super.mouseClicked(click, doubled);
         }
      }
      return super.mouseClicked(click, doubled);
   }

   @Override
   public boolean shouldCloseOnEsc() { return true; }

   @Override
   public void close() {
      settings.save();
      client.setScreen(parent);
   }

   private String fmt(float v) { return String.format("%.1f", v); }

   private void fillCard(DrawContext ctx, float x, float y, float w, float h, int bg, int border) {
      int x0 = (int) x, y0 = (int) y;
      int x1 = (int) (x + w), y1 = (int) (y + h);
      ctx.fill(x0, y0, x1, y1, bg);
      ctx.fill(x0, y0, x1, y0 + 1, border);
      ctx.fill(x0, y1 - 1, x1, y1, border);
      ctx.fill(x0, y0, x0 + 1, y1, border);
      ctx.fill(x1 - 1, y0, x1, y1, border);
   }

   private void drawTextWithShadow(DrawContext ctx, String text, int x, int y, int color, float scale) {
      if (scale != 1f) {
         ctx.getMatrices().pushMatrix();
         ctx.getMatrices().scale(scale, scale);
         ctx.drawText(this.textRenderer, text, (int) (x / scale), (int) (y / scale), color, true);
         ctx.getMatrices().popMatrix();
      } else {
         ctx.drawText(this.textRenderer, text, x, y, color, true);
      }
   }

   /** Слайдер с колбэком. */
   private static class CrosshairSlider extends SliderWidget {
      private final float min;
      private final float max;
      private final Consumer<Float> onChanged;

      CrosshairSlider(int x, int y, int width, String label, float value, float min, float max,
                      Consumer<Float> onChanged) {
         super(x, y, width, 16, Text.of(label), (value - min) / (max - min));
         this.min = min;
         this.max = max;
         this.onChanged = onChanged;
      }

      @Override
      protected void updateMessage() {}

      @Override
      protected void applyValue() {
         onChanged.accept(min + (max - min) * (float) value);
      }
   }
}
