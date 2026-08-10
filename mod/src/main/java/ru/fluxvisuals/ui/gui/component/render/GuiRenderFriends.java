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
 * Вкладка ClickGUI «Друзья»: строка поиска по нику, список игроков на сервере
 * с кнопкой «+» (добавить в друзья) и список друзей с кнопкой «-» (удалить).
 */
@Environment(EnvType.CLIENT)
public class GuiRenderFriends extends GuiScreen {
   public static final float PAD = 4.0F;

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
      float inputH = 24.0F;
      float inputY = GuiLayout.clipY() + 2.0F;

      List<String> online = filteredOnline();
      List<Friend> friends = filteredFriends();
      // "Добавить по нику", если в поиске ник не из списка онлайн.
      boolean showManual = !GuiScreen.friendsSearchText.trim().isEmpty()
            && online.stream().noneMatch(n -> n.equalsIgnoreCase(GuiScreen.friendsSearchText.trim()));

      float headerH = 18.0F;
      float rowH = 20.0F;
      float gapR = 3.0F;
      int onlineRows = online.size() + (showManual ? 1 : 0);
      int friendRows = friends.size();
      float listY0 = inputY + inputH + 6.0F;
      int totalRows = (onlineRows > 0 ? 1 + onlineRows : 0) + (friendRows > 0 ? 1 + friendRows : 0);
      GuiScreen.getScrollUtil().setMax(totalRows * (rowH + gapR) + headerH, GuiLayout.clipHeight() - (inputH + 12.0F));
      float scroll = GuiScreen.getScrollUtil().getScroll();

      // ---- Строка поиска ----
      boolean editing = GuiScreen.friendsSearchEditing;
      r2.rect(x, inputY, w, inputH, 6.0F, editing ? backHover : backThree);
      r2.rectOutline(x, inputY, w, inputH, 6.0F, outlineColor, editing ? 0.35F : 0.2F);
      r2.text(FontRegistry.ICONS, x + 9.0F, inputY + inputH / 2.0F + 0.8F, 14.0F, "h", mutedText);
      String shown = GuiScreen.friendsSearchText.isEmpty() ? "поиск по нику…" : GuiScreen.friendsSearchText;
      r2.text(FontRegistry.INTER_MEDIUM, x + 26.0F, inputY + inputH / 2.0F + 0.8F, 13.0F, shown,
            GuiScreen.friendsSearchText.isEmpty() ? mutedText : textColor);

      float rowIndex = 0.0F;

      // ---- Список онлайн ----
      if (onlineRows > 0) {
         float hy = listY0 + rowIndex * (rowH + gapR) - scroll;
         if (hy >= GuiLayout.clipY() - rowH && hy <= GuiLayout.clipY() + GuiLayout.clipHeight()) {
            r2.text(FontRegistry.INTER_MEDIUM, x + 2.0F, hy + headerH / 2.0F + 0.6F, 11.0F, "На сервере", mutedText);
         }
         rowIndex++;
         if (showManual) {
            float y = listY0 + rowIndex * (rowH + gapR) - scroll;
            rowIndex++;
            if (y >= GuiLayout.clipY() - rowH && y <= GuiLayout.clipY() + GuiLayout.clipHeight()) {
               boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, rowH);
               r2.rect(x, y, w, rowH, 5.0F, hover ? backHover : backThree);
               r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + rowH / 2.0F + 0.8F, 13.0F, GuiScreen.friendsSearchText.trim(), textColor);
               drawSmallButton(r2, x + w - 22.0F, y + (rowH - 14.0F) / 2.0F, mainColor, "+");
            }
         }
         for (String name : online) {
            float y = listY0 + rowIndex * (rowH + gapR) - scroll;
            rowIndex++;
            if (y < GuiLayout.clipY() - rowH || y > GuiLayout.clipY() + GuiLayout.clipHeight()) {
               continue;
            }
            boolean isFriend = FriendManager.isFriend(name);
            boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, rowH);
            r2.rect(x, y, w, rowH, 5.0F, hover ? backHover : backThree);
            if (isFriend) {
               r2.rect(x, y, 2.0F, rowH, 1.0F, mainColor);
            }
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + rowH / 2.0F + 0.8F, 13.0F, name, isFriend ? mainColor : textColor);
            if (!isFriend) {
               drawSmallButton(r2, x + w - 22.0F, y + (rowH - 14.0F) / 2.0F, mainColor, "+");
            } else {
               r2.text(FontRegistry.ICONS, x + w - 22.0F + 7.0F, y + rowH / 2.0F + 0.8F, 10.0F, "e", mainColor);
            }
         }
      }

      // ---- Список друзей ----
      if (friendRows > 0) {
         float hy = listY0 + rowIndex * (rowH + gapR) - scroll;
         rowIndex++;
         if (hy >= GuiLayout.clipY() - rowH && hy <= GuiLayout.clipY() + GuiLayout.clipHeight()) {
            r2.text(FontRegistry.INTER_MEDIUM, x + 2.0F, hy + headerH / 2.0F + 0.6F, 11.0F, "Друзья (" + FriendManager.friends.size() + ")", mutedText);
         }
         for (Friend f : friends) {
            float y = listY0 + rowIndex * (rowH + gapR) - scroll;
            rowIndex++;
            if (y < GuiLayout.clipY() - rowH || y > GuiLayout.clipY() + GuiLayout.clipHeight()) {
               continue;
            }
            boolean hover = GuiRenderMain.isHovered(mouseX, mouseY, x, y, w, rowH);
            r2.rect(x, y, w, rowH, 5.0F, hover ? backHover : backThree);
            r2.rect(x, y, 2.0F, rowH, 1.0F, mainColor);
            r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, y + rowH / 2.0F + 0.8F, 13.0F, f.getName(), mainColor);
            drawSmallButton(r2, x + w - 22.0F, y + (rowH - 14.0F) / 2.0F, dangerColor, "-");
         }
      }

      if (onlineRows == 0 && friendRows == 0) {
         r2.text(FontRegistry.INTER_MEDIUM, x + 8.0F, listY0 + 6.0F, 12.0F,
               onlinePlayers().isEmpty() ? "Нет подключения к серверу" : "Никто не найден", mutedText);
      }
   }

   /** Маленькая круглая кнопка («+»/«-») справа в строке. */
   private static void drawSmallButton(Renderer2D r2, float bx, float by, int color, String symbol) {
      r2.rect(bx, by, 14.0F, 14.0F, 7.0F, Renderer2D.ColorUtil.replAlpha(color, 70));
      r2.text(FontRegistry.INTER_MEDIUM, bx + 5.4F, by + 7.6F, 12.0F, symbol, Renderer2D.ColorUtil.replAlpha(color, 255));
   }

   /** Хит-область кнопки «+»/«-» в строке списка. */
   public static boolean isSmallButton(float rowX, float rowY, float rowW, int mouseX, int mouseY) {
      return GuiRenderMain.isHovered(mouseX, mouseY, rowX + rowW - 22.0F, rowY + (20.0F - 14.0F) / 2.0F, 14.0F, 14.0F);
   }
}
