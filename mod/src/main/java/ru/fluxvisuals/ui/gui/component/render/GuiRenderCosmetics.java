package ru.fluxvisuals.ui.gui.component.render;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
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
   private static final CosmeticEntry.Kind[] TAB_KINDS = CosmeticEntry.Kind.values();
   private static final float PAD = 4.0F;
   private static final float TAB_H = 20.0F;
   private static final float TAB_GAP = 4.0F;
   private static final float CARD_GAP = 4.0F;
   private static final float CARD_RADIUS = 4.0F;
   private static final int COLS = 2;
   private static final float THUMB_PAD = 4.0F;
   private static final float CARD_H = 72.0F;
   private static final float NAME_H = 14.0F;

   private static float cachedTabsX;
   private static float[] cachedTabW = new float[0];

   public static void render(Renderer2D r, MatrixStack pose, int mouseX, int mouseY, float alpha) {
      if (GuiScreen.selectedCategories != Category.Cosmetics) return;

      float areaX = GuiLayout.clipX();
      float areaY = GuiLayout.clipY();
      float areaW = GuiLayout.clipWidth();
      float areaH = GuiLayout.clipHeight();

      int outline = rc(Renderer2D.ColorUtil.getOutLineColor(1, 1), 20, alpha);
      int main    = rc(Renderer2D.ColorUtil.getMainColor(1, 1), 255, alpha);
      int main40  = rc(Renderer2D.ColorUtil.getMainColor(1, 1), 102, alpha);
      int main6   = rc(Renderer2D.ColorUtil.getMainColor(1, 1), 15, alpha);
      int main16  = rc(Renderer2D.ColorUtil.getMainColor(1, 1), 41, alpha);
      int txt     = rc(Renderer2D.ColorUtil.getTextColor(1, 1), 255, alpha);
      int txt2    = rc(Renderer2D.ColorUtil.getTextTwoColor(1, 1), 120, alpha);
      int txt240  = rc(Renderer2D.ColorUtil.getTextTwoColor(1, 1), 60, alpha);
      int bgDim   = rc(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 100, alpha);

      List<CosmeticEntry> entries = getEntries();
      float tabY = areaY + PAD;
      float gridY = tabY + TAB_H + 3.0F;
      float gridH = areaH - TAB_H - PAD - 3.0F;
      float gridBottom = gridY + gridH;

      r.pushRoundedClipRect(areaX, areaY, areaW, areaH, 0, 0, 0, 0);

      renderTabs(r, entries, areaX, tabY, areaW, alpha, main, main40, main6, main16, outline, txt, txt2, txt240);
      renderGrid(r, entries, areaX, gridY, areaW, gridH, gridBottom, alpha, main, main40, main6, main16, outline, bgDim, txt, txt240);

      if (!GuiScreen.cosmeticStatus.isEmpty() && System.currentTimeMillis() < GuiScreen.cosmeticStatusUntil) {
         float rem = (float) (GuiScreen.cosmeticStatusUntil - System.currentTimeMillis());
         float sa = Math.min(1.0F, rem / 500.0F);
         int sc = GuiScreen.cosmeticStatusOk
               ? rc(Renderer2D.ColorUtil.getColor(150, 214, 168), 255, alpha * sa)
               : rc(Renderer2D.ColorUtil.getColor(228, 138, 138), 255, alpha * sa);
         float sw = r.measureText(FontRegistry.INTER_MEDIUM, GuiScreen.cosmeticStatus, 8.0F).width;
         r.text(FontRegistry.INTER_MEDIUM, areaX + (areaW - sw) / 2.0F, gridBottom - 10.0F, 8.0F, GuiScreen.cosmeticStatus, sc);
      }

      r.popClipRect();
   }

   private static void renderTabs(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float alpha,
         int main, int main40, int main6, int main16, int outline, int txt, int txt2, int txt240) {

      float total = 0;
      cachedTabW = new float[TAB_KINDS.length];
      for (int i = 0; i < TAB_KINDS.length; i++) {
         float lw = r.measureText(FontRegistry.INTER_MEDIUM, TAB_KINDS[i].tabName(), 10.0F).width;
         cachedTabW[i] = lw + 14.0F;
         total += cachedTabW[i];
      }
      total += (TAB_KINDS.length - 1) * TAB_GAP;
      cachedTabsX = x + (w - total) / 2.0F;
      float cx = cachedTabsX;

      for (int i = 0; i < TAB_KINDS.length; i++) {
         float tw = cachedTabW[i];
         boolean active = i == GuiScreen.cosmeticTab;
         boolean hov = GuiRenderMain.isHovered(GuiScreen.currentMouseX, GuiScreen.currentMouseY, cx, y, tw, TAB_H);

         int bg = active ? main16 : (hov ? main6 : 0);
         int border = active ? main40 : outline;
         int col = active ? main : txt2;

         r.rectOutline(cx, y, tw, TAB_H, 4.0F, border, 0.1F);
         r.rect(cx, y, tw, TAB_H, 4.0F, bg);
         float lw = r.measureText(FontRegistry.INTER_MEDIUM, TAB_KINDS[i].tabName(), 10.0F).width;
         r.text(FontRegistry.INTER_MEDIUM, cx + (tw - lw) / 2.0F, y + TAB_H / 2.0F + 0.8F, 10.0F, TAB_KINDS[i].tabName(), rc(col, 255, alpha));

         cx += tw + TAB_GAP;
      }
   }

   private static void renderGrid(Renderer2D r, List<CosmeticEntry> entries, float x, float y, float w, float h,
         float bottom, float alpha, int main, int main40, int main6, int main16, int outline, int bgDim, int txt, int txt240) {

      GuiScreen.cosmeticHoverIndex = -1;
      if (entries.isEmpty()) {
         renderEmpty(r, x, y, w, h, alpha, main, txt240);
         return;
      }

      float cardW = (w - PAD * 2.0F - CARD_GAP) / COLS;
      float thumbH = CARD_H - NAME_H - THUMB_PAD * 2.0F;
      float thumbW = cardW - THUMB_PAD * 2.0F;
      float rows = (float) Math.ceil((double) entries.size() / COLS);
      float totalH = rows * CARD_H + Math.max(0, rows - 1) * CARD_GAP;
      GuiScreen.cosmeticMaxScroll = Math.max(0.0F, totalH - h);
      GuiScreen.cosmeticScroll = MathHelper.clamp(GuiScreen.cosmeticScroll, 0.0F, GuiScreen.cosmeticMaxScroll);

      r.pushRoundedClipRect(x, y, w, h, 0, 0, 0, 0);

      for (int i = 0; i < entries.size(); i++) {
         int col = i % COLS;
         int row = i / COLS;
         float cardX = x + PAD + col * (cardW + CARD_GAP);
         float cardY = y + row * (CARD_H + CARD_GAP) - GuiScreen.cosmeticScroll;
         if (cardY + CARD_H < y - 2.0F || cardY > bottom + 2.0F) continue;

         CosmeticEntry entry = entries.get(i);
         boolean applied = FiguraBridge.isApplied(entry);
         boolean hov = GuiRenderMain.isHovered((float)GuiScreen.currentMouseX, (float)GuiScreen.currentMouseY, cardX, cardY, cardW, CARD_H);
         if (hov) GuiScreen.cosmeticHoverIndex = i;

         int cardBg = rc(Renderer2D.ColorUtil.getBackGroundColor(1, 1), 70, alpha);
         int cardBorder = applied ? main40 : outline;
         r.rectOutline(cardX, cardY, cardW, CARD_H, CARD_RADIUS, cardBorder, 0.1F);
         r.rect(cardX, cardY, cardW, CARD_H, CARD_RADIUS, cardBg);

         int glId = CosmeticsRepository.loadPreview(entry);
         float px = cardX + THUMB_PAD;
         float py = cardY + THUMB_PAD;
         if (glId >= 0 && entry.previewWidth() > 0 && entry.previewHeight() > 0) {
            r.rect(px, py, thumbW, thumbH, 3.0F, bgDim);
            float sx = thumbW / (float) entry.previewWidth();
            float sy = thumbH / (float) entry.previewHeight();
            float s = Math.min(sx, sy);
            float dw = entry.previewWidth() * s;
            float dh = entry.previewHeight() * s;
            r.drawRgbaTexture(glId, px + (thumbW - dw) / 2.0F, py + (thumbH - dh) / 2.0F, dw, dh, -1, false);
         } else {
            r.rect(px, py, thumbW, thumbH, 3.0F, main6);
            String icon = entry.kind() == CosmeticEntry.Kind.MODEL ? "M" : (entry.kind() == CosmeticEntry.Kind.HEAD ? "H" : "W");
            float il = r.measureText(FontRegistry.ICONS, icon, 14.0F).width;
            r.text(FontRegistry.ICONS, px + (thumbW - il) / 2.0F, py + (thumbH - 14.0F) / 2.0F + 3.0F, 14.0F, icon, main16);
         }

         if (applied) {
            float bs = 10.0F;
            r.rect(cardX + cardW - THUMB_PAD - bs, cardY + THUMB_PAD, bs, bs, bs / 2.0F, main40);
            String chk = "V";
            float cl = r.measureText(FontRegistry.ICONS, chk, 6.0F).width;
            r.text(FontRegistry.ICONS, cardX + cardW - THUMB_PAD - bs + (bs - cl) / 2.0F, cardY + THUMB_PAD + 1.5F, 6.0F, chk, main);
         }

         String name = truncate(r, entry.displayName(), cardW - 6.0F, 9.0F);
          float nw = r.measureText(FontRegistry.INTER_MEDIUM, name, 9.0F).width;
          r.text(FontRegistry.INTER_MEDIUM, cardX + (cardW - nw) / 2.0F, cardY + CARD_H - NAME_H / 2.0F + 0.8F, 9.0F, name,
                rc(applied ? main : txt, 255, alpha));
      }

      if (GuiScreen.cosmeticMaxScroll > 0.0F && h > 20.0F) {
         float sbH = Math.max(10.0F, h * (h / totalH));
         float sbY = y + (h - sbH) * (GuiScreen.cosmeticScroll / GuiScreen.cosmeticMaxScroll);
         r.rect(x + w - 2.0F, sbY, 1.5F, sbH, 0.75F, main6);
      }

      r.popClipRect();
   }

   private static void renderEmpty(Renderer2D r, float x, float y, float w, float h, float alpha, int main, int txt240) {
      float cx = x + w / 2.0F;
      float cy = y + h / 2.0F;
      boolean exists = false;
      try { exists = java.nio.file.Files.isDirectory(CosmeticsRepository.directory()); } catch (Exception ignored) {}
      String t = exists ? "No avatars in this tab" : "Cosmetics folder not found";
      float tw = r.measureText(FontRegistry.INTER_MEDIUM, t, 9.0F).width;
      r.text(FontRegistry.INTER_MEDIUM, cx - tw / 2.0F, cy - 4.0F, 9.0F, t, txt240);
   }

   private static List<CosmeticEntry> getEntries() {
      if (GuiScreen.cosmeticTab < 0 || GuiScreen.cosmeticTab >= TAB_KINDS.length) return List.of();
      return CosmeticsRepository.of(TAB_KINDS[GuiScreen.cosmeticTab]);
   }

   private static String truncate(Renderer2D r, String text, float max, float size) {
      if (text == null) return "";
      if (r.measureText(FontRegistry.INTER_MEDIUM, text, size).width <= max) return text;
      int end = text.length();
      while (end > 0 && r.measureText(FontRegistry.INTER_MEDIUM, text.substring(0, end) + "...", size).width > max) end--;
      return end <= 0 ? text.substring(0, 1) + "..." : text.substring(0, end).stripTrailing() + "...";
   }

   private static int rc(int color, int alphaMultiplier, float alpha) {
      return ColorUtil.replAlpha(color, (int)(alphaMultiplier * alpha));
   }

   public static boolean handleMouseClicked(double mouseX, double mouseY, int button) {
      if (button != 0) return false;
      if (GuiScreen.selectedCategories != Category.Cosmetics) return false;

      float areaX = GuiLayout.clipX();
      float areaY = GuiLayout.clipY();
      float areaW = GuiLayout.clipWidth();
      float areaH = GuiLayout.clipHeight();

      if (!GuiRenderMain.isHovered((float) mouseX, (float) mouseY, areaX, areaY, areaW, areaH)) return false;

      float tabY = areaY + PAD;
      float cx = cachedTabsX;
      for (int i = 0; i < TAB_KINDS.length; i++) {
         float tw = i < cachedTabW.length ? cachedTabW[i] : 50.0F;
         if (GuiRenderMain.isHovered((float) mouseX, (float) mouseY, cx, tabY, tw, TAB_H)) {
            if (GuiScreen.cosmeticTab != i) {
               GuiScreen.cosmeticTab = i;
               GuiScreen.cosmeticScroll = 0.0F;
               GuiScreen.cosmeticSelectedIndex = -1;
            }
            return true;
         }
         cx += tw + TAB_GAP;
      }

      float gridY = tabY + TAB_H + 3.0F;
      float gridH = areaH - TAB_H - PAD - 3.0F;
      List<CosmeticEntry> entries = getEntries();
      float cardW = (areaW - PAD * 2.0F - CARD_GAP) / COLS;

      for (int i = 0; i < entries.size(); i++) {
         int col = i % COLS;
         int row = i / COLS;
         float cardX = areaX + PAD + col * (cardW + CARD_GAP);
         float cardY = gridY + row * (CARD_H + CARD_GAP) - GuiScreen.cosmeticScroll;
         if (cardY + CARD_H < gridY - 2.0F || cardY > gridY + gridH + 2.0F) continue;
         if (GuiRenderMain.isHovered((float) mouseX, (float) mouseY, cardX, cardY, cardW, CARD_H)) {
            CosmeticEntry entry = entries.get(i);
            GuiScreen.cosmeticSelectedIndex = i;
            toggle(entry);
            return true;
         }
      }

      return true;
   }

   public static boolean handleMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (GuiScreen.selectedCategories != Category.Cosmetics) return false;
      float areaX = GuiLayout.clipX();
      float areaY = GuiLayout.clipY();
      float areaW = GuiLayout.clipWidth();
      float areaH = GuiLayout.clipHeight();
      if (!GuiRenderMain.isHovered((float) mouseX, (float) mouseY, areaX, areaY, areaW, areaH)) return false;
      GuiScreen.cosmeticScroll -= (float) verticalAmount * 18.0F;
      GuiScreen.cosmeticScroll = MathHelper.clamp(GuiScreen.cosmeticScroll, 0.0F, GuiScreen.cosmeticMaxScroll);
      return true;
   }

   private static void toggle(CosmeticEntry entry) {
      if (entry == null) return;
      if (FiguraBridge.isApplied(entry)) {
         boolean ok = FiguraBridge.clear();
         setStatus(ok ? entry.displayName() + " removed" : "Figura is unavailable", ok);
      } else {
         boolean ok = FiguraBridge.apply(entry);
         setStatus(ok ? entry.displayName() + " equipped" : "Figura is unavailable", ok);
      }
   }

   private static void setStatus(String message, boolean ok) {
      GuiScreen.cosmeticStatus = message == null ? "" : message;
      GuiScreen.cosmeticStatusOk = ok;
      GuiScreen.cosmeticStatusUntil = System.currentTimeMillis() + 2600L;
   }
}
