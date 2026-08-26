package ru.fluxvisuals.ui.gui.component.render;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.cfg.ConfigManager;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.StyleConfig;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.api.Theme;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.animation.util.Easings;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Специальные вкладки ClickGUI: Themes / Styles / Configs.
 * Вместо списка модулей показывают панели настроек тем, стилей и конфигов.
 */
@Environment(EnvType.CLIENT)
public class GuiRenderSpecial extends GuiScreen {
   public static final float PAD = 4.0F;

   public static float rowX() {
      return GuiLayout.clipX() + PAD;
   }

   public static float rowY(int index, float scroll) {
      return GuiLayout.clipY() + 3.0F + index * GuiLayout.ROW_STEP - scroll;
   }

   public static float rowW() {
      return GuiLayout.moduleWidth();
   }

   public static float rowH() {
      return GuiLayout.MODULE_HEIGHT;
   }

   public static void render(Renderer2D r2, float mainAlpha, int mouseX, int mouseY) {
      if (GuiScreen.selectedCategories != Category.Configs) {
         return;
      }
      GuiScreen.getScrollUtil().setEnabled(true);
      GuiScreen.getScrollUtil().setSpeed(6.0F);
      renderConfigs(r2, mainAlpha);
   }

   private static void renderThemes(Renderer2D r2, float mainAlpha) {
      Theme[] themes = Theme.values();
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backThree = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      float scroll = GuiScreen.getScrollUtil().getScroll();
      GuiScreen.getScrollUtil().setMax(themes.length * GuiLayout.ROW_STEP, GuiLayout.clipHeight());

      for (int i = 0; i < themes.length; i++) {
         Theme theme = themes[i];
         float y = rowY(i, scroll);
         if (y < GuiLayout.clipY() - GuiLayout.ROW_STEP || y > GuiLayout.clipY() + GuiLayout.clipHeight()) {
            continue;
         }
         boolean selected = theme == GuiScreen.selectedTheme;
         float x = rowX();
         float w = rowW();
         r2.rect(x, y, w, rowH(), 5.0F, selected ? backThree : Renderer2D.ColorUtil.replAlpha(backThree, 90));
         if (selected) {
            r2.rect(x, y, 2.0F, rowH(), 1.0F, mainColor);
         }
         r2.rect(x + 8.0F, y + (rowH() - 14.0F) / 2.0F, 14.0F, 14.0F, 3.0F, theme.getMain().getRGB());
         r2.text(FontRegistry.INTER_MEDIUM, x + 30.0F, y + rowH() / 2.0F + 0.8F, 13.0F, theme.name(), textColor);
         r2.text(FontRegistry.INTER_MEDIUM, x + w - 24.0F, y + rowH() / 2.0F + 0.8F, 12.0F, selected ? "e" : "", mainColor);
      }
   }

   private static void renderStyles(Renderer2D r2, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backThree = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      float scroll = GuiScreen.getScrollUtil().getScroll();
      int rows = styleRows().size();
      GuiScreen.getScrollUtil().setMax(rows * GuiLayout.ROW_STEP, GuiLayout.clipHeight());

      int idx = 0;
      for (StyleRow row : styleRows()) {
         float y = rowY(idx, scroll);
         if (y >= GuiLayout.clipY() - GuiLayout.ROW_STEP && y <= GuiLayout.clipY() + GuiLayout.clipHeight()) {
            float x = rowX();
            float w = rowW();
            r2.rect(x, y, w, rowH(), 5.0F, backThree);
            r2.text(FontRegistry.INTER_MEDIUM, x + 10.0F, y + rowH() / 2.0F + 0.8F, 13.0F, row.name, textColor);
            if (row.get.get()) {
               r2.rect(x + w - 22.0F, y + (rowH() - 14.0F) / 2.0F, 14.0F, 14.0F, 3.0F, mainColor);
            } else {
               r2.rectOutline(x + w - 22.0F, y + (rowH() - 14.0F) / 2.0F, 14.0F, 14.0F, 3.0F, outlineColor, 0.2F);
            }
         }
         idx++;
      }
   }

   private static void renderConfigs(Renderer2D r2, float mainAlpha) {
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backThree = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int backHover = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(18.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int mutedText = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(110.0F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int dangerColor = Renderer2D.ColorUtil.replAlpha(0xFFFF5353, (int)(255.0F * mainAlpha));

      float x = rowX();
      float w = GuiLayout.clipWidth() - 8.0F;
      float inputH = 24.0F;
      float inputY = GuiLayout.clipY() + 2.0F;
      float btnY = inputY + inputH + 6.0F;
      float btnH = 20.0F;
      float gap = 4.0F;
      float btnW = (w - gap * 3.0F) / 4.0F;
      float listY0 = btnY + btnH + 8.0F;
      float listHeaderH = 16.0F;
      float rowH = 20.0F;
      float gapR = 3.0F;

      // ---- Поле ввода имени конфига ----
      boolean editing = GuiScreen.configNameEditing;
      r2.rect(x, inputY, w, inputH, 6.0F, editing ? backHover : backThree);
      r2.rectOutline(x, inputY, w, inputH, 6.0F, outlineColor, editing ? 0.35F : 0.2F);
      r2.text(FontRegistry.ICONS, x + 9.0F, inputY + inputH / 2.0F + 0.8F, 14.0F, "c", mutedText);
      String shown = GuiScreen.configNameText.isEmpty() ? "имя конфига…" : GuiScreen.configNameText;
      r2.text(FontRegistry.INTER_MEDIUM, x + 26.0F, inputY + inputH / 2.0F + 0.8F, 13.0F, shown,
            GuiScreen.configNameText.isEmpty() ? mutedText : textColor);
      r2.text(FontRegistry.INTER_MEDIUM, x + w - 20.0F, inputY + inputH / 2.0F + 0.8F, 13.0F, editing ? "|" : "", mainColor);

      // ---- Компактные кнопки действий ----
      String[] actions = new String[]{"Save", "Load", "Reset", "Dir"};
      for (int i = 0; i < 4; i++) {
         float bx = x + i * (btnW + gap);
         r2.rect(bx, btnY, btnW, btnH, 5.0F, i == 0 ? backHover : backThree);
         r2.rectOutline(bx, btnY, btnW, btnH, 5.0F, outlineColor, 0.2F);
         float tw = r2.measureText(FontRegistry.INTER_MEDIUM, actions[i], 12.0F).width;
         r2.text(FontRegistry.INTER_MEDIUM, bx + btnW / 2.0F - tw / 2.0F, btnY + btnH / 2.0F + 0.8F, 12.0F, actions[i],
               i == 0 ? mainColor : textColor);
      }

      // ---- Список конфигов ----
      float scroll = GuiScreen.getScrollUtil().getScroll();
      List<String> configs = configNames();
      float listHeight = GuiLayout.clipHeight() - (listY0 - GuiLayout.clipY());
      GuiScreen.getScrollUtil().setMax(configs.size() * (rowH + gapR) + listHeaderH + 10.0F, listHeight);

      r2.text(FontRegistry.INTER_MEDIUM, x + 2.0F, listY0, 12.0F, "Конфиги (" + configs.size() + ")", mutedText);
      r2.rect(x + 2.0F, listY0 + listHeaderH - 4.0F, w - 4.0F, 0.6F, 0.3F, Renderer2D.ColorUtil.replAlpha(outlineColor, 40));

      for (int i = 0; i < configs.size(); i++) {
         float y = listY0 + listHeaderH + i * (rowH + gapR) - scroll;
         if (y < listY0 + listHeaderH - rowH || y > listY0 + listHeaderH + listHeight) {
            continue;
         }
         boolean hovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, x, y, w, rowH);
         r2.rect(x, y, w, rowH, 4.0F, hovered ? backHover : backThree);
         r2.text(FontRegistry.INTER_MEDIUM, x + 9.0F, y + rowH / 2.0F + 0.8F, 13.0F, configs.get(i), textColor);
         // Кнопка загрузки (иконка-стрелка)
         r2.text(FontRegistry.ICONS, x + w - 38.0F, y + rowH / 2.0F + 0.8F, 12.0F, "e", mainColor);
         // Кнопка удаления (крестик)
         r2.text(FontRegistry.ICONS, x + w - 20.0F, y + rowH / 2.0F + 0.8F, 12.0F, "x", dangerColor);
      }
   }

   // ===== Data for click handling =====

   public static class StyleRow {
      public final String name;
      public final java.util.function.Supplier<Boolean> get;
      public final Runnable toggle;

      public StyleRow(String name, java.util.function.Supplier<Boolean> get, Runnable toggle) {
         this.name = name;
         this.get = get;
         this.toggle = toggle;
      }
   }

   public static List<StyleRow> styleRows() {
      List<StyleRow> list = new ArrayList<>();
      list.add(new StyleRow("Watermark Logo", () -> StyleConfig.watermarkLogo, () -> StyleConfig.watermarkLogo = !StyleConfig.watermarkLogo));
      list.add(new StyleRow("Watermark Glow", () -> StyleConfig.watermarkGlow, () -> StyleConfig.watermarkGlow = !StyleConfig.watermarkGlow));
      list.add(new StyleRow("Watermark Name", () -> StyleConfig.watermarkName, () -> StyleConfig.watermarkName = !StyleConfig.watermarkName));
      list.add(new StyleRow("ClickGUI Logo", () -> StyleConfig.clickGuiLogo, () -> StyleConfig.clickGuiLogo = !StyleConfig.clickGuiLogo));
      return list;
   }

   public static List<String> configNames() {
      List<String> names = new ArrayList<>();
      if (FluxVisualsClient.get != null && FluxVisualsClient.get.configManager != null) {
         for (ru.fluxvisuals.cfg.Config config : ConfigManager.getLoadedConfigs()) {
            names.add(config.getName());
         }
      }
      return names;
   }
}
