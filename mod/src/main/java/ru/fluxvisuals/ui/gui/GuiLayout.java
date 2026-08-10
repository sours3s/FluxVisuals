package ru.fluxvisuals.ui.gui;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * Single source of truth for the client GUI geometry.
 *
 * Every render pass and click handler derives its coordinates from here instead of
 * hardcoding magic numbers, so the menu stays adaptive: changing {@link #WIDTH},
 * {@link #HEIGHT} or {@link #SIDEBAR} reflows the whole layout consistently and the
 * module grid / hit-testing never drift out of sync.
 *
 * All getters read {@link GuiScreen#x}/{@code y}/{@code width}/{@code height} live, so
 * they are valid both while rendering and while handling input.
 */
@Environment(EnvType.CLIENT)
public final class GuiLayout {
   // ---- Tunable base size (the panel is centered by GuiClient.init) ----
   public static final float WIDTH = 325.0F;
   public static final float HEIGHT = 288.0F;
   public static final float SIDEBAR = 78.0F;

   // ---- Fixed structural metrics ----
   public static final float TOP_BAR = 33.7F;
   public static final float ROUND = 6.5F;

   // Module grid spacing
   private static final float MODULE_LEFT_PAD = 6.0F;   // gap from sidebar edge to first column
   private static final float MODULE_RIGHT_PAD = 5.0F;  // gap from panel edge to last column
   private static final float MODULE_COLUMN_GAP = 4.5F; // gap between the two columns
   public static final float MODULE_HEIGHT = 21.325F;
   public static final float ROW_STEP = 30.325F;
   // Vertical offset of the right column relative to the left, so the two columns interlock
   // (masonry/brick stagger) instead of lining up as rigid rows. Small, to limit wasted top/bottom space.
   public static final float COLUMN_STAGGER = 8.0F;

   private GuiLayout() {
   }

   public static float x() {
      return GuiScreen.x;
   }

   public static float y() {
      return GuiScreen.y;
   }

   public static float w() {
      return GuiScreen.width;
   }

   public static float h() {
      return GuiScreen.height;
   }

   // ---- Content (module list) region ----
   public static float contentX() {
      return x() + SIDEBAR + 0.395F;
   }

   public static float contentY() {
      return y() + TOP_BAR + 0.325F;
   }

   public static float contentWidth() {
      return w() - SIDEBAR - 0.635F;
   }

   public static float clipX() {
      return contentX() + 5.0F;
   }

   public static float clipY() {
      return contentY() + 5.0F;
   }

   public static float clipWidth() {
      return contentWidth() - 10.0F;
   }

   public static float clipHeight() {
      return h() - 39.305F;
   }

   public static float contentRectHeight() {
      return h() - 29.305F;
   }

   public static float scrollHeight() {
      return h() - 44.305F;
   }

   // ---- Module columns ----
   public static float column1X() {
      return x() + SIDEBAR + MODULE_LEFT_PAD;
   }

   public static float moduleWidth() {
      float span = (w() - MODULE_RIGHT_PAD) - (SIDEBAR + MODULE_LEFT_PAD);
      return (span - MODULE_COLUMN_GAP) / 2.0F;
   }

   public static float column2X() {
      return column1X() + moduleWidth() + MODULE_COLUMN_GAP;
   }

   /** X of a module box for the given 1-based render index (odd = col1, even = col2). */
   public static float moduleColumnX(int index) {
      return index % 2 == 0 ? column2X() : column1X();
   }

   public static float moduleNameX(float moduleX) {
      return moduleX + 9.54F;
   }

   public static float moduleToggleX(float moduleX) {
      return moduleX + moduleWidth() - 11.45F;
   }

   public static float moduleSettingsIconX(float moduleX) {
      return moduleX + moduleWidth() - 21.86F;
   }

   public static float settingX(float moduleX) {
      return moduleX + 9.0F;
   }

   public static float settingWidth() {
      return moduleWidth() - 16.0F;
   }

   // ---- Scrollbar ----
   public static float scrollbarX() {
      return contentX() + contentWidth() - 4.0F;
   }

   // ---- Top bar elements ----
   public static float searchX() {
      return x() + SIDEBAR + 7.545F;
   }

   public static float searchY() {
      return y() + 6.185F;
   }

   public static float searchHeight() {
      return 21.325F;
   }

   public static float topButtonSize() {
      return 21.325F;
   }

   /** Right-most top-bar button (client settings) — kept for popup positioning. */
   public static float settingsButtonX() {
      return x() + w() - 6.595F - topButtonSize();
   }

   /** Second top-bar button (web) — kept for layout consistency. */
   public static float webButtonX() {
      return settingsButtonX() - 5.0F - topButtonSize();
   }

   public static float searchWidth() {
      // Top-bar buttons were removed, so the search field stretches to the right edge of the panel.
      return (x() + w() - 6.595F) - searchX();
   }

   // ---- Client settings popup (anchored under the settings button) ----
   public static float popupWidth() {
      return 100.0F;
   }

   public static float popupHeight() {
      return 60.0F;
   }

   public static float popupX() {
      return settingsButtonX() + topButtonSize() - popupWidth();
   }

   public static float popupY() {
      return y() + TOP_BAR + 3.0F;
   }

   // ---- Left sidebar ----
   public static float categoryY(float downY) {
      return y() + 43.365F + downY;
   }

   public static float CATEGORY_HEIGHT = 19.5F;
   public static float CATEGORY_STEP = 22.0F;

   public static float profileY() {
      return y() + h() - 27.085F;
   }
}
