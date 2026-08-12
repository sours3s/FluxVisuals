package ru.fluxvisuals.ui.gui.component.mouse;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import ru.fluxvisuals.client.FluxVisualsClient;
import ru.fluxvisuals.config.friend.Friend;
import ru.fluxvisuals.config.friend.FriendManager;
import ru.fluxvisuals.module.api.Category;
import ru.fluxvisuals.module.impl.visuals.Hud;
import ru.fluxvisuals.ui.gui.GuiLayout;
import ru.fluxvisuals.ui.gui.GuiScreen;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderFriends;
import ru.fluxvisuals.ui.gui.component.render.GuiRenderMain;

/**
 * Клики вкладки «Друзья»: переключение под-вкладок, поиск, добавление/удаление.
 * Индексы строк совпадают с рендером (GuiRenderFriends.rowY).
 */
@Environment(EnvType.CLIENT)
public class GuiMouseClickedFriends extends GuiScreen {
   private GuiMouseClickedFriends() {
   }

   public static boolean mouseClickedFriends(int mouseX, int mouseY, int button) {
      if (button != 0) {
         return false;
      }
      if (GuiScreen.selectedCategories == null || GuiScreen.selectedCategories != Category.Friends) {
         return false;
      }

      float x = GuiRenderFriends.rowX();
      float w = GuiRenderFriends.rowW();
      float scroll = GuiScreen.getScrollUtil().getScroll();

      // ---- Переключение вкладок ----
      float tabW = (w - GuiRenderFriends.TAB_GAP) / 2.0F;
      for (int i = 0; i < 2; i++) {
         float tx = x + i * (tabW + GuiRenderFriends.TAB_GAP);
         if (GuiRenderMain.isHovered(mouseX, mouseY, tx, GuiRenderFriends.tabY(), tabW, GuiRenderFriends.TAB_H)) {
            if (GuiScreen.friendsTab != i) {
               GuiScreen.friendsTab = i;
               GuiScreen.friendsSearchText = "";
               GuiScreen.friendsSearchEditing = false;
            }
            return true;
         }
      }

      // ---- Фокус поля поиска ----
      if (GuiRenderMain.isHovered(mouseX, mouseY, x, GuiRenderFriends.inputY(), w, GuiRenderFriends.INPUT_H)) {
         GuiScreen.friendsSearchEditing = true;
         return true;
      }

      float listY0 = GuiRenderFriends.listY0();

      if (GuiScreen.friendsTab == 1) {
         // ---- Вкладка «Сервер»: добавить по нику / онлайн-игроки ----
         List<String> online = GuiRenderFriends.filteredOnline();
         boolean showManual = !GuiScreen.friendsSearchText.trim().isEmpty()
               && online.stream().noneMatch(n -> n.equalsIgnoreCase(GuiScreen.friendsSearchText.trim()));

         int idx = 0;
         if (showManual) {
            float y = GuiRenderFriends.rowY(idx, scroll);
            idx++;
            if (GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
               addFriend(GuiScreen.friendsSearchText.trim());
               return true;
            }
         }
         for (String name : online) {
            float y = GuiRenderFriends.rowY(idx, scroll);
            idx++;
            if (!FriendManager.isFriend(name) && GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
               addFriend(name);
               return true;
            }
         }
      } else {
         // ---- Вкладка «Друзья»: удаление ----
         List<Friend> friends = GuiRenderFriends.filteredFriends();
         int idx = 0;
         for (Friend f : friends) {
            float y = GuiRenderFriends.rowY(idx, scroll);
            idx++;
            if (GuiRenderFriends.isSmallButton(x, y, w, mouseX, mouseY)) {
               removeFriend(f.getName());
               return true;
            }
         }
      }

      return false;
   }

   /** Публичный вход для добавления по нику (Enter в поле поиска, команды). */
   public static void addFriendDirect(String name) {
      addFriend(name);
   }

   private static void addFriend(String name) {
      if (name == null || name.isEmpty()) {
         return;
      }
      if (FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
         return;
      }
      if (GuiScreen.mc.player != null && name.equalsIgnoreCase(GuiScreen.mc.player.getName().getString())) {
         return; // нельзя добавить себя
      }
      FluxVisualsClient.get.friendManager.add(name);
      FluxVisualsClient.get.manager.get(Hud.class).showNotification("+", "Друг " + name + " добавлен", 3000L, 0xFF00FF88);
   }

   private static void removeFriend(String name) {
      if (name == null || name.isEmpty() || FluxVisualsClient.get == null || FluxVisualsClient.get.friendManager == null) {
         return;
      }
      FluxVisualsClient.get.friendManager.remove(name);
      FluxVisualsClient.get.manager.get(Hud.class).showNotification("-", "Друг " + name + " удалён", 3000L, 0xFFFF5353);
   }
}
