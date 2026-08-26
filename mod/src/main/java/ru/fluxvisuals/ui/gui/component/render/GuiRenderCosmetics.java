package ru.fluxvisuals.ui.gui.component.render;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.color.ColorUtil;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.math.MathHelper;
import ru.fluxvisuals.util.render.text.FontRegistry;
import ru.fluxvisuals.utils.cosmetics.CosmeticEntry;
import ru.fluxvisuals.utils.cosmetics.CosmeticsRepository;
import ru.fluxvisuals.utils.cosmetics.FiguraBridge;

@Environment(EnvType.CLIENT)
public class GuiRenderCosmetics extends GuiScreen {
   private static CosmeticEntry.Kind[] tabKinds = CosmeticEntry.Kind.values();
   private static final float TAB_HEIGHT = 20.0F;
   private static final float TAB_GAP = 4.0F;
   private static final float CARD_GAP = 5.0F;
   private static final float CARD_RADIUS = 5.0F;
   private static final int COLS = 2;
   private static final float THUMB_INSET = 6.0F;
   private static final float CARD_HEIGHT = 92.0F;
   private static final float CARD_TEXT_AREA = 18.0F;
   private static float cachedTabStartX;
   private static float cachedTabTotalWidth;
   private static float[] cachedTabWidths = new float[0];

   public static void render(Renderer2D r, MatrixStack pose, int mouseX, int mouseY, float mainAlpha) {
      if (GuiScreen.selectedCategories != Category.Cosmetics) return;

      float contentX = GuiLayout.contentX();
      float contentY = GuiLayout.contentY();
      float contentW = GuiLayout.contentWidth();
      float contentH = GuiLayout.clipHeight() - 3.0F;

      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int mainColor6 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha));
      int mainColor16 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(40.8F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(120.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(60.0F * mainAlpha));
      int bgDim = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(100.0F * mainAlpha));

      List<CosmeticEntry> entries = getEntries();
      float pad = 5.0F;
      float tabY = contentY + pad;
      float bodyTop = tabY + TAB_HEIGHT + 3.0F;
      float footerH = 18.0F;
      float bodyH = contentH - (TAB_HEIGHT + 3.0F) - footerH - pad;
      float bodyBottom = bodyTop + bodyH;

      r.pushRoundedClipRect(contentX, contentY, contentW, contentH, 0, 0, 0, 0);

      renderTabs(r, entries, contentX, tabY, contentW, pad, mainAlpha, mainColor, mainColor40, mainColor6, mainColor16, outlineColor, textColor, textTwoColor, textTwoColor40);
      renderGrid(r, entries, contentX, bodyTop, contentW, bodyH, bodyBottom, mouseX, mouseY, mainAlpha, mainColor, mainColor40, mainColor6, mainColor16, outlineColor, bgDim, textColor, textTwoColor40);
      renderFooter(r, entries, contentX, bodyBottom + 2.0F, contentW, footerH, mainAlpha, mainColor, mainColor40, mainColor6, outlineColor, textColor, textTwoColor40);

      r.popClipRect();
   }

   private static void renderTabs(Renderer2D r, List<CosmeticEntry> allEntries, float x, float y, float w, float pad, float mainAlpha,
         int mainColor, int mainColor40, int mainColor6, int mainColor16, int outlineColor, int textColor, int textTwoColor, int textTwoColor40) {

      float totalTabWidth = 0;
      cachedTabWidths = new float[tabKinds.length];
      for (int i = 0; i < tabKinds.length; i++) {
         String label = tabKinds[i].tabName();
         float lw = r.measureText(FontRegistry.INTER_MEDIUM, label, 10.0F).width;
         cachedTabWidths[i] = lw + 18.0F;
         totalTabWidth += cachedTabWidths[i];
      }
      totalTabWidth += (tabKinds.length - 1) * TAB_GAP;
      cachedTabStartX = x + (w - totalTabWidth) / 2.0F;
      cachedTabTotalWidth = totalTabWidth;
      float cursorX = cachedTabStartX;

      for (int i = 0; i < tabKinds.length; i++) {
         float tabW = cachedTabWidths[i];
         boolean active = i == GuiScreen.cosmeticTab;
         boolean hovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, cursorX, y, tabW, TAB_HEIGHT);

         int bg = active ? mainColor16 : (hovered ? mainColor6 : 0);
         int border = active ? mainColor40 : outlineColor;
         int labelCol = active ? mainColor : textTwoColor;

         r.rectOutline(cursorX, y, tabW, TAB_HEIGHT, 5.0F, border, 0.1F);
         r.rect(cursorX, y, tabW, TAB_HEIGHT, 5.0F, bg);

         String label = tabKinds[i].tabName();
         float lw = r.measureText(FontRegistry.INTER_MEDIUM, label, 10.0F).width;
         r.text(FontRegistry.INTER_MEDIUM, cursorX + (tabW - lw) / 2.0F, y + 4.0F, 10.0F, label, ColorUtil.replAlpha(labelCol, (int)(255.0F * mainAlpha)));

         cursorX += tabW + TAB_GAP;
      }

      int applied = FiguraBridge.appliedId().isEmpty() ? textTwoColor40 : mainColor40;
      String eqLabel = "Unequip";
      float eqW = r.measureText(FontRegistry.INTER_MEDIUM, eqLabel, 10.0F).width + 12.0F;
      float eqX = x + w - pad - eqW;
      r.rectOutline(eqX, y, eqW, TAB_HEIGHT, 5.0F, outlineColor, 0.1F);
      r.rect(eqX, y, eqW, TAB_HEIGHT, 5.0F, 0);
      r.text(FontRegistry.INTER_MEDIUM, eqX + 6.0F, y + 4.0F, 10.0F, eqLabel, applied);
   }

   private static void renderGrid(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float h,
         float bottom, int mouseX, int mouseY, float mainAlpha,
         int mainColor, int mainColor40, int mainColor6, int mainColor16, int outlineColor, int bgDim, int textColor, int textTwoColor40) {

      GuiScreen.cosmeticHoverIndex = -1;
      if (entries.isEmpty()) {
         renderEmptyState(r, x, y, w, h, mainAlpha, mainColor, textTwoColor40);
         return;
      }

      float pad = 5.0F;
      float cardW = (w - pad * 2.0F - CARD_GAP) / COLS;
      float thumbH = CARD_HEIGHT - CARD_TEXT_AREA - THUMB_INSET;
      float thumbW = cardW - THUMB_INSET * 2.0F;
      float totalRows = (float) Math.ceil((double) entries.size() / COLS);
      float totalH = totalRows * CARD_HEIGHT + Math.max(0, totalRows - 1) * CARD_GAP;
      GuiScreen.cosmeticMaxScroll = Math.max(0.0F, totalH - h);
      GuiScreen.cosmeticScroll = MathHelper.clamp(GuiScreen.cosmeticScroll, 0.0F, GuiScreen.cosmeticMaxScroll);

      r.pushRoundedClipRect(x, y, w, h, 0, 0, 0, 0);

      for (int i = 0; i < entries.size(); i++) {
         int col = i % COLS;
         int row = i / COLS;
         float cardX = x + pad + col * (cardW + CARD_GAP);
         float cardY = y + row * (CARD_HEIGHT + CARD_GAP) - GuiScreen.cosmeticScroll;

         if (cardY + CARD_HEIGHT < y - 2.0F || cardY > bottom + 2.0F) continue;

         CosmeticEntry entry = entries.get(i);
         boolean selected = i == GuiScreen.cosmeticSelectedIndex;
         boolean applied = FiguraBridge.isApplied(entry);
         boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, cardX, cardY, cardW, CARD_HEIGHT);
         if (hovered) GuiScreen.cosmeticHoverIndex = i;

         int cardBg = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(70.0F * mainAlpha));
         int cardBorder = applied ? mainColor40 : (selected ? mainColor6 : outlineColor);

         r.rectOutline(cardX, cardY, cardW, CARD_HEIGHT, CARD_RADIUS, cardBorder, 0.1F);
         r.rect(cardX, cardY, cardW, CARD_HEIGHT, CARD_RADIUS, cardBg);

         int glId = CosmeticsRepository.loadPreview(entry);
         if (glId >= 0 && entry.previewWidth() > 0 && entry.previewHeight() > 0) {
            float px = cardX + THUMB_INSET;
            float py = cardY + THUMB_INSET;
            r.rect(px, py, thumbW, thumbH, 4.0F, bgDim);
            float scaleX = thumbW / (float) entry.previewWidth();
            float scaleY = thumbH / (float) entry.previewHeight();
            float scale = Math.min(scaleX, scaleY);
            float dw = entry.previewWidth() * scale;
            float dh = entry.previewHeight() * scale;
            float dx = px + (thumbW - dw) / 2.0F;
            float dy = py + (thumbH - dh) / 2.0F;
            r.drawRgbaTexture(glId, dx, dy, dw, dh, -1, false);
         } else {
            r.rect(cardX + THUMB_INSET, cardY + THUMB_INSET, thumbW, thumbH, 4.0F, mainColor6);
            String kindLetter = entry.kind() == CosmeticEntry.Kind.MODEL ? "M" : (entry.kind() == CosmeticEntry.Kind.HEAD ? "H" : "W");
            float letterSize = 18.0F;
            float lw = r.measureText(FontRegistry.ICONS, kindLetter, letterSize).width;
            r.text(FontRegistry.ICONS, cardX + THUMB_INSET + (thumbW - lw) / 2.0F, cardY + THUMB_INSET + (thumbH - letterSize) / 2.0F + 4.0F, letterSize, kindLetter, mainColor16);
         }

         if (applied) {
            float badgeSize = 12.0F;
            float badgeX = cardX + cardW - THUMB_INSET - badgeSize;
            float badgeY = cardY + THUMB_INSET;
            r.rect(badgeX, badgeY, badgeSize, badgeSize, badgeSize / 2.0F, mainColor40);
            String check = "V";
            float cl = r.measureText(FontRegistry.ICONS, check, 7.0F).width;
            r.text(FontRegistry.ICONS, badgeX + (badgeSize - cl) / 2.0F, badgeY + 2.0F, 7.0F, check, mainColor);
         }

         float textY = cardY + CARD_HEIGHT - CARD_TEXT_AREA;
         String displayName = truncate(r, entry.displayName(), cardW - 8.0F, 9.0F);
         float nameW = r.measureText(FontRegistry.INTER_MEDIUM, displayName, 9.0F).width;
         r.text(FontRegistry.INTER_MEDIUM, cardX + (cardW - nameW) / 2.0F, textY + 2.0F, 9.0F, displayName,
               ColorUtil.replAlpha(applied ? mainColor : textColor, (int)(255.0F * mainAlpha)));
      }

      if (GuiScreen.cosmeticMaxScroll > 0.0F && h > 20.0F) {
         float scrollBarH = Math.max(12.0F, h * (h / totalH));
         float scrollBarY = y + (h - scrollBarH) * (GuiScreen.cosmeticScroll / GuiScreen.cosmeticMaxScroll);
         r.rect(x + w - 2.5F, scrollBarY, 1.5F, scrollBarH, 0.75F, mainColor6);
      }

      r.popClipRect();
   }

   private static void renderEmptyState(Renderer2D r, float x, float y, float w, float h, float mainAlpha,
         int mainColor, int textTwoColor40) {
      float centerX = x + w / 2.0F;
      float centerY = y + h / 2.0F;
      boolean folderExists = false;
      try {
         folderExists = java.nio.file.Files.isDirectory(CosmeticsRepository.directory());
      } catch (Exception ignored) {}

      float boxSize = 36.0F;
      r.rect(centerX - boxSize / 2.0F, centerY - boxSize / 2.0F - 10.0F, boxSize, boxSize, boxSize / 2.0F,
            Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(80.0F * mainAlpha)));

      String title = folderExists ? "No avatars in this tab" : "Cosmetics folder not found";
      float titleW = r.measureText(FontRegistry.INTER_MEDIUM, title, 10.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, centerX - titleW / 2.0F, centerY + 18.0F, 10.0F, title, textTwoColor40);

      String sub = "Drop Figura avatars into fluxvisuals/cosmetics/";
      float subW = r.measureText(FontRegistry.INTER_MEDIUM, sub, 8.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, centerX - subW / 2.0F, centerY + 32.0F, 8.0F, sub,
            Renderer2D.ColorUtil.replAlpha(textTwoColor40, (int)(140.0F * mainAlpha)));
   }

   private static void renderFooter(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float h,
         float mainAlpha, int mainColor, int mainColor40, int mainColor6, int outlineColor, int textColor, int textTwoColor40) {

      CosmeticEntry selected = getSelectedEntry();
      boolean hasSelection = selected != null;
      boolean isApplied = hasSelection && FiguraBridge.isApplied(selected);

      float btnW = 55.0F;
      float btnH = 14.0F;
      float btnGap = 3.0F;
      float btnY = y + (h - btnH) / 2.0F;

      float equipX = x + w - 5.0F - btnW;
      boolean equipHovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, equipX, btnY, btnW, btnH);
      int equipBg = isApplied
            ? Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(160.0F * mainAlpha))
            : (equipHovered ? mainColor : Renderer2D.ColorUtil.replAlpha(mainColor, (int)(200.0F * mainAlpha)));
      r.rectOutline(equipX, btnY, btnW, btnH, 3.0F, isApplied ? outlineColor : mainColor40, 0.1F);
      r.rect(equipX, btnY, btnW, btnH, 3.0F, equipBg);
      String eqLabel = isApplied ? "Unequip" : "Equip";
      float eqLW = r.measureText(FontRegistry.INTER_MEDIUM, eqLabel, 9.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, equipX + (btnW - eqLW) / 2.0F, btnY + 2.5F, 9.0F, eqLabel, textColor);

      float refreshX = equipX - btnGap - btnW;
      boolean refreshHovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, refreshX, btnY, btnW, btnH);
      r.rectOutline(refreshX, btnY, btnW, btnH, 3.0F, outlineColor, 0.1F);
      r.rect(refreshX, btnY, btnW, btnH, 3.0F, refreshHovered ? mainColor6 : 0);
      String refLabel = "Refresh";
      float refLW = r.measureText(FontRegistry.INTER_MEDIUM, refLabel, 9.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, refreshX + (btnW - refLW) / 2.0F, btnY + 2.5F, 9.0F, refLabel, textColor);

      float infoX = x + 5.0F;
      float infoMaxW = refreshX - infoX - 8.0F;
      if (hasSelection) {
         String selName = truncate(r, selected.displayName(), infoMaxW, 9.0F);
         r.text(FontRegistry.INTER_MEDIUM, infoX, btnY + 2.5F, 9.0F, selName, mainColor40);
      } else {
         String hint = FiguraBridge.isAvailable() ? "Select an avatar" : "Figura not installed";
         r.text(FontRegistry.INTER_MEDIUM, infoX, btnY + 2.5F, 9.0F, hint, textTwoColor40);
      }

      if (!GuiScreen.cosmeticStatus.isEmpty() && System.currentTimeMillis() < GuiScreen.cosmeticStatusUntil) {
         long remaining = GuiScreen.cosmeticStatusUntil - System.currentTimeMillis();
         float alpha = Math.min(1.0F, remaining / 500.0F);
         int statusColor = GuiScreen.cosmeticStatusOk
               ? Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getColor(150, 214, 168), (int)(255.0F * alpha * mainAlpha))
               : Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getColor(228, 138, 138), (int)(255.0F * alpha * mainAlpha));
         float sw = r.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.cosmeticStatus, 8.0F).width;
         r.text(FontRegistry.INTER_MEDIUM, x + (w - sw) / 2.0F, btnY - 10.0F, 8.0F, GuiScreen.cosmeticStatus, statusColor);
      }
   }

   private static List<CosmeticEntry> getEntries() {
      if (GuiScreen.cosmeticTab < 0 || GuiScreen.cosmeticTab >= tabKinds.length) return List.of();
      return CosmeticsRepository.of(tabKinds[GuiScreen.cosmeticTab]);
   }

   private static CosmeticEntry getSelectedEntry() {
      List<CosmeticEntry> entries = getEntries();
      if (GuiScreen.cosmeticSelectedIndex >= 0 && GuiScreen.cosmeticSelectedIndex < entries.size()) {
         return entries.get(GuiScreen.cosmeticSelectedIndex);
      }
      return null;
   }

   private static String truncate(Renderer2D r, String text, float maxWidth, float size) {
      if (text == null) return "";
      if (r.measureText(FontRegistry.INTER_MEDIUM, text, size).width <= maxWidth) return text;
      int end = text.length();
      while (end > 0 && r.measureText(FontRegistry.INTER_MEDIUM, text.substring(0, end) + "...", size).width > maxWidth) {
         end--;
      }
      return end <= 0 ? text.substring(0, 1) + "..." : text.substring(0, end).stripTrailing() + "...";
   }

   public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) return false;
      if (GuiScreen.selectedCategories != Category.Cosmetics) return false;

      float contentX = GuiLayout.contentX();
      float contentY = GuiLayout.contentY();
      float contentW = GuiLayout.contentWidth();
      float contentH = GuiLayout.clipHeight() - 3.0F;
      float pad = 5.0F;

      if (!GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, contentX, contentY, contentW, contentH)) {
         return false;
      }

      int mx = GuiScreen.currentMouseX;
      int my = GuiScreen.currentMouseY;

      float tabY = contentY + pad;
      float cursorX = cachedTabStartX;

      for (int i = 0; i < tabKinds.length; i++) {
         float tabW = i < cachedTabWidths.length ? cachedTabWidths[i] : 60.0F;
         if (GuiRenderMain.isHovered(mx, my, cursorX, tabY, tabW, TAB_HEIGHT)) {
            if (GuiScreen.cosmeticTab != i) {
               GuiScreen.cosmeticTab = i;
               GuiScreen.cosmeticScroll = 0.0F;
               GuiScreen.cosmeticSelectedIndex = -1;
            }
            return true;
         }
         cursorX += tabW + TAB_GAP;
      }

      float bodyTop = tabY + TAB_HEIGHT + 3.0F;
      float footerH = 18.0F;
      float bodyH = contentH - (TAB_HEIGHT + 3.0F) - footerH - pad;
      float bodyBottom = bodyTop + bodyH;

      float btnW = 55.0F;
      float btnH = 14.0F;
      float btnGap = 3.0F;
      float btnY = bodyBottom + 2.0F + (footerH - btnH) / 2.0F;

      float equipX = contentX + contentW - 5.0F - btnW;
      if (GuiRenderMain.isHovered(mx, my, equipX, btnY, btnW, btnH)) {
         handleEquip();
         return true;
      }
      float refreshX = equipX - btnGap - btnW;
      if (GuiRenderMain.isHovered(mx, my, refreshX, btnY, btnW, btnH)) {
         CosmeticsRepository.rescan();
         GuiScreen.cosmeticScroll = 0.0F;
         GuiScreen.cosmeticSelectedIndex = -1;
         setStatus("Refreshed", true);
         return true;
      }

      float cardW = (contentW - pad * 2.0F - CARD_GAP) / COLS;
      for (int i = 0; i < getEntries().size(); i++) {
         int col = i % COLS;
         int row = i / COLS;
         float cardX = contentX + pad + col * (cardW + CARD_GAP);
         float cardY = bodyTop + row * (CARD_HEIGHT + CARD_GAP) - GuiScreen.cosmeticScroll;
         if (cardY + CARD_HEIGHT < bodyTop - 2.0F || cardY > bodyBottom + 2.0F) continue;
         if (GuiRenderMain.isHovered(mx, my, cardX, cardY, cardW, CARD_HEIGHT)) {
            GuiScreen.cosmeticSelectedIndex = i;
            return true;
         }
      }

      return true;
   }

   public static boolean handleMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (GuiScreen.selectedCategories != Category.Cosmetics) return false;
      float contentX = GuiLayout.contentX();
      float contentY = GuiLayout.contentY();
      float contentW = GuiLayout.contentWidth();
      float contentH = GuiLayout.clipHeight() - 3.0F;
      if (!GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, contentX, contentY, contentW, contentH)) {
         return false;
      }
      GuiScreen.cosmeticScroll -= (float) verticalAmount * 18.0F;
      GuiScreen.cosmeticScroll = MathHelper.clamp(GuiScreen.cosmeticScroll, 0.0F, GuiScreen.cosmeticMaxScroll);
      return true;
   }

   private static void handleEquip() {
      CosmeticEntry selected = getSelectedEntry();
      if (selected == null) {
         CosmeticEntry applied = CosmeticsRepository.byId(FiguraBridge.appliedId());
         if (applied != null) {
            selected = applied;
         }
      }
      if (selected == null) return;

      if (FiguraBridge.isApplied(selected)) {
         boolean ok = FiguraBridge.clear();
         setStatus(ok ? "Avatar removed" : "Figura is unavailable", ok);
      } else {
         boolean ok = FiguraBridge.apply(selected);
         setStatus(ok ? selected.displayName() + " equipped" : "Figura is unavailable", ok);
      }
   }

   private static void setStatus(String message, boolean ok) {
      GuiScreen.cosmeticStatus = message == null ? "" : message;
      GuiScreen.cosmeticStatusOk = ok;
      GuiScreen.cosmeticStatusUntil = System.currentTimeMillis() + 2600L;
   }
}
