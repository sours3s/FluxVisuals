package ru.fluxvisuals.ui.gui.component.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.Friend;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.util.render.core.Renderer2D;
import ru.fluxvisuals.util.render.text.FontRegistry;

/**
 * Вкладка ClickGUI «Друзья» с двумя под-вкладками: «Друзья» (сохранённые) и «Сервер» (онлайн).
 * Единый расчёт rowY — клики и рендер всегда совпадают.
 */
@Environment(EnvType.CLIENT)
public class GuiRenderFriends extends GuiScreen {
   public static final float PAD = 4.0F;
   public static final float TAB_H = 22.0F;
   public static final float TAB_GAP = 4.0F;
   public static final float INPUT_H = 24.0F;
   public static final float ROW_H = 20.0F;
   public static final float ROW_GAP = 3.0F;

   private GuiRenderFriends() {
   }

   /** Список онлайн-игроков (имена), исключая самого игрока. */
   public static List<String> onlinePlayers() {
      List<String> names = new ArrayList<>();
      MinecraftClient mc = MinecraftClient.getInstance();
      if (mc.getNetworkHandler() == null || mc.player == null) {
         return names;
      }
      String self = mc.player.getName().getString();
      Collection<PlayerListEntry> entries = mc.getNetworkHandler().getPlayerList();
      if (entries == null) {
         return names;
      }
      for (PlayerListEntry entry : entries) {
         if (entry.getProfile() == null || entry.getProfile().name() == null) {
            continue;
         }
         String name = entry.getProfile().name();
         if (!name.equalsIgnoreCase(self)) {
            names.add(name);
         }
      }
      return names;
   }

   /** Онлайн-игроки, отфильтрованные по строке поиска. */
   public static List<String> filteredOnline() {
      String q = GuiScreen.friendsSearchText.trim().toLowerCase(Locale.ROOT);
      List<String> all = onlinePlayers();
      if (q.isEmpty()) {
         return all;
      }
      List<String> out = new ArrayList<>();
      for (String n : all) {
         if (n.toLowerCase(Locale.ROOT).contains(q)) {
            out.add(n);
         }
      }
      return out;
   }

   /** Игроки из списка друзей, отфильтрованные по строке поиска. */
   public static List<Friend> filteredFriends() {
      String q = GuiScreen.friendsSearchText.trim().toLowerCase(Locale.ROOT);
      List<Friend> out = new ArrayList<>();
      for (Friend f : FriendManager.friends) {
         if (q.isEmpty() || f.getName().toLowerCase(Locale.ROOT).contains(q)) {
            out.add(f);
         }
      }
      return out;
   }

   public static float rowX() {
      return GuiLayout.clipX() + PAD;
   }

   public static float rowW() {
      return GuiLayout.clipWidth() - 8.0F;
   }

   public static float tabY() {
      return GuiLayout.clipY() + 2.0F;
   }

   public static float inputY() {
      return tabY() + TAB_H + 4.0F;
   }

   public static float listY0() {
      return inputY() + INPUT_H + 6.0F;
   }

   /** Единый расчёт Y строки по индексу — обязателен и в рендере, и в кликах. */
   public static float rowY(int index, float scroll) {
      return listY0() + index * (ROW_H + ROW_GAP) - scroll;
   }

   public static void render(Renderer2D r2, float mainAlpha, int mouseX, int mouseY) {
      if (GuiScreen.selectedCategories != Category.Friends) {
         return;
      }
      int outlineColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getOutLineColor(1, 1), (int) (20.4F * mainAlpha));
      int backThree = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (10.2F * mainAlpha));
      int backHover = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (18.0F * mainAlpha));
      int textColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int) (255.0F * mainAlpha));
      int mutedText = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getTextColor(1, 1), (int) (110.0F * mainAlpha));
      int mainColor = Renderer2D.ColorUtil.replAlpha(Renderer2D.ColorUtil.getMainColor(1, 1), (int) (255.0F * mainAlpha));
      int dangerColor = Renderer2D.ColorUtil.replAlpha(0xFFFF5353, (int) (255.0F * mainAlpha));

      GuiScreen.getScrollUtil().setEnabled(true);
      GuiScreen.getScrollUtil().setSpeed(6.0F);

      float x = rowX();
      float w = rowW();

      // ---- Вкладки ----
      float tabW = (w - TAB_GAP) / 2.0F;
      String[] tabs = new String[]{"Друзья", "Сервер"};
      for (int i = 0; i < 2; i++) {
         float tx = x + i * (tabW + TAB_GAP);
         boolean active = GuiScreen.friendsTab == i;
         boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, tx, tabY(), tabW, TAB_H);
         r2.rect(tx, tabY(), tabW, TAB_H, 6.0F, active ? backHover : backThree);
         r2.rectOutline(tx, tabY(), tabW, TAB_H, 6.0F, outlineColor, active ? 0.35F : 0.2F);
         float tw = r2.measureText(FontRegistry.INTER_MEDIUM, tabs[i], 12.0F).width;
         r2.text(FontRegistry.INTER_MEDIUM, tx + tabW / 2.0F - tw / 2.0F, tabY() + TAB_H / 2.0F + 0.8F, 12.0F, tabs[i],
               active ? mainColor : textColor);
      }

      // ---- Строка поиска ----
      boolean editing = GuiScreen.friendsSearchEditing;
      r2.rect(x, inputY(), w, INPUT_H, 6.0F, editing ? backHover : backThree);
      r2.rectOutline(x, inputY(), w, INPUT_H, 6.0F, outlineColor, editing ? 0.35F : 0.2F);
      r2.text(FontRegistry.ICONS, x + 9.0F, inputY() + INPUT_H / 2.0F + 0.8F, 14.0F, "h", mutedText);
      String shown = GuiScreen.friendsSearchText.isEmpty() ? "поиск по нику…" : GuiScreen.friendsSearchText;
      r2.text(FontRegistry.INTER_MEDIUM, x + 26.0F, inputY() + INPUT_H / 2.0F + 0.8F, 13.0F, shown,
            GuiScreen.friendsSearchText.isEmpty() ? mutedText : textColor);
      r2.text(FontRegistry.INTER_MEDIUM, x + w - 20.0F, inputY() + INPUT_H / 2.0F + 0.8F, 13.0F, editing ? "|" : "", mainColor);

      // ---- Список ----
      float scroll = GuiScreen.getScrollUtil().getScroll();
      float viewTop = GuiLayout.clipY() - ROW_H;
      float viewBottom = GuiLayout.clipY() + GuiLayout.clipHeight();
      float listH = GuiLayout.clipHeight() - (listY0() - GuiLayout.clipY());

      if (GuiScreen.friendsTab == 1) {
         List<String> online = filteredOnline();
         boolean showManual = !GuiScreen.friendsSearchText.trim().isEmpty()
               && online.stream().noneMatch(n -> n.equalsIgnoreCase(GuiScreen.friendsSearchText.trim()));
         int rows = online.size() + (showManual ? 1 : 0);
         GuiScreen.getScrollUtil().setMax(rows * (ROW_H + ROW_GAP) + 6.0F, listH);

         if (rows == 0) {
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, listY0() + 6.0F, 12.0F,
                  onlinePlayers().isEmpty() ? "Нет подключения к серверу" : "Никто не найден", mutedText);
         }

         int idx = 0;
         if (showManual) {
            float y = rowY(idx, scroll);
            idx++;
            if (y >= viewTop && y <= viewBottom) {
               boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, ROW_H);
               r2.rect(x, y, w, ROW_H, 5.0F, hover ? backHover : backThree);
               r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + ROW_H / 2.0F + 0.8F, 13.0F, GuiScreen.friendsSearchText.trim(), textColor);
               drawSmallButton(r2, x + w - 22.0F, y + (ROW_H - 14.0F) / 2.0F, mainColor, "+");
            }
         }
         for (String name : online) {
            float y = rowY(idx, scroll);
            idx++;
            if (y < viewTop || y > viewBottom) {
               continue;
            }
            boolean isFriend = FriendManager.isFriend(name);
            boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, ROW_H);
            r2.rect(x, y, w, ROW_H, 5.0F, hover ? backHover : backThree);
            if (isFriend) {
               r2.rect(x, y, 2.0F, ROW_H, 1.0F, mainColor);
            }
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + ROW_H / 2.0F + 0.8F, 13.0F, name, isFriend ? mainColor : textColor);
            if (!isFriend) {
               drawSmallButton(r2, x + w - 22.0F, y + (ROW_H - 14.0F) / 2.0F, mainColor, "+");
            } else {
               r2.text(FontRegistry.ICONS, x + w - 22.0F + 7.0F, y + ROW_H / 2.0F + 0.8F, 10.0F, "e", mainColor);
            }
         }
      } else {
         List<Friend> friends = filteredFriends();
         int rows = friends.size();
         GuiScreen.getScrollUtil().setMax(rows * (ROW_H + ROW_GAP) + 6.0F, listH);

         if (rows == 0) {
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, listY0() + 6.0F, 12.0F,
                  "Друзей пока нет — добавь через вкладку «Сервер»", mutedText);
         }

         int idx = 0;
         for (Friend f : friends) {
            float y = rowY(idx, scroll);
            idx++;
            if (y < viewTop || y > viewBottom) {
               continue;
            }
            boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, ROW_H);
            r2.rect(x, y, w, ROW_H, 5.0F, hover ? backHover : backThree);
            r2.rect(x, y, 2.0F, ROW_H, 1.0F, mainColor);
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + ROW_H / 2.0F + 0.8F, 13.0F, f.getName(), mainColor);
            drawSmallButton(r2, x + w - 22.0F, y + (ROW_H - 14.0F) / 2.0F, dangerColor, "-");
         }
      }
   }

   /** Маленькая круглая кнопка («+»/«-») справа в строке, символ по центру. */
   private static void drawSmallButton(Renderer2D r2, float bx, float by, int color, String symbol) {
      r2.rect(bx, by, 14.0F, 14.0F, 7.0F, Renderer2D.ColorUtil.replAlpha(color, 70));
      float sw = r2.measureText(FontRegistry.INTER_MEDIUM, symbol, 12.0F).width;
      r2.text(FontRegistry.INTER_MEDIUM, bx + 7.0F - sw / 2.0F, by + 7.0F - 3.2F, 12.0F, symbol,
            Renderer2D.ColorUtil.replAlpha(color, 255));
   }

   /** Хит-область кнопки «+»/«-» в строке списка. */
   public static boolean isSmallButton(float rowX, float rowY, float rowW, int mouseX, int mouseY) {
      return GuiRenderMain.isHovered(mouseX, mouseY, rowX + rowW - 22.0F, rowY + (ROW_H - 14.0F) / 2.0F, 14.0F, 14.0F);
   }
}
