package ru.fluxvisuals.ui.gui.component.render;

import java.io.File;
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
   private static final float CONTENT_PAD = 5.0F;
   private static final float TAB_HEIGHT = 18.0F;
   private static final float TAB_GAP = 3.0F;
   private static final float TAB_RADIUS = 4.0F;
   private static final float CARD_GAP = 4.0F;
   private static final float CARD_RADIUS = 4.0F;
   private static final int COLS = 2;
   private static final float HEADER_HEIGHT = 16.0F;
   private static final float FOOTER_HEIGHT = 22.0F;

   private static CosmeticEntry.Kind[] tabKinds = CosmeticEntry.Kind.values();

   public static void render(Renderer2D renderer2D, MatrixStack pose, int mouseX, int mouseY, float mainAlpha) {
      if (GuiScreen.selectedCategories != Category.Cosmetics) return;

      float contentX = GuiLayout.contentX();
      float contentY = GuiLayout.contentY();
      float contentW = GuiLayout.contentWidth();
      float contentH = GuiLayout.clipHeight() - 3.0F;

      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int)(20.4F * mainAlpha));
      int backGroundThreeColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(10.2F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(255.0F * mainAlpha));
      int mainColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(102.0F * mainAlpha));
      int mainColor6 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int)(15.3F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int)(255.0F * mainAlpha));
      int textTwoColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(120.0F * mainAlpha));
      int textTwoColor40 = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextTwoColor(1, 1), (int)(60.0F * mainAlpha));

      List<CosmeticEntry> entries = getEntries();
      float tabY = contentY + CONTENT_PAD;
      float bodyTop = tabY + TAB_HEIGHT + 4.0F;
      float bodyH = contentH - (TAB_HEIGHT + 4.0F) - 4.0F;
      float bodyBottom = bodyTop + bodyH;

      renderer2D.pushRoundedClipRect(contentX, contentY, contentW, contentH, 0.0F, 0.0F, 0.0F, 0.0F);

      renderTabs(renderer2D, contentX, tabY, contentW, mainAlpha, mainColor, mainColor40, mainColor6, outlineColor, textColor, textTwoColor);
      renderGrid(renderer2D, entries, contentX, bodyTop, contentW, bodyH, bodyBottom, mouseX, mouseY, mainAlpha, mainColor, mainColor40, mainColor6, outlineColor, textColor, textTwoColor, textTwoColor40);
      renderFooter(renderer2D, entries, contentX, bodyBottom - FOOTER_HEIGHT + 2.0F, contentW, mainAlpha, mainColor, mainColor6, mainColor40, outlineColor, textColor, textTwoColor40);

      renderer2D.popClipRect();
   }

   private static void renderTabs(Renderer2D r, float x, float y, float w, float mainAlpha,
         int mainColor, int mainColor40, int mainColor6, int outlineColor, int textColor, int textTwoColor) {
      float cursorX = x + CONTENT_PAD;
      for (int i = 0; i < tabKinds.length; i++) {
         CosmeticEntry.Kind kind = tabKinds[i];
         String label = kind.tabName();
         float labelW = r.measureText(FontRegistry.INTER_MEDIUM, label, 11.0F).width;
         float tabW = labelW + 12.0F;
         boolean active = i == GuiScreen.cosmeticTab;
         boolean hovered = GuiRenderMain.isHovered(
               GuiScreen.currentMouseX, GuiScreen.currentMouseY,
               cursorX, y, tabW, TAB_HEIGHT);

         int bgColor = active ? mainColor6 : (hovered ? mainColor6 : 0);
         int borderColor = active ? mainColor40 : outlineColor;
         int labelColor = active ? mainColor : textTwoColor;

         r.rectOutline(cursorX, y, tabW, TAB_HEIGHT, TAB_RADIUS, borderColor, 0.1F);
         r.rect(cursorX, y, tabW, TAB_HEIGHT, TAB_RADIUS, bgColor);
         r.text(FontRegistry.INTER_MEDIUM, cursorX + 6.0F, y + 3.5F, 11.0F, label, ColorUtil.replAlpha(labelColor, (int)(255.0F * mainAlpha)));

         cursorX += tabW + TAB_GAP;
      }
   }

   private static void renderGrid(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float h,
         float bottom, int mouseX, int mouseY, float mainAlpha,
         int mainColor, int mainColor40, int mainColor6, int outlineColor, int textColor, int textTwoColor, int textTwoColor40) {

      GuiScreen.cosmeticHoverIndex = -1;
      if (entries.isEmpty()) {
         renderEmptyState(r, x, y, w, h, mainAlpha, mainColor, textTwoColor40, outlineColor);
         return;
      }

      float cardW = (w - CONTENT_PAD * 2.0F - CARD_GAP) / COLS;
      float cardH = 44.0F;
      float totalRows = (float) Math.ceil((double) entries.size() / COLS);
      float totalH = totalRows * cardH + Math.max(0, totalRows - 1) * CARD_GAP;
      GuiScreen.cosmeticMaxScroll = Math.max(0.0F, totalH - h);
      GuiScreen.cosmeticScroll = MathHelper.clamp(GuiScreen.cosmeticScroll, 0.0F, GuiScreen.cosmeticMaxScroll);

      r.pushRoundedClipRect(x, y, w, h, 0.0F, 0.0F, 0.0F, 0.0F);

      for (int i = 0; i < entries.size(); i++) {
         int col = i % COLS;
         int row = i / COLS;
         float cardX = x + CONTENT_PAD + col * (cardW + CARD_GAP);
         float cardY = y + row * (cardH + CARD_GAP) - GuiScreen.cosmeticScroll;

         if (cardY + cardH < y - 2.0F || cardY > bottom + 2.0F) continue;

         CosmeticEntry entry = entries.get(i);
         boolean selected = i == GuiScreen.cosmeticSelectedIndex;
         boolean applied = FiguraBridge.isApplied(entry);
         boolean hovered = GuiRenderMain.isHovered(mouseX, mouseY, cardX, cardY, cardW, cardH);
         if (hovered) GuiScreen.cosmeticHoverIndex = i;

         int cardBg = hovered ? mainColor6 : Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(60.0F * mainAlpha));
         int cardBorder = applied ? mainColor40 : (selected ? mainColor6 : outlineColor);

         r.rectOutline(cardX, cardY, cardW, cardH, CARD_RADIUS, cardBorder, 0.1F);
         r.rect(cardX, cardY, cardW, cardH, CARD_RADIUS, cardBg);

         if (applied) {
            float dotSize = 5.0F;
            r.rect(cardX + cardW - dotSize - 4.0F, cardY + 4.0F, dotSize, dotSize, dotSize / 2.0F, mainColor40);
            r.rect(cardX + cardW - dotSize - 4.0F + 1.25F, cardY + 4.0F + 1.25F, 2.5F, 2.5F, 1.25F, mainColor);
         }

         String kindIcon = entry.kind() == CosmeticEntry.Kind.MODEL ? "M" : (entry.kind() == CosmeticEntry.Kind.HEAD ? "H" : "W");
         r.rect(cardX + 5.0F, cardY + 5.0F, 16.0F, 16.0F, 4.0F, mainColor6);
         r.text(FontRegistry.ICONS, cardX + 5.0F + 4.0F, cardY + 5.0F + 3.0F, 10.0F, kindIcon, mainColor40);

         String displayName = truncate(r, entry.displayName(), cardW - 30.0F, 11.0F);
         r.text(FontRegistry.INTER_MEDIUM, cardX + 24.0F, cardY + 6.0F, 11.0F, displayName, ColorUtil.replAlpha(textColor, (int)(255.0F * mainAlpha)));
         r.text(FontRegistry.INTER_MEDIUM, cardX + 24.0F, cardY + 20.0F, 9.0F, kindIcon.equals("M") ? "Model" : (kindIcon.equals("H") ? "Head" : "Weapon"), textTwoColor40);
      }

      if (GuiScreen.cosmeticMaxScroll > 0.0F && h > 20.0F) {
         float scrollBarH = Math.max(12.0F, h * (h / totalH));
         float scrollBarY = y + (h - scrollBarH) * (GuiScreen.cosmeticScroll / GuiScreen.cosmeticMaxScroll);
         r.rect(x + w - 2.5F, scrollBarY, 1.5F, scrollBarH, 0.75F, mainColor6);
      }

      r.popClipRect();
   }

   private static void renderEmptyState(Renderer2D r, float x, float y, float w, float h, float mainAlpha,
         int mainColor, int textTwoColor40, int outlineColor) {
      float centerX = x + w / 2.0F;
      float centerY = y + h / 2.0F;
      boolean folderExists = false;
      try {
         folderExists = java.nio.file.Files.isDirectory(CosmeticsRepository.directory());
      } catch (Exception ignored) {}

      String title = folderExists ? "No avatars in this tab" : "Cosmetics folder not found";
      String subtitle = "Drop Figura avatars into fluxvisuals/cosmetics/";

      r.text(FontRegistry.INTER_MEDIUM, centerX - r.measureText(FontRegistry.INTER_MEDIUM, title, 12.0F).width / 2.0F,
            centerY - 8.0F, 12.0F, title, textTwoColor40);
      r.text(FontRegistry.INTER_MEDIUM, centerX - r.measureText(FontRegistry.INTER_MEDIUM, subtitle, 9.0F).width / 2.0F,
            centerY + 8.0F, 9.0F, subtitle, Renderer2D.ColorUtil.replAlpha(textTwoColor40, (int)(120.0F * mainAlpha)));
   }

   private static void renderFooter(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float mainAlpha,
         int mainColor, int mainColor6, int mainColor40, int outlineColor, int textColor, int textTwoColor40) {
      float btnW = 60.0F;
      float btnH = 16.0F;
      float btnGap = 4.0F;

      CosmeticEntry selected = getSelectedEntry();
      boolean hasSelection = selected != null;
      boolean isApplied = hasSelection && FiguraBridge.isApplied(selected);

      float equipX = x + w - CONTENT_PAD - btnW;
      boolean equipHovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, equipX, y, btnW, btnH);
      int equipBg = isApplied ? Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getBackGroundColor(1, 1), (int)(140.0F * mainAlpha))
            : (equipHovered ? mainColor : Renderer2D.ColorUtil.replAlpha(mainColor, (int)(220.0F * mainAlpha)));
      int equipBorder = isApplied ? outlineColor : mainColor40;
      r.rectOutline(equipX, y, btnW, btnH, 3.0F, equipBorder, 0.1F);
      r.rect(equipX, y, btnW, btnH, 3.0F, equipBg);
      String equipLabel = isApplied ? "Unequip" : "Equip";
      float equipLabelW = r.measureText(FontRegistry.INTER_MEDIUM, equipLabel, 10.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, equipX + (btnW - equipLabelW) / 2.0F, y + 3.0F, 10.0F, equipLabel, textColor);

      float refreshX = equipX - btnGap - btnW;
      boolean refreshHovered = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, refreshX, y, btnW, btnH);
      r.rectOutline(refreshX, y, btnW, btnH, 3.0F, outlineColor, 0.1F);
      r.rect(refreshX, y, btnW, btnH, 3.0F, refreshHovered ? mainColor6 : 0);
      String refreshLabel = "Refresh";
      float refreshLabelW = r.measureText(FontRegistry.INTER_MEDIUM, refreshLabel, 10.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, refreshX + (btnW - refreshLabelW) / 2.0F, y + 3.0F, 10.0F, refreshLabel, textColor);

      if (hasSelection) {
         float selX = x + CONTENT_PAD;
         String selName = selected.displayName();
         String truncated = truncate(r, selName, w - btnW * 2.0F - btnGap * 2.0F - 10.0F, 10.0F);
         r.text(FontRegistry.INTER_MEDIUM, selX, y + 3.0F, 10.0F, truncated, ColorUtil.replAlpha(mainColor40, (int)(255.0F * mainAlpha)));
      } else {
         String hint = FiguraBridge.isAvailable() ? "Select an avatar to equip" : "Figura not installed";
         r.text(FontRegistry.INTER_MEDIUM, x + CONTENT_PAD, y + 3.0F, 10.0F, hint,
               Renderer2D.ColorUtil.replAlpha(textTwoColor40, (int)(150.0F * mainAlpha)));
      }

      if (!GuiScreen.cosmeticStatus.isEmpty() && System.currentTimeMillis() < GuiScreen.cosmeticStatusUntil) {
         long remaining = GuiScreen.cosmeticStatusUntil - System.currentTimeMillis();
         float alpha = Math.min(1.0F, remaining / 500.0F);
         int statusColor = GuiScreen.cosmeticStatusOk
               ? Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getColor(150, 214, 168), (int)(255.0F * alpha * mainAlpha))
               : Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getColor(228, 138, 138), (int)(255.0F * alpha * mainAlpha));
         r.text(FontRegistry.INTER_MEDIUM, x + CONTENT_PAD, y - 10.0F, 9.0F, GuiScreen.cosmeticStatus, statusColor);
      }
   }

   private static List<CosmeticEntry> getEntries() {
      CosmeticEntry.Kind[] kinds = tabKinds;
      if (GuiScreen.cosmeticTab < 0 || GuiScreen.cosmeticTab >= kinds.length) return List.of();
      return CosmeticsRepository.of(kinds[GuiScreen.cosmeticTab]);
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
      return text.substring(0, end).stripTrailing() + "...";
   }

   public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) return false;
      if (GuiScreen.selectedCategories != Category.Cosmetics) return false;

      float contentX = GuiLayout.contentX();
      float contentY = GuiLayout.contentY();
      float contentW = GuiLayout.contentWidth();
      float contentH = GuiLayout.clipHeight() - 3.0F;

      if (!GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, contentX, contentY, contentW, contentH)) {
         return false;
      }

      float tabY = contentY + CONTENT_PAD;
      float cursorX = contentX + CONTENT_PAD;
      for (int i = 0; i < tabKinds.length; i++) {
         String label = tabKinds[i].tabName();
         float tabW = 12.0F + 12.0F;
         if (GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, cursorX, tabY, tabW, TAB_HEIGHT)) {
            if (GuiScreen.cosmeticTab != i) {
               GuiScreen.cosmeticTab = i;
               GuiScreen.cosmeticScroll = 0.0F;
               GuiScreen.cosmeticSelectedIndex = -1;
            }
            return true;
         }
         cursorX += tabW + TAB_GAP;
      }

      float bodyTop = tabY + TAB_HEIGHT + 4.0F;
      float bodyH = contentH - (TAB_HEIGHT + 4.0F) - 4.0F;
      float bodyBottom = bodyTop + bodyH;

      float footerY = bodyBottom - FOOTER_HEIGHT + 2.0F;
      float btnW = 60.0F;
      float btnH = 16.0F;
      float btnGap = 4.0F;
      float equipX = contentX + contentW - CONTENT_PAD - btnW;
      if (GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, equipX, footerY, btnW, btnH)) {
         handleEquip();
         return true;
      }
      float refreshX = equipX - btnGap - btnW;
      if (GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, refreshX, footerY, btnW, btnH)) {
         CosmeticsRepository.rescan();
         GuiScreen.cosmeticScroll = 0.0F;
         GuiScreen.cosmeticSelectedIndex = -1;
         return true;
      }

      if (GuiScreen.cosmeticHoverIndex >= 0) {
         GuiScreen.cosmeticSelectedIndex = GuiScreen.cosmeticHoverIndex;
         return true;
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
